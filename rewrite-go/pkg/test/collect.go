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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// TypedNodes gathers, in visit order, the nodes whose type slots a test wants
// to assert on. Drive it with visitor.Init(&TypedNodes{}).
type TypedNodes struct {
	visitor.GoVisitor
	Invocations        []*java.MethodInvocation
	Composites         []*golang.Composite
	ArrayAccesses      []*java.ArrayAccess
	Unaries            []*java.Unary
	GoUnaries          []*golang.Unary
	ParameterizedTypes []*java.ParameterizedType
	Conversions        []*java.TypeCast
}

func (v *TypedNodes) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	v.Invocations = append(v.Invocations, mi)
	return v.GoVisitor.VisitMethodInvocation(mi, p)
}

func (v *TypedNodes) VisitTypeCast(tc *java.TypeCast, p any) java.J {
	v.Conversions = append(v.Conversions, tc)
	return v.GoVisitor.VisitTypeCast(tc, p)
}

func (v *TypedNodes) VisitComposite(c *golang.Composite, p any) java.J {
	v.Composites = append(v.Composites, c)
	return v.GoVisitor.VisitComposite(c, p)
}

func (v *TypedNodes) VisitArrayAccess(aa *java.ArrayAccess, p any) java.J {
	v.ArrayAccesses = append(v.ArrayAccesses, aa)
	return v.GoVisitor.VisitArrayAccess(aa, p)
}

func (v *TypedNodes) VisitUnary(u *java.Unary, p any) java.J {
	v.Unaries = append(v.Unaries, u)
	return v.GoVisitor.VisitUnary(u, p)
}

func (v *TypedNodes) VisitGoUnary(u *golang.Unary, p any) java.J {
	v.GoUnaries = append(v.GoUnaries, u)
	return v.GoVisitor.VisitGoUnary(u, p)
}

func (v *TypedNodes) VisitParameterizedType(pt *java.ParameterizedType, p any) java.J {
	v.ParameterizedTypes = append(v.ParameterizedTypes, pt)
	return v.GoVisitor.VisitParameterizedType(pt, p)
}

// CollectTypedNodes walks t and returns the nodes it gathered.
func CollectTypedNodes(t java.Tree) *TypedNodes {
	v := visitor.Init(&TypedNodes{})
	v.Visit(t, nil)
	return v
}
