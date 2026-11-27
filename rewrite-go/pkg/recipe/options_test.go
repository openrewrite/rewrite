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

package recipe

import (
	"encoding/json"
	"math"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// optionsRecipe exercises every option field kind the binder supports.
type optionsRecipe struct {
	Base
	Text     string
	Flag     bool
	Count    int
	Small    int8
	Unsigned uint16
	Ratio    float64
	Alias    *string
	Enabled  *bool
	Patterns []string
	Sizes    []int
	Raw      any
	Big      uint64
	URL      string
}

func (r *optionsRecipe) Name() string        { return "com.example.Options" }
func (r *optionsRecipe) DisplayName() string { return "Options" }
func (r *optionsRecipe) Description() string { return "Binds options of every supported kind." }

func bind(t *testing.T, options map[string]any) (*optionsRecipe, error) {
	t.Helper()
	inst, err := newReflectConstructor(&optionsRecipe{})(options)
	if err != nil {
		return nil, err
	}
	return inst.(*optionsRecipe), nil
}

func TestBindCoercesToDeclaredFieldType(t *testing.T) {
	tests := []struct {
		name   string
		option string
		value  any
		got    func(*optionsRecipe) any
		want   any
	}{
		{"string passthrough", "text", "hello", func(r *optionsRecipe) any { return r.Text }, "hello"},
		{"bool passthrough", "flag", true, func(r *optionsRecipe) any { return r.Flag }, true},
		{"string to bool true", "flag", "true", func(r *optionsRecipe) any { return r.Flag }, true},
		{"string to bool false", "flag", "false", func(r *optionsRecipe) any { return r.Flag }, false},
		{"string to bool mixed case", "flag", "TRUE", func(r *optionsRecipe) any { return r.Flag }, true},
		{"float64 to int", "count", float64(42), func(r *optionsRecipe) any { return r.Count }, 42},
		{"string to int", "count", "42", func(r *optionsRecipe) any { return r.Count }, 42},
		{"string to negative int", "count", "-7", func(r *optionsRecipe) any { return r.Count }, -7},
		{"float64 to uint", "unsigned", float64(65535), func(r *optionsRecipe) any { return r.Unsigned }, uint16(65535)},
		{"string to uint", "unsigned", "12", func(r *optionsRecipe) any { return r.Unsigned }, uint16(12)},
		{"float64 to float", "ratio", 1.5, func(r *optionsRecipe) any { return r.Ratio }, 1.5},
		{"string to float", "ratio", "1.5", func(r *optionsRecipe) any { return r.Ratio }, 1.5},
		{"bool to string", "text", true, func(r *optionsRecipe) any { return r.Text }, "true"},
		{"integral float64 to string", "text", float64(42), func(r *optionsRecipe) any { return r.Text }, "42"},
		{"any passthrough", "raw", []any{"a"}, func(r *optionsRecipe) any { return r.Raw }, []any{"a"}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, err := bind(t, map[string]any{tt.option: tt.value})
			require.NoError(t, err)
			assert.Equal(t, tt.want, tt.got(r))
		})
	}
}

func TestBindPointerOptions(t *testing.T) {
	r, err := bind(t, map[string]any{"alias": "fmtutil", "enabled": "false"})
	require.NoError(t, err)
	require.NotNil(t, r.Alias)
	assert.Equal(t, "fmtutil", *r.Alias)
	require.NotNil(t, r.Enabled)
	assert.False(t, *r.Enabled)

	// A pointer option distinguishes "unset" from "set to the zero value".
	r, err = bind(t, map[string]any{"alias": nil})
	require.NoError(t, err)
	assert.Nil(t, r.Alias)
}

func TestBindSliceOptions(t *testing.T) {
	r, err := bind(t, map[string]any{
		"patterns": []any{"a", "b"},
		"sizes":    []any{float64(1), "2"},
	})
	require.NoError(t, err)
	assert.Equal(t, []string{"a", "b"}, r.Patterns)
	assert.Equal(t, []int{1, 2}, r.Sizes)
}

func TestBindRejectsOutOfRangeValues(t *testing.T) {
	for _, tt := range []struct {
		name   string
		option string
		value  any
	}{
		{"int8 overflow from number", "small", float64(300)},
		{"int8 overflow from string", "small", "300"},
		{"uint from negative number", "unsigned", float64(-1)},
		{"uint from negative string", "unsigned", "-1"},
		{"int from non-integral number", "count", 1.5},
		{"int from number beyond int64 range", "count", math.MaxFloat64},
	} {
		t.Run(tt.name, func(t *testing.T) {
			_, err := bind(t, map[string]any{tt.option: tt.value})
			require.Error(t, err)
			assert.Contains(t, err.Error(), tt.option)
		})
	}
}

