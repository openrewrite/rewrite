/*
 * Copyright 2025 the original author or authors.
 */
package test

import (
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type renameXToFlag struct {
	recipe.Base
}

func (r *renameXToFlag) Name() string        { return "test.RenameXToFlag" }
func (r *renameXToFlag) DisplayName() string { return "Rename x to flag" }
func (r *renameXToFlag) Description() string { return "Renames identifier x to flag." }
func (r *renameXToFlag) Editor() recipe.TreeVisitor {
	v := &renameXVisitor{}
	v.Self = v // enable virtual dispatch
	return v
}

type renameXVisitor struct {
	visitor.GoVisitor
}

func (v *renameXVisitor) VisitIdentifier(ident *tree.Identifier, p any) tree.J {
	ident = v.GoVisitor.VisitIdentifier(ident, p).(*tree.Identifier)
	if ident.Name == "x" {
		c := *ident
		c.Name = "flag"
		return &c
	}
	return ident
}

func TestGoNativeRecipeRenameIdentifier(t *testing.T) {
	after := "package main\n\nfunc f() {\n\tvar flag = true\n\t_ = flag\n}\n"
	spec := NewRecipeSpec()
	spec.Recipe = &renameXToFlag{}
	spec.RewriteRun(t,
		SourceSpec{
			Before: "package main\n\nfunc f() {\n\tvar x = true\n\t_ = x\n}\n",
			After:  &after,
			Path:   "test.go",
		})
}
