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

	. "github.com/openrewrite/rewrite/pkg/test"
)

func TestParseTypeAlias(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type MyInt int
		`),
	)
}

func TestParseTypeAliasEquals(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type MyInt = int
		`),
	)
}

func TestParseTypeStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Point struct {
				X int
				Y int
			}
		`),
	)
}

func TestParseTypeEmptyStruct(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Empty struct{}
		`),
	)
}

func TestParseTypeEmptyInterface(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Any interface{}
		`),
	)
}

func TestParseStructWithTag(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Person struct {
				Name string `+"`"+`json:"name"`+"`"+`
				Age  int    `+"`"+`json:"age"`+"`"+`
			}
		`),
	)
}

func TestParseStructWithEmbeddedType(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Server struct {
				Config
				Name string
			}
		`),
	)
}

func TestParseInterfaceWithMethods(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Reader interface {
				Read(p []byte) (int, error)
			}
		`),
	)
}

func TestParseInterfaceMultipleMethods(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type ReadWriter interface {
				Read(p []byte) (int, error)
				Write(p []byte) (int, error)
			}
		`),
	)
}

func TestParseGroupedTypeDecl(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type (
				MyInt   int
				MyFloat float64
			)
		`),
	)
}

func TestParseInterfaceWithEmbedded(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type ReadCloser interface {
				Reader
				Close() error
			}
		`),
	)
}

func TestParseStructGroupedFields(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Rect struct {
				X, Y int
				W, H int
			}
		`),
	)
}
