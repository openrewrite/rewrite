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
	"testing"
	"unsafe"
)

func TestDecodeBatch_DecodesTypedFields(t *testing.T) {
	// given
	data := []byte(`[
		{"state":"ADD","valueType":"org.openrewrite.marker.SearchResult","value":{"id":"x"},"ref":7},
		{"state":"NO_CHANGE"},
		{"state":"CHANGE","value":" "}
	]`)

	// when
	batch, err := DecodeBatch(data, nil)

	// then
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(batch) != 3 {
		t.Fatalf("expected 3 messages, got %d", len(batch))
	}
	if batch[0].State != Add || batch[0].ValueType == nil || *batch[0].ValueType != "org.openrewrite.marker.SearchResult" || batch[0].Ref == nil || *batch[0].Ref != 7 {
		t.Fatalf("message 0 decoded wrong: %+v", batch[0])
	}
	if m, ok := batch[0].Value.(map[string]any); !ok || m["id"] != "x" {
		t.Fatalf("message 0 value not a decoded map: %#v", batch[0].Value)
	}
	if batch[1].State != NoChange || batch[1].Value != nil {
		t.Fatalf("message 1 decoded wrong: %+v", batch[1])
	}
	if batch[2].State != Change || batch[2].Value != " " {
		t.Fatalf("message 2 decoded wrong: %+v", batch[2])
	}
}

func TestDecodeBatch_InternsDuplicateStrings(t *testing.T) {
	// given
	data := []byte(`[
		{"state":"CHANGE","value":"\n\t"},
		{"state":"CHANGE","value":"\n\t"},
		{"state":"ADD","valueType":"m","value":{"a":"\n\t","b":"\n\t"}}
	]`)
	intern := make(map[string]string)

	// when
	batch, err := DecodeBatch(data, intern)

	// then
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	s0 := batch[0].Value.(string)
	s1 := batch[1].Value.(string)
	nested := batch[2].Value.(map[string]any)
	sa := nested["a"].(string)
	sb := nested["b"].(string)
	if s0 != "\n\t" || s1 != "\n\t" || sa != "\n\t" || sb != "\n\t" {
		t.Fatalf("interning corrupted values: %q %q %q %q", s0, s1, sa, sb)
	}
	for _, s := range []string{s1, sa, sb} {
		if strData(s0) != strData(s) {
			t.Fatalf("duplicate string not interned to shared backing")
		}
	}
}

func TestDecodeBatch_NilInternKeepsDistinctBacking(t *testing.T) {
	// given
	data := []byte(`[{"state":"CHANGE","value":"dup"},{"state":"CHANGE","value":"dup"}]`)

	// when
	batch, err := DecodeBatch(data, nil)

	// then
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if batch[0].Value.(string) != "dup" || batch[1].Value.(string) != "dup" {
		t.Fatalf("values decoded wrong without interning")
	}
}

func strData(s string) unsafe.Pointer {
	return unsafe.Pointer(unsafe.StringData(s))
}
