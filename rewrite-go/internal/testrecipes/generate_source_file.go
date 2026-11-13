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
	"path"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// GenerateSourceFile is a test recipe used to validate generated files over the RPC path.
type GenerateSourceFile struct {
	recipe.ScanningBase
	SourcePath string
	Text       string
}

func (r *GenerateSourceFile) Name() string        { return "org.openrewrite.golang.test.GenerateSourceFile" }
func (r *GenerateSourceFile) DisplayName() string { return "Generate a source file (test)" }
func (r *GenerateSourceFile) Description() string {
	return "Test recipe that generates a Go or `go.mod` file, parsed according to its name, unless it already exists."
}

func (r *GenerateSourceFile) Options() []recipe.OptionDescriptor {
	return []recipe.OptionDescriptor{
		recipe.Option("sourcePath", "Source path", "The source path of the file to create.").
			WithExample("doc.go").WithValue(r.SourcePath),
		recipe.Option("text", "Text", "The content of the file to create.").
			WithExample("package main\n").WithValue(r.Text),
	}
}

type generateSourceFileAcc struct {
	exists bool
}

func (r *GenerateSourceFile) InitialValue(_ *recipe.ExecutionContext) any {
	return &generateSourceFileAcc{}
}

func (r *GenerateSourceFile) Scanner(acc any) recipe.TreeVisitor {
	return scanVisitor(func(t java.Tree) {
		if sourcePathOf(t) == r.SourcePath {
			acc.(*generateSourceFileAcc).exists = true
		}
	})
}

func (r *GenerateSourceFile) Generate(acc any, _ *recipe.ExecutionContext) []java.Tree {
	if acc.(*generateSourceFileAcc).exists {
		return nil
	}
	return parseSourceFile(r.SourcePath, r.Text)
}

type scanVisitor func(t java.Tree)

func (f scanVisitor) Visit(t java.Tree, _ any) java.Tree {
	f(t)
	return t
}

func sourcePathOf(t java.Tree) string {
	if sp, ok := t.(interface{ GetSourcePath() string }); ok {
		return sp.GetSourcePath()
	}
	return ""
}

func printSourceFile(t java.Tree) string {
	if gm, ok := t.(*golang.GoMod); ok {
		return printer.PrintGoMod(gm)
	}
	return printer.Print(t)
}

func parseSourceFile(sourcePath, text string) []java.Tree {
	if path.Base(sourcePath) == "go.mod" {
		gm, err := parser.ParseGoModFile(sourcePath, text)
		if err != nil {
			return nil
		}
		return []java.Tree{gm}
	}
	cu, err := parser.NewGoParser().Parse(sourcePath, text)
	if err != nil {
		return nil
	}
	return []java.Tree{cu}
}
