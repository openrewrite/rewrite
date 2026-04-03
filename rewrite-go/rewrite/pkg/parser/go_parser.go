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

package parser

import (
	"fmt"
	"go/ast"
	"go/importer"
	"go/parser"
	"go/token"
	"go/types"
	"strings"

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// GoParser parses Go source code into OpenRewrite LST nodes.
type GoParser struct {
	// Importer resolves imported packages for type checking.
	// Defaults to importer.Default() which resolves stdlib packages.
	Importer types.Importer
}

func NewGoParser() *GoParser {
	return &GoParser{
		Importer: importer.Default(),
	}
}

// Parse parses the given Go source code and returns a CompilationUnit.
func (gp *GoParser) Parse(sourcePath string, source string) (*tree.CompilationUnit, error) {
	fset := token.NewFileSet()
	file, err := parser.ParseFile(fset, sourcePath, source, parser.ParseComments)
	if err != nil {
		return nil, fmt.Errorf("parse error: %w", err)
	}

	// Run type checking to populate type information
	typeInfo := &types.Info{
		Types:      make(map[ast.Expr]types.TypeAndValue),
		Defs:       make(map[*ast.Ident]types.Object),
		Uses:       make(map[*ast.Ident]types.Object),
		Selections: make(map[*ast.SelectorExpr]*types.Selection),
	}

	conf := types.Config{
		Importer: gp.Importer,
		// Don't fail on type errors — we want partial type info even when
		// imports can't be resolved (single-file mode).
		Error: func(err error) {},
	}

	// Determine package name from the parsed file
	pkgName := "main"
	if file.Name != nil {
		pkgName = file.Name.Name
	}

	// Type-check; errors are non-fatal (unresolvable imports are expected)
	_, _ = conf.Check(pkgName, fset, []*ast.File{file}, typeInfo)

	ctx := &parseContext{
		src:      []byte(source),
		fset:     fset,
		file:     fset.File(file.Pos()),
		astFile:  file,
		cursor:   0,
		typeInfo: typeInfo,
		mapper:   newTypeMapper(),
	}

	return ctx.mapFile(file, sourcePath), nil
}

// parseContext holds the state needed during AST-to-LST mapping.
type parseContext struct {
	src      []byte
	fset     *token.FileSet
	file     *token.File
	astFile  *ast.File
	cursor   int // current byte offset into src, tracks consumed positions
	typeInfo *types.Info
	mapper   *typeMapper
}

// prefix extracts the whitespace and comments between the current cursor
// position and the given token position.
func (ctx *parseContext) prefix(pos token.Pos) tree.Space {
	if !pos.IsValid() {
		return tree.EmptySpace
	}
	targetOffset := ctx.file.Offset(pos)
	if targetOffset <= ctx.cursor || targetOffset > len(ctx.src) {
		return tree.EmptySpace
	}
	raw := string(ctx.src[ctx.cursor:targetOffset])
	ctx.cursor = targetOffset
	return tree.ParseSpace(raw)
}

// skip advances the cursor past n bytes (for consuming keywords, operators, etc.)
func (ctx *parseContext) skip(n int) {
	ctx.cursor += n
}

// skipTo advances the cursor to the given position.
func (ctx *parseContext) skipTo(pos token.Pos) {
	if pos.IsValid() {
		off := ctx.file.Offset(pos)
		if off <= len(ctx.src) {
			ctx.cursor = off
		}
	}
}

// prefixAndSkip extracts the prefix before pos and advances past length bytes.
func (ctx *parseContext) prefixAndSkip(pos token.Pos, length int) tree.Space {
	space := ctx.prefix(pos)
	ctx.skip(length)
	return space
}

// mapFile maps an ast.File to a CompilationUnit.
func (ctx *parseContext) mapFile(file *ast.File, sourcePath string) *tree.CompilationUnit {
	// "package" keyword
	prefix := ctx.prefixAndSkip(file.Package, len("package"))

	// Package name identifier
	pkgName := ctx.mapIdent(file.Name)
	paddedPkgName := tree.RightPadded[*tree.Identifier]{Element: pkgName}

	// Imports
	var imports *tree.Container[*tree.Import]
	if len(file.Imports) > 0 {
		imports = ctx.mapImports(file)
	}

	// Top-level declarations (functions, types, vars, consts - excluding imports)
	var stmts []tree.RightPadded[tree.Statement]
	for _, decl := range file.Decls {
		if gd, ok := decl.(*ast.GenDecl); ok && gd.Tok == token.IMPORT {
			continue
		}
		stmt := ctx.mapDecl(decl)
		if stmt != nil {
			stmts = append(stmts, tree.RightPadded[tree.Statement]{Element: stmt})
		}
	}

	// EOF
	eof := tree.EmptySpace
	if ctx.cursor < len(ctx.src) {
		eof = tree.ParseSpace(string(ctx.src[ctx.cursor:]))
	}

	return &tree.CompilationUnit{
		ID:          uuid.New(),
		Prefix:      prefix,
		SourcePath:  sourcePath,
		PackageDecl: &paddedPkgName,
		Imports:     imports,
		Statements:  stmts,
		EOF:         eof,
	}
}

// mapImports maps all import declarations in the file into a single Container.
// Go allows multiple import blocks; subsequent blocks are tracked via ImportBlock markers.
func (ctx *parseContext) mapImports(file *ast.File) *tree.Container[*tree.Import] {
	// Collect all import GenDecls in order.
	var importDecls []*ast.GenDecl
	for _, decl := range file.Decls {
		if gd, ok := decl.(*ast.GenDecl); ok && gd.Tok == token.IMPORT {
			importDecls = append(importDecls, gd)
		}
	}
	if len(importDecls) == 0 {
		return nil
	}

	var elements []tree.RightPadded[*tree.Import]
	var containerMarkers tree.Markers
	prevGrouped := false

	// First import block: captured into Container.Before and Container.Markers
	first := importDecls[0]
	before := ctx.prefixAndSkip(first.Pos(), len("import"))

	if first.Lparen.IsValid() {
		prevGrouped = true
		openParenPrefix := ctx.prefix(first.Lparen)
		ctx.skip(1) // skip "("
		containerMarkers = tree.Markers{
			ID: uuid.New(),
			Entries: []tree.Marker{
				tree.GroupedImport{Ident: uuid.New(), Before: openParenPrefix},
			},
		}
	}

	for _, spec := range first.Specs {
		is := spec.(*ast.ImportSpec)
		imp := ctx.mapImportSpec(is)
		elements = append(elements, tree.RightPadded[*tree.Import]{Element: imp})
	}

	if first.Lparen.IsValid() {
		closeParen := ctx.prefix(first.Rparen)
		ctx.skip(1) // skip ")"
		if len(elements) > 0 {
			elements[len(elements)-1].After = closeParen
		}
	}

	// Subsequent import blocks: attach ImportBlock marker to first import of each
	for _, importDecl := range importDecls[1:] {
		blockBefore := ctx.prefixAndSkip(importDecl.Pos(), len("import"))
		grouped := importDecl.Lparen.IsValid()
		var groupedBefore tree.Space
		if grouped {
			groupedBefore = ctx.prefix(importDecl.Lparen)
			ctx.skip(1) // skip "("
		}

		ctx.mapImportBlockSpecs(importDecl, &elements, tree.ImportBlock{
			Ident:         uuid.New(),
			ClosePrevious: prevGrouped,
			Before:        blockBefore,
			Grouped:       grouped,
			GroupedBefore: groupedBefore,
		})

		if grouped {
			closeParen := ctx.prefix(importDecl.Rparen)
			ctx.skip(1) // skip ")"
			if len(elements) > 0 {
				elements[len(elements)-1].After = closeParen
			}
		}
		prevGrouped = grouped
	}

	container := tree.Container[*tree.Import]{Before: before, Elements: elements, Markers: containerMarkers}
	return &container
}

// mapImportBlockSpecs maps the specs of a subsequent import block, attaching
// the ImportBlock marker to the first spec's Import node.
func (ctx *parseContext) mapImportBlockSpecs(decl *ast.GenDecl, elements *[]tree.RightPadded[*tree.Import], marker tree.ImportBlock) {
	for j, spec := range decl.Specs {
		is := spec.(*ast.ImportSpec)
		imp := ctx.mapImportSpec(is)
		if j == 0 {
			imp.Markers = tree.Markers{
				ID:      uuid.New(),
				Entries: []tree.Marker{marker},
			}
		}
		*elements = append(*elements, tree.RightPadded[*tree.Import]{Element: imp})
	}
}

// mapImportSpec maps a single import spec.
func (ctx *parseContext) mapImportSpec(spec *ast.ImportSpec) *tree.Import {
	prefix := ctx.prefix(spec.Pos())

	var alias *tree.LeftPadded[*tree.Identifier]
	if spec.Name != nil {
		ident := ctx.mapIdent(spec.Name)
		lp := tree.LeftPadded[*tree.Identifier]{Element: ident}
		alias = &lp
	}

	path := ctx.mapBasicLit(spec.Path)

	return &tree.Import{ID: uuid.New(), Prefix: prefix, Qualid: path, Alias: alias}
}

// mapDecl maps a top-level declaration.
func (ctx *parseContext) mapDecl(decl ast.Decl) tree.Statement {
	switch d := decl.(type) {
	case *ast.FuncDecl:
		return ctx.mapFuncDecl(d)
	case *ast.GenDecl:
		return ctx.mapGenDecl(d)
	default:
		return nil
	}
}

// mapGenDecl maps a general declaration (var, const, type).
func (ctx *parseContext) mapGenDecl(decl *ast.GenDecl) tree.Statement {
	switch decl.Tok {
	case token.VAR, token.CONST:
		return ctx.mapVarConstDecl(decl)
	case token.TYPE:
		return ctx.mapTypeDecl(decl)
	default:
		return nil
	}
}

// mapVarConstDecl maps `var x int`, `var x = 5`, `const x = 5`, etc.
func (ctx *parseContext) mapVarConstDecl(decl *ast.GenDecl) tree.Statement {
	prefix := ctx.prefix(decl.Pos())
	keyword := decl.Tok.String()
	ctx.skip(len(keyword))

	if len(decl.Specs) == 1 && !decl.Lparen.IsValid() {
		// Single declaration: var x int = 5
		spec := decl.Specs[0].(*ast.ValueSpec)
		return ctx.mapValueSpec(spec, prefix, keyword)
	}

	// Grouped declaration: var ( ... ) or const ( ... )
	lparenPrefix := ctx.prefix(decl.Lparen)
	ctx.skip(1) // "("

	var elements []tree.RightPadded[tree.Statement]
	for _, s := range decl.Specs {
		spec := s.(*ast.ValueSpec)
		innerPrefix := ctx.prefix(spec.Pos())
		vd := ctx.mapValueSpec(spec, innerPrefix, keyword)
		vd.Markers.Entries = append(vd.Markers.Entries, tree.GroupedSpec{Ident: uuid.New()})
		elements = append(elements, tree.RightPadded[tree.Statement]{Element: vd})
	}

	rparenPrefix := ctx.prefix(decl.Rparen)
	ctx.skip(1) // ")"

	if len(elements) > 0 {
		elements[len(elements)-1].After = rparenPrefix
	}

	var markerEntries []tree.Marker
	if keyword == "var" {
		markerEntries = append(markerEntries, tree.VarKeyword{Ident: uuid.New()})
	} else if keyword == "const" {
		markerEntries = append(markerEntries, tree.ConstDecl{Ident: uuid.New()})
	}

	specs := &tree.Container[tree.Statement]{Before: lparenPrefix, Elements: elements}
	return &tree.VariableDeclarations{
		ID:      uuid.New(),
		Prefix:  prefix,
		Markers: tree.Markers{ID: uuid.New(), Entries: markerEntries},
		Specs:   specs,
	}
}

// mapValueSpec maps a single var/const spec.
func (ctx *parseContext) mapValueSpec(spec *ast.ValueSpec, prefix tree.Space, keyword string) *tree.VariableDeclarations {
	// Source order: keyword name[, name]... [type] [= value]
	// Map names first (they appear first in source after keyword)
	// Handle commas between multiple names
	var nameIdents []*tree.Identifier
	var nameAfters []tree.Space
	for i, name := range spec.Names {
		nameIdents = append(nameIdents, ctx.mapIdent(name))
		if i < len(spec.Names)-1 {
			// Capture space before comma and skip comma
			commaOff := ctx.findNext(',')
			var after tree.Space
			if commaOff >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOff))
				ctx.skip(1) // ","
			}
			nameAfters = append(nameAfters, after)
		} else {
			nameAfters = append(nameAfters, tree.Space{})
		}
	}

	// Then type (appears after name in source)
	var typeExpr tree.Expression
	if spec.Type != nil {
		typeExpr = ctx.mapTypeExpr(spec.Type)
	}

	// Then initializers (after type and `=` in source)
	// In Go, multi-value declarations use a single `=` followed by comma-separated values:
	//   var a, b = val1, val2
	var variables []tree.RightPadded[*tree.VariableDeclarator]
	for i, nameIdent := range nameIdents {
		var init *tree.LeftPadded[tree.Expression]
		if i < len(spec.Values) {
			var sepPrefix tree.Space
			if i == 0 {
				eqOff := ctx.findNext('=')
				if eqOff >= 0 {
					sepPrefix = ctx.prefix(ctx.file.Pos(eqOff))
					ctx.skip(1) // "="
				}
			} else {
				commaOff := ctx.findNext(',')
				if commaOff >= 0 {
					sepPrefix = ctx.prefix(ctx.file.Pos(commaOff))
					ctx.skip(1) // ","
				}
			}
			val := ctx.mapExpr(spec.Values[i])
			lp := tree.LeftPadded[tree.Expression]{Before: sepPrefix, Element: val}
			init = &lp
		}

		vd := &tree.VariableDeclarator{
			ID:          uuid.New(),
			Name:        nameIdent,
			Initializer: init,
		}
		variables = append(variables, tree.RightPadded[*tree.VariableDeclarator]{Element: vd, After: nameAfters[i]})
	}

	var markerEntries []tree.Marker
	if keyword == "var" {
		markerEntries = append(markerEntries, tree.VarKeyword{Ident: uuid.New()})
	} else if keyword == "const" {
		markerEntries = append(markerEntries, tree.ConstDecl{Ident: uuid.New()})
	}
	var markers tree.Markers
	if len(markerEntries) > 0 {
		markers = tree.Markers{ID: uuid.New(), Entries: markerEntries}
	}

	return &tree.VariableDeclarations{
		ID:        uuid.New(),
		Prefix:    prefix,
		Markers:   markers,
		TypeExpr:  typeExpr,
		Variables: variables,
	}
}

