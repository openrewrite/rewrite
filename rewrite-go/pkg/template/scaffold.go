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
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
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
// go/parser can parse it. Returns the full source and the number of
// preamble statements to skip when extracting the target node.
func buildScaffold(code string, captures map[string]*Capture, imports []string, kind ScaffoldKind) (string, int) {
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

	switch kind {
	case ScaffoldExpression:
		body := ""
		if preamble != "" {
			body = preamble + "\n"
		}
		body += "var __v__ = " + code
		return fmt.Sprintf("package __tmpl__\n%s\n%s\n", importBlock, body), preambleCount
	case ScaffoldStatement:
		body := ""
		if preamble != "" {
			body = preamble + "\n"
		}
		body += code
		return fmt.Sprintf("package __tmpl__\n%s\nfunc __f__() {\n%s\n}\n", importBlock, body), preambleCount
	case ScaffoldTopLevel:
		body := ""
		if preamble != "" {
			body = preamble + "\n"
		}
		body += code
		return fmt.Sprintf("package __tmpl__\n%s\n%s\n", importBlock, body), preambleCount
	default:
		panic(fmt.Sprintf("unknown scaffold kind: %d", kind))
	}
}

// parseScaffold parses the scaffold source and extracts the target node,
// skipping the package declaration, imports, and preamble variables.
func parseScaffold(code string, captures map[string]*Capture, imports []string, kind ScaffoldKind) (tree.J, error) {
	source, preambleCount := buildScaffold(code, captures, imports, kind)

	p := parser.NewGoParser()
	cu, err := p.Parse("__template__.go", source)
	if err != nil {
		return nil, fmt.Errorf("template parse error: %w\nsource:\n%s", err, source)
	}

	return extractTarget(cu, kind, preambleCount)
}

// extractTarget navigates the parsed CompilationUnit to find the template node.
func extractTarget(cu *tree.CompilationUnit, kind ScaffoldKind, preambleCount int) (tree.J, error) {
	stmts := cu.Statements

	switch kind {
	case ScaffoldExpression:
		// Statements: [preamble vars...] [var __v__ = <expr>]
		targetIdx := preambleCount
		if targetIdx >= len(stmts) {
			return nil, fmt.Errorf("expression scaffold: expected statement at index %d, got %d statements", targetIdx, len(stmts))
		}
		vd, ok := stmts[targetIdx].Element.(*tree.VariableDeclarations)
		if !ok {
			return nil, fmt.Errorf("expression scaffold: expected VariableDeclarations, got %T", stmts[targetIdx].Element)
		}
		if len(vd.Variables) == 0 {
			return nil, fmt.Errorf("expression scaffold: no variables in declaration")
		}
		init := vd.Variables[0].Element.Initializer
		if init == nil {
			return nil, fmt.Errorf("expression scaffold: variable has no initializer")
		}
		return init.Element, nil

	case ScaffoldStatement:
		// Statements: [func __f__() { preamble...; <target> }]
		// The func is the first (and only) top-level statement after preamble decls.
		// Find the function declaration.
		for _, stmt := range stmts {
			md, ok := stmt.Element.(*tree.MethodDeclaration)
			if !ok {
				continue
			}
			if md.Name.Name != "__f__" || md.Body == nil {
				continue
			}
			bodyStmts := md.Body.Statements
			targetIdx := preambleCount
			if targetIdx >= len(bodyStmts) {
				return nil, fmt.Errorf("statement scaffold: expected body statement at index %d, got %d", targetIdx, len(bodyStmts))
			}
			return bodyStmts[targetIdx].Element, nil
		}
		return nil, fmt.Errorf("statement scaffold: could not find __f__ function")

	case ScaffoldTopLevel:
		// Statements: [preamble vars...] [<target>]
		targetIdx := preambleCount
		if targetIdx >= len(stmts) {
			return nil, fmt.Errorf("top-level scaffold: expected statement at index %d, got %d statements", targetIdx, len(stmts))
		}
		return stmts[targetIdx].Element, nil

	default:
		return nil, fmt.Errorf("unknown scaffold kind: %d", kind)
	}
}
