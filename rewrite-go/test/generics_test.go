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

package test

import (
	"testing"

	. "github.com/openrewrite/rewrite/rewrite-go/pkg/test"
)

func TestParseGenericFunc(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func Min[T int | float64](a, b T) T {
				if a < b {
					return a
				}
				return b
			}
		`))
}

func TestParseGenericStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Pair[T any] struct {
				First  T
				Second T
			}
		`))
}

func TestParseGenericInstantiation(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				p := Pair[int]{First: 1, Second: 2}
				use(p)
			}
		`))
}

func TestParseGenericConstraintInterface(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Number interface {
				int | int8 | int16 | int32 | int64 | float32 | float64
			}
		`))
}

func TestParseGenericFuncCall(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				result := Min[int](3, 5)
				use(result)
			}
		`))
}

func TestParseGenericMethodOnStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Stack[T any] struct {
				items []T
			}

			func (s *Stack[T]) Push(item T) {
				s.items = append(s.items, item)
			}
		`))
}

func TestParseGenericMultipleTypeParams(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func Map[T any, U any](s []T, f func(T) U) []U {
				result := make([]U, len(s))
				for i, v := range s {
					result[i] = f(v)
				}
				return result
			}
		`))
}

func TestParseTildeConstraint(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Signed interface {
				~int | ~int8 | ~int16 | ~int32 | ~int64
			}
		`))
}

func TestParseGenericsIntenseWhitespace(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func Map[T any,/*c1*/U ~int](s  []T) []U {
				var r  []U
				return/*c2*/r
			}
		`))
}