// mapTypeDecl maps a `type Name ...` declaration.
func (ctx *parseContext) mapTypeDecl(decl *ast.GenDecl) tree.Statement {
	prefix := ctx.prefixAndSkip(decl.Pos(), len("type"))

	if len(decl.Specs) == 1 && !decl.Lparen.IsValid() {
		spec := decl.Specs[0].(*ast.TypeSpec)
		return ctx.mapTypeSpec(spec, prefix)
	}

	// Grouped type declaration: type ( ... )
	lparenPrefix := ctx.prefix(decl.Lparen)
	ctx.skip(1) // "("

	var elements []tree.RightPadded[tree.Statement]
	for _, s := range decl.Specs {
		spec := s.(*ast.TypeSpec)
		innerPrefix := ctx.prefix(spec.Pos())
		td := ctx.mapTypeSpec(spec, innerPrefix)
		td.Markers.Entries = append(td.Markers.Entries, tree.GroupedSpec{Ident: uuid.New()})
		elements = append(elements, tree.RightPadded[tree.Statement]{Element: td})
	}

	rparenPrefix := ctx.prefix(decl.Rparen)
	ctx.skip(1) // ")"

	if len(elements) > 0 {
		elements[len(elements)-1].After = rparenPrefix
	}

	specs := &tree.Container[tree.Statement]{Before: lparenPrefix, Elements: elements}
	return &tree.TypeDecl{
		ID:     uuid.New(),
		Prefix: prefix,
		Specs:  specs,
	}
}

// mapTypeSpec maps a single type spec: `type Name Type` or `type Name = Type`.
func (ctx *parseContext) mapTypeSpec(spec *ast.TypeSpec, prefix tree.Space) *tree.TypeDecl {
	name := ctx.mapIdent(spec.Name)

	var assign *tree.LeftPadded[tree.Space]
	if spec.Assign.IsValid() {
		eqPrefix := ctx.prefix(spec.Assign)
		ctx.skip(1) // "="
		lp := tree.LeftPadded[tree.Space]{Before: eqPrefix}
		assign = &lp
	}

	def := ctx.mapTypeExpr(spec.Type)

	return &tree.TypeDecl{
		ID:         uuid.New(),
		Prefix:     prefix,
		Name:       name,
		Assign:     assign,
		Definition: def,
	}
}

// mapFuncDecl maps a function declaration.
func (ctx *parseContext) mapFuncDecl(decl *ast.FuncDecl) *tree.MethodDeclaration {
	prefix := ctx.prefixAndSkip(decl.Pos(), len("func"))

	var receiver *tree.Container[tree.Statement]
	if decl.Recv != nil && len(decl.Recv.List) > 0 {
		recv := ctx.mapFieldListAsParams(decl.Recv)
		receiver = &recv
	}

	name := ctx.mapIdent(decl.Name)
	params := ctx.mapFieldListAsParams(decl.Type.Params)
	returnType := ctx.mapReturnType(decl.Type.Results)

	var body *tree.Block
	if decl.Body != nil {
		body = ctx.mapBlockStmt(decl.Body)
	}

	md := &tree.MethodDeclaration{
		ID:         uuid.New(),
		Prefix:     prefix,
		Receiver:   receiver,
		Name:       name,
		Parameters: params,
		ReturnType: returnType,
		Body:       body,
	}

	// Type attribution for method declaration
	if obj, ok := ctx.typeInfo.Defs[decl.Name]; ok && obj != nil {
		if fn, ok := obj.(*types.Func); ok {
			md.MethodType = ctx.mapper.mapMethodObject(fn)
		}
	}

	return md
}

// mapReturnType maps function return types.
// Returns nil for no return, a single Expression for one type, or a TypeList for multiple.
// Handles both unnamed `(int, error)` and named `(n int, err error)` returns.
func (ctx *parseContext) mapReturnType(results *ast.FieldList) tree.Expression {
	if results == nil || len(results.List) == 0 {
		return nil
	}

	// Check if return types are parenthesized
	if results.Opening.IsValid() {
		// Parenthesized: `(int, error)`, `(int)`, or `(n int, err error)`
		before := ctx.prefix(results.Opening)
		ctx.skip(1) // "("

		var elements []tree.RightPadded[tree.Statement]
		for i, field := range results.List {
			if len(field.Names) == 0 {
				// Unnamed return: just a type expression
				typeExpr := ctx.mapTypeExpr(field.Type)
				vd := &tree.VariableDeclarations{
					ID:       uuid.New(),
					TypeExpr: typeExpr,
					Variables: []tree.RightPadded[*tree.VariableDeclarator]{
						{Element: &tree.VariableDeclarator{ID: uuid.New(), Name: &tree.Identifier{ID: uuid.New()}}},
					},
				}
				var after tree.Space
				if i < len(results.List)-1 {
					commaOffset := ctx.findNext(',')
					if commaOffset >= 0 {
						after = ctx.prefix(ctx.file.Pos(commaOffset))
						ctx.skip(1) // ","
					}
				}
				elements = append(elements, tree.RightPadded[tree.Statement]{Element: vd, After: after})
			} else {
				// Named return(s): `n int` or `x, y int`
				var vars []tree.RightPadded[*tree.VariableDeclarator]
				for j, fieldName := range field.Names {
					nameIdent := ctx.mapIdent(fieldName)
					var nameAfter tree.Space
					if j < len(field.Names)-1 {
						commaOffset := ctx.findNext(',')
						if commaOffset >= 0 {
							nameAfter = ctx.prefix(ctx.file.Pos(commaOffset))
							ctx.skip(1) // ","
						}
					}
					vars = append(vars, tree.RightPadded[*tree.VariableDeclarator]{
						Element: &tree.VariableDeclarator{ID: uuid.New(), Name: nameIdent},
						After:   nameAfter,
					})
				}
				typeExpr := ctx.mapTypeExpr(field.Type)
				vd := &tree.VariableDeclarations{
					ID:        uuid.New(),
					TypeExpr:  typeExpr,
					Variables: vars,
				}
				var after tree.Space
				if i < len(results.List)-1 {
					commaOffset := ctx.findNext(',')
					if commaOffset >= 0 {
						after = ctx.prefix(ctx.file.Pos(commaOffset))
						ctx.skip(1) // ","
					}
				}
				elements = append(elements, tree.RightPadded[tree.Statement]{Element: vd, After: after})
			}
		}

		closePrefix := ctx.prefix(results.Closing)
		ctx.skip(1) // ")"

		if len(elements) > 0 {
			elements[len(elements)-1].After = closePrefix
		}

		return &tree.TypeList{
			ID:    uuid.New(),
			Types: tree.Container[tree.Statement]{Before: before, Elements: elements},
		}
	}

	// Single non-parenthesized return type
	return ctx.mapTypeExpr(results.List[0].Type)
}

