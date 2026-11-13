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

package template

import (
	"reflect"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parenthesize"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// placeAt fits result into the site it replaces. site is the replaced node.
func placeAt(result java.J, site *visitor.Cursor) java.J {
	replaced, ok := site.Value().(java.J)
	if !ok {
		return result
	}
	var out java.J
	_, isStatement := result.(java.Statement)
	if expr, ok := result.(java.Expression); ok && !isStatement && inStatementSlot(site) {
		out = &golang.ExpressionStatement{
			ID:         uuid.New(),
			Prefix:     getLeadingPrefix(replaced),
			Expression: setLeadingPrefix(expr, java.EmptySpace).(java.Expression),
		}
	} else {
		out = parenthesized(setLeadingPrefix(result, getLeadingPrefix(replaced)), site)
	}
	if formatted, ok := format.AutoFormat(out, nil, nil, site.Parent()).(java.J); ok {
		return formatted
	}
	return out
}

// parenthesized applies parenthesize.Maybe to anything that is an expression.
// A template result can be a statement or a declaration, which never regroups.
func parenthesized(j java.J, site *visitor.Cursor) java.J {
	expr, ok := j.(java.Expression)
	if !ok {
		return j
	}
	return parenthesize.Maybe(expr, site)
}

var statementType = reflect.TypeOf((*java.Statement)(nil)).Elem()

// inStatementSlot reports whether the parent declares the field holding the
// node at site as a java.Statement, where an expression alone cannot go.
func inStatementSlot(site *visitor.Cursor) bool {
	parent := site.Parent()
	if parent == nil {
		return false
	}
	node, holder := reflect.ValueOf(site.Value()), reflect.ValueOf(parent.Value())
	if node.Kind() != reflect.Pointer || holder.Kind() != reflect.Pointer || holder.Elem().Kind() != reflect.Struct {
		return false
	}
	return heldAsStatement(holder.Elem(), node.Pointer())
}

// heldAsStatement looks through the padding and lists of v, but not into
// other nodes, for an interface of type java.Statement pointing at node.
func heldAsStatement(v reflect.Value, node uintptr) bool {
	if t := v.Type(); t == spaceType || t == markersType || t.Implements(javaTypeType) {
		return false
	}
	switch v.Kind() {
	case reflect.Interface:
		e := v.Elem()
		return v.Type() == statementType && e.Kind() == reflect.Pointer && e.Pointer() == node
	case reflect.Pointer:
		return !v.IsNil() && !v.Type().Implements(jType) && heldAsStatement(v.Elem(), node)
	case reflect.Struct:
		for i := range v.NumField() {
			if heldAsStatement(v.Field(i), node) {
				return true
			}
		}
	case reflect.Slice:
		for i := range v.Len() {
			if heldAsStatement(v.Index(i), node) {
				return true
			}
		}
	}
	return false
}
