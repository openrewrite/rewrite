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
	"math"
	"math/big"
	"math/rand"
	"strings"
	"testing"
)

func encoded(t *testing.T, batch []RpcObjectData) string {
	t.Helper()
	got, err := AppendBatch(nil, batch)
	if err != nil {
		t.Fatalf("AppendBatch: %v", err)
	}
	return string(got)
}

// Agreement includes refusal: a batch json.Marshal rejects must not be written out anyway.
func requireSameAsMarshal(t *testing.T, name string, batch []RpcObjectData) {
	t.Helper()
	want, wantErr := json.Marshal(batch)
	got, gotErr := AppendBatch(nil, batch)
	switch {
	case wantErr != nil && gotErr == nil:
		t.Errorf("%s: json.Marshal refused it (%v), AppendBatch wrote %s", name, wantErr, got)
	case wantErr == nil && gotErr != nil:
		t.Errorf("%s: json.Marshal wrote %s, AppendBatch refused it (%v)", name, want, gotErr)
	case wantErr == nil && string(got) != string(want):
		t.Errorf("%s:\n json.Marshal: %s\n  AppendBatch: %s", name, want, got)
	}
}

// Every value shape the send queue can put on the wire, and the awkward ones it should not but
// would be silently mis-encoded if it did.
func TestAppendBatchMatchesMarshal(t *testing.T) {
	valueType := "org.openrewrite.java.tree.J$Identifier"
	ref := 7
	negative := -1
	zero := 0
	empty := ""

	values := []any{
		nil,
		"", "A", "someIdentifier", "\n    ",
		"quote\"backslash\\slash/",
		"\b\f\n\r\t", "\x00\x01\x1f", "\x7f",
		"<script>&</script>", "\u2028\u2029",
		"é中\U0001F600", string([]byte{0xff, 0xfe}), "a\xffb",
		strings.Repeat("x", 300),
		true, false,
		0, 1, -1, 255, 256, math.MaxInt64, math.MinInt64,
		int64(0), int64(-9223372036854775808),
		json.Number("0"), json.Number("3.0"), json.Number("-0.5"), json.Number("1e3"),
		json.Number("1E+2"), json.Number("1.7976931348623157e308"),
		json.Number("not a number"),
		[]any{}, []any{0, -1, 2}, []any{"a", nil, true, json.Number("1.0")},
		[]any{[]any{}, map[string]any{}},
		map[string]any{},
		// A nil map or slice is null, not empty delimiters. The type switch matches a nil value
		// of the type, so these only differ from the empty cases above if that is handled.
		[]any(nil), map[string]any(nil),
		map[string]any{"b": 1, "a": "x"},
		map[string]any{"z": nil, "m": []any{1, 2}, "a": map[string]any{"deep": "\n"}},
		map[string]any{"<&>": "\u2028", "": ""},
		big.NewInt(0), new(big.Int).SetUint64(math.MaxUint64),
		3.5, math.Pi, 0.0,
	}

	for _, state := range []State{NoChange, Add, Delete, Change, EndOfObject, State(99)} {
		for _, value := range values {
			requireSameAsMarshal(t, "state and value", []RpcObjectData{{State: state, Value: value}})
		}
	}

	for _, vt := range []*string{nil, &valueType, &empty} {
		for _, r := range []*int{nil, &ref, &negative, &zero} {
			requireSameAsMarshal(t, "valueType and ref", []RpcObjectData{
				{State: Add, ValueType: vt, Value: "v", Ref: r},
			})
		}
	}

	requireSameAsMarshal(t, "empty batch", []RpcObjectData{})
	requireSameAsMarshal(t, "nil batch", nil)
	requireSameAsMarshal(t, "a page", benchBatch(200))
}

// Random strings are where an escaping table is actually tested: the single-byte cases above miss
// a boundary between a multi-byte rune and an escape.
func TestAppendBatchMatchesMarshalOnRandomStrings(t *testing.T) {
	runes := []rune{'"', '\\', '/', '<', '>', '&', '\b', '\f', '\n', '\r', '\t', 0x00, 0x1f,
		0x7f, 'a', 'é', '中', 0x1F600, '\u2028', '\u2029', 0xFFFD}
	rng := rand.New(rand.NewSource(7))
	for n := 0; n < 20000; n++ {
		var sb strings.Builder
		for k := rng.Intn(10); k >= 0; k-- {
			sb.WriteRune(runes[rng.Intn(len(runes))])
		}
		if rng.Intn(4) == 0 {
			sb.WriteByte(byte(rng.Intn(256))) // a lone byte, valid UTF-8 or not
		}
		text := sb.String()
		batch := []RpcObjectData{{State: Change, ValueType: &text, Value: text}}
		want, err := json.Marshal(batch)
		if err != nil {
			t.Fatalf("json.Marshal %q: %v", text, err)
		}
		if got := encoded(t, batch); got != string(want) {
			t.Fatalf("%q:\n json.Marshal: %s\n  AppendBatch: %s", text, want, got)
		}
	}
}

func TestAppendBatchAppendsToWhatIsAlreadyThere(t *testing.T) {
	dst := []byte(`{"result":`)
	got, err := AppendBatch(dst, []RpcObjectData{{State: Delete}})
	if err != nil {
		t.Fatal(err)
	}
	if want := `{"result":[{"state":"DELETE"}]`; string(got) != want {
		t.Errorf("got %s, want %s", got, want)
	}
}
