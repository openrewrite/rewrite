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
	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TypeMatchingMode says how a match reads the attribution a pattern and a
// candidate carry. The JavaScript matcher compares no types at all; the
// Python one always does, leniently.
type TypeMatchingMode int

const (
	// TypeMatchingOff leaves unread every type slot but the one a capture
	// declared, which is what a pattern written without attribution in mind
	// expects.
	TypeMatchingOff TypeMatchingMode = iota

	// TypeMatchingLenient compares two attributions and accepts one that is
	// present on a single side. Go attribution is complete for a package or
	// absent for all of it, so this reads a package go/types could not
	// resolve as matching rather than as different.
	TypeMatchingLenient

	// TypeMatchingStrict requires both sides to be attributed and to agree.
	TypeMatchingStrict
)

// compareTypes mirrors the Python matcher's _compare_types: an attribution
// present on one side only is the mode's decision to make. The second result
// is false where the mode decided, which is what Explain counts.
func compareTypes(a, b java.JavaType, mode TypeMatchingMode) (bool, bool) {
	if a == nil && b == nil {
		return true, true
	}
	if a == nil || b == nil {
		return mode == TypeMatchingLenient, false
	}

	// A literal's keyword names a class of Go types rather than one, so two
	// of them agree when the classes overlap. matcher.IsSameGoType answers
	// false for a keyword by design and cannot serve here.
	if isPrimitive(a) && isPrimitive(b) {
		return matcher.SharesGoTypeName(a, b), true
	}
	if ma, mb := matcher.AsMethod(a), matcher.AsMethod(b); ma != nil && mb != nil {
		return compareMethodTypes(ma, mb, mode)
	}
	if va, ok := a.(*java.JavaTypeVariable); ok {
		if vb, ok := b.(*java.JavaTypeVariable); ok {
			return compareVariableTypes(va, vb, mode)
		}
	}

	fa, fb := matcher.GetFullyQualifiedName(a), matcher.GetFullyQualifiedName(b)
	if fa == "" || fb == "" {
		return mode == TypeMatchingLenient, false
	}
	return fa == fb, true
}

func isPrimitive(t java.JavaType) bool {
	_, ok := t.(*java.JavaTypePrimitive)
	return ok
}

// A variable is the name it was declared with and the type it holds. Its
// owner is not compared: a pattern declares its variables in a package of its
// own, and would otherwise answer to no source at all.
func compareVariableTypes(a, b *java.JavaTypeVariable, mode TypeMatchingMode) (bool, bool) {
	if a.Name != b.Name {
		return false, true
	}
	return compareTypes(a.Type, b.Type, mode)
}

func compareMethodTypes(a, b *java.JavaTypeMethod, mode TypeMatchingMode) (bool, bool) {
	if a.Name != b.Name {
		return false, true
	}
	if a.DeclaringType == nil || b.DeclaringType == nil {
		return mode == TypeMatchingLenient, false
	}
	return a.DeclaringType.GetFullyQualifiedName() == b.DeclaringType.GetFullyQualifiedName(), true
}

// matchByDeclaringType compares two resolved calls whose receiver names a
// package by the type declaring them and the name they call, so a pattern
// naming fmt matches source that imported it as f. Mirrors the case Python's
// comparator documents for `os.path.join` against a bare `join`.
func (c *patternComparator) matchByDeclaringType(pattern, candidate *java.MethodInvocation) (bool, bool) {
	if c.mode == TypeMatchingOff || !matcher.IsResolved(pattern) || !matcher.IsResolved(candidate) {
		return false, false
	}
	if !namesPackage(pattern.Select) || !namesPackage(candidate.Select) {
		return false, false
	}
	if matcher.DeclaringTypeFQN(pattern) != matcher.DeclaringTypeFQN(candidate) ||
		pattern.MethodType.Name != candidate.MethodType.Name {
		return false, true
	}
	if !c.matchContainer(pattern.TypeParameters, candidate.TypeParameters) {
		return false, true
	}
	return matchList(c, pattern.Arguments.Elements, candidate.Arguments.Elements), true
}

// namesPackage reports whether a receiver is an import qualifier rather than a
// value. Attribution gives a value's identifier the variable it reads; a
// package name reads no variable, being no value.
func namesPackage(receiver *java.RightPadded[java.Expression]) bool {
	if receiver == nil {
		return false
	}
	ident, ok := receiver.Element.(*java.Identifier)
	return ok && ident.FieldType == nil && !IsPlaceholder(ident.Name)
}
