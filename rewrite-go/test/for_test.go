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

func TestParseClassicForLoop(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func count() {
				for i := 0; i < 10; i++ {
				}
			}
		`))
}

func TestParseConditionOnlyForLoop(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func wait(done bool) {
				for !done {
				}
			}
		`))
}

func TestParseInfiniteForLoop(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func loop() {
				for {
				}
			}
		`))
}

func TestParseForRangeWithKeyValue(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func iterate(items []string) {
				for k, v := range items {
				}
			}
		`))
}

func TestParseForIntenseWhitespace(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func count(items []string) {
				for i := 0;/*c1*/i < 10; i++ {
					use(i)
				}
				for _, v := range items {
					_ =/*c2*/v
				}
			}
		`))
}
