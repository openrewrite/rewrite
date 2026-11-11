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

package visitor_test

import (
	"fmt"
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

type binaryReplacer struct {
	visitor.GoVisitor
	identifiers []string
}

func (r *binaryReplacer) VisitExpression(e java.Expression, p any) java.J {
	if b, ok := e.(*java.Binary); ok {
		return &java.Identifier{ID: uuid.New(), Prefix: b.Prefix, Name: "d"}
	}
	return e
}

func (r *binaryReplacer) VisitIdentifier(ident *java.Identifier, p any) java.J {
	r.identifiers = append(r.identifiers, ident.Name)
	return r.GoVisitor.VisitIdentifier(ident, p)
}

func TestVisitExpressionReplacesTheNodeWithoutVisitingItsChildren(t *testing.T) {
	// given
	cu, err := parser.NewGoParser().Parse("hooks.go", "package main\n\nvar a = b + c\n")
	require.NoError(t, err)
	r := &binaryReplacer{}
	r.Self = r

	// when
	out := r.Visit(cu, nil)

	// then
	assert.Equal(t, "package main\n\nvar a = d\n", printer.Print(out))
	assert.NotContains(t, r.identifiers, "b")
	assert.NotContains(t, r.identifiers, "c")
}

type statementDeleter struct {
	visitor.GoVisitor
}

func (d *statementDeleter) VisitStatement(s java.Statement, p any) java.J {
	if _, ok := s.(*java.MethodInvocation); ok {
		return nil
	}
	return s
}

func TestVisitStatementReturningNilDeletesTheStatement(t *testing.T) {
	// given
	cu, err := parser.NewGoParser().Parse("hooks.go", "package main\n\nfunc f() {\n\tg()\n\th := 1\n\t_ = h\n}\n")
	require.NoError(t, err)
	d := &statementDeleter{}
	d.Self = d

	// when
	out := d.Visit(cu, nil)

	// then
	assert.Equal(t, "package main\n\nfunc f() {\n\th := 1\n\t_ = h\n}\n", printer.Print(out))
}

type hookOrderRecorder struct {
	visitor.GoVisitor
	calls map[string][]string
}

func (r *hookOrderRecorder) VisitStatement(s java.Statement, p any) java.J {
	r.calls[fmt.Sprintf("%T", s)] = append(r.calls[fmt.Sprintf("%T", s)], "Statement")
	return s
}

func (r *hookOrderRecorder) VisitExpression(e java.Expression, p any) java.J {
	r.calls[fmt.Sprintf("%T", e)] = append(r.calls[fmt.Sprintf("%T", e)], "Expression")
	return e
}

func TestHooksRunInJavaVisitorOrder(t *testing.T) {
	// given
	cu, err := parser.NewGoParser().Parse("hooks.go", "package main\n\nvar a = s.N\nvar b = g()\n")
	require.NoError(t, err)
	r := &hookOrderRecorder{calls: map[string][]string{}}
	r.Self = r

	// when
	r.Visit(cu, nil)

	// then
	assert.Equal(t, []string{"Expression", "Statement"}, r.calls["*java.FieldAccess"])
	assert.Equal(t, []string{"Statement", "Expression"}, r.calls["*java.MethodInvocation"])
}