// mapFieldListAsParams maps function parameters.
// Handles named (x int), unnamed (int), and grouped (a, b int) parameters.
// Each ast.Field becomes one VariableDeclarations (possibly with multiple names).
func (ctx *parseContext) mapFieldListAsParams(fl *ast.FieldList) tree.Container[tree.Statement] {
	before := ctx.prefix(fl.Opening)
	ctx.skip(1) // "("

	var elements []tree.RightPadded[tree.Statement]
	for i, field := range fl.List {
		if len(field.Names) == 0 {
			// Unnamed parameter: just a type expression (e.g., `int` in `func(int)`)
			typeExpr, varargs := ctx.mapTypeExprVariadic(field.Type)
			vd := &tree.VariableDeclarations{
				ID:       uuid.New(),
				Varargs:  varargs,
				TypeExpr: typeExpr,
				Variables: []tree.RightPadded[*tree.VariableDeclarator]{
					{Element: &tree.VariableDeclarator{ID: uuid.New(), Name: &tree.Identifier{ID: uuid.New()}}},
				},
			}
			var after tree.Space
			if i < len(fl.List)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			elements = append(elements, tree.RightPadded[tree.Statement]{Element: vd, After: after})
		} else {
			// Named parameter(s): `a int` or `a, b int` (grouped names sharing a type)
			// Map all names first (source order), then the shared type
			var vars []tree.RightPadded[*tree.VariableDeclarator]
			for j, fieldName := range field.Names {
				nameIdent := ctx.mapIdent(fieldName)
				var nameAfter tree.Space
				if j < len(field.Names)-1 {
					commaOffset := ctx.findNext(',')
					if commaOffset >= 0 {
						nameAfter = ctx.prefix(ctx.file.Pos(commaOffset))
						ctx.skip(1) // ","
					}
				}
				vars = append(vars, tree.RightPadded[*tree.VariableDeclarator]{
					Element: &tree.VariableDeclarator{ID: uuid.New(), Name: nameIdent},
					After:   nameAfter,
				})
			}

			typeExpr, varargs := ctx.mapTypeExprVariadic(field.Type)
			vd := &tree.VariableDeclarations{
				ID:        uuid.New(),
				Varargs:   varargs,
				TypeExpr:  typeExpr,
				Variables: vars,
			}

			var after tree.Space
			if i < len(fl.List)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			elements = append(elements, tree.RightPadded[tree.Statement]{Element: vd, After: after})
		}
	}

	closeParen := ctx.prefix(fl.Closing)
	ctx.skip(1) // ")"

	if len(elements) > 0 {
		elements[len(elements)-1].After = closeParen
	}

	return tree.Container[tree.Statement]{Before: before, Elements: elements}
}

// mapBlockStmt maps a block statement.
func (ctx *parseContext) mapBlockStmt(block *ast.BlockStmt) *tree.Block {
	prefix := ctx.prefix(block.Lbrace)
	ctx.skip(1) // "{"

	var stmts []tree.RightPadded[tree.Statement]
	for _, stmt := range block.List {
		mapped := ctx.mapStmt(stmt)
		if mapped != nil {
			stmts = append(stmts, tree.RightPadded[tree.Statement]{Element: mapped})
		}
	}

	end := ctx.prefix(block.Rbrace)
	ctx.skip(1) // "}"

	return &tree.Block{ID: uuid.New(), Prefix: prefix, Statements: stmts, End: end}
}

// mapStmt maps a statement.
func (ctx *parseContext) mapStmt(stmt ast.Stmt) tree.Statement {
	switch s := stmt.(type) {
	case *ast.ReturnStmt:
		return ctx.mapReturnStmt(s)
	case *ast.AssignStmt:
		return ctx.mapAssignStmt(s)
	case *ast.ExprStmt:
		return ctx.mapExprStmt(s)
	case *ast.IfStmt:
		return ctx.mapIfStmt(s)
	case *ast.SwitchStmt:
		return ctx.mapSwitchStmt(s)
	case *ast.CaseClause:
		return ctx.mapCaseClause(s)
	case *ast.ForStmt:
		return ctx.mapForStmt(s)
	case *ast.RangeStmt:
		return ctx.mapRangeStmt(s)
	case *ast.IncDecStmt:
		return ctx.mapIncDecStmt(s)
	case *ast.GoStmt:
		return ctx.mapGoStmt(s)
	case *ast.DeferStmt:
		return ctx.mapDeferStmt(s)
	case *ast.SendStmt:
		return ctx.mapSendStmt(s)
	case *ast.BranchStmt:
		return ctx.mapBranchStmt(s)
	case *ast.LabeledStmt:
		return ctx.mapLabeledStmt(s)
	case *ast.BlockStmt:
		return ctx.mapBlockStmt(s)
	case *ast.DeclStmt:
		return ctx.mapDeclStmt(s)
	case *ast.SelectStmt:
		return ctx.mapSelectStmt(s)
	case *ast.TypeSwitchStmt:
		return ctx.mapTypeSwitchStmt(s)
	case *ast.CommClause:
		return ctx.mapCommClause(s)
	case *ast.EmptyStmt:
		return ctx.mapEmptyStmt(s)
	default:
		return nil
	}
}

// mapReturnStmt maps a return statement.
func (ctx *parseContext) mapReturnStmt(stmt *ast.ReturnStmt) *tree.Return {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("return"))

	var exprs []tree.RightPadded[tree.Expression]
	for i, expr := range stmt.Results {
		mapped := ctx.mapExpr(expr)
		var after tree.Space
		if i < len(stmt.Results)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		}
		exprs = append(exprs, tree.RightPadded[tree.Expression]{Element: mapped, After: after})
	}

	return &tree.Return{ID: uuid.New(), Prefix: prefix, Expressions: exprs}
}

// mapAssignStmt maps an assignment statement.
func (ctx *parseContext) mapAssignStmt(stmt *ast.AssignStmt) tree.Statement {
	// Check for compound assignment operators (+=, -=, etc.) — always single LHS/RHS
	if len(stmt.Lhs) == 1 && len(stmt.Rhs) == 1 {
		if op, ok := mapAssignmentOp(stmt.Tok); ok {
			lhs := ctx.mapExpr(stmt.Lhs[0])
			opPrefix := ctx.prefix(stmt.TokPos)
			ctx.skip(len(stmt.Tok.String()))
			rhs := ctx.mapExpr(stmt.Rhs[0])
			return &tree.AssignmentOperation{
				ID:         uuid.New(),
				Variable:   lhs,
				Operator:   tree.LeftPadded[tree.AssignmentOperator]{Before: opPrefix, Element: op},
				Assignment: rhs,
			}
		}
	}

	// Multi-value assignment: x, y = 1, 2 or x, y := f()
	if len(stmt.Lhs) > 1 || len(stmt.Rhs) > 1 {
		var lhsExprs []tree.RightPadded[tree.Expression]
		for i, expr := range stmt.Lhs {
			mapped := ctx.mapExpr(expr)
			var after tree.Space
			if i < len(stmt.Lhs)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			lhsExprs = append(lhsExprs, tree.RightPadded[tree.Expression]{Element: mapped, After: after})
		}

		opPrefix := ctx.prefix(stmt.TokPos)
		ctx.skip(len(stmt.Tok.String()))

		var rhsExprs []tree.RightPadded[tree.Expression]
		for i, expr := range stmt.Rhs {
			mapped := ctx.mapExpr(expr)
			var after tree.Space
			if i < len(stmt.Rhs)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			rhsExprs = append(rhsExprs, tree.RightPadded[tree.Expression]{Element: mapped, After: after})
		}

		var markers tree.Markers
		if stmt.Tok == token.DEFINE {
			markers = tree.Markers{
				ID:      uuid.New(),
				Entries: []tree.Marker{tree.ShortVarDecl{Ident: uuid.New()}},
			}
		}

		return &tree.MultiAssignment{
			ID:        uuid.New(),
			Markers:   markers,
			Variables: lhsExprs,
			Operator:  tree.LeftPadded[tree.Space]{Before: opPrefix},
			Values:    rhsExprs,
		}
	}

	// Single assignment: x = 1 or x := 1
	lhs := ctx.mapExpr(stmt.Lhs[0])
	opPrefix := ctx.prefix(stmt.TokPos)
	ctx.skip(len(stmt.Tok.String()))
	rhs := ctx.mapExpr(stmt.Rhs[0])

	var markers tree.Markers
	if stmt.Tok == token.DEFINE {
		markers = tree.Markers{
			ID:      uuid.New(),
			Entries: []tree.Marker{tree.ShortVarDecl{Ident: uuid.New()}},
		}
	}

	return &tree.Assignment{
		ID:       uuid.New(),
		Prefix:   tree.EmptySpace,
		Markers:  markers,
		Variable: lhs,
		Value:    tree.LeftPadded[tree.Expression]{Before: opPrefix, Element: rhs},
	}
}

func mapAssignmentOp(tok token.Token) (tree.AssignmentOperator, bool) {
	switch tok {
	case token.ADD_ASSIGN:
		return tree.AddAssign, true
	case token.SUB_ASSIGN:
		return tree.SubAssign, true
	case token.MUL_ASSIGN:
		return tree.MulAssign, true
	case token.QUO_ASSIGN:
		return tree.DivAssign, true
	case token.REM_ASSIGN:
		return tree.ModAssign, true
	case token.AND_ASSIGN:
		return tree.AndAssign, true
	case token.OR_ASSIGN:
		return tree.OrAssign, true
	case token.XOR_ASSIGN:
		return tree.XorAssign, true
	case token.SHL_ASSIGN:
		return tree.ShlAssign, true
	case token.SHR_ASSIGN:
		return tree.ShrAssign, true
	case token.AND_NOT_ASSIGN:
		return tree.AndNotAssign, true
	default:
		return 0, false
	}
}

