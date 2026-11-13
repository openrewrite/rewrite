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
package printer

import (
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"testing"
	"unicode"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// What these code points quote as holds in every Unicode version so far, so
// the expectations do not move with the toolchain running the test.
func TestQuote(t *testing.T) {
	quoted := map[string]string{
		`json:"a"`:             `"json:\"a\""`,
		`back\slash`:           `"back\\slash"`,
		"tab\tnewline\n":       `"tab\tnewline\n"`,
		"\a\b\f\r\v":           `"\a\b\f\r\v"`,
		"\x00\x1f\x7f":         `"\x00\x1f\x7f"`,
		"\u00e9 \u0436 \u8a9e": "\"\u00e9 \u0436 \u8a9e\"",
		"no-break\u00a0space":  `"no-break\u00a0space"`,
		"soft\u00adhyphen":     `"soft\u00adhyphen"`,
		"line\u2028separator":  `"line\u2028separator"`,
		"private\ue000use":     `"private\ue000use"`,
		"replacement\ufffd":    "\"replacement\ufffd\"",
		"\U0001f600":           "\"\U0001f600\"",
		"tag\U000e0001":        `"tag\U000e0001"`,
		"last\U0010ffff":       `"last\U0010ffff"`,
		"not utf-8 \xff\xc0":   `"not utf-8 \xff\xc0"`,
	}
	for s, want := range quoted {
		assert.Equal(t, want, quoteString(s))
		assert.Equal(t, strconv.Quote(s), quoteString(s))
	}
}

// The ranges are those of a Go release on one Unicode version, which a
// toolchain on that version can check in full.
func TestPrintableRangesAreStrconvsForTheirUnicodeVersion(t *testing.T) {
	if unicode.Version != printableUnicode {
		t.Skipf("this toolchain is on Unicode %s, the ranges on %s", unicode.Version, printableUnicode)
	}
	for r := rune(0); r <= unicode.MaxRune; r++ {
		if isPrint(r) != strconv.IsPrint(r) {
			t.Fatalf("%U: isPrint is %v", r, isPrint(r))
		}
	}
}

func TestJavaHoldsTheSameRanges(t *testing.T) {
	source, err := os.ReadFile(filepath.Join("..", "..", "src", "main", "java",
		"org", "openrewrite", "golang", "internal", "StrconvQuote.java"))
	require.NoError(t, err)
	table := regexp.MustCompile(`(?s)int\[\] PRINTABLE = \{(.*?)\};`).FindSubmatch(source)
	require.NotNil(t, table)
	assert.Contains(t, string(source), "Unicode "+printableUnicode)

	var held []rune
	for _, number := range regexp.MustCompile(`0x[0-9a-f]+`).FindAll(table[1], -1) {
		r, err := strconv.ParseInt(string(number), 0, 32)
		require.NoError(t, err)
		held = append(held, rune(r))
	}
	assert.Equal(t, printable[:], held)
}
