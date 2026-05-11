/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Package preconditions mirrors org.openrewrite.Preconditions for Go RPC
// recipes. A recipe author writes:
//
//	func (r *MyRecipe) Editor() recipe.TreeVisitor {
//	    return preconditions.Check(
//	        preconditions.Or(
//	            UsesMethod("*..* filemode(..)"),
//	            UsesType("tarfile"),
//	        ),
//	        myEditor(),
//	    )
//	}
//
// The framework introspects the wrapper at PrepareRecipe time and emits
// the gate's identity in the editPreconditions slot of the
// PrepareRecipeResponse. The Java host then evaluates the precondition
// against the local source file *before* dispatching the visit RPC,
// skipping the entire RPC round trip when the precondition does not match.
package preconditions

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// CheckArg is the operand type accepted by Check / Or / And / Not.
// Any of:
//   - recipe.TreeVisitor
//   - recipe.Recipe (its Editor() is used as the gate)
//   - *RecipeRef (a wire-only placeholder for a Java search recipe)
//   - *Composite (nested composites)
type CheckArg any

// RecipeRef is a wire-side placeholder that names a Java recipe by class
// name + options. Helpers like UsesMethod and UsesType return *RecipeRef
// so a recipe author can declare a precondition without firing an RPC at
// Editor() time. The Java host's PreparedRecipeCache.instantiateVisitor
// constructs the named recipe via Jackson and uses its visitor.
//
// LocalVisitor is optional. When provided, in-process callers (without
// an active RPC connection) evaluate the gate against it instead of
// short-circuiting to "always matches" — this preserves real filtering
// behavior in unit tests while still letting the host evaluate the
// gate over the wire when an RPC connection is present.
type RecipeRef struct {
	RecipeName   string
	Options      map[string]any
	LocalVisitor recipe.TreeVisitor
}

// Composite combines nested precondition operands with an operator.
// Mirrors Java's Preconditions.or / and / not.
type Composite struct {
	Op       string // "or", "and", or "not"
	Operands []CheckArg
}

// CheckVisitor wraps an editor with a precondition gate. Mirrors Java's
// org.openrewrite.Preconditions.Check: the precondition runs first; if
// it returns a different tree (e.g. by adding a SearchResult marker),
// the wrapped visitor runs. Otherwise the wrapped visitor is skipped
// and the original tree is returned unchanged.
//
// For non-SourceFile trees the precondition is bypassed — preconditions
// are designed to evaluate at the root and may assume the root layout.
//
// The framework also introspects this wrapper at PrepareRecipe time and
// promotes Check to the editPreconditions wire slot so that the Java
// host can evaluate the precondition locally and skip the visit RPC
// entirely. In that case the wrapped V is what runs Go-side, so the
// Visit method below is the fallback for in-process callers and for
// non-RPC dispatch paths.
type CheckVisitor struct {
	Check CheckArg
	V     recipe.TreeVisitor
}

// Visit runs the gate, then defers to the wrapped visitor when the gate
// matches. RecipeRef operands are treated as "always matches" in-process
// (they're wire-only placeholders).
func (c *CheckVisitor) Visit(t tree.Tree, p any) tree.Tree {
	sf, isSourceFile := t.(tree.SourceFile)
	if !isSourceFile {
		return c.V.Visit(t, p)
	}
	if c.matches(sf, p) {
		return c.V.Visit(t, p)
	}
	return t
}

func (c *CheckVisitor) matches(t tree.Tree, p any) bool {
	return operandMatches(c.Check, t, p)
}

func operandMatches(operand CheckArg, t tree.Tree, p any) bool {
	switch op := operand.(type) {
	case nil:
		return true
	case *RecipeRef:
		// If a LocalVisitor was bundled, evaluate against it for real;
		// otherwise short-circuit to "always matches" so the wrapped
		// visitor still runs in unit tests. The host evaluates the
		// gate for real once the response goes over the wire.
		if op.LocalVisitor == nil {
			return true
		}
		return runVisitor(op.LocalVisitor, t, p)
	case *Composite:
		return evaluateComposite(op, t, p)
	case recipe.Recipe:
		return runVisitor(op.Editor(), t, p)
	case recipe.TreeVisitor:
		return runVisitor(op, t, p)
	default:
		// Unknown operand type — treat as "matches" rather than blocking.
		return true
	}
}

func evaluateComposite(c *Composite, t tree.Tree, p any) bool {
	switch c.Op {
	case "or":
		for _, operand := range c.Operands {
			if operandMatches(operand, t, p) {
				return true
			}
		}
		return false
	case "and":
		for _, operand := range c.Operands {
			if !operandMatches(operand, t, p) {
				return false
			}
		}
		return true
	case "not":
		if len(c.Operands) != 1 {
			panic("preconditions: Not requires exactly one operand")
		}
		return !operandMatches(c.Operands[0], t, p)
	default:
		panic("preconditions: unknown composite op " + c.Op)
	}
}