// mapDeclStmt maps a declaration statement inside a function body.
func (ctx *parseContext) mapDeclStmt(stmt *ast.DeclStmt) tree.Statement {
	switch d := stmt.Decl.(type) {
	case *ast.GenDecl:
		return ctx.mapGenDecl(d)
	default:
		return nil
	}
}

// mapExprStmt maps an expression statement.
func (ctx *parseContext) mapExprStmt(stmt *ast.ExprStmt) tree.Statement {
	expr := ctx.mapExpr(stmt.X)
	if expr == nil {
		return nil
	}
	if s, ok := expr.(tree.Statement); ok {
		return s
	}
	return nil
}

// mapIfStmt maps an if statement, including optional init: `if init; cond { }`.
func (ctx *parseContext) mapIfStmt(stmt *ast.IfStmt) *tree.If {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("if"))

	var init *tree.RightPadded[tree.Statement]
	if stmt.Init != nil {
		initStmt := ctx.mapStmt(stmt.Init)
		semicolonOffset := ctx.findNext(';')
		var after tree.Space
		if semicolonOffset >= 0 {
			after = ctx.prefix(ctx.file.Pos(semicolonOffset))
			ctx.skip(1) // ";"
		}
		rp := tree.RightPadded[tree.Statement]{Element: initStmt, After: after}
		init = &rp
	}

	cond := ctx.mapExpr(stmt.Cond)
	body := ctx.mapBlockStmt(stmt.Body)

	var elsePart *tree.RightPadded[tree.J]
	if stmt.Else != nil {
		// Find the `else` keyword between the closing `}` of Then and the start of Else
		elseOff := ctx.findNextString("else")
		if elseOff >= 0 {
			elsePrefix := ctx.prefix(ctx.file.Pos(elseOff))
			ctx.skip(len("else"))
			elseBody := ctx.mapStmt(stmt.Else)
			if elseBody != nil {
				rp := tree.RightPadded[tree.J]{Element: elseBody, After: elsePrefix}
				elsePart = &rp
			}
		}
	}

	return &tree.If{
		ID:        uuid.New(),
		Prefix:    prefix,
		Init:      init,
		Condition: cond,
		Then:      body,
		ElsePart:  elsePart,
	}
}

// mapSwitchStmt maps a switch statement.
func (ctx *parseContext) mapSwitchStmt(stmt *ast.SwitchStmt) *tree.Switch {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("switch"))

	var init *tree.RightPadded[tree.Statement]
	if stmt.Init != nil {
		initStmt := ctx.mapStmt(stmt.Init)
		semicolonOffset := ctx.findNext(';')
		var after tree.Space
		if semicolonOffset >= 0 {
			after = ctx.prefix(ctx.file.Pos(semicolonOffset))
			ctx.skip(1) // ";"
		}
		rp := tree.RightPadded[tree.Statement]{Element: initStmt, After: after}
		init = &rp
	}

	var tag *tree.RightPadded[tree.Expression]
	if stmt.Tag != nil {
		tagExpr := ctx.mapExpr(stmt.Tag)
		rp := tree.RightPadded[tree.Expression]{Element: tagExpr}
		tag = &rp
	}

	body := ctx.mapBlockStmt(stmt.Body)

	return &tree.Switch{
		ID:      uuid.New(),
		Prefix:  prefix,
		Init:    init,
		Tag:     tag,
		Body:    body,
	}
}

// mapCaseClause maps a case or default clause.
func (ctx *parseContext) mapCaseClause(clause *ast.CaseClause) *tree.Case {
	prefix := ctx.prefix(clause.Pos())

	var exprs tree.Container[tree.Expression]
	if len(clause.List) > 0 {
		// case expr1, expr2:
		ctx.skip(len("case"))
		var elements []tree.RightPadded[tree.Expression]
		for i, expr := range clause.List {
			mapped := ctx.mapExpr(expr)
			var after tree.Space
			if i < len(clause.List)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			elements = append(elements, tree.RightPadded[tree.Expression]{Element: mapped, After: after})
		}
		exprs = tree.Container[tree.Expression]{Elements: elements}
	} else {
		// default:
		ctx.skip(len("default"))
	}

	// Skip the colon
	colonOffset := ctx.findNext(':')
	var colonPrefix tree.Space
	if colonOffset >= 0 {
		colonPrefix = ctx.prefix(ctx.file.Pos(colonOffset))
		ctx.skip(1) // ":"
	}

	// For case: last expression's After gets the space before colon
	// For default: exprs.Before gets the space before colon
	if len(exprs.Elements) > 0 {
		exprs.Elements[len(exprs.Elements)-1].After = colonPrefix
	} else {
		exprs.Before = colonPrefix
	}

	// Body statements
	var body []tree.RightPadded[tree.Statement]
	for _, stmt := range clause.Body {
		mapped := ctx.mapStmt(stmt)
		if mapped != nil {
			body = append(body, tree.RightPadded[tree.Statement]{Element: mapped})
		}
	}

	return &tree.Case{
		ID:          uuid.New(),
		Prefix:      prefix,
		Expressions: exprs,
		Body:        body,
	}
}

// mapSelectStmt maps a select statement, reusing Switch+Case with SelectStmt marker.
func (ctx *parseContext) mapSelectStmt(stmt *ast.SelectStmt) *tree.Switch {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("select"))
	body := ctx.mapBlockStmt(stmt.Body)

	return &tree.Switch{
		ID:     uuid.New(),
		Prefix: prefix,
		Markers: tree.Markers{
			ID:      uuid.New(),
			Entries: []tree.Marker{tree.SelectStmt{Ident: uuid.New()}},
		},
		Body: body,
	}
}

// mapCommClause maps a communication clause in a select statement.
func (ctx *parseContext) mapCommClause(clause *ast.CommClause) *tree.CommClause {
	prefix := ctx.prefix(clause.Pos())

	var comm tree.Statement
	if clause.Comm != nil {
		// case <-ch: or case ch <- val: or case v := <-ch:
		ctx.skip(len("case"))
		comm = ctx.mapStmt(clause.Comm)
	} else {
		// default:
		ctx.skip(len("default"))
	}

	// Skip the colon
	colonOffset := ctx.findNext(':')
	var colonPrefix tree.Space
	if colonOffset >= 0 {
		colonPrefix = ctx.prefix(ctx.file.Pos(colonOffset))
		ctx.skip(1)
	}

	var body []tree.RightPadded[tree.Statement]
	for _, stmt := range clause.Body {
		mapped := ctx.mapStmt(stmt)
		if mapped != nil {
			body = append(body, tree.RightPadded[tree.Statement]{Element: mapped})
		}
	}

	return &tree.CommClause{
		ID:     uuid.New(),
		Prefix: prefix,
		Comm:   comm,
		Colon:  colonPrefix,
		Body:   body,
	}
}

// mapTypeSwitchStmt maps a type switch statement.
// Uses Switch with TypeSwitchGuard marker. The assign/guard becomes the tag.
func (ctx *parseContext) mapTypeSwitchStmt(stmt *ast.TypeSwitchStmt) *tree.Switch {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("switch"))

	var init *tree.RightPadded[tree.Statement]
	if stmt.Init != nil {
		initStmt := ctx.mapStmt(stmt.Init)
		semicolonOffset := ctx.findNext(';')
		var after tree.Space
		if semicolonOffset >= 0 {
			after = ctx.prefix(ctx.file.Pos(semicolonOffset))
			ctx.skip(1)
		}
		rp := tree.RightPadded[tree.Statement]{Element: initStmt, After: after}
		init = &rp
	}

	// The assign is `x.(type)` (ExprStmt) or `v := x.(type)` (AssignStmt)
	var tag *tree.RightPadded[tree.Expression]
	switch a := stmt.Assign.(type) {
	case *ast.ExprStmt:
		// `x.(type)` — map the inner expression directly
		expr := ctx.mapExpr(a.X)
		if expr != nil {
			rp := tree.RightPadded[tree.Expression]{Element: expr}
			tag = &rp
		}
	case *ast.AssignStmt:
		// `v := x.(type)` — map as assignment (which is also an Expression-like construct here)
		assignStmt := ctx.mapAssignStmt(a)
		if expr, ok := assignStmt.(tree.Expression); ok {
			rp := tree.RightPadded[tree.Expression]{Element: expr}
			tag = &rp
		}
	}

	body := ctx.mapBlockStmt(stmt.Body)

	return &tree.Switch{
		ID:     uuid.New(),
		Prefix: prefix,
		Markers: tree.Markers{
			ID:      uuid.New(),
			Entries: []tree.Marker{tree.TypeSwitchGuard{Ident: uuid.New()}},
		},
		Init: init,
		Tag:  tag,
		Body: body,
	}
}

// mapEmptyStmt maps an empty statement (bare semicolons).
func (ctx *parseContext) mapEmptyStmt(stmt *ast.EmptyStmt) *tree.Empty {
	prefix := ctx.prefix(stmt.Pos())
	if !stmt.Implicit {
		ctx.skip(1) // explicit ";"
	}
	return &tree.Empty{ID: uuid.New(), Prefix: prefix}
}

