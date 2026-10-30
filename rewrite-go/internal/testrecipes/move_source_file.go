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

package testrecipes

import (
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// MoveSourceFile is a test recipe used to validate a file generated from a scanned file
// which is deleted in the same run, over the RPC path.
type MoveSourceFile struct {
	recipe.ScanningBase
	From string
	To   string
}

func (r *MoveSourceFile) Name() string        { return "org.openrewrite.golang.test.MoveSourceFile" }
func (r *MoveSourceFile) DisplayName() string { return "Move a source file (test)" }
func (r *MoveSourceFile) Description() string {
	return "Test recipe that generates a copy of a Go or `go.mod` file under a new source path and deletes the original."
}

func (r *MoveSourceFile) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("from", "From", "The source path of the file to move.").
			WithExample("old.go").WithValue(r.From),
		recipe.Option("to", "To", "The source path to move the file to.").
			WithExample("new.go").WithValue(r.To),
	}
}

type moveSourceFileAcc struct {
	text         *string
	targetExists bool
}

func (r *MoveSourceFile) InitialValue(_ *recipe.ExecutionContext) any {
	return &moveSourceFileAcc{}
}

func (r *MoveSourceFile) Scanner(acc any) recipe.TreeVisitor {
	return scanVisitor(func(t java.Tree) {
		a := acc.(*moveSourceFileAcc)
		switch sourcePathOf(t) {
		case r.From:
			text := printSourceFile(t)
			a.text = &text
		case r.To:
			a.targetExists = true
		}
	})
}

func (r *MoveSourceFile) Generate(acc any, _ *recipe.ExecutionContext) []java.Tree {
	a := acc.(*moveSourceFileAcc)
	if a.text == nil || a.targetExists {
		return nil
	}
	return parseSourceFile(r.To, *a.text)
}

func (r *MoveSourceFile) EditorWithData(_ any) recipe.TreeVisitor {
	return &deleteSourceFileVisitor{sourcePath: r.From}
}
