/*
 * Copyright 2025 the original author or authors.
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
