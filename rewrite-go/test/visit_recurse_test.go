/*
 * Copyright 2025 the original author or authors.
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
	"testing"

	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

// These tests guard against the GoVisitor skipping a node's children.
// The renameXToFlag recipe (see visit_recipe_test.go) rewrites every
// identifier named `x`. If a visit method fails to descend into one of
// its children, the `x` nested in that child is never visited and the
// rename silently no-ops, so the printed output still contains `x` and
// the assertion fails.

func TestGoVisitorRecursesIntoDefer(t *testing.T) {
	before := `package main

func sink(int) {}

func f(x int) {
	defer sink(x)
}
`
	after := `package main

func sink(int) {}

func f(flag int) {
	defer sink(flag)
}
`
	spec := NewRecipeSpec()
	spec.Recipe = &renameXToFlag{}
	spec.RewriteRun(t, SourceSpec{Before: before, After: &after, Path: "defer.go"})
}

func TestGoVisitorRecursesIntoStatementChildren(t *testing.T) {
	before := `package main

func sink(int) {}

func f(x int, arr []int, ch chan int) {
	go sink(x)
	ch <- x
	_ = arr[x]
	_ = arr[x:x]
	_ = (x)
	for range arr {
	}
}
`
	after := `package main

func sink(int) {}

func f(flag int, arr []int, ch chan int) {
	go sink(flag)
	ch <- flag
	_ = arr[flag]
	_ = arr[flag:flag]
	_ = (flag)
	for range arr {
	}
}
`
	spec := NewRecipeSpec()
	spec.Recipe = &renameXToFlag{}
	spec.RewriteRun(t, SourceSpec{Before: before, After: &after, Path: "stmts.go"})
}

func TestGoVisitorRecursesIntoCompositeAndTypes(t *testing.T) {
	before := `package main

func f(x int) {
	_ = map[int]int{x: x}
	_ = []int{x}
	var p *int = &x
	_ = p
}
`
	after := `package main

func f(flag int) {
	_ = map[int]int{flag: flag}
	_ = []int{flag}
	var p *int = &flag
	_ = p
}
`
	spec := NewRecipeSpec()
	spec.Recipe = &renameXToFlag{}
	spec.RewriteRun(t, SourceSpec{Before: before, After: &after, Path: "composite.go"})
}