// mapForStmt maps a for statement (classic 3-clause, condition-only, or infinite).
func (ctx *parseContext) mapForStmt(stmt *ast.ForStmt) *tree.ForLoop {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("for"))

	control := tree.ForControl{ID: uuid.New()}

	// Determine if this is a 3-clause for (has semicolons) or a simple for cond / for {}
	// Go's AST normalizes `for ; cond; {}` to Init=nil, Post=nil, same as `for cond {}`.
	// We detect semicolons by looking at the source text between for keyword and body.
	is3Clause := stmt.Init != nil || stmt.Post != nil
	if !is3Clause {
		// Check for semicolons in the source between cursor and the body brace
		bodyStart := int(stmt.Body.Lbrace) - ctx.file.Base()
		if ctx.findNextBefore(';', bodyStart) >= 0 {
			is3Clause = true
		}
	}

	if is3Clause {
		// 3-clause for: for [init]; [cond]; [post] {}
		if stmt.Init != nil {
			init := ctx.mapStmt(stmt.Init)
			semicolonOffset := ctx.findNext(';')
			var after tree.Space
			if semicolonOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(semicolonOffset))
				ctx.skip(1) // skip ";"
			}
			initRP := tree.RightPadded[tree.Statement]{Element: init, After: after}
			control.Init = &initRP
		} else {
			// No init but semicolons present: `for ; cond; post {}`
			semicolonOffset := ctx.findNext(';')
			var after tree.Space
			if semicolonOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(semicolonOffset))
				ctx.skip(1) // skip ";"
			}
			initRP := tree.RightPadded[tree.Statement]{Element: &tree.Empty{ID: uuid.New()}, After: after}
			control.Init = &initRP
		}

		if stmt.Cond != nil {
			cond := ctx.mapExpr(stmt.Cond)
			semicolonOffset := ctx.findNext(';')
			after := tree.EmptySpace
			if semicolonOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(semicolonOffset))
				ctx.skip(1) // skip ";"
			}
			condRP := tree.RightPadded[tree.Expression]{Element: cond, After: after}
			control.Condition = &condRP
		} else {
			semicolonOffset := ctx.findNext(';')
			after := tree.EmptySpace
			if semicolonOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(semicolonOffset))
				ctx.skip(1) // skip ";"
			}
			emptyRP := tree.RightPadded[tree.Expression]{Element: &tree.Empty{ID: uuid.New()}, After: after}
			control.Condition = &emptyRP
		}

		if stmt.Post != nil {
			post := ctx.mapStmt(stmt.Post)
			postRP := tree.RightPadded[tree.Statement]{Element: post}
			control.Update = &postRP
		}
	} else if stmt.Cond != nil {
		// Condition-only: for cond {}
		cond := ctx.mapExpr(stmt.Cond)
		condRP := tree.RightPadded[tree.Expression]{Element: cond}
		control.Condition = &condRP
	}
	// else: infinite loop, all nil

	body := ctx.mapBlockStmt(stmt.Body)

	return &tree.ForLoop{
		ID:      uuid.New(),
		Prefix:  prefix,
		Control: control,
		Body:    body,
	}
}

// mapRangeStmt maps a for-range statement.
func (ctx *parseContext) mapRangeStmt(stmt *ast.RangeStmt) *tree.ForEachLoop {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("for"))

	control := tree.ForEachControl{ID: uuid.New()}

	if stmt.Key != nil {
		// Has key variable
		key := ctx.mapExpr(stmt.Key)

		if stmt.Value != nil {
			// for k, v := range expr {}
			// Key.After captures comma space
			commaOffset := ctx.findNext(',')
			var keyAfter tree.Space
			if commaOffset >= 0 {
				keyAfter = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
			keyRP := tree.RightPadded[tree.Expression]{Element: key, After: keyAfter}
			control.Key = &keyRP

			value := ctx.mapExpr(stmt.Value)
			// Value.After captures space before operator
			opPrefix := ctx.prefix(stmt.TokPos)
			valueRP := tree.RightPadded[tree.Expression]{Element: value, After: opPrefix}
			control.Value = &valueRP
		} else {
			// for k := range expr {} — no value
			opPrefix := ctx.prefix(stmt.TokPos)
			keyRP := tree.RightPadded[tree.Expression]{Element: key, After: opPrefix}
			control.Key = &keyRP
		}

		// Parse operator (:= or =)
		var op tree.AssignOp
		if stmt.Tok == token.DEFINE {
			op = tree.AssignOpDefine
		} else {
			op = tree.AssignOpEquals
		}
		ctx.skip(len(stmt.Tok.String()))

		// Space between operator and "range"
		rangePrefix := ctx.prefix(stmt.Range)
		control.Operator = tree.LeftPadded[tree.AssignOp]{Before: rangePrefix, Element: op}
	} else {
		// for range expr {} — no variable
		control.Prefix = ctx.prefix(stmt.Range)
	}
	ctx.skip(len("range"))

	iterable := ctx.mapExpr(stmt.X)
	control.Iterable = iterable

	body := ctx.mapBlockStmt(stmt.Body)

	return &tree.ForEachLoop{
		ID:      uuid.New(),
		Prefix:  prefix,
		Control: control,
		Body:    body,
	}
}

// mapIncDecStmt maps an increment/decrement statement (x++ or x--).
func (ctx *parseContext) mapIncDecStmt(stmt *ast.IncDecStmt) *tree.Unary {
	operand := ctx.mapExpr(stmt.X)
	opPrefix := ctx.prefix(stmt.TokPos)
	ctx.skip(len(stmt.Tok.String()))

	var op tree.UnaryOperator
	if stmt.Tok == token.INC {
		op = tree.PostIncrement
	} else {
		op = tree.PostDecrement
	}

	// For postfix operators, the operand carries its own prefix (space before expression).
	// The Unary's prefix is empty; operator prefix captures space between operand and ++/--.
	return &tree.Unary{
		ID:       uuid.New(),
		Operator: tree.LeftPadded[tree.UnaryOperator]{Before: opPrefix, Element: op},
		Operand:  operand,
	}
}

// mapGoStmt maps a `go expr` statement.
func (ctx *parseContext) mapGoStmt(stmt *ast.GoStmt) *tree.GoStmt {
	prefix := ctx.prefixAndSkip(stmt.Go, len("go"))
	expr := ctx.mapExpr(stmt.Call)
	return &tree.GoStmt{ID: uuid.New(), Prefix: prefix, Expr: expr}
}

// mapDeferStmt maps a `defer expr` statement.
func (ctx *parseContext) mapDeferStmt(stmt *ast.DeferStmt) *tree.Defer {
	prefix := ctx.prefixAndSkip(stmt.Defer, len("defer"))
	expr := ctx.mapExpr(stmt.Call)
	return &tree.Defer{ID: uuid.New(), Prefix: prefix, Expr: expr}
}

// mapSendStmt maps a channel send statement `ch <- value`.
func (ctx *parseContext) mapSendStmt(stmt *ast.SendStmt) *tree.Send {
	ch := ctx.mapExpr(stmt.Chan)
	arrowPrefix := ctx.prefix(stmt.Arrow)
	ctx.skip(2) // "<-"
	value := ctx.mapExpr(stmt.Value)
	return &tree.Send{
		ID:      uuid.New(),
		Channel: ch,
		Arrow:   tree.LeftPadded[tree.Expression]{Before: arrowPrefix, Element: value},
	}
}

// mapBranchStmt maps break, continue, goto, fallthrough.
func (ctx *parseContext) mapBranchStmt(stmt *ast.BranchStmt) tree.Statement {
	switch stmt.Tok {
	case token.BREAK:
		prefix := ctx.prefixAndSkip(stmt.TokPos, len("break"))
		var label *tree.Identifier
		if stmt.Label != nil {
			label = ctx.mapIdent(stmt.Label)
		}
		return &tree.Break{ID: uuid.New(), Prefix: prefix, Label: label}
	case token.CONTINUE:
		prefix := ctx.prefixAndSkip(stmt.TokPos, len("continue"))
		var label *tree.Identifier
		if stmt.Label != nil {
			label = ctx.mapIdent(stmt.Label)
		}
		return &tree.Continue{ID: uuid.New(), Prefix: prefix, Label: label}
	case token.GOTO:
		prefix := ctx.prefixAndSkip(stmt.TokPos, len("goto"))
		label := ctx.mapIdent(stmt.Label)
		return &tree.Goto{ID: uuid.New(), Prefix: prefix, Label: label}
	case token.FALLTHROUGH:
		prefix := ctx.prefixAndSkip(stmt.TokPos, len("fallthrough"))
		return &tree.Fallthrough{ID: uuid.New(), Prefix: prefix}
	default:
		return nil
	}
}

// mapLabeledStmt maps a labeled statement `label: stmt`.
func (ctx *parseContext) mapLabeledStmt(stmt *ast.LabeledStmt) *tree.Label {
	prefix := ctx.prefix(stmt.Label.Pos())
	name := ctx.mapIdent(stmt.Label)
	colonPrefix := ctx.prefix(stmt.Colon)
	ctx.skip(1) // ":"
	body := ctx.mapStmt(stmt.Stmt)
	return &tree.Label{
		ID:        uuid.New(),
		Prefix:    prefix,
		Name:      tree.RightPadded[*tree.Identifier]{Element: name, After: colonPrefix},
		Statement: body,
	}
}

// mapExpr maps an expression.
func (ctx *parseContext) mapExpr(expr ast.Expr) tree.Expression {
	switch e := expr.(type) {
	case *ast.Ident:
		return ctx.mapIdent(e)
	case *ast.BasicLit:
		return ctx.mapBasicLit(e)
	case *ast.BinaryExpr:
		return ctx.mapBinaryExpr(e)
	case *ast.CallExpr:
		return ctx.mapCallExpr(e)
	case *ast.SelectorExpr:
		return ctx.mapSelectorExpr(e)
	case *ast.UnaryExpr:
		return ctx.mapUnaryExpr(e)
	case *ast.CompositeLit:
		return ctx.mapCompositeLit(e)
	case *ast.ParenExpr:
		return ctx.mapParenExpr(e)
	case *ast.StarExpr:
		return ctx.mapStarExpr(e)
	case *ast.ArrayType:
		return ctx.mapArrayType(e)
	case *ast.IndexExpr:
		return ctx.mapIndexExpr(e)
	case *ast.IndexListExpr:
		return ctx.mapIndexListExpr(e)
	case *ast.TypeAssertExpr:
		return ctx.mapTypeAssertExpr(e)
	case *ast.FuncLit:
		return ctx.mapFuncLit(e)
	case *ast.KeyValueExpr:
		return ctx.mapKeyValueExpr(e)
	case *ast.SliceExpr:
		return ctx.mapSliceExpr(e)
	case *ast.MapType:
		return ctx.mapMapType(e)
	case *ast.ChanType:
		return ctx.mapChanType(e)
	case *ast.FuncType:
		return ctx.mapFuncType(e)
	case *ast.InterfaceType:
		return ctx.mapInterfaceType(e)
	case *ast.StructType:
		return ctx.mapStructType(e)
	case *ast.Ellipsis:
		return ctx.mapEllipsis(e)
	default:
		return nil
	}
}

