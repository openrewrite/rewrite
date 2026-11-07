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
	"fmt"
	"math"
	"reflect"
	"strconv"
	"strings"
)

// OptionBindError reports an option value that could not be bound to the
// recipe field it names.
type OptionBindError struct {
	Recipe string
	Option string
	Target reflect.Type
	Value  any
}

func (e *OptionBindError) Error() string {
	return fmt.Sprintf("recipe %s: option %q expects %s, got %s %#v",
		e.Recipe, e.Option, e.Target, reflect.TypeOf(e.Value), e.Value)
}

// coerceOption converts a recipe option value to the declared type of the field
// it binds to. Options are JSON-decoded into `any`, so a number is a float64
// whatever the field's width, and the Moderne CLI declares -P as a
// Map<String, Object>, making every command-line option a string. Conversions
// are drawn from Jackson (reached via RecipeLoader's convertValue fallback, not
// RecipeIntrospectionUtils.convert) and C#'s Convert.ChangeType.
func coerceOption(val any, t reflect.Type) (reflect.Value, bool) {
	if val == nil {
		return reflect.Zero(t), true
	}
	v := reflect.ValueOf(val)
	if v.Type().AssignableTo(t) {
		return v, true
	}

	if t.Kind() == reflect.Ptr {
		elem, ok := coerceOption(val, t.Elem())
		if !ok {
			return reflect.Value{}, false
		}
		p := reflect.New(t.Elem())
		p.Elem().Set(elem)
		return p, true
	}

	switch t.Kind() {
	case reflect.Bool:
		// Only a string spelling: a number's truthiness is ambiguous.
		if s, ok := val.(string); ok {
			if b, err := strconv.ParseBool(s); err == nil {
				return reflect.ValueOf(b).Convert(t), true
			}
		}
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		if n, ok := asInt64(val); ok && !reflect.Zero(t).OverflowInt(n) {
			return reflect.ValueOf(n).Convert(t), true
		}
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		if n, ok := asUint64(val); ok && !reflect.Zero(t).OverflowUint(n) {
			return reflect.ValueOf(n).Convert(t), true
		}
	case reflect.Float32, reflect.Float64:
		if f, ok := asFloat64(val); ok && !reflect.Zero(t).OverflowFloat(f) {
			return reflect.ValueOf(f).Convert(t), true
		}
	case reflect.String:
		if s, ok := asString(val); ok {
			return reflect.ValueOf(s).Convert(t), true
		}
	case reflect.Slice:
		if elems, ok := val.([]any); ok {
			out := reflect.MakeSlice(t, len(elems), len(elems))
			for i, e := range elems {
				ev, ok := coerceOption(e, t.Elem())
				if !ok {
					return reflect.Value{}, false
				}
				out.Index(i).Set(ev)
			}
			return out, true
		}
	}
	return reflect.Value{}, false
}

// asInt64 accepts an integer's wire forms — a decimal string and a json.Number
// holding the literal digits — plus the numeric types an in-process caller
// passes directly. RecipeConstructor is exported and takes map[string]any, so
// both origins reach here.
func asInt64(val any) (int64, bool) {
	switch x := val.(type) {
	case string:
		n, err := strconv.ParseInt(x, 10, 64)
		return n, err == nil
	case json.Number:
		n, err := strconv.ParseInt(string(x), 10, 64)
		return n, err == nil
	}
	v := reflect.ValueOf(val)
	switch v.Kind() {
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		return v.Int(), true
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		if u := v.Uint(); u <= math.MaxInt64 {
			return int64(u), true
		}
	case reflect.Float32, reflect.Float64:
		// Compare before converting: a float outside int64's range converts to
		// an implementation-defined value rather than saturating.
		f := v.Float()
		if f == math.Trunc(f) && f >= math.MinInt64 && f < math.MaxInt64 {
			return int64(f), true
		}
	}
	return 0, false
}

// asUint64 parses the full unsigned range, which int64 cannot hold.
func asUint64(val any) (uint64, bool) {
	switch x := val.(type) {
	case string:
		n, err := strconv.ParseUint(x, 10, 64)
		return n, err == nil
	case json.Number:
		n, err := strconv.ParseUint(string(x), 10, 64)
		return n, err == nil
	}
	v := reflect.ValueOf(val)
	switch v.Kind() {
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		if n := v.Int(); n >= 0 {
			return uint64(n), true
		}
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		return v.Uint(), true
	case reflect.Float32, reflect.Float64:
		f := v.Float()
		if f == math.Trunc(f) && f >= 0 && f < math.MaxUint64 {
			return uint64(f), true
		}
	}
	return 0, false
}

func asFloat64(val any) (float64, bool) {
	switch x := val.(type) {
	case string:
		f, err := strconv.ParseFloat(x, 64)
		return f, err == nil
	case json.Number:
		f, err := strconv.ParseFloat(string(x), 64)
		return f, err == nil
	}
	v := reflect.ValueOf(val)
	switch v.Kind() {
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		return float64(v.Int()), true
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		return float64(v.Uint()), true
	case reflect.Float32, reflect.Float64:
		return v.Float(), true
	}
	return 0, false
}

// asString renders the scalars a string option can arrive as, matching Java's
// Objects.toString(o, null) for String-typed options. Decimal notation keeps a
// JSON number reading back as it was written.
func asString(val any) (string, bool) {
	switch x := val.(type) {
	case string:
		return x, true
	case json.Number:
		return string(x), true
	case bool:
		return strconv.FormatBool(x), true
	}
	v := reflect.ValueOf(val)
	switch v.Kind() {
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		return strconv.FormatInt(v.Int(), 10), true
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		return strconv.FormatUint(v.Uint(), 10), true
	case reflect.Float32, reflect.Float64:
		return strconv.FormatFloat(v.Float(), 'f', -1, 64), true
	}
	return "", false
}

// optionField resolves an option name to the struct field it binds to. The
// leading letter is capitalized so "oldName" reaches OldName, and the
// fold-equal fallback lets "url" reach a URL field, matching the Java host
// (MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES) and the C# server
// (BindingFlags.IgnoreCase).
func optionField(elem reflect.Value, name string) reflect.Value {
	if name == "" {
		return reflect.Value{}
	}
	if f := elem.FieldByName(strings.ToUpper(name[:1]) + name[1:]); f.IsValid() {
		return f
	}
	return elem.FieldByNameFunc(func(n string) bool { return strings.EqualFold(n, name) })
}
