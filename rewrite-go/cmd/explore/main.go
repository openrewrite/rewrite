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

// explore parses a Go source file, type-checks it, and dumps the AST
// along with type information, comment associations, and whitespace
// positions. This is a learning tool for understanding how Go's compiler
// represents source code internally, which informs the OpenRewrite LST mapping.
package main

import (
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"go/types"
	"os"
	"reflect"
	"strings"
)

const sampleSource = `package main

import (
	"fmt"
	"strings"
)

// Greeter generates greeting messages.
type Greeter struct {
	Prefix string
}

// Greet returns a greeting for the given name.
func (g *Greeter) Greet(name string) string {
	// Trim whitespace from the name
	name = strings.TrimSpace(name)
	if name == "" {
		return g.Prefix + "World"
	}
	return fmt.Sprintf("%s%s", g.Prefix, name)
}

func main() {
	g := &Greeter{Prefix: "Hello, "}
	fmt.Println(g.Greet("Go"))
}
`

func main() {
	fset := token.NewFileSet()

	file, err := parser.ParseFile(fset, "sample.go", sampleSource, parser.ParseComments)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Parse error: %v\n", err)
		os.Exit(1)
	}

	// --- Section 1: AST dump with positions ---
	fmt.Println("=== AST Nodes ===")
	fmt.Println()
	depth := 0
	ast.Inspect(file, func(n ast.Node) bool {
		if n == nil {
			depth--
			return false
		}
		indent := strings.Repeat("  ", depth)
		typeName := reflect.TypeOf(n).Elem().Name()
		pos := fset.Position(n.Pos())
		end := fset.Position(n.End())
		fmt.Printf("%s%s [%d:%d - %d:%d]\n",
			indent, typeName,
			pos.Line, pos.Column,
			end.Line, end.Column,
		)
		depth++
		return true
	})

	// --- Section 2: Type checking ---
	fmt.Println()
	fmt.Println("=== Type Information ===")
	fmt.Println()

	conf := types.Config{Importer: nil} // nil importer: won't resolve imports but shows the API
	info := &types.Info{
		Types: make(map[ast.Expr]types.TypeAndValue),
		Defs:  make(map[*ast.Ident]types.Object),
		Uses:  make(map[*ast.Ident]types.Object),
	}

	// Type-check (will have errors due to nil importer, but populates info for local types)
	_, _ = conf.Check("main", fset, []*ast.File{file}, info)

	fmt.Println("-- Definitions --")
	for ident, obj := range info.Defs {
		if obj != nil {
			pos := fset.Position(ident.Pos())
			fmt.Printf("  %s at %d:%d → %s (%s)\n",
				ident.Name, pos.Line, pos.Column,
				obj.Type(), reflect.TypeOf(obj).Elem().Name(),
			)
		}
	}

	fmt.Println()
	fmt.Println("-- Uses --")
	for ident, obj := range info.Uses {
		pos := fset.Position(ident.Pos())
		fmt.Printf("  %s at %d:%d → %s (defined at %s)\n",
			ident.Name, pos.Line, pos.Column,
			obj.Type(), fset.Position(obj.Pos()),
		)
	}

	// --- Section 3: Comment associations ---
	fmt.Println()
	fmt.Println("=== Comment Map ===")
	fmt.Println()

	cmap := ast.NewCommentMap(fset, file, file.Comments)
	for node, groups := range cmap {
		typeName := reflect.TypeOf(node).Elem().Name()
		pos := fset.Position(node.Pos())
		fmt.Printf("  %s at %d:%d:\n", typeName, pos.Line, pos.Column)
		for _, group := range groups {
			for _, comment := range group.List {
				cpos := fset.Position(comment.Pos())
				fmt.Printf("    [%d:%d] %s\n", cpos.Line, cpos.Column, comment.Text)
			}
		}
	}

	// --- Section 4: Whitespace reconstruction ---
	fmt.Println()
	fmt.Println("=== Whitespace Between Tokens ===")
	fmt.Println()
	fmt.Println("Go's AST stores token positions (line:col), not whitespace.")
	fmt.Println("Whitespace must be reconstructed from the original source.")
	fmt.Println()

	src := []byte(sampleSource)
	tokenFile := fset.File(file.Pos())

	// Collect all positions in order by walking the AST
	var positions []struct {
		pos  token.Pos
		name string
	}

	ast.Inspect(file, func(n ast.Node) bool {
		if n == nil {
			return false
		}
		typeName := reflect.TypeOf(n).Elem().Name()
		positions = append(positions, struct {
			pos  token.Pos
			name string
		}{n.Pos(), typeName})
		return true
	})

	// Show whitespace gaps between consecutive node start positions
	fmt.Println("First 10 whitespace gaps between nodes:")
	shown := 0
	for i := 1; i < len(positions) && shown < 10; i++ {
		startOffset := tokenFile.Offset(positions[i-1].pos)
		endOffset := tokenFile.Offset(positions[i].pos)
		if endOffset > startOffset {
			between := string(src[startOffset:endOffset])
			// Only show gaps that contain whitespace beyond the node's own text
			if strings.ContainsAny(between, " \t\n") {
				fmt.Printf("  Between %s and %s:\n", positions[i-1].name, positions[i].name)
				fmt.Printf("    raw: %q\n", between)
				shown++
			}
		}
	}

	fmt.Println()
	fmt.Println("Key observations for OpenRewrite LST mapping:")
	fmt.Println("  1. ast.Node.Pos() gives the start position of a node's first token")
	fmt.Println("  2. ast.Node.End() gives the position just past the node's last token")
	fmt.Println("  3. Whitespace between Pos() and the previous node's End() is the 'prefix'")
	fmt.Println("  4. Comments are stored separately in ast.File.Comments")
	fmt.Println("  5. ast.CommentMap associates comments with their nearest AST node")
	fmt.Println("  6. To build Space objects, read src[prevEnd:nextPos] for each gap")
}