// mapIdent maps an identifier.
func (ctx *parseContext) mapIdent(ident *ast.Ident) *tree.Identifier {
	prefix := ctx.prefix(ident.Pos())
	ctx.skip(len(ident.Name))
	id := &tree.Identifier{ID: uuid.New(), Prefix: prefix, Name: ident.Name}

	// Type attribution: look up in Defs first, then Uses
	if obj, ok := ctx.typeInfo.Defs[ident]; ok && obj != nil {
		id.Type = ctx.mapper.mapObject(obj)
		id.FieldType = ctx.mapper.mapObjectToVariable(obj)
	} else if obj, ok := ctx.typeInfo.Uses[ident]; ok && obj != nil {
		id.Type = ctx.mapper.mapObject(obj)
		id.FieldType = ctx.mapper.mapObjectToVariable(obj)
	}

	return id
}

// mapBasicLit maps a basic literal (string, int, float, etc.)
func (ctx *parseContext) mapBasicLit(lit *ast.BasicLit) *tree.Literal {
	prefix := ctx.prefix(lit.Pos())
	ctx.skip(len(lit.Value))

	var kind tree.LiteralKind
	switch lit.Kind {
	case token.INT:
		kind = tree.IntLiteral
	case token.FLOAT:
		kind = tree.FloatLiteral
	case token.STRING:
		kind = tree.StringLiteral
	case token.CHAR:
		kind = tree.CharLiteral
	default:
		kind = tree.StringLiteral
	}

	l := &tree.Literal{ID: uuid.New(), Prefix: prefix, Kind: kind, Value: lit.Value, Source: lit.Value}

	// Type attribution for literal
	if tv, ok := ctx.typeInfo.Types[lit]; ok {
		l.Type = ctx.mapper.mapType(tv.Type)
	}

	return l
}

// mapBinaryExpr maps a binary expression.
func (ctx *parseContext) mapBinaryExpr(expr *ast.BinaryExpr) *tree.Binary {
	left := ctx.mapExpr(expr.X)
	opPrefix := ctx.prefix(expr.OpPos)
	op := mapBinaryOp(expr.Op)
	ctx.skip(len(expr.Op.String()))
	right := ctx.mapExpr(expr.Y)

	b := &tree.Binary{
		ID:       uuid.New(),
		Left:     left,
		Operator: tree.LeftPadded[tree.BinaryOperator]{Before: opPrefix, Element: op},
		Right:    right,
	}

	// Type attribution for binary expression
	if tv, ok := ctx.typeInfo.Types[expr]; ok {
		b.Type = ctx.mapper.mapType(tv.Type)
	}

	return b
}

// mapCallExpr maps a function/method call.
func (ctx *parseContext) mapCallExpr(expr *ast.CallExpr) tree.Expression {
	fun := ctx.mapExpr(expr.Fun)

	var sel *tree.RightPadded[tree.Expression]
	var name *tree.Identifier

	switch f := fun.(type) {
	case *tree.FieldAccess:
		selRP := tree.RightPadded[tree.Expression]{Element: f.Target}
		sel = &selRP
		name = f.Name.Element
	case *tree.Identifier:
		name = f
	default:
		// Callee is a complex expression (func literal, parenthesized, etc.)
		// Store it as Select with empty Name
		selRP := tree.RightPadded[tree.Expression]{Element: fun}
		sel = &selRP
		name = &tree.Identifier{ID: uuid.New()}
	}

	argsBefore := ctx.prefix(expr.Lparen)
	ctx.skip(1) // "("

	var argElements []tree.RightPadded[tree.Expression]
	for i, arg := range expr.Args {
		mapped := ctx.mapExpr(arg)
		// Handle variadic spread: last arg with `...`
		if expr.Ellipsis.IsValid() && i == len(expr.Args)-1 {
			ellipsisPrefix := ctx.prefix(expr.Ellipsis)
			ctx.skip(3) // "..."
			mapped = &tree.Unary{
				ID:       uuid.New(),
				Operator: tree.LeftPadded[tree.UnaryOperator]{Before: ellipsisPrefix, Element: tree.SpreadPostfix},
				Operand:  mapped,
			}
		}
		after := tree.EmptySpace
		if i < len(expr.Args)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		}
		argElements = append(argElements, tree.RightPadded[tree.Expression]{Element: mapped, After: after})
	}

	// Check for trailing comma before closing paren
	var markers tree.Markers
	if len(argElements) > 0 {
		// Look for a comma between current position and closing paren
		trailingCommaOff := ctx.findNextBefore(',', int(expr.Rparen)-ctx.file.Base())
		if trailingCommaOff >= 0 {
			commaBefore := ctx.prefix(ctx.file.Pos(trailingCommaOff))
			ctx.skip(1) // ","
			commaAfter := ctx.prefix(expr.Rparen)
			ctx.skip(1) // ")"
			markers = tree.Markers{
				ID: uuid.New(),
				Entries: []tree.Marker{tree.TrailingComma{
					Ident:  uuid.New(),
					Before: commaBefore,
					After:  commaAfter,
				}},
			}
		} else {
			closePrefix := ctx.prefix(expr.Rparen)
			ctx.skip(1) // ")"
			argElements[len(argElements)-1].After = closePrefix
		}
	} else {
		ctx.prefix(expr.Rparen) // consume space
		ctx.skip(1)             // ")"
	}

	mi := &tree.MethodInvocation{
		ID:        uuid.New(),
		Prefix:    tree.EmptySpace,
		Markers:   markers,
		Select:    sel,
		Name:      name,
		Arguments: tree.Container[tree.Expression]{Before: argsBefore, Elements: argElements},
	}

	// Type attribution for method invocation
	if selExpr, ok := expr.Fun.(*ast.SelectorExpr); ok {
		if selection, ok := ctx.typeInfo.Selections[selExpr]; ok {
			mi.MethodType = ctx.mapper.mapSelectionToMethod(selection)
		} else if obj, ok := ctx.typeInfo.Uses[selExpr.Sel]; ok {
			// Qualified identifier (pkg.Func) — not a selection, but Sel is in Uses
			if fn, ok := obj.(*types.Func); ok {
				mi.MethodType = ctx.mapper.mapMethodObject(fn)
			}
		}
	} else if ident, ok := expr.Fun.(*ast.Ident); ok {
		if obj, ok := ctx.typeInfo.Uses[ident]; ok {
			if fn, ok := obj.(*types.Func); ok {
				mi.MethodType = ctx.mapper.mapMethodObject(fn)
			}
		}
	}

	return mi
}

// mapSelectorExpr maps a selector expression (e.g., pkg.Name).
func (ctx *parseContext) mapSelectorExpr(expr *ast.SelectorExpr) *tree.FieldAccess {
	target := ctx.mapExpr(expr.X)

	dotOffset := ctx.findNext('.')
	var dotPrefix tree.Space
	if dotOffset >= 0 {
		dotPrefix = ctx.prefix(ctx.file.Pos(dotOffset))
		ctx.skip(1) // "."
	}

	sel := ctx.mapIdent(expr.Sel)

	fa := &tree.FieldAccess{
		ID:     uuid.New(),
		Target: target,
		Name:   tree.LeftPadded[*tree.Identifier]{Before: dotPrefix, Element: sel},
	}

	// Type attribution for selector (field access or method access)
	if selection, ok := ctx.typeInfo.Selections[expr]; ok {
		fa.Type = ctx.mapper.mapSelection(selection)
	} else if tv, ok := ctx.typeInfo.Types[expr]; ok {
		// Qualified identifier (e.g., pkg.ExportedName) — not a selection
		fa.Type = ctx.mapper.mapType(tv.Type)
	}

	return fa
}

// mapUnaryExpr maps a unary expression.
func (ctx *parseContext) mapUnaryExpr(expr *ast.UnaryExpr) tree.Expression {
	prefix := ctx.prefix(expr.OpPos)
	ctx.skip(len(expr.Op.String()))
	operand := ctx.mapExpr(expr.X)

	var op tree.UnaryOperator
	switch expr.Op {
	case token.SUB:
		op = tree.Negate
	case token.NOT:
		op = tree.Not
	case token.XOR:
		op = tree.BitwiseNot
	case token.MUL:
		op = tree.Deref
	case token.AND:
		op = tree.AddressOf
	case token.ARROW:
		op = tree.Receive
	case token.ADD:
		op = tree.Positive
	case token.TILDE:
		op = tree.Tilde
	}

	return &tree.Unary{
		ID:       uuid.New(),
		Prefix:   prefix,
		Operator: tree.LeftPadded[tree.UnaryOperator]{Element: op},
		Operand:  operand,
	}
}

// mapCompositeLit maps a composite literal (e.g., Type{elem1, elem2}).
func (ctx *parseContext) mapCompositeLit(expr *ast.CompositeLit) tree.Expression {
	var typeExpr tree.Expression
	if expr.Type != nil {
		typeExpr = ctx.mapTypeExpr(expr.Type)
	}
	lbracePrefix := ctx.prefix(expr.Lbrace)
	ctx.skip(1) // "{"

	var elements []tree.RightPadded[tree.Expression]
	for i, elt := range expr.Elts {
		mapped := ctx.mapExpr(elt)
		var after tree.Space
		if i < len(expr.Elts)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		}
		elements = append(elements, tree.RightPadded[tree.Expression]{Element: mapped, After: after})
	}

	// Check for trailing comma before closing brace
	var compMarkers tree.Markers
	if len(elements) > 0 {
		trailingCommaOff := ctx.findNextBefore(',', int(expr.Rbrace)-ctx.file.Base())
		if trailingCommaOff >= 0 {
			commaBefore := ctx.prefix(ctx.file.Pos(trailingCommaOff))
			ctx.skip(1) // ","
			commaAfter := ctx.prefix(expr.Rbrace)
			ctx.skip(1) // "}"
			compMarkers = tree.Markers{
				ID: uuid.New(),
				Entries: []tree.Marker{tree.TrailingComma{
					Ident:  uuid.New(),
					Before: commaBefore,
					After:  commaAfter,
				}},
			}
		} else {
			rbracePrefix := ctx.prefix(expr.Rbrace)
			ctx.skip(1) // "}"
			elements[len(elements)-1].After = rbracePrefix
		}
	} else {
		ctx.prefix(expr.Rbrace) // consume space
		ctx.skip(1)             // "}"
	}

	return &tree.Composite{
		ID:        uuid.New(),
		Markers:   compMarkers,
		TypeExpr:  typeExpr,
		Elements:  tree.Container[tree.Expression]{Before: lbracePrefix, Elements: elements},
	}
}

