/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rpc

import (
	"encoding/json"
	"os"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// loadBatchOfMaps converts a JSON array of message-maps into one RpcObjectData batch.
func loadBatchOfMaps(t *testing.T, raw []any) []RpcObjectData {
	t.Helper()
	batch := make([]RpcObjectData, 0, len(raw))
	for _, item := range raw {
		m, ok := item.(map[string]any)
		if !ok {
			t.Fatalf("batch item is not a map: %T", item)
		}
		batch = append(batch, ParseObjectData(m))
	}
	return batch
}

// TestPrintWireCollapsesUnchangedSubtree replays the EXACT wire captured from a
// real moderne-cli run of RenameXToFlag on:
//
//	func Bind(x int) int {
//	    value := x
//	    if value == 1 {       // <- untouched by the recipe
//	        value = 2
//	    }
//	    fmt.Println(value, x)
//	    return value
//	}
//
// `print_collapse_baseline.json` is Go's reconstruction baseline at print time
// (verified PERFECT in the capture: baselineCollapsed=false).
// `print_collapse_wire.json` is the GetObject delta Java's GolangSender sent to
// Go to print the edited tree.
//
// Applying that delta to the perfect baseline via GoReceiver reproduces the
// end-to-end bug: the untouched `if` block collapses to `if value == 1{value=2}`.
func TestPrintWireCollapsesUnchangedSubtree(t *testing.T) {
	// --- reconstruct the baseline (full ADD wire) ---
	var baselineRaw []any
	b, err := os.ReadFile("testdata/print_collapse_baseline.json")
	if err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal(b, &baselineRaw); err != nil {
		t.Fatal(err)
	}

	refs := make(map[int]any)
	delivered := false
	baselineBatch := loadBatchOfMaps(t, baselineRaw)
	bq := NewReceiveQueue(refs, func() []RpcObjectData {
		if delivered {
			return nil
		}
		delivered = true
		return baselineBatch
	})
	brcv := NewGoReceiver()
	baseline := bq.Receive(nil, func(v any) any {
		if t, ok := v.(java.Tree); ok {
			return brcv.Visit(t, bq)
		}
		return v
	})
	baseCU, ok := baseline.(*golang.CompilationUnit)
	if !ok {
		t.Fatalf("baseline did not reconstruct to a CompilationUnit: %T", baseline)
	}
	baseSrc := printer.Print(baseCU)
	if strings.Contains(baseSrc, "1{") {
		t.Fatalf("precondition failed: baseline is already collapsed:\n%s", baseSrc)
	}
	if !strings.Contains(baseSrc, "if value == 1 {") {
		t.Fatalf("precondition failed: baseline does not contain the expected if block:\n%s", baseSrc)
	}

	// --- replay Java's print delta against the perfect baseline ---
	var wireBatches []any
	w, err := os.ReadFile("testdata/print_collapse_wire.json")
	if err != nil {
		t.Fatal(err)
	}
	if err := json.Unmarshal(w, &wireBatches); err != nil {
		t.Fatal(err)
	}
	bi := 0
	pq := NewReceiveQueue(refs, func() []RpcObjectData {
		if bi >= len(wireBatches) {
			return nil
		}
		raw := wireBatches[bi].([]any)
		bi++
		return loadBatchOfMaps(t, raw)
	})
	prcv := NewGoReceiver()
	result := pq.Receive(baseCU, func(v any) any {
		if t, ok := v.(java.Tree); ok {
			return prcv.Visit(t, pq)
		}
		return v
	})
	resCU, ok := result.(*golang.CompilationUnit)
	if !ok {
		t.Fatalf("result did not reconstruct to a CompilationUnit: %T", result)
	}
	resSrc := printer.Print(resCU)

	// The bug: the unchanged if block loses ALL its whitespace.
	if !strings.Contains(resSrc, "if value == 1 {\n\t\tvalue = 2\n\t}") {
		t.Errorf("REPRODUCED: print wire collapsed the unchanged if block.\n--- baseline (perfect) ---\n%s\n--- result (collapsed) ---\n%s", baseSrc, resSrc)
	}
}
