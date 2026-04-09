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

func TestParseMethodReceiver(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func (s *Server) Start() {
			}
		`),
	)
}

func TestParseMethodReceiverWithReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func (s *Server) Name() string {
				return s.name
			}
		`),
	)
}

func TestParseMultipleReturnValues(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func divide(a, b int) (int, error) {
				return a / b, nil
			}
		`),
	)
}

func TestParseSingleParenthesizedReturn(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() (int) {
				return 1
			}
		`),
	)
}

func TestParseMultipleParams(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func add(x int, y int) int {
				return x + y
			}
		`),
	)
}

func TestParseValueReceiver(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func (p Point) Distance() float64 {
				return 0
			}
		`),
	)
}

func TestParseNamedReturnValues(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func divide(a, b int) (result int, err error) {
				return a / b, nil
			}
		`),
	)
}

func TestParseNamedReturnGrouped(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() (x, y int) {
				return 1, 2
			}
		`),
	)
}
