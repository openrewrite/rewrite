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

import "strings"

const (
	placeholderPrefix = "__plh_"
	placeholderSuffix = "__"
)

// ToPlaceholder converts a capture name to a valid Go identifier
// that can be used in scaffold source code.
// Example: "expr" -> "__plh_expr__"
func ToPlaceholder(name string) string {
	return placeholderPrefix + name + placeholderSuffix
}

// FromPlaceholder extracts the capture name from a placeholder identifier.
// Returns the name and true if the identifier is a placeholder, or "" and false otherwise.
func FromPlaceholder(identifier string) (string, bool) {
	if !IsPlaceholder(identifier) {
		return "", false
	}
	name := identifier[len(placeholderPrefix) : len(identifier)-len(placeholderSuffix)]
	return name, true
}

// IsPlaceholder returns true if the identifier is a template placeholder.
func IsPlaceholder(identifier string) bool {
	return strings.HasPrefix(identifier, placeholderPrefix) &&
		strings.HasSuffix(identifier, placeholderSuffix) &&
		len(identifier) > len(placeholderPrefix)+len(placeholderSuffix)
}
