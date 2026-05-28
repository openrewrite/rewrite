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

package template

import "fmt"

// CaptureKind indicates the syntactic position a capture occupies.
type CaptureKind int

const (
	CaptureExpression CaptureKind = iota // expression position (default)
	CaptureType                          // type position
	CaptureName                          // identifier/name position
	CaptureStatement                     // statement position
)

// Capture represents a named placeholder in a pattern or template.
// It defines what kind of AST subtree can be matched and optionally
// constrains the match by Go type name.
type Capture struct {
	name     string
	kind     CaptureKind
	typeName string // optional Go type FQN for type-constrained captures
	variadic bool   // true for captures that match zero or more items
	minCount int    // minimum items for variadic captures
	maxCount int    // maximum items for variadic (-1 = unlimited)
}

// Name returns the capture's name.
func (c *Capture) Name() string { return c.name }

// Kind returns the capture's syntactic kind.
func (c *Capture) Kind() CaptureKind { return c.kind }

// TypeName returns the optional Go type constraint.
func (c *Capture) TypeName() string { return c.typeName }

// IsVariadic returns true if this capture matches zero or more items.
func (c *Capture) IsVariadic() bool { return c.variadic }

// MinCount returns the minimum number of items for variadic captures.
func (c *Capture) MinCount() int { return c.minCount }

// MaxCount returns the maximum number of items for variadic captures (-1 = unlimited).
func (c *Capture) MaxCount() int { return c.maxCount }

// Placeholder returns the placeholder identifier used in scaffold source code.
func (c *Capture) Placeholder() string { return ToPlaceholder(c.name) }

// String returns the placeholder identifier, making Capture usable with fmt.Sprintf.
func (c *Capture) String() string { return c.Placeholder() }

// Expr creates a capture for an expression position.
func Expr(name string) *Capture {
	return &Capture{name: name, kind: CaptureExpression, maxCount: -1}
}

// Stmt creates a capture for a statement position.
func Stmt(name string) *Capture {
	return &Capture{name: name, kind: CaptureStatement, maxCount: -1}
}

// TypeExpr creates a capture for a type position.
func TypeExpr(name string) *Capture {
	return &Capture{name: name, kind: CaptureType, maxCount: -1}
}

// Ident creates a capture for an identifier/name position.
func Ident(name string) *Capture {
	return &Capture{name: name, kind: CaptureName, maxCount: -1}
}

// WithType returns a copy of the capture with a type constraint.
// The type name is used in the scaffold preamble for type-attributed matching.
func (c *Capture) WithType(typeName string) *Capture {
	cp := *c
	cp.typeName = typeName
	return &cp
}

// Variadic returns a copy of the capture configured to match multiple items.
// min and max set bounds (-1 for max means unlimited).
func (c *Capture) Variadic(min, max int) *Capture {
	cp := *c
	cp.variadic = true
	cp.minCount = min
	cp.maxCount = max
	return &cp
}

// captureMap builds a name -> Capture lookup from a slice of captures.
func captureMap(captures []*Capture) map[string]*Capture {
	m := make(map[string]*Capture, len(captures))
	for _, c := range captures {
		if _, exists := m[c.name]; exists {
			panic(fmt.Sprintf("duplicate capture name: %q", c.name))
		}
		m[c.name] = c
	}
	return m
}
