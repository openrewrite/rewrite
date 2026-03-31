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

package recipe

import "github.com/openrewrite/rewrite/pkg/tree"

// ScanningRecipe extends Recipe with a two-phase scan-then-edit pattern.
// The first phase collects data across all source files into an accumulator,
// and the second phase uses that accumulated data to make transformations.
//
// Implementations use `any` for the accumulator type and cast internally:
//
//	type MyScanningRecipe struct {
//	    recipe.ScanningBase
//	}
//
//	func (r *MyScanningRecipe) InitialValue(ctx *ExecutionContext) any {
//	    return &myAccumulator{found: make(map[string]bool)}
//	}
//
//	func (r *MyScanningRecipe) Scanner(acc any) TreeVisitor {
//	    a := acc.(*myAccumulator)
//	    // return a visitor that populates a
//	}
type ScanningRecipe interface {
	Recipe

	// InitialValue returns the initial accumulator value for the scanning phase.
	InitialValue(ctx *ExecutionContext) any

	// Scanner returns a visitor that collects data into the accumulator
	// without making modifications. The visitor receives the ExecutionContext
	// as its second parameter.
	Scanner(acc any) TreeVisitor

	// EditorWithData returns a transformation visitor using accumulated data.
	// Returns nil if this scanning recipe only generates new files.
	EditorWithData(acc any) TreeVisitor

	// Generate creates new source files based on accumulated data.
	// Returns nil if no new files are generated.
	Generate(acc any, ctx *ExecutionContext) []tree.Tree
}

// ScanningBase provides default implementations for optional ScanningRecipe methods.
// Embed it in your scanning recipe struct.
type ScanningBase struct {
	Base
}

func (ScanningBase) Scanner(acc any) TreeVisitor                         { return nil }
func (ScanningBase) EditorWithData(acc any) TreeVisitor                  { return nil }
func (ScanningBase) Generate(acc any, ctx *ExecutionContext) []tree.Tree { return nil }
