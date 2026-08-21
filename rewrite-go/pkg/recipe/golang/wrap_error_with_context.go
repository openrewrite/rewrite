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

package golang

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// WrapErrorWithContext rewrites `return err` into
// `return fmt.Errorf("<func>: %w", err)` and adds the `fmt` import. It stands
// in for the codequality recipe of the same name, so tests can compose a
// recipe that builds an un-attributed reference with one that reads imports.
type WrapErrorWithContext struct {
	recipe.Base
}

func (r *WrapErrorWithContext) Name() string {
	return "org.openrewrite.golang.test.WrapErrorWithContext"
}
func (r *WrapErrorWithContext) DisplayName() string { return "Wrap error with context (test)" }
func (r *WrapErrorWithContext) Description() string {
	return "Test recipe that replaces `return err` with `return fmt.Errorf(\"funcName: %w\", err)`."
}

func (r *WrapErrorWithContext) Editor() recipe.TreeVisitor {
	return visitor.Init(&wrapErrorWithContextVisitor{})
}

type wrapErrorWithContextVisitor struct {
	visitor.GoVisitor
	funcName string
}

func (v *wrapErrorWithContextVisitor) VisitMethodDeclaration(md *java.MethodDeclaration, p any) java.J {
	outer := v.funcName
	if md.Name != nil {
		v.funcName = md.Name.Name
	}
	result := v.GoVisitor.VisitMethodDeclaration(md, p)
	v.funcName = outer
	return result
}

func (v *wrapErrorWithContextVisitor) VisitReturn(ret *java.Return, p any) java.J {
	ret = v.GoVisitor.VisitReturn(ret, p).(*java.Return)
	ident, ok := ret.Expression.(*java.Identifier)
	if !ok || ident.Name != "err" || v.funcName == "" {
		return ret
	}
	MaybeAddImport(v, "fmt", nil, false)
	c := *ret
	c.Expression = &java.MethodInvocation{
		Prefix: java.SingleSpace,
		Select: &java.RightPadded[java.Expression]{Element: &java.Identifier{Name: "fmt"}},
		Name:   &java.Identifier{Name: "Errorf"},
		Arguments: java.Container[java.Expression]{
			Elements: []java.RightPadded[java.Expression]{
				{Element: &java.Literal{Source: `"` + v.funcName + `: %w"`}},
				{Element: &java.Identifier{Prefix: java.SingleSpace, Name: "err"}},
			},
		},
	}
	return &c
}
