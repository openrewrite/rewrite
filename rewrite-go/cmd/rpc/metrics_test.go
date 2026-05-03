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

package main

import (
	"encoding/csv"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"sync"
	"testing"
	"time"
)

// newTestServer wires a server pointed at a temp metrics CSV. Cleanup is
// registered with t.Cleanup. The server's stdin/stdout aren't used by the
// tests — they only invoke safeHandleRequest directly.
func newTestServer(t *testing.T) (*server, string) {
	t.Helper()
	dir := t.TempDir()
	csvPath := filepath.Join(dir, "metrics.csv")
	logPath := filepath.Join(dir, "server.log")
	s := newServer(serverConfig{
		logFile:    logPath,
		metricsCsv: csvPath,
	})
	t.Cleanup(s.closeMetrics)
	return s, csvPath
}

// readMetricsCSV parses the metrics file and returns its header and rows.
// Caller must close the writer (via closeMetrics) before reading so the
// flush completes.
func readMetricsCSV(t *testing.T, path string) (header []string, rows [][]string) {
	t.Helper()
	f, err := os.Open(path)
	if err != nil {
		t.Fatalf("open metrics csv: %v", err)
	}
	defer f.Close()
	r := csv.NewReader(f)
	all, err := r.ReadAll()
	if err != nil {
		t.Fatalf("parse metrics csv: %v", err)
	}
	if len(all) == 0 {
		t.Fatal("metrics csv is empty (no header)")
	}
	return all[0], all[1:]
}

func TestMetricsCSVHeaderWritten(t *testing.T) {
	s, csvPath := newTestServer(t)
	s.closeMetrics()

	header, rows := readMetricsCSV(t, csvPath)
	want := []string{"timestamp", "method", "duration_ms", "error"}
	if len(header) != len(want) {
		t.Fatalf("header columns: want %v, got %v", want, header)
	}
	for i, col := range want {
		if header[i] != col {
			t.Errorf("header[%d]: want %q, got %q", i, col, header[i])
		}
	}
	if len(rows) != 0 {
		t.Errorf("want no rows before any RPC, got %d", len(rows))
	}
}

func TestMetricsCSVRowPerRequest(t *testing.T) {
	s, csvPath := newTestServer(t)

	// Fire a few requests serially. GetLanguages has no params and is
	// always available; perfect for an isolation test.
	calls := []string{"GetLanguages", "GetLanguages", "GetLanguages"}
	for _, m := range calls {
		s.safeHandleRequest(&jsonRPCRequest{JSONRPC: "2.0", ID: json.RawMessage("1"), Method: m})
	}
	s.closeMetrics()

	_, rows := readMetricsCSV(t, csvPath)
	if len(rows) != len(calls) {
		t.Fatalf("rows: want %d, got %d (%v)", len(calls), len(rows), rows)
	}
	for i, row := range rows {
		if len(row) != 4 {
			t.Errorf("row[%d] columns: want 4, got %d (%v)", i, len(row), row)
			continue
		}
		if row[1] != calls[i] {
			t.Errorf("row[%d].method: want %q, got %q", i, calls[i], row[1])
		}
		if row[3] != "" {
			t.Errorf("row[%d].error: want empty, got %q", i, row[3])
		}
		// duration_ms is non-negative integer; not necessarily > 0 for
		// trivial methods on fast machines.
		if _, err := strconv.Atoi(row[2]); err != nil {
			t.Errorf("row[%d].duration_ms: not an int: %q", i, row[2])
		}
		if _, err := time.Parse(time.RFC3339Nano, row[0]); err != nil {
			t.Errorf("row[%d].timestamp: not RFC3339Nano: %q (%v)", i, row[0], err)
		}
	}
}

func TestMetricsCSVCapturesErrors(t *testing.T) {
	s, csvPath := newTestServer(t)
	s.safeHandleRequest(&jsonRPCRequest{JSONRPC: "2.0", ID: json.RawMessage("1"), Method: "BogusMethodThatDoesNotExist"})
	s.closeMetrics()

	_, rows := readMetricsCSV(t, csvPath)
	if len(rows) != 1 {
		t.Fatalf("rows: want 1, got %d", len(rows))
	}
	if rows[0][1] != "BogusMethodThatDoesNotExist" {
		t.Errorf("method: %q", rows[0][1])
	}
	if rows[0][3] == "" {
		t.Errorf("expected error column populated for unknown method, got empty")
	}
}

func TestMetricsCSVConcurrentLoad(t *testing.T) {
	// Concurrent RPC dispatch must serialize CSV writes; otherwise rows
	// interleave and the parser fails or row counts drift. This test
	// fires N parallel safeHandleRequest calls and asserts the file has
	// exactly N well-formed rows.
	s, csvPath := newTestServer(t)

	const goroutines = 16
	const perGoroutine = 50
	const total = goroutines * perGoroutine

	var wg sync.WaitGroup
	wg.Add(goroutines)
	for g := 0; g < goroutines; g++ {
		gid := g
		go func() {
			defer wg.Done()
			for i := 0; i < perGoroutine; i++ {
				s.safeHandleRequest(&jsonRPCRequest{
					JSONRPC: "2.0",
					ID:      json.RawMessage(strconv.Itoa(gid*perGoroutine + i)),
					Method:  "GetLanguages",
				})
			}
		}()
	}
	wg.Wait()
	s.closeMetrics()

	_, rows := readMetricsCSV(t, csvPath)
	if len(rows) != total {
		t.Fatalf("rows: want %d, got %d", total, len(rows))
	}
	// Every row must have 4 columns and a parseable timestamp+duration.
	// If writes interleaved, csv.NewReader.ReadAll above would fail or
	// produce malformed rows; we re-validate here in case ReadAll silently
	// padded.
	for i, row := range rows {
		if len(row) != 4 {
			t.Fatalf("row[%d] columns: want 4, got %d (%v)", i, len(row), row)
		}
		if row[1] != "GetLanguages" {
			t.Errorf("row[%d].method: want GetLanguages, got %q", i, row[1])
		}
		if _, err := time.Parse(time.RFC3339Nano, row[0]); err != nil {
			t.Errorf("row[%d].timestamp malformed: %q (%v)", i, row[0], err)
		}
		if _, err := strconv.Atoi(row[2]); err != nil {
			t.Errorf("row[%d].duration_ms not an int: %q", i, row[2])
		}
	}
	fmt.Printf("concurrent metrics test: wrote %d rows across %d goroutines\n", total, goroutines)
}

func TestMetricsCSVDisabledWhenFlagEmpty(t *testing.T) {
	dir := t.TempDir()
	logPath := filepath.Join(dir, "server.log")
	s := newServer(serverConfig{logFile: logPath})
	t.Cleanup(s.closeMetrics)

	// Should be a no-op; if writer-or-file leaks it would panic on close.
	s.safeHandleRequest(&jsonRPCRequest{JSONRPC: "2.0", ID: json.RawMessage("1"), Method: "GetLanguages"})
	if s.metricsWriter != nil || s.metricsFile != nil {
		t.Errorf("metrics writer should be nil when flag empty (writer=%v file=%v)", s.metricsWriter, s.metricsFile)
	}
}
