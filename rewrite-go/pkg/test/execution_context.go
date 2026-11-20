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

package test

import (
	"fmt"
	"math"
	"reflect"
	"sort"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

// contextChange is a message a recipe put on the ExecutionContext.
type contextChange struct {
	recipe, key, where string
}

// watch notes the messages on ctx before rc runs, returning what records the
// ones it put there once it has, with where it was then.
func (c *runChecks) watch(rc recipe.Recipe, ctx *recipe.ExecutionContext) func(where string) {
	if c == nil || !c.executionContextUnchanged {
		return func(string) {}
	}
	before := messages(ctx)
	return func(where string) {
		for _, key := range changedMessages(before, ctx) {
			c.contextChanged(contextChange{recipe: rc.Name(), key: key, where: where})
		}
	}
}

func (c *runChecks) contextChanged(change contextChange) {
	for _, known := range c.contextChanges {
		if known.recipe == change.recipe && known.key == change.key {
			return
		}
	}
	c.contextChanges = append(c.contextChanges, change)
}

func messages(ctx *recipe.ExecutionContext) map[string]any {
	m := map[string]any{}
	for _, k := range ctx.MessageKeys() {
		m[k], _ = ctx.GetMessage(k)
	}
	return m
}

// changedMessages lists the keys of the messages ctx holds that it did not hold
// in before, or held another value of, other than the framework's own.
func changedMessages(before map[string]any, ctx *recipe.ExecutionContext) []string {
	var changed []string
	for _, k := range ctx.MessageKeys() {
		if k == recipe.DataTableStoreKey {
			continue
		}
		v, _ := ctx.GetMessage(k)
		if old, ok := before[k]; !ok || !sameMessage(old, v) {
			changed = append(changed, k)
		}
	}
	sort.Strings(changed)
	return changed
}

// sameMessage reports whether b is a, comparing pointers, maps, slices and funcs
// by identity wherever they sit, and floats bit for bit so that NaN is itself.
func sameMessage(a, b any) bool {
	return sameValue(reflect.ValueOf(a), reflect.ValueOf(b))
}

func sameValue(a, b reflect.Value) bool {
	if !a.IsValid() || !b.IsValid() {
		return a.IsValid() == b.IsValid()
	}
	if a.Type() != b.Type() {
		return false
	}
	switch a.Kind() {
	case reflect.Pointer, reflect.Map, reflect.Chan, reflect.Func, reflect.UnsafePointer:
		return a.Pointer() == b.Pointer()
	case reflect.Slice:
		return a.Pointer() == b.Pointer() && a.Len() == b.Len()
	case reflect.Interface:
		return sameValue(a.Elem(), b.Elem())
	case reflect.Struct:
		for i := 0; i < a.NumField(); i++ {
			if !sameValue(a.Field(i), b.Field(i)) {
				return false
			}
		}
		return true
	case reflect.Array:
		for i := 0; i < a.Len(); i++ {
			if !sameValue(a.Index(i), b.Index(i)) {
				return false
			}
		}
		return true
	case reflect.Float32, reflect.Float64:
		return math.Float64bits(a.Float()) == math.Float64bits(b.Float())
	}
	return a.Equal(b)
}

func contextChangesFailure(changes []contextChange) string {
	var found strings.Builder
	for _, c := range changes {
		fmt.Fprintf(&found, "  %s put %q on the ExecutionContext %s\n", c.recipe, c.key, c.where)
	}
	return "Keep state that spans source files in a ScanningRecipe's accumulator, and state within one source file " +
		"in cursor messages, with v.Cursor().PutMessage(...) or v.Cursor().PutMessageOnFirstEnclosing(...), " +
		"instead of on the ExecutionContext:\n" + found.String() +
		"Every recipe and source file in a run shares the ExecutionContext, so what a recipe keeps there leaks " +
		"into the others. A recipe that must change the ExecutionContext opts out with " +
		"spec.CheckExecutionContextUnchanged = false."
}
