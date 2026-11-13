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

// DeleteSourceFile is a test recipe used to validate deletions over the RPC path.
type DeleteSourceFile struct {
	recipe.Base
	SourcePath string
}

func (r *DeleteSourceFile) Name() string        { return "org.openrewrite.golang.test.DeleteSourceFile" }
func (r *DeleteSourceFile) DisplayName() string { return "Delete a source file (test)" }
func (r *DeleteSourceFile) Description() string {
	return "Test recipe that deletes the Go, `go.mod` or `go.sum` file at the given source path."
}

func (r *DeleteSourceFile) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("sourcePath", "Source path", "The source path of the file to delete.").
			WithExample("go.sum").WithValue(r.SourcePath),
	}
}

func (r *DeleteSourceFile) Editor() recipe.TreeVisitor {
	return &deleteSourceFileVisitor{sourcePath: r.SourcePath}
}

type deleteSourceFileVisitor struct {
	sourcePath string
}

func (v *deleteSourceFileVisitor) Visit(t java.Tree, _ any) java.Tree {
	if sourcePathOf(t) == v.sourcePath {
		return nil
	}
	return t
}