// mapParenExpr maps a parenthesized expression.
func (ctx *parseContext) mapParenExpr(expr *ast.ParenExpr) tree.Expression {
	prefix := ctx.prefix(expr.Lparen)
	ctx.skip(1) // "("
	inner := ctx.mapExpr(expr.X)
	closePrefix := ctx.prefix(expr.Rparen)
	ctx.skip(1) // ")"

	return &tree.Parentheses{
		ID:     uuid.New(),
		Prefix: prefix,
		Tree:   tree.RightPadded[tree.Expression]{Element: inner, After: closePrefix},
	}
}

// mapStarExpr maps a star expression (pointer type or dereference).
func (ctx *parseContext) mapStarExpr(expr *ast.StarExpr) tree.Expression {
	prefix := ctx.prefix(expr.Star)
	ctx.skip(1) // "*"
	operand := ctx.mapExpr(expr.X)

	return &tree.Unary{
		ID:       uuid.New(),
		Prefix:   prefix,
		Operator: tree.LeftPadded[tree.UnaryOperator]{Element: tree.Deref},
		Operand:  operand,
	}
}

// mapPointerType maps a star expression in a type context as a pointer type.
func (ctx *parseContext) mapPointerType(expr *ast.StarExpr) tree.Expression {
	prefix := ctx.prefix(expr.Star)
	ctx.skip(1) // "*"
	elem := ctx.mapTypeExpr(expr.X)

	return &tree.PointerType{
		ID:      uuid.New(),
		Prefix:  prefix,
		Elem:    elem,
	}
}

// mapTypeExpr maps an expression that is known to be in a type position.
// It delegates to mapExpr but overrides StarExpr to produce PointerType.
func (ctx *parseContext) mapTypeExpr(expr ast.Expr) tree.Expression {
	if star, ok := expr.(*ast.StarExpr); ok {
		return ctx.mapPointerType(star)
	}
	return ctx.mapExpr(expr)
}

