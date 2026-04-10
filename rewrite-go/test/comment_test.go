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

func TestParseLineComment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			// hello returns a greeting
			func hello() string {
				return "hello"
			}
		`))
}

func TestParseBlockComment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			/* hello returns a greeting */
			func hello() string {
				return "hello"
			}
		`))
}

func TestParseTrailingLineComment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func hello() string {
				return "hello" // greeting
			}
		`))
}

func TestParseMultipleLineComments(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			// line one
			// line two
			func hello() {
			}
		`))
}

func TestParseCommentOnImport(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import (
				// standard library
				"fmt"
			)

			func hello() {
			}
		`))
}

func TestParseCommentInsideEmptyDelimiters(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import ( /* unused */ )

			type ( /* placeholder */ )

			var ( /* placeholder */ )

			type Client struct{}

			func (c *Client) Unsubscribe() {}

			type Migration struct {
				File string
			}

			func foo( /* deprecated */ ) {}

			func main() {
				c := &Client{}
				c.Unsubscribe( /* all */ )
				_ = &Migration{ /* auto detect file name */ }
			}
		`))
}

func TestParseMultilineBlockComment(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			/*
			 * hello returns a greeting
			 */
			func hello() string {
				return "hello"
			}
		`))
}
