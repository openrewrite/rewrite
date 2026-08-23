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

package template

import (
	"fmt"
	"go/types"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// ScaffoldKind indicates what kind of Go construct the template represents.
type ScaffoldKind int

const (
	ScaffoldExpression ScaffoldKind = iota // wraps in: var __v__ = <code>
	ScaffoldStatement                      // wraps in: func __f__() { <code> }
	ScaffoldTopLevel                       // wraps in: package __p__; <code>
)

// buildPreamble generates variable/type declarations for captures so that
// go/parser can resolve placeholder identifiers in the scaffold source.
func buildPreamble(captures map[string]*Capture) string {
	if len(captures) == 0 {
		return ""
	}
	var lines []string
	for _, cap := range captures {
		ph := cap.Placeholder()
		switch cap.Kind() {
		case CaptureExpression, CaptureStatement:
			typeName := "any"
			if cap.TypeName() != "" {
				typeName = cap.TypeName()
			}
			lines = append(lines, fmt.Sprintf("var %s %s", ph, typeName))
		case CaptureType:
			lines = append(lines, fmt.Sprintf("type %s = any", ph))
		case CaptureName:
			// No preamble needed for name captures — they appear as identifiers
			// and will be matched structurally.
		}
	}
	return strings.Join(lines, "\n")
}

// buildScaffold wraps the template code in a compilable Go source so that
// go/parser can parse it, and reports how many statements precede the target
// inside the wrapper function.
//
// At the top level the target comes first, Go resolving a name declared after
// its use. A function body resolves no such name, so a statement scaffold
// declares its captures ahead of the code and counts them.
func buildScaffold(code string, captures map[string]*Capture, imports []string, context []string, kind ScaffoldKind) (string, int) {
	preamble := buildPreamble(captures)
	preambleCount := 0
	if preamble != "" {
		preambleCount = strings.Count(preamble, "\n") + 1
	}

	var importBlock string
	if len(imports) > 0 {
		var importLines []string
		for _, imp := range imports {
			importLines = append(importLines, fmt.Sprintf("\t%q", imp))
		}
		importBlock = "import (\n" + strings.Join(importLines, "\n") + "\n)\n"
	}

	// A declaration between the target and what follows it marks where the
	// template code ends, so code declaring nothing is told apart from code
	// whose declaration is the context's first.
	trailing := strings.Join(append([]string{scaffoldEnd}, append(append([]string{}, context...), preamble)...), "\n")

	switch kind {
	case ScaffoldExpression:
		return fmt.Sprintf("package __tmpl__\n%s\nvar __v__ = %s\n%s\n", importBlock, code, trailing), 0
	case ScaffoldStatement:
		body := ""
		if preamble != "" {
			body = preamble + "\n"
		}
		body += code
		contextBlock := strings.Join(context, "\n")
		if contextBlock != "" {
			contextBlock += "\n"
		}
		return fmt.Sprintf("package __tmpl__\n%s\n%sfunc __f__() {\n%s\n}\n", importBlock, contextBlock, body), preambleCount
	case ScaffoldTopLevel:
		return fmt.Sprintf("package __tmpl__\n%s\n%s\n%s\n", importBlock, code, trailing), 0
	default:
		panic(fmt.Sprintf("unknown scaffold kind: %d", kind))
	}
}

// parseScaffold parses the scaffold source and extracts the target node,
// skipping the package declaration, imports, and preamble variables.
func parseScaffold(code string, captures map[string]*Capture, imports, context []string, kind ScaffoldKind, imp types.Importer) (java.J, error) {
	source, precedingCount := buildScaffold(code, captures, imports, context, kind)

	p := parser.NewGoParser()
	if imp != nil {
		p.Importer = imp
	}
	cu, err := p.Parse("__template__.go", source)
	if err != nil {
		return nil, fmt.Errorf("template parse error: %w\nsource:\n%s", err, source)
	}

	return extractTarget(cu, kind, precedingCount)
}

// extractTarget navigates the parsed CompilationUnit to find the template node.
func extractTarget(cu *golang.CompilationUnit, kind ScaffoldKind, preceding int) (java.J, error) {
	stmts := cu.Statements

	switch kind {
	case ScaffoldExpression:
		// The target is the initializer of the one variable the scaffold
		// names __v__; context and preamble declare the others.
		for _, stmt := range stmts {
			vd, ok := stmt.Element.(*java.VariableDeclarations)
			if !ok || len(vd.Variables) == 0 || vd.Variables[0].Element.Name.Name != "__v__" {
				continue
			}
			init := vd.Variables[0].Element.Initializer
			if init == nil {
				return nil, fmt.Errorf("expression scaffold: variable has no initializer")
			}
			return init.Element, nil
		}
		return nil, fmt.Errorf("expression scaffold: could not find __v__ declaration")

	case ScaffoldStatement:
		// Statements: [func __f__() { preamble...; <target> }]
		// The func is the first (and only) top-level statement after preamble decls.
		// Find the function declaration.
		for _, stmt := range stmts {
			md, ok := stmt.Element.(*java.MethodDeclaration)
			if !ok {
				continue
			}
			if md.Name.Name != "__f__" || md.Body == nil {
				continue
			}
			bodyStmts := md.Body.Statements
			targetIdx := preceding
			if targetIdx >= len(bodyStmts) {
				return nil, fmt.Errorf("statement scaffold: expected body statement at index %d, got %d", targetIdx, len(bodyStmts))
			}
			return bodyStmts[targetIdx].Element, nil
		}
		return nil, fmt.Errorf("statement scaffold: could not find __f__ function")

	case ScaffoldTopLevel:
		if len(stmts) == 0 || declaresScaffoldEnd(stmts[0].Element) {
			return nil, fmt.Errorf("top-level scaffold: no declaration in the template code")
		}
		return stmts[0].Element, nil

	default:
		return nil, fmt.Errorf("unknown scaffold kind: %d", kind)
	}
}

// scaffoldEnd is the declaration the scaffold writes after the template code.
const scaffoldEnd = "type __end__ = int"

func declaresScaffoldEnd(stmt java.J) bool {
	decl, ok := stmt.(*golang.TypeDecl)
	return ok && decl.Name != nil && decl.Name.Name == "__end__"
}