// mapArrayType maps an array/slice type expression like `[]string` or `[5]int`.
func (ctx *parseContext) mapArrayType(expr *ast.ArrayType) tree.Expression {
	prefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["

	var length tree.Expression
	if expr.Len != nil {
		length = ctx.mapExpr(expr.Len)
	}

	// Find the `]`
	var closePrefix tree.Space
	rbrackOff := ctx.findNext(']')
	if rbrackOff >= 0 {
		closePrefix = ctx.prefix(ctx.file.Pos(rbrackOff))
		ctx.skip(1) // "]"
	}

	elt := ctx.mapTypeExpr(expr.Elt)

	return &tree.ArrayType{
		ID:          uuid.New(),
		Prefix:      prefix,
		Dimension:   tree.LeftPadded[tree.Space]{Element: closePrefix},
		Length:      length,
		ElementType: elt,
	}
}

// mapIndexExpr maps an index expression like `a[i]` or `m[key]`.
func (ctx *parseContext) mapIndexExpr(expr *ast.IndexExpr) tree.Expression {
	target := ctx.mapExpr(expr.X)
	lbrackPrefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["
	index := ctx.mapExpr(expr.Index)
	rbrackPrefix := ctx.prefix(expr.Rbrack)
	ctx.skip(1) // "]"

	return &tree.ArrayAccess{
		ID:      uuid.New(),
		Indexed: target,
		Dimension: &tree.ArrayDimension{
			ID:     uuid.New(),
			Prefix: lbrackPrefix,
			Index:  tree.RightPadded[tree.Expression]{Element: index, After: rbrackPrefix},
		},
	}
}

// mapIndexListExpr maps a multi-index expression like `Map[int, string]` (generic instantiation).
func (ctx *parseContext) mapIndexListExpr(expr *ast.IndexListExpr) tree.Expression {
	target := ctx.mapExpr(expr.X)
	lbrackPrefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["

	var elements []tree.RightPadded[tree.Expression]
	for i, idx := range expr.Indices {
		mapped := ctx.mapExpr(idx)
		var after tree.Space
		if i < len(expr.Indices)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		} else {
			after = ctx.prefix(expr.Rbrack)
		}
		elements = append(elements, tree.RightPadded[tree.Expression]{Element: mapped, After: after})
	}
	ctx.skip(1) // "]"

	return &tree.IndexList{
		ID:      uuid.New(),
		Target:  target,
		Indices: tree.Container[tree.Expression]{Before: lbrackPrefix, Elements: elements},
	}
}

// mapTypeAssertExpr maps a type assertion `x.(T)`.
func (ctx *parseContext) mapTypeAssertExpr(expr *ast.TypeAssertExpr) tree.Expression {
	x := ctx.mapExpr(expr.X)
	// dot before (
	dotOff := ctx.findNext('.')
	var dotPrefix tree.Space
	if dotOff >= 0 {
		dotPrefix = ctx.prefix(ctx.file.Pos(dotOff))
		ctx.skip(1) // "."
	}
	lparenPrefix := ctx.prefix(expr.Lparen)
	ctx.skip(1) // "("

	var typeExpr tree.Expression
	if expr.Type != nil {
		typeExpr = ctx.mapTypeExpr(expr.Type)
	} else {
		// type switch: x.(type)
		typePrefix := ctx.prefix(expr.Lparen + 1)
		ctx.skip(len("type"))
		typeExpr = &tree.Identifier{ID: uuid.New(), Prefix: typePrefix, Name: "type"}
	}

	rparenPrefix := ctx.prefix(expr.Rparen)
	ctx.skip(1) // ")"

	clazz := &tree.ControlParentheses{
		ID:     uuid.New(),
		Prefix: lparenPrefix,
		Tree:   tree.RightPadded[tree.Expression]{Element: typeExpr, After: rparenPrefix},
	}

	_ = dotPrefix // dot is between Expr and Clazz; stored in Expr's suffix isn't ideal
	// We'll use prefix of the type cast for the dot
	return &tree.TypeCast{
		ID:    uuid.New(),
		Prefix: dotPrefix,
		Clazz: clazz,
		Expr:  x,
	}
}

// mapFuncLit maps a function literal (closure).
func (ctx *parseContext) mapFuncLit(expr *ast.FuncLit) tree.Expression {
	prefix := ctx.prefixAndSkip(expr.Type.Func, len("func"))
	params := ctx.mapFieldListAsParams(expr.Type.Params)
	returnType := ctx.mapReturnType(expr.Type.Results)
	body := ctx.mapBlockStmt(expr.Body)

	md := &tree.MethodDeclaration{
		ID:         uuid.New(),
		Name:       &tree.Identifier{ID: uuid.New(), Name: ""},
		Parameters: params,
		ReturnType: returnType,
		Body:       body,
	}
	// Wrap in StatementExpression so the MethodDeclaration (a Statement) can
	// appear in expression contexts like return statements, assignments, and
	// call arguments.
	return &tree.StatementExpression{
		ID:        uuid.New(),
		Prefix:    prefix,
		Statement: md,
	}
}

// mapKeyValueExpr maps a key:value expression in composite literals.
func (ctx *parseContext) mapKeyValueExpr(expr *ast.KeyValueExpr) tree.Expression {
	key := ctx.mapExpr(expr.Key)
	colonPrefix := ctx.prefix(expr.Colon)
	ctx.skip(1) // ":"
	value := ctx.mapExpr(expr.Value)
	return &tree.KeyValue{
		ID:    uuid.New(),
		Key:   key,
		Value: tree.LeftPadded[tree.Expression]{Before: colonPrefix, Element: value},
	}
}

// mapSliceExpr maps a slice expression like `a[low:high]` or `a[low:high:max]`.
func (ctx *parseContext) mapSliceExpr(expr *ast.SliceExpr) tree.Expression {
	target := ctx.mapExpr(expr.X)
	lbrackPrefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["

	var low tree.Expression
	if expr.Low != nil {
		low = ctx.mapExpr(expr.Low)
	} else {
		low = &tree.Empty{ID: uuid.New()}
	}

	colon1Off := ctx.findNext(':')
	var colon1Prefix tree.Space
	if colon1Off >= 0 {
		colon1Prefix = ctx.prefix(ctx.file.Pos(colon1Off))
		ctx.skip(1)
	}

	var high tree.Expression
	if expr.High != nil {
		high = ctx.mapExpr(expr.High)
	} else {
		high = &tree.Empty{ID: uuid.New()}
	}

	var max tree.Expression
	var colon2Prefix tree.Space
	if expr.Slice3 {
		colon2Off := ctx.findNext(':')
		if colon2Off >= 0 {
			colon2Prefix = ctx.prefix(ctx.file.Pos(colon2Off))
			ctx.skip(1)
		}
		if expr.Max != nil {
			max = ctx.mapExpr(expr.Max)
		} else {
			max = &tree.Empty{ID: uuid.New()}
		}
	}

	rbrackPrefix := ctx.prefix(expr.Rbrack)
	ctx.skip(1) // "]"

	return &tree.Slice{
		ID:           uuid.New(),
		Indexed:      target,
		OpenBracket:  lbrackPrefix,
		Low:          tree.RightPadded[tree.Expression]{Element: low, After: colon1Prefix},
		High:         tree.RightPadded[tree.Expression]{Element: high, After: colon2Prefix},
		Max:          max,
		CloseBracket: rbrackPrefix,
	}
}

// mapMapType maps a map type expression like `map[K]V`.
func (ctx *parseContext) mapMapType(expr *ast.MapType) tree.Expression {
	prefix := ctx.prefixAndSkip(expr.Map, len("map"))
	lbrackPrefix := ctx.prefix(expr.Map + token.Pos(len("map")))
	ctx.skip(1) // "["
	key := ctx.mapTypeExpr(expr.Key)
	rbrackOff := ctx.findNext(']')
	var rbrackPrefix tree.Space
	if rbrackOff >= 0 {
		rbrackPrefix = ctx.prefix(ctx.file.Pos(rbrackOff))
		ctx.skip(1)
	}
	value := ctx.mapTypeExpr(expr.Value)

	return &tree.MapType{
		ID:          uuid.New(),
		Prefix:      prefix,
		OpenBracket: lbrackPrefix,
		Key:         tree.RightPadded[tree.Expression]{Element: key, After: rbrackPrefix},
		Value:       value,
	}
}

// mapChanType maps a channel type expression.
func (ctx *parseContext) mapChanType(expr *ast.ChanType) tree.Expression {
	prefix := ctx.prefix(expr.Begin)

	var dir tree.ChanDir
	switch expr.Dir {
	case ast.SEND:
		dir = tree.ChanSendOnly
		ctx.skip(len("chan<-"))
	case ast.RECV:
		dir = tree.ChanRecvOnly
		ctx.skip(len("<-chan"))
	default:
		dir = tree.ChanBidi
		ctx.skip(len("chan"))
	}

	value := ctx.mapTypeExpr(expr.Value)
	return &tree.Channel{
		ID:     uuid.New(),
		Prefix: prefix,
		Dir:    dir,
		Value:  value,
	}
}

// mapFuncType maps a function type expression like `func(int) string`.
func (ctx *parseContext) mapFuncType(expr *ast.FuncType) tree.Expression {
	prefix := ctx.prefixAndSkip(expr.Func, len("func"))
	params := ctx.mapFieldListAsParams(expr.Params)
	returnType := ctx.mapReturnType(expr.Results)

	return &tree.FuncType{
		ID:         uuid.New(),
		Prefix:     prefix,
		Parameters: params,
		ReturnType: returnType,
	}
}

// mapInterfaceType maps an interface type expression: `interface { methods }`.
func (ctx *parseContext) mapInterfaceType(expr *ast.InterfaceType) tree.Expression {
	prefix := ctx.prefixAndSkip(expr.Interface, len("interface"))
	body := ctx.mapFieldListAsInterfaceBody(expr.Methods)
	return &tree.InterfaceType{
		ID:     uuid.New(),
		Prefix: prefix,
		Body:   body,
	}
}

// mapStructType maps a struct type expression: `struct { fields }`.
func (ctx *parseContext) mapStructType(expr *ast.StructType) tree.Expression {
	prefix := ctx.prefixAndSkip(expr.Struct, len("struct"))
	body := ctx.mapFieldListAsStructBody(expr.Fields)
	return &tree.StructType{
		ID:     uuid.New(),
		Prefix: prefix,
		Body:   body,
	}
}

// mapFieldListAsStructBody maps a struct's field list to a Block.
// Each field becomes a VariableDeclarations statement.
func (ctx *parseContext) mapFieldListAsStructBody(fl *ast.FieldList) *tree.Block {
	blockPrefix := ctx.prefix(fl.Opening)
	ctx.skip(1) // "{"

	var stmts []tree.RightPadded[tree.Statement]
	for _, field := range fl.List {
		if len(field.Names) == 0 {
			// Embedded type (e.g., `io.Reader` in struct)
			typeExpr := ctx.mapTypeExpr(field.Type)
			vd := &tree.VariableDeclarations{
				ID:       uuid.New(),
				TypeExpr: typeExpr,
				Variables: []tree.RightPadded[*tree.VariableDeclarator]{
					{Element: &tree.VariableDeclarator{ID: uuid.New(), Name: &tree.Identifier{ID: uuid.New()}}},
				},
			}
			// Map struct tag if present
			if field.Tag != nil {
				ctx.mapStructTag(vd, field.Tag)
			}
			stmts = append(stmts, tree.RightPadded[tree.Statement]{Element: vd})
		} else {
			// Named field(s): `X int` or `X, Y int`
			var vars []tree.RightPadded[*tree.VariableDeclarator]
			for j, fieldName := range field.Names {
				nameIdent := ctx.mapIdent(fieldName)
				var nameAfter tree.Space
				if j < len(field.Names)-1 {
					commaOffset := ctx.findNext(',')
					if commaOffset >= 0 {
						nameAfter = ctx.prefix(ctx.file.Pos(commaOffset))
						ctx.skip(1) // ","
					}
				}
				vars = append(vars, tree.RightPadded[*tree.VariableDeclarator]{
					Element: &tree.VariableDeclarator{ID: uuid.New(), Name: nameIdent},
					After:   nameAfter,
				})
			}
			typeExpr := ctx.mapTypeExpr(field.Type)
			vd := &tree.VariableDeclarations{
				ID:        uuid.New(),
				TypeExpr:  typeExpr,
				Variables: vars,
			}
			// Map struct tag if present
			if field.Tag != nil {
				ctx.mapStructTag(vd, field.Tag)
			}
			stmts = append(stmts, tree.RightPadded[tree.Statement]{Element: vd})
		}
	}

	end := ctx.prefix(fl.Closing)
	ctx.skip(1) // "}"

	return &tree.Block{ID: uuid.New(), Prefix: blockPrefix, Statements: stmts, End: end}
}

// mapStructTag maps a struct field tag (e.g., `json:"name"`).
// Tags are stored as markers on the VariableDeclarations.
func (ctx *parseContext) mapStructTag(vd *tree.VariableDeclarations, tag *ast.BasicLit) {
	tagLit := ctx.mapBasicLit(tag)
	vd.Markers.Entries = append(vd.Markers.Entries, tree.StructTag{
		Ident: uuid.New(),
		Tag:   tagLit,
	})
}

// mapFieldListAsInterfaceBody maps an interface's method list to a Block.
// Each method becomes a MethodDeclaration (no body) or embedded type reference.
func (ctx *parseContext) mapFieldListAsInterfaceBody(fl *ast.FieldList) *tree.Block {
	blockPrefix := ctx.prefix(fl.Opening)
	ctx.skip(1) // "{"

	var stmts []tree.RightPadded[tree.Statement]
	for _, field := range fl.List {
		if len(field.Names) == 0 {
			// Embedded interface type (e.g., `io.Reader`)
			typeExpr := ctx.mapTypeExpr(field.Type)
			vd := &tree.VariableDeclarations{
				ID:       uuid.New(),
				TypeExpr: typeExpr,
				Variables: []tree.RightPadded[*tree.VariableDeclarator]{
					{Element: &tree.VariableDeclarator{ID: uuid.New(), Name: &tree.Identifier{ID: uuid.New()}}},
				},
			}
			stmts = append(stmts, tree.RightPadded[tree.Statement]{Element: vd})
		} else {
			// Method signature: `Name(params) returnType`
			name := ctx.mapIdent(field.Names[0])
			funcType := field.Type.(*ast.FuncType)
			params := ctx.mapFieldListAsParams(funcType.Params)
			returnType := ctx.mapReturnType(funcType.Results)

			md := &tree.MethodDeclaration{
				ID:   uuid.New(),
				Name: name,
				Markers: tree.Markers{
					Entries: []tree.Marker{tree.InterfaceMethod{Ident: uuid.New()}},
				},
				Parameters: params,
				ReturnType: returnType,
				// Body is nil — interface method has no body
			}
			stmts = append(stmts, tree.RightPadded[tree.Statement]{Element: md})
		}
	}

	end := ctx.prefix(fl.Closing)
	ctx.skip(1) // "}"

	return &tree.Block{ID: uuid.New(), Prefix: blockPrefix, Statements: stmts, End: end}
}

// mapTypeExprVariadic maps a type expression, detecting variadic `...T` and
// returning the element type plus a non-nil Varargs Space for the `...` prefix.
func (ctx *parseContext) mapTypeExprVariadic(typ ast.Expr) (tree.Expression, *tree.Space) {
	if ell, ok := typ.(*ast.Ellipsis); ok {
		ellipsisPrefix := ctx.prefix(ell.Ellipsis)
		ctx.skip(3) // "..."
		elemType := ctx.mapTypeExpr(ell.Elt)
		return elemType, &ellipsisPrefix
	}
	return ctx.mapTypeExpr(typ), nil
}

// mapEllipsis maps `...T` in function parameters.
func (ctx *parseContext) mapEllipsis(expr *ast.Ellipsis) tree.Expression {
	prefix := ctx.prefix(expr.Ellipsis)
	ctx.skip(3) // "..."
	elt := ctx.mapTypeExpr(expr.Elt)
	return &tree.Unary{
		ID:       uuid.New(),
		Prefix:   prefix,
		Operator: tree.LeftPadded[tree.UnaryOperator]{Element: tree.Spread},
		Operand:  elt,
	}
}

// findNextFrom finds the next occurrence of ch starting from a given offset.
func (ctx *parseContext) findNextFrom(ch byte, from int) int {
	for i := from; i < len(ctx.src); i++ {
		if ctx.src[i] == ch {
			return i
		}
	}
	return -1
}

func mapBinaryOp(op token.Token) tree.BinaryOperator {
	switch op {
	case token.ADD:
		return tree.Add
	case token.SUB:
		return tree.Subtract
	case token.MUL:
		return tree.Multiply
	case token.QUO:
		return tree.Divide
	case token.REM:
		return tree.Modulo
	case token.EQL:
		return tree.Equal
	case token.NEQ:
		return tree.NotEqual
	case token.LSS:
		return tree.LessThan
	case token.LEQ:
		return tree.LessThanOrEqual
	case token.GTR:
		return tree.GreaterThan
	case token.GEQ:
		return tree.GreaterThanOrEqual
	case token.LAND:
		return tree.LogicalAnd
	case token.LOR:
		return tree.LogicalOr
	case token.AND:
		return tree.BitwiseAnd
	case token.OR:
		return tree.BitwiseOr
	case token.XOR:
		return tree.BitwiseXor
	case token.SHL:
		return tree.LeftShift
	case token.SHR:
		return tree.RightShift
	case token.AND_NOT:
		return tree.AndNot
	default:
		return tree.Add
	}
}

// findNext returns the byte offset of the next occurrence of ch from the current cursor.
func (ctx *parseContext) findNext(ch byte) int {
	for i := ctx.cursor; i < len(ctx.src); i++ {
		if ctx.src[i] == ch {
			return i
		}
	}
	return -1
}

// findNextBefore returns the byte offset of the next occurrence of ch from the
// current cursor, but only if it appears before the `before` byte offset.
func (ctx *parseContext) findNextBefore(ch byte, before int) int {
	for i := ctx.cursor; i < len(ctx.src) && i < before; i++ {
		if ctx.src[i] == ch {
			return i
		}
	}
	return -1
}

// findNextString finds the next occurrence of s from the current cursor.
func (ctx *parseContext) findNextString(s string) int {
	idx := strings.Index(string(ctx.src[ctx.cursor:]), s)
	if idx < 0 {
		return -1
	}
	return ctx.cursor + idx
}

// prefixString returns the raw source between cursor and pos, for debugging.
func (ctx *parseContext) prefixString(pos token.Pos) string {
	if !pos.IsValid() {
		return ""
	}
	targetOffset := ctx.file.Offset(pos)
	if targetOffset <= ctx.cursor || targetOffset > len(ctx.src) {
		return ""
	}
	return strings.TrimRight(string(ctx.src[ctx.cursor:targetOffset]), " \t\n")
}
