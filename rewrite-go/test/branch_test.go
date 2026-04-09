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

func TestParseBreakStatement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func loop() {
				for {
					break
				}
			}
		`))
}

func TestParseContinueStatement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func loop() {
				for {
					continue
				}
			}
		`))
}

func TestParseFallthroughStatement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func check(x int) {
				switch x {
				case 1:
					fallthrough
				default:
				}
			}
		`))
}

func TestParseLabeledBreak(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func loop() {
			outer:
				for {
					break outer
				}
			}
		`))
}

func TestParseGotoStatement(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func jump() {
				goto end
			end:
			}
		`))
}
