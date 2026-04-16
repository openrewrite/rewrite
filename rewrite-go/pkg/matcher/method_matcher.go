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

package matcher

import (
	"regexp"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// MethodMatcher matches method invocations against an AspectJ-style pattern.
//
// Pattern format: "DeclaringType MethodName(ArgType1, ArgType2, ..)"
//
// Examples:
//   - "fmt Println(..)"             — fmt.Println with any args
//   - "fmt Sprintf(string, ..)"     — first arg string, rest any
//   - "time.Time Sub(time.Time)"    — method on time.Time
//   - "* Sub(..)"                   — any type's Sub method
//   - "strings Contains(string, string)" — exact match
//
// Wildcards:
//   - "*" in declaring type: matches any single package/type name
//   - "*..*" in declaring type: matches any type in any package
//   - ".." in args: matches zero or more arguments of any type
type MethodMatcher struct {
	declaringTypePattern *regexp.Regexp
	methodNamePattern    *regexp.Regexp
	argPatterns          []argMatcher
	matchesAnyArgs       bool // true when pattern is (..)
}

// argMatcher matches a single argument type.
type argMatcher interface {
	matches(t tree.JavaType) bool
}

// typeArgMatcher matches a specific type by FQN pattern.
type typeArgMatcher struct {
	pattern *regexp.Regexp
	raw     string
}

func (m *typeArgMatcher) matches(t tree.JavaType) bool {
	fqn := GetFullyQualifiedName(t)
	if fqn == "" {
		// If type info is not available, match by raw name against common Go types.
		return false
	}
	return m.pattern.MatchString(fqn)
}

// wildcardArgMatcher matches any type (the ".." wildcard).
type wildcardArgMatcher struct{}

func (m *wildcardArgMatcher) matches(t tree.JavaType) bool { return true }

// NewMethodMatcher creates a MethodMatcher from the given pattern string.
// Pattern format: "DeclaringType MethodName(ArgTypes)"
func NewMethodMatcher(pattern string) *MethodMatcher {
	pattern = strings.TrimSpace(pattern)

	// Split into: declaringType, methodName(args)
	spaceIdx := strings.Index(pattern, " ")
	if spaceIdx < 0 {
		// No declaring type: treat whole thing as method name with any args
		return &MethodMatcher{
			methodNamePattern: globToRegexp(pattern),
			matchesAnyArgs:    true,
		}
	}

	declaringType := pattern[:spaceIdx]
	rest := pattern[spaceIdx+1:]

	// Parse method name and arguments
	parenIdx := strings.Index(rest, "(")
	var methodName, argsStr string
	if parenIdx < 0 {
		methodName = rest
		argsStr = ".."
	} else {
		methodName = rest[:parenIdx]
		argsStr = strings.TrimSuffix(strings.TrimPrefix(rest[parenIdx:], "("), ")")
	}

	mm := &MethodMatcher{
		declaringTypePattern: globToRegexp(declaringType),
		methodNamePattern:    globToRegexp(methodName),
	}

	// Parse argument patterns
	argsStr = strings.TrimSpace(argsStr)
	if argsStr == ".." || argsStr == "" {
		mm.matchesAnyArgs = true
	} else {
		parts := strings.Split(argsStr, ",")
		for _, part := range parts {
			part = strings.TrimSpace(part)
			if part == ".." {
				mm.matchesAnyArgs = true
				break
			}
			mm.argPatterns = append(mm.argPatterns, &typeArgMatcher{
				pattern: globToRegexp(resolveGoType(part)),
				raw:     part,
			})
		}
	}

	return mm
}

// Matches checks if the given MethodInvocation matches this pattern.
func (m *MethodMatcher) Matches(mi *tree.MethodInvocation) bool {
	// Match method name
	if m.methodNamePattern != nil && !m.methodNamePattern.MatchString(mi.Name.Name) {
		return false
	}

	// Match declaring type
	if m.declaringTypePattern != nil {
		declFQN := DeclaringTypeFQN(mi)
		if declFQN == "" {
			// No type info and no select — can't match declaring type.
			// For built-in functions (len, append, etc.) there is no declaring type.
			return false
		}
		if !m.declaringTypePattern.MatchString(declFQN) {
			return false
		}
	}

	// Match arguments
	if m.matchesAnyArgs {
		return true
	}

	args := realArgs(mi.Arguments.Elements)
	if len(args) != len(m.argPatterns) {
		return false
	}
	for i, argMatcher := range m.argPatterns {
		argType := TypeOfExpression(args[i])
		if argType == nil {
			// No type info on this argument. For now, accept the match if the
			// pattern is a simple wildcard (*), reject otherwise.
			if _, ok := argMatcher.(*wildcardArgMatcher); !ok {
				return false
			}
			continue
		}
		if !argMatcher.matches(argType) {
			return false
		}
	}
	return true
}

// MatchesMethod checks if a JavaTypeMethod matches this pattern.
func (m *MethodMatcher) MatchesMethod(mt *tree.JavaTypeMethod) bool {
	if mt == nil {
		return false
	}

	// Match method name
	if m.methodNamePattern != nil && !m.methodNamePattern.MatchString(mt.Name) {
		return false
	}

	// Match declaring type
	if m.declaringTypePattern != nil {
		declFQN := ""
		if mt.DeclaringType != nil {
			declFQN = mt.DeclaringType.FullyQualifiedName
		}
		if !m.declaringTypePattern.MatchString(declFQN) {
			return false
		}
	}

	// Match parameter types
	if m.matchesAnyArgs {
		return true
	}
	if len(mt.ParameterTypes) != len(m.argPatterns) {
		return false
	}
	for i, argMatcher := range m.argPatterns {
		if !argMatcher.matches(mt.ParameterTypes[i]) {
			return false
		}
	}
	return true
}

// MatchesName checks only the declaring type and method name, ignoring arguments.
// Useful as a fast pre-check before deeper matching.
func (m *MethodMatcher) MatchesName(mi *tree.MethodInvocation) bool {
	if m.methodNamePattern != nil && !m.methodNamePattern.MatchString(mi.Name.Name) {
		return false
	}
	if m.declaringTypePattern != nil {
		declFQN := DeclaringTypeFQN(mi)
		if declFQN == "" {
			return false
		}
		return m.declaringTypePattern.MatchString(declFQN)
	}
	return true
}

// realArgs filters out Empty sentinel nodes from an argument list.
func realArgs(args []tree.RightPadded[tree.Expression]) []tree.Expression {
	var result []tree.Expression
	for _, a := range args {
		if _, isEmpty := a.Element.(*tree.Empty); !isEmpty {
			result = append(result, a.Element)
		}
	}
	return result
}

// globToRegexp converts an AspectJ-style glob pattern to a compiled regexp.
// "*" matches any sequence of non-dot characters.
// "*..*" or ".." matches any sequence including dots.
func globToRegexp(pattern string) *regexp.Regexp {
	// Escape regex special chars, then convert glob wildcards
	var b strings.Builder
	b.WriteString("^")

	i := 0
	for i < len(pattern) {
		switch {
		case i+3 < len(pattern) && pattern[i:i+4] == "*..*":
			b.WriteString(".*")
			i += 4
		case i+1 < len(pattern) && pattern[i:i+2] == "..":
			b.WriteString(".*")
			i += 2
		case pattern[i] == '*':
			b.WriteString("[^.]*")
			i++
		case isRegexpMeta(pattern[i]):
			b.WriteByte('\\')
			b.WriteByte(pattern[i])
			i++
		default:
			b.WriteByte(pattern[i])
			i++
		}
	}

	b.WriteString("$")
	return regexp.MustCompile(b.String())
}

func isRegexpMeta(ch byte) bool {
	return strings.ContainsRune(`\.+?{}[]()^$|`, rune(ch))
}

// resolveGoType maps common Go type names to their FQN equivalents
// used by the type mapper. This allows patterns like "string" to match
// both the primitive keyword and the FQN.
func resolveGoType(name string) string {
	// The type mapper maps Go primitives to JavaTypePrimitive keywords.
	// Most Go types are already their own FQN.
	switch name {
	case "string":
		return "String" // JavaTypePrimitive keyword for Go strings
	case "bool":
		return "boolean"
	case "int", "int8", "int16", "int32", "int64":
		return name
	case "uint", "uint8", "uint16", "uint32", "uint64", "uintptr":
		return name
	case "float32", "float64":
		return name
	case "byte":
		return "byte"
	case "rune":
		return "char"
	case "error":
		return "error"
	default:
		return name
	}
}
