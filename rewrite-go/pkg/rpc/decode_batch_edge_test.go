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

import "testing"

func TestDecodeBatch_EmptyArray(t *testing.T) {
	// given
	data := []byte(`[]`)

	// when
	batch, err := DecodeBatch(data, nil)

	// then
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(batch) != 0 {
		t.Fatalf("expected empty batch, got %d", len(batch))
	}
}

func TestDecodeBatch_NullPayload(t *testing.T) {
	// given
	data := []byte(`null`)

	// when
	batch, err := DecodeBatch(data, nil)

	// then
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(batch) != 0 {
		t.Fatalf("expected empty batch for null, got %d", len(batch))
	}
}

func TestDecodeBatch_NonArrayIsError(t *testing.T) {
	// given
	data := []byte(`{"state":"ADD"}`)

	// when
	_, err := DecodeBatch(data, nil)

	// then
	if err == nil {
		t.Fatalf("expected error for non-array payload")
	}
}

func TestDecodeBatch_StreamsManyMessages(t *testing.T) {
	// given
	data := benchBatchJSON(500)

	// when
	batch, err := DecodeBatch(data, make(map[string]string))

	// then
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(batch) != 500 {
		t.Fatalf("expected 500 messages, got %d", len(batch))
	}
	if positions, ok := batch[2].Value.([]any); !ok || len(positions) != 5 {
		t.Fatalf("message 2 positions not decoded as a list: %#v", batch[2].Value)
	}
	if m, ok := batch[3].Value.(map[string]any); !ok || m["desc"] != "\n    " {
		t.Fatalf("message 3 marker map not decoded: %#v", batch[3].Value)
	}
}
