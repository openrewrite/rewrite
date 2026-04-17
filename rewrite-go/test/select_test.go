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

func TestParseSelectWithDefault(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				select {
				case v := <-ch:
					use(v)
				default:
					wait()
				}
			}
		`),
	)
}

func TestParseSelectSend(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				select {
				case ch <- 1:
					done()
				}
			}
		`),
	)
}

func TestParseSelectIntenseWhitespace(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				select {
				case v :=/*c1*/<-ch:
					use(v)
				case ch  <- 1:
					done()
				/*c2*/default:
					wait()
				}
			}
		`),
	)
}