func TestBindErrorNamesRecipeOptionTypeAndValue(t *testing.T) {
	_, err := bind(t, map[string]any{"flag": "yes"})
	require.Error(t, err)
	msg := err.Error()
	assert.Contains(t, msg, "com.example.Options")
	assert.Contains(t, msg, "flag")
	assert.Contains(t, msg, "bool")
	assert.Contains(t, msg, "yes")
}

func TestBindRejectsUnconvertibleValues(t *testing.T) {
	for _, tt := range []struct {
		name   string
		option string
		value  any
	}{
		{"non-boolean string to bool", "flag", "yes"},
		{"non-numeric string to int", "count", "abc"},
		{"object to string", "text", map[string]any{"a": 1}},
		{"scalar to slice", "patterns", "a"},
		{"unconvertible slice element", "sizes", []any{"abc"}},
	} {
		t.Run(tt.name, func(t *testing.T) {
			_, err := bind(t, map[string]any{tt.option: tt.value})
			require.Error(t, err)
		})
	}
}

func TestBindIgnoresUnknownOptions(t *testing.T) {
	r, err := bind(t, map[string]any{"text": "hi", "noSuchOption": "x"})
	require.NoError(t, err)
	assert.Equal(t, "hi", r.Text)
}

func TestBindLeavesUnsetOptionsAtZeroValue(t *testing.T) {
	r, err := bind(t, nil)
	require.NoError(t, err)
	assert.Equal(t, "", r.Text)
	assert.False(t, r.Flag)
	assert.Nil(t, r.Alias)
}

func TestBindLargeUnsignedOption(t *testing.T) {
	r, err := bind(t, map[string]any{"big": "18446744073709551615"})
	require.NoError(t, err)
	assert.Equal(t, uint64(math.MaxUint64), r.Big)
}

func TestBindPreservesIntegerPrecision(t *testing.T) {
	r, err := bind(t, map[string]any{"count": json.Number("9007199254740993")})
	require.NoError(t, err)
	assert.Equal(t, 9007199254740993, r.Count)

	r, err = bind(t, map[string]any{"ratio": json.Number("1.5")})
	require.NoError(t, err)
	assert.Equal(t, 1.5, r.Ratio)

	r, err = bind(t, map[string]any{"text": json.Number("42")})
	require.NoError(t, err)
	assert.Equal(t, "42", r.Text)

	_, err = bind(t, map[string]any{"count": json.Number("1.5")})
	require.Error(t, err)
}

func TestBindMatchesFieldNamesCaseInsensitively(t *testing.T) {
	for _, name := range []string{"url", "URL", "Url"} {
		r, err := bind(t, map[string]any{name: "https://example.com"})
		require.NoErrorf(t, err, "option %q", name)
		assert.Equalf(t, "https://example.com", r.URL, "option %q", name)
	}
}

func TestBindIgnoresEmptyOptionName(t *testing.T) {
	r, err := bind(t, map[string]any{"": "x", "text": "hi"})
	require.NoError(t, err)
	assert.Equal(t, "hi", r.Text)
}

func TestBindGoNativeNumericOptions(t *testing.T) {
	tests := []struct {
		name   string
		option string
		value  any
		got    func(*optionsRecipe) any
		want   any
	}{
		{"int to int8", "small", 42, func(r *optionsRecipe) any { return r.Small }, int8(42)},
		{"int to uint64", "big", 42, func(r *optionsRecipe) any { return r.Big }, uint64(42)},
		{"int to float64", "ratio", 42, func(r *optionsRecipe) any { return r.Ratio }, float64(42)},
		{"int to string", "text", 42, func(r *optionsRecipe) any { return r.Text }, "42"},
		{"int64 to int", "count", int64(42), func(r *optionsRecipe) any { return r.Count }, 42},
		{"uint8 to int", "count", uint8(42), func(r *optionsRecipe) any { return r.Count }, 42},
		{"float32 to float64", "ratio", float32(1.5), func(r *optionsRecipe) any { return r.Ratio }, 1.5},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, err := bind(t, map[string]any{tt.option: tt.value})
			require.NoError(t, err)
			assert.Equal(t, tt.want, tt.got(r))
		})
	}
}

func TestBindRejectsOutOfRangeGoNativeNumbers(t *testing.T) {
	for _, tt := range []struct {
		name   string
		option string
		value  any
	}{
		{"negative int to uint", "unsigned", -1},
		{"int overflowing int8", "small", 300},
		{"uint64 overflowing int64", "count", uint64(math.MaxUint64)},
	} {
		t.Run(tt.name, func(t *testing.T) {
			_, err := bind(t, map[string]any{tt.option: tt.value})
			require.Error(t, err)
		})
	}
}