func runVisitor(v recipe.TreeVisitor, t tree.Tree, p any) bool {
	if v == nil {
		return true
	}
	result := v.Visit(t, p)
	return result != t
}

// Check wraps an editor with a precondition gate.
//
// Argument shapes accepted as the gate (CheckArg):
//   - recipe.TreeVisitor — runs in-process as the gate
//   - recipe.Recipe — its Editor() is used as the gate
//   - *RecipeRef — placeholder for a Java search recipe; the framework
//     emits the recipe identity in the editPreconditions wire slot at
//     PrepareRecipe time so the host evaluates it locally without a Go RPC
//   - *Composite — combination via Or / And / Not
func Check(check CheckArg, v recipe.TreeVisitor) recipe.TreeVisitor {
	return &CheckVisitor{Check: check, V: v}
}

// Or combines precondition checks with OR semantics. Mirrors Java's
// Preconditions.or(visitor...): the gate matches if any operand matches.
// Requires at least two operands; a single-operand Or is the same as
// a bare Check.
func Or(operands ...CheckArg) *Composite {
	if len(operands) < 2 {
		panic("preconditions: Or requires at least two operands")
	}
	return &Composite{Op: "or", Operands: operands}
}

// And combines precondition checks with AND semantics. The outer
// editPreconditions list is already AND-composed by the host, so this
// is mainly useful as an operand of Or / Not.
func And(operands ...CheckArg) *Composite {
	if len(operands) < 2 {
		panic("preconditions: And requires at least two operands")
	}
	return &Composite{Op: "and", Operands: operands}
}

// Not negates a precondition check. Mirrors Java's Preconditions.not(visitor):
// the gate matches iff the operand does not.
func Not(operand CheckArg) *Composite {
	return &Composite{Op: "not", Operands: []CheckArg{operand}}
}

// HasSourcePath matches source files by path glob. Delegates to
// org.openrewrite.FindSourceFiles on the Java host. Bundles a native
// IsSourceFileVisitor so unit tests without an active RPC connection
// still see real filtering.
func HasSourcePath(filePattern string) *RecipeRef {
	return &RecipeRef{
		RecipeName:   "org.openrewrite.FindSourceFiles",
		Options:      map[string]any{"filePattern": filePattern},
		LocalVisitor: NewIsSourceFile(filePattern),
	}
}

// UsesMethod matches files using a specific method. Delegates to
// org.openrewrite.java.search.HasMethod on the Java host. Bundles a
// native UsesMethodVisitor so unit tests without an active RPC
// connection still see real filtering.
func UsesMethod(methodPattern string) *RecipeRef {
	return &RecipeRef{
		RecipeName:   "org.openrewrite.java.search.HasMethod",
		Options:      map[string]any{"methodPattern": methodPattern, "matchOverrides": false},
		LocalVisitor: NewUsesMethod(methodPattern),
	}
}

// UsesType matches files using a specific type. Delegates to
// org.openrewrite.java.search.HasType on the Java host. Bundles a
// native UsesTypeVisitor so unit tests without an active RPC
// connection still see real filtering.
func UsesType(fullyQualifiedType string) *RecipeRef {
	return &RecipeRef{
		RecipeName:   "org.openrewrite.java.search.HasType",
		Options:      map[string]any{"fullyQualifiedTypeName": fullyQualifiedType, "checkAssignability": false},
		LocalVisitor: NewUsesType(fullyQualifiedType),
	}
}

// FindMethods finds and marks methods matching a pattern. Delegates
// to org.openrewrite.java.search.FindMethods on the Java host. Bundles
// a native UsesMethodVisitor so unit tests without an active RPC
// connection still see real filtering.
func FindMethods(methodPattern string) *RecipeRef {
	return &RecipeRef{
		RecipeName:   "org.openrewrite.java.search.FindMethods",
		Options:      map[string]any{"methodPattern": methodPattern, "matchOverrides": false},
		LocalVisitor: NewUsesMethod(methodPattern),
	}
}

// FindTypes finds and marks usages of a type. Delegates to
// org.openrewrite.java.search.FindTypes on the Java host. Bundles a
// native UsesTypeVisitor so unit tests without an active RPC
// connection still see real filtering.
func FindTypes(fullyQualifiedType string) *RecipeRef {
	return &RecipeRef{
		RecipeName:   "org.openrewrite.java.search.FindTypes",
		Options:      map[string]any{"fullyQualifiedTypeName": fullyQualifiedType},
		LocalVisitor: NewUsesType(fullyQualifiedType),
	}
}
