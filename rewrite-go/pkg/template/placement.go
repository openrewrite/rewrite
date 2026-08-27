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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/format"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parenthesize"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// placeAt fits result into the site it replaces. site is the replaced node.
func placeAt(result java.J, site *visitor.Cursor) java.J {
	replaced, ok := site.Value().(java.J)
	if !ok {
		return result
	}
	out := parenthesized(setLeadingPrefix(result, getLeadingPrefix(replaced)), site)
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
