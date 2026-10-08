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
	"math/rand"
	"strings"
	"testing"
)

// Escapes and the number grammar used to be encoding/json's to enforce. These hold the reader to
// what encoding/json accepted and to what it produced.

func TestDecodeBatchReadsEveryEscapeEncodingJSONWrites(t *testing.T) {
	runes := []rune{'"', '\\', '/', '\b', '\f', '\n', '\r', '\t', 0x00, 0x1f, 0x7f, 'a', 'é', '中', 0x1F600}
	rng := rand.New(rand.NewSource(1))
	for n := 0; n < 5000; n++ {
		var sb strings.Builder
		for k := rng.Intn(12); k >= 0; k-- {
			sb.WriteRune(runes[rng.Intn(len(runes))])
		}
		want := sb.String()
		encoded, err := json.Marshal(want)
		if err != nil {
			t.Fatalf("marshal %q: %v", want, err)
		}
		batch, err := DecodeBatch([]byte(`[{"state":"CHANGE","value":`+string(encoded)+`}]`), nil)
		if err != nil {
			t.Fatalf("%s: %v", encoded, err)
		}
		if got := batch[0].Value; got != want {
			t.Fatalf("%s decoded to %q, want %q", encoded, got, want)
		}
	}
}

func TestDecodeBatchReadsSurrogatePairsAndLoneSurrogates(t *testing.T) {
	for _, tc := range []struct{ literal, want string }{
		{`"😀"`, "\U0001F600"},
		{`"\uD83D"`, "�"},
		{`"\uDE00"`, "�"},
		{`"\uD83DA"`, "�A"},
		{`"\u0000"`, "\x00"},
		{`"\/"`, "/"},
	} {
		batch, err := DecodeBatch([]byte(`[{"state":"CHANGE","value":`+tc.literal+`}]`), nil)
		if err != nil {
			t.Errorf("%s: %v", tc.literal, err)
			continue
		}
		if got := batch[0].Value; got != tc.want {
			t.Errorf("%s decoded to %q, want %q", tc.literal, got, tc.want)
		}
	}
}

func TestDecodeBatchRejectsWhatIsNotJSON(t *testing.T) {
	for _, literal := range []string{
		"01", "+1", ".5", "1.", "1e", "-", "--1", "1x", "NaN", "0x1", "1_000",
		`"\x41"`, `"\a"`, `"\u00"`, `"\u 041"`, "tru",
	} {
		if batch, err := DecodeBatch([]byte(`[{"state":"CHANGE","value":`+literal+`}]`), nil); err == nil {
			t.Errorf("%s was read as %#v", literal, batch[0].Value)
		}
	}
}

// A page cut short mid-transfer is an error, not a batch of however many messages arrived.
func TestDecodeBatchRejectsATruncatedPage(t *testing.T) {
	for _, truncated := range []string{
		`[{"state":"CHANGE"}`,
		`[{"state":"CHANGE"`,
		`[{"state":"CHANGE","value":`,
		`[`,
	} {
		if batch, err := DecodeBatch([]byte(truncated), nil); err == nil {
			t.Errorf("%s was read as %#v", truncated, batch)
		}
	}
}

// Binding into the struct dropped a valueType or ref whose JSON type did not match the field.
func TestDecodeBatchIgnoresAMistypedValueTypeOrRef(t *testing.T) {
	for _, message := range []string{
		`{"state":"CHANGE","valueType":null}`,
		`{"state":"CHANGE","valueType":7}`,
		`{"state":"CHANGE","valueType":[]}`,
		`{"state":"CHANGE","ref":null}`,
		`{"state":"CHANGE","ref":"7"}`,
		`{"state":"CHANGE","ref":true}`,
	} {
		batch, err := DecodeBatch([]byte("["+message+"]"), nil)
		if err != nil {
			t.Errorf("%s: %v", message, err)
			continue
		}
		if batch[0].ValueType != nil || batch[0].Ref != nil {
			t.Errorf("%s set valueType=%v ref=%v", message, batch[0].ValueType, batch[0].Ref)
		}
	}
}

func TestDecodeBatchSkipsMembersItDoesNotKnow(t *testing.T) {
	data := []byte(`[{"unknown":{"nested":[1,2,{"deep":"\n"}]},"state":"CHANGE","after":[],"value":1,"last":null}]`)
	batch, err := DecodeBatch(data, nil)
	if err != nil {
		t.Fatal(err)
	}
	if len(batch) != 1 || batch[0].State != Change || batch[0].Value != int64(1) {
		t.Fatalf("read %#v", batch)
	}
}
