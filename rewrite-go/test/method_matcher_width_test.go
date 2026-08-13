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
	"testing"

	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/matcher"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// Narrowing by integer width must work end to end: a pattern naming int64 matches
// an int64 argument but rejects an int one, and vice versa.
func TestMethodMatcherDistinguishesIntWidths(t *testing.T) {
	cu, err := parser.NewGoParser().Parse("test.go", `package main

import "strconv"

func main() {
	var i64 int64
	var u64 uint64
	var i int
	_ = strconv.FormatInt(i64, 10)
	_ = strconv.FormatUint(u64, 10)
	_ = strconv.Itoa(i)
}
`)
	require.NoError(t, err)

	calls := map[string]*java.MethodInvocation{}
	visitor.Walk(cu, func(n java.Tree) bool {
		if mi, ok := n.(*java.MethodInvocation); ok {
			calls[mi.Name.Name] = mi
		}
		return true
	})

	cases := []struct {
		pattern string
		call    string
		want    bool
	}{
		// int64 argument: matches an int64 pattern, not an int pattern.
		{"strconv FormatInt(int64, int)", "FormatInt", true},
		{"strconv FormatInt(int, int)", "FormatInt", false},
		// uint64 argument: matches a uint64 pattern, and is not confused with int64.
		{"strconv FormatUint(uint64, int)", "FormatUint", true},
		{"strconv FormatUint(int64, int)", "FormatUint", false},
		// int argument: matches an int pattern, not an int64 pattern.
		{"strconv Itoa(int)", "Itoa", true},
		{"strconv Itoa(int64)", "Itoa", false},
	}

	for _, tc := range cases {
		mi, ok := calls[tc.call]
		require.Truef(t, ok, "no invocation of %q found in parsed tree", tc.call)
		if got := matcher.NewMethodMatcher(tc.pattern).Matches(mi); got != tc.want {
			t.Errorf("NewMethodMatcher(%q).Matches(%s call) = %v, want %v", tc.pattern, tc.call, got, tc.want)
		}
	}
}
