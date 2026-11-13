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

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// renameWhileScanning renames a to b, scanning with the visitor it edits with.
type renameWhileScanning struct {
	recipe.ScanningBase
}

func (r *renameWhileScanning) Name() string {
	return "org.openrewrite.golang.test.RenameWhileScanning"
}
func (r *renameWhileScanning) DisplayName() string { return "Rename while scanning" }
func (r *renameWhileScanning) Description() string {
	return "Renames `a` to `b`, scanning with the visitor it edits with."
}

func (r *renameWhileScanning) InitialValue(*recipe.ExecutionContext) any { return nil }

func (r *renameWhileScanning) Scanner(acc any) recipe.TreeVisitor { return r.EditorWithData(acc) }

func (r *renameWhileScanning) EditorWithData(any) recipe.TreeVisitor {
	return visitor.Init(&identifierRenamer{recipe: &renameIdentifiers{rename: renameAToB}})
}

// deleteWhileScanning scans with a visitor that deletes a file.
type deleteWhileScanning struct {
	recipe.ScanningBase
	path string
}

func (r *deleteWhileScanning) Name() string {
	return "org.openrewrite.golang.test.DeleteWhileScanning"
}
func (r *deleteWhileScanning) DisplayName() string { return "Delete while scanning" }
func (r *deleteWhileScanning) Description() string {
	return "Scans with a visitor that deletes the source file at a path."
}

func (r *deleteWhileScanning) InitialValue(*recipe.ExecutionContext) any { return nil }

func (r *deleteWhileScanning) Scanner(any) recipe.TreeVisitor {
	return visitor.Init(&fileDeleter{path: r.path})
}

func TestScannerChangingASourceFileFails(t *testing.T) {
	// given a scanning recipe that scans with its editing visitor
	spec := NewRecipeSpec().WithRecipe(&renameWhileScanning{})

	// when it runs
	got := failures(t, spec, Golang(`
		package main

		func a() {}
	`, `
		package main

		func b() {}
	`))

	// then the scanner's edit is reported
	assert.Equal(t, []string{
		"Record what the scanner finds in the accumulator, and make the edits in EditorWithData(acc), " +
			"since the scanning phase discards them:\n" +
			"  org.openrewrite.golang.test.RenameWhileScanning's scanner changed test.go\n" +
			"A scanner that reuses an editing visitor opts out with spec.CheckNoEditsInScanningPhase = false.",
	}, got)
}

func TestScannerDeletingASourceFileFails(t *testing.T) {
	// given a scanning recipe whose scanner deletes a file
	spec := NewRecipeSpec().WithRecipe(&deleteWhileScanning{path: "a.go"})

	// when it runs
	got := failures(t, spec,
		Golang(`
			package main
		`).WithPath("a.go"),
		Golang(`
			package main
		`).WithPath("b.go"),
	)

	// then the deletion is reported, and only for the file the scanner deleted
	assert.Equal(t, []string{
		"Record what the scanner finds in the accumulator, and make the edits in EditorWithData(acc), " +
			"since the scanning phase discards them:\n" +
			"  org.openrewrite.golang.test.DeleteWhileScanning's scanner deleted a.go\n" +
			"A scanner that reuses an editing visitor opts out with spec.CheckNoEditsInScanningPhase = false.",
	}, got)
}

func TestScannerThatOnlyRecordsPasses(t *testing.T) {
	NewRecipeSpec().
		WithRecipe(&reportSawA{}).
		RewriteRun(t, Golang(`
			package main

			func a() {}
		`, `
			/*~~(saw a: true)~~>*/package main

			func a() {}
		`))
}

func TestScannerEditCheckCanBeTurnedOff(t *testing.T) {
	spec := NewRecipeSpec().WithRecipe(&renameWhileScanning{})
	spec.CheckNoEditsInScanningPhase = false
	spec.RewriteRun(t, Golang(`
		package main

		func a() {}
	`, `
		package main

		func b() {}
	`))
}
