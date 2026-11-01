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

// sameMessage reports whether b is a: the same pointer, map, slice or func, or
// an equal value.
func sameMessage(a, b any) bool {
	va, vb := reflect.ValueOf(a), reflect.ValueOf(b)
	if !va.IsValid() || !vb.IsValid() {
		return va.IsValid() == vb.IsValid()
	}
	if va.Type() != vb.Type() {
		return false
	}
	switch va.Kind() {
	case reflect.Pointer, reflect.Map, reflect.Chan, reflect.Func, reflect.UnsafePointer:
		return va.Pointer() == vb.Pointer()
	case reflect.Slice:
		return va.Pointer() == vb.Pointer() && va.Len() == vb.Len()
	}
	if va.Comparable() {
		return va.Equal(vb)
	}
	return reflect.DeepEqual(a, b)
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
