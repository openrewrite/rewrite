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
	"go/build"
	"go/importer"
	"go/parser"
	"go/token"
	"go/types"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// GoParser parses Go source code into OpenRewrite LST nodes.
type GoParser struct {
	// Importer resolves imported packages for type checking.
	// Defaults to importer.Default() which resolves stdlib packages.
	Importer types.Importer

	// BuildContext drives `//go:build` and filename-suffix constraint
	// evaluation in ParsePackage. Defaults to build.Default (the host's
	// GOOS/GOARCH). Recipe authors that need cross-platform analysis can
	// set this explicitly via NewGoParserWithBuildContext.
	BuildContext build.Context
}

func NewGoParser() *GoParser {
	return &GoParser{
		Importer:     importer.Default(),
		BuildContext: build.Default,
	}
}

// NewGoParserWithBuildContext returns a parser that filters input files
// against the given build context. Useful for recipes that need to
// analyze code as it would compile under a specific GOOS/GOARCH/cgo
// configuration. To switch contexts, build a new parser — A3 keeps
// BuildContext immutable per parser to avoid cache-key complexity.
func NewGoParserWithBuildContext(buildCtx build.Context) *GoParser {
	return &GoParser{
		Importer:     importer.Default(),
		BuildContext: buildCtx,
	}
}

// FileInput is one file given to ParsePackage.
type FileInput struct {
	Path    string
	Content string
}

// Parse parses a single Go source file and returns its CompilationUnit.
// Convenience wrapper around ParsePackage for the common one-file case;
// type attribution that depends on sibling files in the same package
// won't resolve here. Use ParsePackage when sibling files matter.
func (gp *GoParser) Parse(sourcePath string, source string) (*golang.CompilationUnit, error) {
	cus, err := gp.ParsePackage([]FileInput{{Path: sourcePath, Content: source}})
	if err != nil {
		return nil, err
	}
	if len(cus) == 0 {
		return nil, fmt.Errorf("no compilation unit produced")
	}
	return cus[0], nil
}

// ParsePackage parses every file in a single Go package together so
// type-checking sees them as one unit. File A's reference to file B's
// symbol resolves; the resulting CompilationUnits share a single
// types.Info populated by one types.Config.Check call.
//
// All files MUST belong to the same package (same `package` clause).
// Order in the returned slice matches the input order.
func (gp *GoParser) ParsePackage(files []FileInput) ([]*golang.CompilationUnit, error) {
	if len(files) == 0 {
		return nil, nil
	}

	// Filter out files excluded by the build context — `//go:build` /
	// `// +build` constraints and OS/arch filename suffixes. Skipped
	// files don't appear in the output at all (they're as if they
	// weren't passed in).
	filtered := make([]FileInput, 0, len(files))
	for _, f := range files {
		if MatchBuildContext(gp.BuildContext, filepath.Base(f.Path), f.Content) {
			filtered = append(filtered, f)
		}
	}
	files = filtered
	if len(files) == 0 {
		return nil, nil
	}

	fset := token.NewFileSet()
	asts := make([]*ast.File, 0, len(files))
	for _, f := range files {
		a, err := parser.ParseFile(fset, f.Path, f.Content, parser.ParseComments)
		if err != nil {
			return nil, fmt.Errorf("parse %s: %w", f.Path, err)
		}
		asts = append(asts, a)
	}

	typeInfo := &types.Info{
		Types:      make(map[ast.Expr]types.TypeAndValue),
		Defs:       make(map[*ast.Ident]types.Object),
		Uses:       make(map[*ast.Ident]types.Object),
		Selections: make(map[*ast.SelectorExpr]*types.Selection),
		// Instances records identifiers denoting generic functions/types that are
		// instantiated with explicit type arguments, e.g. the `Map` in `Map[int]`.
		// Used to distinguish generic instantiation from ordinary indexing.
		Instances: make(map[*ast.Ident]types.Instance),
	}
	conf := types.Config{
		Importer: gp.Importer,
		// Don't fail on type errors — we want partial type info even when
		// some imports can't be resolved.
		Error: func(error) {},
	}

	// Use the first file's package name as the type-checker hint;
	// types.Config.Check validates that all files agree.
	pkgName := "main"
	if asts[0].Name != nil {
		pkgName = asts[0].Name.Name
	}
	_, _ = conf.Check(pkgName, fset, asts, typeInfo)

	mapper := newTypeMapper()
	cus := make([]*golang.CompilationUnit, 0, len(files))
	for i, f := range files {
		ctx := &parseContext{
			src:      []byte(f.Content),
			fset:     fset,
			file:     fset.File(asts[i].Pos()),
			astFile:  asts[i],
			cursor:   0,
			typeInfo: typeInfo,
			mapper:   mapper,
		}
		cus = append(cus, ctx.mapFile(asts[i], f.Path))
	}
	return cus, nil
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
func (ctx *parseContext) prefix(pos token.Pos) java.Space {
	if !pos.IsValid() {
		return java.EmptySpace
	}
	targetOffset := ctx.file.Offset(pos)
	if targetOffset <= ctx.cursor || targetOffset > len(ctx.src) {
		return java.EmptySpace
	}
	raw := string(ctx.src[ctx.cursor:targetOffset])
	ctx.cursor = targetOffset
	return java.ParseSpace(raw)
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
func (ctx *parseContext) prefixAndSkip(pos token.Pos, length int) java.Space {
	space := ctx.prefix(pos)
	ctx.skip(length)
	return space
}

// mapFile maps an ast.File to a CompilationUnit.
func (ctx *parseContext) mapFile(file *ast.File, sourcePath string) *golang.CompilationUnit {
	// "package" keyword
	prefix := ctx.prefixAndSkip(file.Package, len("package"))

	// Package name identifier
	pkgName := ctx.mapIdent(file.Name)
	paddedPkgName := java.RightPadded[*java.Identifier]{Element: pkgName}

	// Imports
	var imports *java.Container[*java.Import]
	imports = ctx.mapImports(file)

	// Top-level declarations (functions, types, vars, consts - excluding imports)
	var stmts []java.RightPadded[java.Statement]
	for _, decl := range file.Decls {
		if gd, ok := decl.(*ast.GenDecl); ok && gd.Tok == token.IMPORT {
			continue
		}
		stmt := ctx.mapDecl(decl)
		if stmt != nil {
			stmts = append(stmts, java.RightPadded[java.Statement]{Element: stmt})
		}
	}

	// EOF
	eof := java.EmptySpace
	if ctx.cursor < len(ctx.src) {
		eof = java.ParseSpace(string(ctx.src[ctx.cursor:]))
	}

	return &golang.CompilationUnit{
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
func (ctx *parseContext) mapImports(file *ast.File) *java.Container[*java.Import] {
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

	var elements []java.RightPadded[*java.Import]
	var containerMarkers java.Markers
	prevGrouped := false

	// First import block: captured into Container.Before and Container.Markers
	first := importDecls[0]
	before := ctx.prefixAndSkip(first.Pos(), len("import"))

	if first.Lparen.IsValid() {
		prevGrouped = true
		openParenPrefix := ctx.prefix(first.Lparen)
		ctx.skip(1) // skip "("
		containerMarkers = java.Markers{
			ID: uuid.New(),
			Entries: []java.Marker{
				golang.GroupedImport{Ident: uuid.New(), Before: openParenPrefix},
			},
		}
	}

	for _, spec := range first.Specs {
		is := spec.(*ast.ImportSpec)
		imp := ctx.mapImportSpec(is)
		elements = append(elements, java.RightPadded[*java.Import]{Element: imp})
	}

	if first.Lparen.IsValid() {
		closeParen := ctx.prefix(first.Rparen)
		ctx.skip(1) // skip ")"
		if len(elements) > 0 {
			elements[len(elements)-1].After = closeParen
		} else if len(closeParen.Comments) > 0 {
			elements = append(elements, java.RightPadded[*java.Import]{
				Element: &java.Import{ID: uuid.New(), Qualid: &java.Empty{ID: uuid.New()}},
				After:   closeParen,
			})
		}
	}

	// Subsequent import blocks: attach ImportBlock marker to first import of each
	for _, importDecl := range importDecls[1:] {
		blockBefore := ctx.prefixAndSkip(importDecl.Pos(), len("import"))
		grouped := importDecl.Lparen.IsValid()
		var groupedBefore java.Space
		if grouped {
			groupedBefore = ctx.prefix(importDecl.Lparen)
			ctx.skip(1) // skip "("
		}

		importBlockMarker := golang.ImportBlock{
			Ident:         uuid.New(),
			ClosePrevious: prevGrouped,
			Before:        blockBefore,
			Grouped:       grouped,
			GroupedBefore: groupedBefore,
		}
		ctx.mapImportBlockSpecs(importDecl, &elements, importBlockMarker)

		if grouped {
			closeParen := ctx.prefix(importDecl.Rparen)
			ctx.skip(1) // skip ")"
			if len(importDecl.Specs) > 0 {
				elements[len(elements)-1].After = closeParen
			} else if len(closeParen.Comments) > 0 {
				imp := &java.Import{ID: uuid.New(), Qualid: &java.Empty{ID: uuid.New()}}
				imp.Markers = java.Markers{
					ID:      uuid.New(),
					Entries: []java.Marker{importBlockMarker},
				}
				elements = append(elements, java.RightPadded[*java.Import]{
					Element: imp,
					After:   closeParen,
				})
			}
		}
		prevGrouped = grouped
	}

	container := java.Container[*java.Import]{Before: before, Elements: elements, Markers: containerMarkers}
	return &container
}

// mapImportBlockSpecs maps the specs of a subsequent import block, attaching
// the ImportBlock marker to the first spec's Import node.
func (ctx *parseContext) mapImportBlockSpecs(decl *ast.GenDecl, elements *[]java.RightPadded[*java.Import], marker golang.ImportBlock) {
	for j, spec := range decl.Specs {
		is := spec.(*ast.ImportSpec)
		imp := ctx.mapImportSpec(is)
		if j == 0 {
			imp.Markers = java.Markers{
				ID:      uuid.New(),
				Entries: []java.Marker{marker},
			}
		}
		*elements = append(*elements, java.RightPadded[*java.Import]{Element: imp})
	}
}

// mapImportSpec maps a single import spec.
func (ctx *parseContext) mapImportSpec(spec *ast.ImportSpec) *java.Import {
	prefix := ctx.prefix(spec.Pos())

	var alias *java.LeftPadded[*java.Identifier]
	if spec.Name != nil {
		ident := ctx.mapIdent(spec.Name)
		lp := java.LeftPadded[*java.Identifier]{Element: ident}
		alias = &lp
	}

	path := ctx.mapBasicLit(spec.Path)

	return &java.Import{ID: uuid.New(), Prefix: prefix, Qualid: path, Alias: alias}
}

// mapDecl maps a top-level declaration.
func (ctx *parseContext) mapDecl(decl ast.Decl) java.Statement {
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
func (ctx *parseContext) mapGenDecl(decl *ast.GenDecl) java.Statement {
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
func (ctx *parseContext) mapVarConstDecl(decl *ast.GenDecl) java.Statement {
	prefix := ctx.prefix(decl.Pos())
	leadingAnns, prefix := extractDirectives(prefix)
	keyword := decl.Tok.String()
	ctx.skip(len(keyword))

	if len(decl.Specs) == 1 && !decl.Lparen.IsValid() {
		// Single declaration: var x int = 5
		spec := decl.Specs[0].(*ast.ValueSpec)
		vd := ctx.mapValueSpec(spec, prefix, keyword)
		vd.LeadingAnnotations = leadingAnns
		return vd
	}

	// Grouped declaration: var ( ... ) or const ( ... )
	lparenPrefix := ctx.prefix(decl.Lparen)
	ctx.skip(1) // "("

	var elements []java.RightPadded[java.Statement]
	for _, s := range decl.Specs {
		spec := s.(*ast.ValueSpec)
		innerPrefix := ctx.prefix(spec.Pos())
		vd := ctx.mapValueSpec(spec, innerPrefix, keyword)
		vd.Markers.Entries = append(vd.Markers.Entries, golang.GroupedSpec{Ident: uuid.New()})
		elements = append(elements, java.RightPadded[java.Statement]{Element: vd})
	}

	rparenPrefix := ctx.prefix(decl.Rparen)
	ctx.skip(1) // ")"

	if len(elements) > 0 {
		elements[len(elements)-1].After = rparenPrefix
	} else if len(rparenPrefix.Comments) > 0 {
		elements = append(elements, java.RightPadded[java.Statement]{
			Element: &java.Empty{ID: uuid.New()},
			After:   rparenPrefix,
		})
	}

	kind := golang.DeclVar
	if keyword == "const" {
		kind = golang.DeclConst
	}

	specs := &java.Container[java.Statement]{Before: lparenPrefix, Elements: elements}
	return &golang.DeclarationBlock{
		ID:                 uuid.New(),
		Prefix:             prefix,
		Markers:            java.Markers{ID: uuid.New()},
		LeadingAnnotations: leadingAnns,
		Kind:               kind,
		Specs:              specs,
	}
}

// mapValueSpec maps a single var/const spec.
func (ctx *parseContext) mapValueSpec(spec *ast.ValueSpec, prefix java.Space, keyword string) *java.VariableDeclarations {
	// Source order: keyword name[, name]... [type] [= value]
	// Map names first (they appear first in source after keyword)
	// Handle commas between multiple names
	var nameIdents []*java.Identifier
	var nameAfters []java.Space
	for i, name := range spec.Names {
		nameIdents = append(nameIdents, ctx.mapIdent(name))
		if i < len(spec.Names)-1 {
			// Capture space before comma and skip comma
			commaOff := ctx.findNext(',')
			var after java.Space
			if commaOff >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOff))
				ctx.skip(1) // ","
			}
			nameAfters = append(nameAfters, after)
		} else {
			nameAfters = append(nameAfters, java.Space{})
		}
	}

	// Then type (appears after name in source)
	var typeExpr java.Expression
	if spec.Type != nil {
		typeExpr = ctx.mapTypeExpr(spec.Type)
	}

	// Then initializers (after type and `=` in source)
	// In Go, multi-value declarations use a single `=` followed by comma-separated values:
	//   var a, b = val1, val2
	var variables []java.RightPadded[*java.VariableDeclarator]
	for i, nameIdent := range nameIdents {
		var init *java.LeftPadded[java.Expression]
		if i < len(spec.Values) {
			var sepPrefix java.Space
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
			lp := java.LeftPadded[java.Expression]{Before: sepPrefix, Element: val}
			init = &lp
		}

		vd := &java.VariableDeclarator{
			ID:          uuid.New(),
			Name:        nameIdent,
			Initializer: init,
		}
		variables = append(variables, java.RightPadded[*java.VariableDeclarator]{Element: vd, After: nameAfters[i]})
	}

	var markerEntries []java.Marker
	if keyword == "var" {
		markerEntries = append(markerEntries, golang.VarKeyword{Ident: uuid.New()})
	} else if keyword == "const" {
		markerEntries = append(markerEntries, golang.ConstDecl{Ident: uuid.New()})
	}
	var markers java.Markers
	if len(markerEntries) > 0 {
		markers = java.Markers{ID: uuid.New(), Entries: markerEntries}
	}

	return &java.VariableDeclarations{
		ID:        uuid.New(),
		Prefix:    prefix,
		Markers:   markers,
		TypeExpr:  typeExpr,
		Variables: variables,
	}
}

// mapTypeDecl maps a `type Name ...` declaration.
func (ctx *parseContext) mapTypeDecl(decl *ast.GenDecl) java.Statement {
	prefix := ctx.prefixAndSkip(decl.Pos(), len("type"))
	leadingAnns, prefix := extractDirectives(prefix)

	if len(decl.Specs) == 1 && !decl.Lparen.IsValid() {
		spec := decl.Specs[0].(*ast.TypeSpec)
		td := ctx.mapTypeSpec(spec, prefix)
		td.LeadingAnnotations = leadingAnns
		return td
	}

	// Grouped type declaration: type ( ... )
	lparenPrefix := ctx.prefix(decl.Lparen)
	ctx.skip(1) // "("

	var elements []java.RightPadded[java.Statement]
	for _, s := range decl.Specs {
		spec := s.(*ast.TypeSpec)
		innerPrefix := ctx.prefix(spec.Pos())
		td := ctx.mapTypeSpec(spec, innerPrefix)
		td.Markers.Entries = append(td.Markers.Entries, golang.GroupedSpec{Ident: uuid.New()})
		elements = append(elements, java.RightPadded[java.Statement]{Element: td})
	}

	rparenPrefix := ctx.prefix(decl.Rparen)
	ctx.skip(1) // ")"

	if len(elements) > 0 {
		elements[len(elements)-1].After = rparenPrefix
	} else if len(rparenPrefix.Comments) > 0 {
		elements = append(elements, java.RightPadded[java.Statement]{
			Element: &java.Empty{ID: uuid.New()},
			After:   rparenPrefix,
		})
	}

	specs := &java.Container[java.Statement]{Before: lparenPrefix, Elements: elements}
	return &golang.TypeDecl{
		ID:                 uuid.New(),
		Prefix:             prefix,
		LeadingAnnotations: leadingAnns,
		Specs:              specs,
	}
}

// mapTypeSpec maps a single type spec: `type Name Type` or `type Name = Type`.
func (ctx *parseContext) mapTypeSpec(spec *ast.TypeSpec, prefix java.Space) *golang.TypeDecl {
	name := ctx.mapIdent(spec.Name)
	typeParams := ctx.mapTypeParams(spec.TypeParams)

	var assign *java.LeftPadded[java.Space]
	if spec.Assign.IsValid() {
		eqPrefix := ctx.prefix(spec.Assign)
		ctx.skip(1) // "="
		lp := java.LeftPadded[java.Space]{Before: eqPrefix}
		assign = &lp
	}

	def := ctx.mapTypeExpr(spec.Type)

	return &golang.TypeDecl{
		ID:             uuid.New(),
		Prefix:         prefix,
		Name:           name,
		TypeParameters: typeParams,
		Assign:         assign,
		Definition:     def,
	}
}

// mapFuncDecl maps a function declaration. Free functions map to a bare
// java.MethodDeclaration (mirroring J.MethodDeclaration); methods carrying a
// receiver (`func (s *Service) Run()`) wrap that declaration in a
// golang.MethodDeclaration, since J.MethodDeclaration has no receiver slot.
func (ctx *parseContext) mapFuncDecl(decl *ast.FuncDecl) java.Statement {
	prefix := ctx.prefixAndSkip(decl.Pos(), len("func"))
	leadingAnns, prefix := extractDirectives(prefix)

	var receiver *java.Container[java.Statement]
	if decl.Recv != nil && len(decl.Recv.List) > 0 {
		recv := ctx.mapFieldListAsParams(decl.Recv)
		receiver = &recv
	}

	name := ctx.mapIdent(decl.Name)
	typeParams := ctx.mapTypeParams(decl.Type.TypeParams)
	params := ctx.mapFieldListAsParams(decl.Type.Params)
	returnType := ctx.mapReturnType(decl.Type.Results)

	var body *java.Block
	if decl.Body != nil {
		body = ctx.mapBlockStmt(decl.Body)
	}

	md := &java.MethodDeclaration{
		ID:                 uuid.New(),
		Prefix:             prefix,
		LeadingAnnotations: leadingAnns,
		Name:               name,
		TypeParameters:     typeParams,
		Parameters:         params,
		ReturnType:         returnType,
		Body:               body,
	}

	// Type attribution for method declaration
	if obj, ok := ctx.typeInfo.Defs[decl.Name]; ok && obj != nil {
		if fn, ok := obj.(*types.Func); ok {
			md.MethodType = ctx.mapper.mapMethodObject(fn)
		}
	}

	if receiver != nil {
		// The prefix (whitespace before `func`, after any `//go:` directives)
		// belongs on the outermost node, so it moves to the wrapper; the inner
		// declaration keeps its leading directives but is otherwise prefix-less.
		declPrefix := md.Prefix
		md.Prefix = java.EmptySpace
		return &golang.MethodDeclaration{
			ID:          uuid.New(),
			Prefix:      declPrefix,
			Receiver:    *receiver,
			Declaration: md,
		}
	}

	return md
}

// mapTypeParams maps a declaration-site type parameter list `[...]` (Go 1.18+
// generics) to a J.TypeParameters. Returns nil when there are no type
// parameters. Go groups names sharing a constraint into one field, e.g.
// `[T, U any]`; each name becomes its own J.TypeParameter and only the last
// name in a group carries the shared constraint as its single bound.
func (ctx *parseContext) mapTypeParams(fl *ast.FieldList) *java.TypeParameters {
	if fl == nil || len(fl.List) == 0 {
		return nil
	}
	before := ctx.prefix(fl.Opening)
	ctx.skip(1) // "["

	type unit struct {
		name     *ast.Ident
		field    *ast.Field
		hasBound bool
	}
	var units []unit
	for _, field := range fl.List {
		for j := range field.Names {
			units = append(units, unit{name: field.Names[j], field: field, hasBound: j == len(field.Names)-1})
		}
	}

	var elements []java.RightPadded[java.J]
	for i, u := range units {
		tp := &java.TypeParameter{ID: uuid.New(), Name: ctx.mapIdent(u.name)}
		if u.hasBound {
			constraint := ctx.mapTypeExpr(u.field.Type)
			tp.Bounds = &java.Container[java.Expression]{
				Elements: []java.RightPadded[java.Expression]{{Element: constraint}},
			}
		}
		var after java.Space
		if i < len(units)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		}
		elements = append(elements, java.RightPadded[java.J]{Element: tp, After: after})
	}

	closePrefix := ctx.prefix(fl.Closing)
	ctx.skip(1) // "]"
	if len(elements) > 0 {
		elements[len(elements)-1].After = closePrefix
	}

	return &java.TypeParameters{
		ID:             uuid.New(),
		Prefix:         before,
		TypeParameters: elements,
	}
}

// mapReturnType maps function return types.
// Returns nil for no return, a single Expression for one type, or a TypeList for multiple.
// Handles both unnamed `(int, error)` and named `(n int, err error)` returns.
func (ctx *parseContext) mapReturnType(results *ast.FieldList) java.Expression {
	if results == nil || len(results.List) == 0 {
		return nil
	}

	// Check if return types are parenthesized
	if results.Opening.IsValid() {
		// Parenthesized: `(int, error)`, `(int)`, or `(n int, err error)`
		before := ctx.prefix(results.Opening)
		ctx.skip(1) // "("

		var elements []java.RightPadded[java.Statement]
		for i, field := range results.List {
			if len(field.Names) == 0 {
				// Unnamed return: just a type expression
				typeExpr := ctx.mapTypeExpr(field.Type)
				vd := &java.VariableDeclarations{
					ID:       uuid.New(),
					TypeExpr: typeExpr,
					Variables: []java.RightPadded[*java.VariableDeclarator]{
						{Element: &java.VariableDeclarator{ID: uuid.New(), Name: &java.Identifier{ID: uuid.New()}}},
					},
				}
				var after java.Space
				if i < len(results.List)-1 {
					commaOffset := ctx.findNext(',')
					if commaOffset >= 0 {
						after = ctx.prefix(ctx.file.Pos(commaOffset))
						ctx.skip(1) // ","
					}
				}
				elements = append(elements, java.RightPadded[java.Statement]{Element: vd, After: after})
			} else {
				// Named return(s): `n int` or `x, y int`
				var vars []java.RightPadded[*java.VariableDeclarator]
				for j, fieldName := range field.Names {
					nameIdent := ctx.mapIdent(fieldName)
					var nameAfter java.Space
					if j < len(field.Names)-1 {
						commaOffset := ctx.findNext(',')
						if commaOffset >= 0 {
							nameAfter = ctx.prefix(ctx.file.Pos(commaOffset))
							ctx.skip(1) // ","
						}
					}
					vars = append(vars, java.RightPadded[*java.VariableDeclarator]{
						Element: &java.VariableDeclarator{ID: uuid.New(), Name: nameIdent},
						After:   nameAfter,
					})
				}
				typeExpr := ctx.mapTypeExpr(field.Type)
				vd := &java.VariableDeclarations{
					ID:        uuid.New(),
					TypeExpr:  typeExpr,
					Variables: vars,
				}
				var after java.Space
				if i < len(results.List)-1 {
					commaOffset := ctx.findNext(',')
					if commaOffset >= 0 {
						after = ctx.prefix(ctx.file.Pos(commaOffset))
						ctx.skip(1) // ","
					}
				}
				elements = append(elements, java.RightPadded[java.Statement]{Element: vd, After: after})
			}
		}

		closePrefix := ctx.prefix(results.Closing)
		ctx.skip(1) // ")"

		if len(elements) > 0 {
			elements[len(elements)-1].After = closePrefix
		}

		return &golang.TypeList{
			ID:    uuid.New(),
			Types: java.Container[java.Statement]{Before: before, Elements: elements},
		}
	}

	// Single non-parenthesized return type
	return ctx.mapTypeExpr(results.List[0].Type)
}

// mapFieldListAsParams maps function parameters.
// Handles named (x int), unnamed (int), and grouped (a, b int) parameters.
// Each ast.Field becomes one VariableDeclarations (possibly with multiple names).
func (ctx *parseContext) mapFieldListAsParams(fl *ast.FieldList) java.Container[java.Statement] {
	before := ctx.prefix(fl.Opening)
	ctx.skip(1) // "("

	var elements []java.RightPadded[java.Statement]
	for i, field := range fl.List {
		if len(field.Names) == 0 {
			// Unnamed parameter: just a type expression (e.g., `int` in `func(int)`)
			typeExpr := ctx.mapTypeExpr(field.Type)
			var varargs *java.Space
			if vrd, ok := typeExpr.(*golang.Variadic); ok && !vrd.Postfix {
				varargs = &vrd.Prefix
				typeExpr = vrd.Element
			}
			vd := &java.VariableDeclarations{
				ID:       uuid.New(),
				TypeExpr: typeExpr,
				Varargs:  varargs,
				Variables: []java.RightPadded[*java.VariableDeclarator]{
					{Element: &java.VariableDeclarator{ID: uuid.New(), Name: &java.Identifier{ID: uuid.New()}}},
				},
			}
			var after java.Space
			if i < len(fl.List)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			elements = append(elements, java.RightPadded[java.Statement]{Element: vd, After: after})
		} else {
			// Named parameter(s): `a int` or `a, b int` (grouped names sharing a type)
			// Map all names first (source order), then the shared type
			var vars []java.RightPadded[*java.VariableDeclarator]
			for j, fieldName := range field.Names {
				nameIdent := ctx.mapIdent(fieldName)
				var nameAfter java.Space
				if j < len(field.Names)-1 {
					commaOffset := ctx.findNext(',')
					if commaOffset >= 0 {
						nameAfter = ctx.prefix(ctx.file.Pos(commaOffset))
						ctx.skip(1) // ","
					}
				}
				vars = append(vars, java.RightPadded[*java.VariableDeclarator]{
					Element: &java.VariableDeclarator{ID: uuid.New(), Name: nameIdent},
					After:   nameAfter,
				})
			}

			typeExpr := ctx.mapTypeExpr(field.Type)
			var varargs *java.Space
			if vrd, ok := typeExpr.(*golang.Variadic); ok && !vrd.Postfix {
				varargs = &vrd.Prefix
				typeExpr = vrd.Element
			}
			vd := &java.VariableDeclarations{
				ID:        uuid.New(),
				TypeExpr:  typeExpr,
				Varargs:   varargs,
				Variables: vars,
			}

			var after java.Space
			if i < len(fl.List)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			elements = append(elements, java.RightPadded[java.Statement]{Element: vd, After: after})
		}
	}

	var markers java.Markers
	if len(elements) > 0 {
		trailingCommaOff := ctx.findNextBefore(',', int(fl.Closing)-ctx.file.Base())
		if trailingCommaOff >= 0 {
			commaBefore := ctx.prefix(ctx.file.Pos(trailingCommaOff))
			ctx.skip(1) // ","
			commaAfter := ctx.prefix(fl.Closing)
			ctx.skip(1) // ")"
			markers = java.Markers{
				ID: uuid.New(),
				Entries: []java.Marker{golang.TrailingComma{
					Ident:  uuid.New(),
					Before: commaBefore,
					After:  commaAfter,
				}},
			}
		} else {
			closePrefix := ctx.prefix(fl.Closing)
			ctx.skip(1) // ")"
			elements[len(elements)-1].After = closePrefix
		}
	} else {
		closeParen := ctx.prefix(fl.Closing)
		ctx.skip(1) // ")"
		if !closeParen.IsEmpty() {
			elements = append(elements, java.RightPadded[java.Statement]{
				Element: &java.Empty{ID: uuid.New()},
				After:   closeParen,
			})
		}
	}

	return java.Container[java.Statement]{Before: before, Elements: elements, Markers: markers}
}

// mapBlockStmt maps a block statement.
//
// Multi-statements-per-line (`_ = 1; _ = 2`) carry a literal `;` that
// Go's tokenizer recognizes but doesn't surface as part of either
// statement's AST. To round-trip the source, we look for an inline `;`
// between this statement and the next, capture the leading whitespace
// as RightPadded.After, mark the entry with a Semicolon marker, and
// advance the cursor past the `;`. The Block printer emits `;` when
// the marker is present.
func (ctx *parseContext) mapBlockStmt(block *ast.BlockStmt) *java.Block {
	prefix := ctx.prefix(block.Lbrace)
	ctx.skip(1) // "{"

	var stmts []java.RightPadded[java.Statement]
	for i, stmt := range block.List {
		mapped := ctx.mapStmt(stmt)
		if mapped == nil {
			continue
		}
		rp := java.RightPadded[java.Statement]{Element: mapped}

		// Detect inline `;` separator. Go inserts implicit semicolons
		// at end-of-line, so a literal `;` between two statements only
		// appears when they share a source line (or when a `;` appears
		// before the closing `}` on the last statement's line). We
		// avoid scanning comments/strings for stray `;` bytes by
		// gating on line numbers from the tokenizer.
		stmtEndLine := ctx.file.Position(stmt.End()).Line
		var nextStartLine int
		if i+1 < len(block.List) {
			nextStartLine = ctx.file.Position(block.List[i+1].Pos()).Line
		} else {
			nextStartLine = ctx.file.Position(block.Rbrace).Line
		}
		if stmtEndLine == nextStartLine {
			// Same line — look for the explicit `;`.
			boundary := 0
			if i+1 < len(block.List) {
				boundary = ctx.file.Offset(block.List[i+1].Pos())
			} else {
				boundary = ctx.file.Offset(block.Rbrace)
			}
			semiOffset := ctx.findNextBefore(';', boundary)
			if semiOffset >= 0 {
				rp.After = ctx.prefix(ctx.file.Pos(semiOffset))
				ctx.skip(1) // consume ";"
				rp.Markers = java.AddMarker(rp.Markers, golang.NewSemicolon())
			}
		}

		stmts = append(stmts, rp)
	}

	end := ctx.prefix(block.Rbrace)
	ctx.skip(1) // "}"

	return &java.Block{ID: uuid.New(), Prefix: prefix, Statements: stmts, End: end}
}

// mapStmt maps a statement.
func (ctx *parseContext) mapStmt(stmt ast.Stmt) java.Statement {
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

// mapReturnStmt maps a return statement. Zero- and single-value returns map to
// java.Return (mirroring Java's J.Return); multi-value returns (`return 0, nil`)
// map to golang.Return, just as multi-value assignments map to MultiAssignment.
func (ctx *parseContext) mapReturnStmt(stmt *ast.ReturnStmt) java.Statement {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("return"))

	var exprs []java.RightPadded[java.Expression]
	for i, expr := range stmt.Results {
		mapped := ctx.mapExpr(expr)
		var after java.Space
		if i < len(stmt.Results)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		}
		exprs = append(exprs, java.RightPadded[java.Expression]{Element: mapped, After: after})
	}

	if len(exprs) > 1 {
		return &golang.Return{ID: uuid.New(), Prefix: prefix, Expressions: exprs}
	}

	var single java.Expression
	if len(exprs) == 1 {
		single = exprs[0].Element
	}
	return &java.Return{ID: uuid.New(), Prefix: prefix, Expression: single}
}

// mapAssignStmt maps an assignment statement.
func (ctx *parseContext) mapAssignStmt(stmt *ast.AssignStmt) java.Statement {
	// Check for compound assignment operators (+=, -=, etc.) — always single LHS/RHS
	if len(stmt.Lhs) == 1 && len(stmt.Rhs) == 1 {
		// Go's `&^=` (bit-clear assign) has no J.AssignmentOperation.Type
		// equivalent → golang.AssignmentOperation.
		if stmt.Tok == token.AND_NOT_ASSIGN {
			lhs := ctx.mapExpr(stmt.Lhs[0])
			opPrefix := ctx.prefix(stmt.TokPos)
			ctx.skip(len(stmt.Tok.String()))
			rhs := ctx.mapExpr(stmt.Rhs[0])
			return &golang.AssignmentOperation{
				ID:         uuid.New(),
				Variable:   lhs,
				Operator:   java.LeftPadded[golang.AssignmentOperator]{Before: opPrefix, Element: golang.AssignAndNot},
				Assignment: rhs,
			}
		}
		if op, ok := mapAssignmentOp(stmt.Tok); ok {
			lhs := ctx.mapExpr(stmt.Lhs[0])
			opPrefix := ctx.prefix(stmt.TokPos)
			ctx.skip(len(stmt.Tok.String()))
			rhs := ctx.mapExpr(stmt.Rhs[0])
			return &java.AssignmentOperation{
				ID:         uuid.New(),
				Variable:   lhs,
				Operator:   java.LeftPadded[java.AssignmentOperator]{Before: opPrefix, Element: op},
				Assignment: rhs,
			}
		}
	}

	// Multi-value assignment: x, y = 1, 2 or x, y := f()
	if len(stmt.Lhs) > 1 || len(stmt.Rhs) > 1 {
		var lhsExprs []java.RightPadded[java.Expression]
		for i, expr := range stmt.Lhs {
			mapped := ctx.mapExpr(expr)
			var after java.Space
			if i < len(stmt.Lhs)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			lhsExprs = append(lhsExprs, java.RightPadded[java.Expression]{Element: mapped, After: after})
		}

		opPrefix := ctx.prefix(stmt.TokPos)
		ctx.skip(len(stmt.Tok.String()))

		var rhsExprs []java.RightPadded[java.Expression]
		for i, expr := range stmt.Rhs {
			mapped := ctx.mapExpr(expr)
			var after java.Space
			if i < len(stmt.Rhs)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			rhsExprs = append(rhsExprs, java.RightPadded[java.Expression]{Element: mapped, After: after})
		}

		var markers java.Markers
		if stmt.Tok == token.DEFINE {
			markers = java.Markers{
				ID:      uuid.New(),
				Entries: []java.Marker{golang.ShortVarDecl{Ident: uuid.New()}},
			}
		}

		return &golang.MultiAssignment{
			ID:        uuid.New(),
			Markers:   markers,
			Variables: lhsExprs,
			Operator:  java.LeftPadded[java.Space]{Before: opPrefix},
			Values:    rhsExprs,
		}
	}

	// Single assignment: x = 1 or x := 1
	lhs := ctx.mapExpr(stmt.Lhs[0])
	opPrefix := ctx.prefix(stmt.TokPos)
	ctx.skip(len(stmt.Tok.String()))
	rhs := ctx.mapExpr(stmt.Rhs[0])

	var markers java.Markers
	if stmt.Tok == token.DEFINE {
		markers = java.Markers{
			ID:      uuid.New(),
			Entries: []java.Marker{golang.ShortVarDecl{Ident: uuid.New()}},
		}
	}

	return &java.Assignment{
		ID:       uuid.New(),
		Prefix:   java.EmptySpace,
		Markers:  markers,
		Variable: lhs,
		Value:    java.LeftPadded[java.Expression]{Before: opPrefix, Element: rhs},
	}
}

func mapAssignmentOp(tok token.Token) (java.AssignmentOperator, bool) {
	switch tok {
	case token.ADD_ASSIGN:
		return java.AddAssign, true
	case token.SUB_ASSIGN:
		return java.SubAssign, true
	case token.MUL_ASSIGN:
		return java.MulAssign, true
	case token.QUO_ASSIGN:
		return java.DivAssign, true
	case token.REM_ASSIGN:
		return java.ModAssign, true
	case token.AND_ASSIGN:
		return java.AndAssign, true
	case token.OR_ASSIGN:
		return java.OrAssign, true
	case token.XOR_ASSIGN:
		return java.XorAssign, true
	case token.SHL_ASSIGN:
		return java.ShlAssign, true
	case token.SHR_ASSIGN:
		return java.ShrAssign, true
	case token.AND_NOT_ASSIGN:
		return java.AndNotAssign, true
	default:
		return 0, false
	}
}

// mapDeclStmt maps a declaration statement inside a function body.
func (ctx *parseContext) mapDeclStmt(stmt *ast.DeclStmt) java.Statement {
	switch d := stmt.Decl.(type) {
	case *ast.GenDecl:
		return ctx.mapGenDecl(d)
	default:
		return nil
	}
}

// mapExprStmt maps an expression statement.
func (ctx *parseContext) mapExprStmt(stmt *ast.ExprStmt) java.Statement {
	expr := ctx.mapExpr(stmt.X)
	if expr == nil {
		return nil
	}
	if s, ok := expr.(java.Statement); ok {
		return s
	}
	return nil
}

// mapIfStmt maps an if statement, including optional init: `if init; cond { }`.
func (ctx *parseContext) mapIfStmt(stmt *ast.IfStmt) *java.If {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("if"))

	var init *java.RightPadded[java.Statement]
	if stmt.Init != nil {
		initStmt := ctx.mapStmt(stmt.Init)
		semicolonOffset := ctx.findNext(';')
		var after java.Space
		if semicolonOffset >= 0 {
			after = ctx.prefix(ctx.file.Pos(semicolonOffset))
			ctx.skip(1) // ";"
		}
		rp := java.RightPadded[java.Statement]{Element: initStmt, After: after}
		init = &rp
	}

	cond := ctx.mapExpr(stmt.Cond)
	body := ctx.mapBlockStmt(stmt.Body)

	var elsePart *java.RightPadded[java.J]
	if stmt.Else != nil {
		// Find the `else` keyword between the closing `}` of Then and the start of Else
		elseOff := ctx.findNextString("else")
		if elseOff >= 0 {
			elsePrefix := ctx.prefix(ctx.file.Pos(elseOff))
			ctx.skip(len("else"))
			elseBody := ctx.mapStmt(stmt.Else)
			if elseBody != nil {
				rp := java.RightPadded[java.J]{Element: elseBody, After: elsePrefix}
				elsePart = &rp
			}
		}
	}

	return &java.If{
		ID:        uuid.New(),
		Prefix:    prefix,
		Init:      init,
		Condition: cond,
		Then:      body,
		ElsePart:  elsePart,
	}
}

// mapSwitchStmt maps a switch statement.
func (ctx *parseContext) mapSwitchStmt(stmt *ast.SwitchStmt) *java.Switch {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("switch"))

	var init *java.RightPadded[java.Statement]
	if stmt.Init != nil {
		initStmt := ctx.mapStmt(stmt.Init)
		semicolonOffset := ctx.findNext(';')
		var after java.Space
		if semicolonOffset >= 0 {
			after = ctx.prefix(ctx.file.Pos(semicolonOffset))
			ctx.skip(1) // ";"
		}
		rp := java.RightPadded[java.Statement]{Element: initStmt, After: after}
		init = &rp
	}

	var tag *java.RightPadded[java.Expression]
	if stmt.Tag != nil {
		tagExpr := ctx.mapExpr(stmt.Tag)
		rp := java.RightPadded[java.Expression]{Element: tagExpr}
		tag = &rp
	}

	body := ctx.mapBlockStmt(stmt.Body)

	return &java.Switch{
		ID:     uuid.New(),
		Prefix: prefix,
		Init:   init,
		Tag:    tag,
		Body:   body,
	}
}

// mapCaseClause maps a case or default clause.
func (ctx *parseContext) mapCaseClause(clause *ast.CaseClause) *java.Case {
	prefix := ctx.prefix(clause.Pos())

	var exprs java.Container[java.Expression]
	if len(clause.List) > 0 {
		// case expr1, expr2:
		ctx.skip(len("case"))
		var elements []java.RightPadded[java.Expression]
		for i, expr := range clause.List {
			mapped := ctx.mapExpr(expr)
			var after java.Space
			if i < len(clause.List)-1 {
				commaOffset := ctx.findNext(',')
				if commaOffset >= 0 {
					after = ctx.prefix(ctx.file.Pos(commaOffset))
					ctx.skip(1) // ","
				}
			}
			elements = append(elements, java.RightPadded[java.Expression]{Element: mapped, After: after})
		}
		exprs = java.Container[java.Expression]{Elements: elements}
	} else {
		// default:
		ctx.skip(len("default"))
	}

	// Skip the colon
	colonOffset := ctx.findNext(':')
	var colonPrefix java.Space
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
	var body []java.RightPadded[java.Statement]
	for _, stmt := range clause.Body {
		mapped := ctx.mapStmt(stmt)
		if mapped != nil {
			body = append(body, java.RightPadded[java.Statement]{Element: mapped})
		}
	}

	return &java.Case{
		ID:          uuid.New(),
		Prefix:      prefix,
		Expressions: exprs,
		Body:        body,
	}
}

// mapSelectStmt maps a select statement, reusing Switch+Case with SelectStmt marker.
func (ctx *parseContext) mapSelectStmt(stmt *ast.SelectStmt) *java.Switch {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("select"))
	body := ctx.mapBlockStmt(stmt.Body)

	return &java.Switch{
		ID:     uuid.New(),
		Prefix: prefix,
		Markers: java.Markers{
			ID:      uuid.New(),
			Entries: []java.Marker{golang.SelectStmt{Ident: uuid.New()}},
		},
		Body: body,
	}
}

// mapCommClause maps a communication clause in a select statement.
func (ctx *parseContext) mapCommClause(clause *ast.CommClause) *golang.CommClause {
	prefix := ctx.prefix(clause.Pos())

	var comm java.Statement
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
	var colonPrefix java.Space
	if colonOffset >= 0 {
		colonPrefix = ctx.prefix(ctx.file.Pos(colonOffset))
		ctx.skip(1)
	}

	var body []java.RightPadded[java.Statement]
	for _, stmt := range clause.Body {
		mapped := ctx.mapStmt(stmt)
		if mapped != nil {
			body = append(body, java.RightPadded[java.Statement]{Element: mapped})
		}
	}

	return &golang.CommClause{
		ID:     uuid.New(),
		Prefix: prefix,
		Comm:   comm,
		Colon:  colonPrefix,
		Body:   body,
	}
}

// mapTypeSwitchStmt maps a type switch statement.
// Uses Switch with TypeSwitchGuard marker. The assign/guard becomes the tag.
func (ctx *parseContext) mapTypeSwitchStmt(stmt *ast.TypeSwitchStmt) *java.Switch {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("switch"))

	var init *java.RightPadded[java.Statement]
	if stmt.Init != nil {
		initStmt := ctx.mapStmt(stmt.Init)
		semicolonOffset := ctx.findNext(';')
		var after java.Space
		if semicolonOffset >= 0 {
			after = ctx.prefix(ctx.file.Pos(semicolonOffset))
			ctx.skip(1)
		}
		rp := java.RightPadded[java.Statement]{Element: initStmt, After: after}
		init = &rp
	}

	// The assign is `x.(type)` (ExprStmt) or `v := x.(type)` (AssignStmt)
	var tag *java.RightPadded[java.Expression]
	switch a := stmt.Assign.(type) {
	case *ast.ExprStmt:
		// `x.(type)` — map the inner expression directly
		expr := ctx.mapExpr(a.X)
		if expr != nil {
			rp := java.RightPadded[java.Expression]{Element: expr}
			tag = &rp
		}
	case *ast.AssignStmt:
		// `v := x.(type)` — map as assignment (which is also an Expression-like construct here)
		assignStmt := ctx.mapAssignStmt(a)
		if expr, ok := assignStmt.(java.Expression); ok {
			rp := java.RightPadded[java.Expression]{Element: expr}
			tag = &rp
		}
	}

	body := ctx.mapBlockStmt(stmt.Body)

	return &java.Switch{
		ID:     uuid.New(),
		Prefix: prefix,
		Markers: java.Markers{
			ID:      uuid.New(),
			Entries: []java.Marker{golang.TypeSwitchGuard{Ident: uuid.New()}},
		},
		Init: init,
		Tag:  tag,
		Body: body,
	}
}

// mapEmptyStmt maps an empty statement (bare semicolons).
func (ctx *parseContext) mapEmptyStmt(stmt *ast.EmptyStmt) *java.Empty {
	prefix := ctx.prefix(stmt.Pos())
	if !stmt.Implicit {
		ctx.skip(1) // explicit ";"
	}
	return &java.Empty{ID: uuid.New(), Prefix: prefix}
}

// mapForStmt maps a for statement (classic 3-clause, condition-only, or infinite).
func (ctx *parseContext) mapForStmt(stmt *ast.ForStmt) *java.ForLoop {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("for"))

	control := java.ForControl{ID: uuid.New()}

	// Determine if this is a 3-clause for (has semicolons) or a simple for cond / for {}
	// Go's AST normalizes `for ; cond; {}` to Init=nil, Post=nil, same as `for cond {}`.
	// We detect semicolons by looking at the source text between for keyword and body.
	is3Clause := stmt.Init != nil || stmt.Post != nil
	bodyStart := int(stmt.Body.Lbrace) - ctx.file.Base()
	if !is3Clause {
		// `for ; cond; {}` has no Init/Post in the AST but is still
		// syntactically 3-clause. Detect by scanning for `;` in the
		// header — but skip rune/string literals so a `';'` inside the
		// condition (e.g. `for tok != ';'`) isn't mistaken for one.
		if ctx.findNextPositionOf(';', bodyStart) >= 0 {
			is3Clause = true
		}
	}

	if is3Clause {
		// 3-clause for: for [init]; [cond]; [post] {}
		if stmt.Init != nil {
			init := ctx.mapStmt(stmt.Init)
			semicolonOffset := ctx.findNextPositionOf(';', bodyStart)
			var after java.Space
			if semicolonOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(semicolonOffset))
				ctx.skip(1) // skip ";"
			}
			initRP := java.RightPadded[java.Statement]{Element: init, After: after}
			control.Init = &initRP
		} else {
			// No init but semicolons present: `for ; cond; post {}`
			semicolonOffset := ctx.findNextPositionOf(';', bodyStart)
			var after java.Space
			if semicolonOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(semicolonOffset))
				ctx.skip(1) // skip ";"
			}
			initRP := java.RightPadded[java.Statement]{Element: &java.Empty{ID: uuid.New()}, After: after}
			control.Init = &initRP
		}

		if stmt.Cond != nil {
			cond := ctx.mapExpr(stmt.Cond)
			semicolonOffset := ctx.findNextPositionOf(';', bodyStart)
			after := java.EmptySpace
			if semicolonOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(semicolonOffset))
				ctx.skip(1) // skip ";"
			}
			condRP := java.RightPadded[java.Expression]{Element: cond, After: after}
			control.Condition = &condRP
		} else {
			semicolonOffset := ctx.findNextPositionOf(';', bodyStart)
			after := java.EmptySpace
			if semicolonOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(semicolonOffset))
				ctx.skip(1) // skip ";"
			}
			emptyRP := java.RightPadded[java.Expression]{Element: &java.Empty{ID: uuid.New()}, After: after}
			control.Condition = &emptyRP
		}

		if stmt.Post != nil {
			post := ctx.mapStmt(stmt.Post)
			postRP := java.RightPadded[java.Statement]{Element: post}
			control.Update = &postRP
		}
	} else if stmt.Cond != nil {
		// Condition-only: for cond {}
		cond := ctx.mapExpr(stmt.Cond)
		condRP := java.RightPadded[java.Expression]{Element: cond}
		control.Condition = &condRP
	}
	// else: infinite loop, all nil

	body := ctx.mapBlockStmt(stmt.Body)

	return &java.ForLoop{
		ID:      uuid.New(),
		Prefix:  prefix,
		Control: control,
		Body:    body,
	}
}

// mapRangeStmt maps a for-range statement.
func (ctx *parseContext) mapRangeStmt(stmt *ast.RangeStmt) *java.ForEachLoop {
	prefix := ctx.prefixAndSkip(stmt.Pos(), len("for"))

	control := java.ForEachControl{ID: uuid.New()}

	if stmt.Key != nil {
		// Has key variable
		key := ctx.mapExpr(stmt.Key)

		if stmt.Value != nil {
			// for k, v := range expr {}
			// Key.After captures comma space
			commaOffset := ctx.findNext(',')
			var keyAfter java.Space
			if commaOffset >= 0 {
				keyAfter = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
			keyRP := java.RightPadded[java.Expression]{Element: key, After: keyAfter}
			control.Key = &keyRP

			value := ctx.mapExpr(stmt.Value)
			// Value.After captures space before operator
			opPrefix := ctx.prefix(stmt.TokPos)
			valueRP := java.RightPadded[java.Expression]{Element: value, After: opPrefix}
			control.Value = &valueRP
		} else {
			// for k := range expr {} — no value
			opPrefix := ctx.prefix(stmt.TokPos)
			keyRP := java.RightPadded[java.Expression]{Element: key, After: opPrefix}
			control.Key = &keyRP
		}

		// Parse operator (:= or =)
		var op java.AssignOp
		if stmt.Tok == token.DEFINE {
			op = java.AssignOpDefine
		} else {
			op = java.AssignOpEquals
		}
		ctx.skip(len(stmt.Tok.String()))

		// Space between operator and "range"
		rangePrefix := ctx.prefix(stmt.Range)
		control.Operator = java.LeftPadded[java.AssignOp]{Before: rangePrefix, Element: op}
	} else {
		// for range expr {} — no variable
		control.Prefix = ctx.prefix(stmt.Range)
	}
	ctx.skip(len("range"))

	iterable := ctx.mapExpr(stmt.X)
	control.Iterable = iterable

	body := ctx.mapBlockStmt(stmt.Body)

	return &java.ForEachLoop{
		ID:      uuid.New(),
		Prefix:  prefix,
		Control: control,
		Body:    body,
	}
}

// mapIncDecStmt maps an increment/decrement statement (x++ or x--).
func (ctx *parseContext) mapIncDecStmt(stmt *ast.IncDecStmt) *java.Unary {
	operand := ctx.mapExpr(stmt.X)
	opPrefix := ctx.prefix(stmt.TokPos)
	ctx.skip(len(stmt.Tok.String()))

	var op java.UnaryOperator
	if stmt.Tok == token.INC {
		op = java.PostIncrement
	} else {
		op = java.PostDecrement
	}

	// For postfix operators, the operand carries its own prefix (space before expression).
	// The Unary's prefix is empty; operator prefix captures space between operand and ++/--.
	return &java.Unary{
		ID:       uuid.New(),
		Operator: java.LeftPadded[java.UnaryOperator]{Before: opPrefix, Element: op},
		Operand:  operand,
	}
}

// mapGoStmt maps a `go expr` statement.
func (ctx *parseContext) mapGoStmt(stmt *ast.GoStmt) *golang.GoStmt {
	prefix := ctx.prefixAndSkip(stmt.Go, len("go"))
	expr := ctx.mapExpr(stmt.Call)
	return &golang.GoStmt{ID: uuid.New(), Prefix: prefix, Expr: expr}
}

// mapDeferStmt maps a `defer expr` statement.
func (ctx *parseContext) mapDeferStmt(stmt *ast.DeferStmt) *golang.Defer {
	prefix := ctx.prefixAndSkip(stmt.Defer, len("defer"))
	expr := ctx.mapExpr(stmt.Call)
	return &golang.Defer{ID: uuid.New(), Prefix: prefix, Expr: expr}
}

// mapSendStmt maps a channel send statement `ch <- value`.
func (ctx *parseContext) mapSendStmt(stmt *ast.SendStmt) *golang.Send {
	ch := ctx.mapExpr(stmt.Chan)
	arrowPrefix := ctx.prefix(stmt.Arrow)
	ctx.skip(2) // "<-"
	value := ctx.mapExpr(stmt.Value)
	return &golang.Send{
		ID:      uuid.New(),
		Channel: ch,
		Arrow:   java.LeftPadded[java.Expression]{Before: arrowPrefix, Element: value},
	}
}

// mapBranchStmt maps break, continue, goto, fallthrough.
func (ctx *parseContext) mapBranchStmt(stmt *ast.BranchStmt) java.Statement {
	switch stmt.Tok {
	case token.BREAK:
		prefix := ctx.prefixAndSkip(stmt.TokPos, len("break"))
		var label *java.Identifier
		if stmt.Label != nil {
			label = ctx.mapIdent(stmt.Label)
		}
		return &java.Break{ID: uuid.New(), Prefix: prefix, Label: label}
	case token.CONTINUE:
		prefix := ctx.prefixAndSkip(stmt.TokPos, len("continue"))
		var label *java.Identifier
		if stmt.Label != nil {
			label = ctx.mapIdent(stmt.Label)
		}
		return &java.Continue{ID: uuid.New(), Prefix: prefix, Label: label}
	case token.GOTO:
		prefix := ctx.prefixAndSkip(stmt.TokPos, len("goto"))
		label := ctx.mapIdent(stmt.Label)
		return &golang.Goto{ID: uuid.New(), Prefix: prefix, Label: label}
	case token.FALLTHROUGH:
		prefix := ctx.prefixAndSkip(stmt.TokPos, len("fallthrough"))
		return &golang.Fallthrough{ID: uuid.New(), Prefix: prefix}
	default:
		return nil
	}
}

// mapLabeledStmt maps a labeled statement `label: stmt`.
func (ctx *parseContext) mapLabeledStmt(stmt *ast.LabeledStmt) *java.Label {
	prefix := ctx.prefix(stmt.Label.Pos())
	name := ctx.mapIdent(stmt.Label)
	colonPrefix := ctx.prefix(stmt.Colon)
	ctx.skip(1) // ":"
	body := ctx.mapStmt(stmt.Stmt)
	return &java.Label{
		ID:        uuid.New(),
		Prefix:    prefix,
		Name:      java.RightPadded[*java.Identifier]{Element: name, After: colonPrefix},
		Statement: body,
	}
}

// mapExpr maps an expression.
func (ctx *parseContext) mapExpr(expr ast.Expr) java.Expression {
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
func (ctx *parseContext) mapIdent(ident *ast.Ident) *java.Identifier {
	prefix := ctx.prefix(ident.Pos())
	ctx.skip(len(ident.Name))
	id := &java.Identifier{ID: uuid.New(), Prefix: prefix, Name: ident.Name}

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
func (ctx *parseContext) mapBasicLit(lit *ast.BasicLit) *java.Literal {
	prefix := ctx.prefix(lit.Pos())
	ctx.skip(len(lit.Value))

	var kind java.LiteralKind
	switch lit.Kind {
	case token.INT:
		kind = java.IntLiteral
	case token.FLOAT:
		kind = java.FloatLiteral
	case token.STRING:
		kind = java.StringLiteral
	case token.CHAR:
		kind = java.CharLiteral
	default:
		kind = java.StringLiteral
	}

	l := &java.Literal{ID: uuid.New(), Prefix: prefix, Kind: kind, Value: lit.Value, Source: lit.Value}

	// Type attribution for literal
	if tv, ok := ctx.typeInfo.Types[lit]; ok {
		l.Type = ctx.mapper.mapType(tv.Type)
	}

	return l
}

// mapBinaryExpr maps a binary expression.
func (ctx *parseContext) mapBinaryExpr(expr *ast.BinaryExpr) java.Expression {
	left := ctx.mapExpr(expr.X)
	opPrefix := ctx.prefix(expr.OpPos)
	op := mapBinaryOp(expr.Op)
	ctx.skip(len(expr.Op.String()))
	right := ctx.mapExpr(expr.Y)

	// Go's `&^` (bit clear) has no J.Binary.Type equivalent → golang.Binary.
	if expr.Op == token.AND_NOT {
		return &golang.Binary{
			ID:       uuid.New(),
			Left:     left,
			Operator: java.LeftPadded[golang.BinaryOperator]{Before: opPrefix, Element: golang.BinAndNot},
			Right:    right,
		}
	}

	b := &java.Binary{
		ID:       uuid.New(),
		Left:     left,
		Operator: java.LeftPadded[java.BinaryOperator]{Before: opPrefix, Element: op},
		Right:    right,
	}

	// Type attribution for binary expression
	if tv, ok := ctx.typeInfo.Types[expr]; ok {
		b.Type = ctx.mapper.mapType(tv.Type)
	}

	return b
}

// mapCallExpr maps a function/method call.
func (ctx *parseContext) mapCallExpr(expr *ast.CallExpr) java.Expression {
	// `Map[int](42)` / `Pair[K, V](...)`: the callee is a generic function (or
	// type) instantiated with explicit type arguments. Go reuses *ast.IndexExpr
	// for both this and ordinary indexing (`funcs[0]()`), so disambiguate via the
	// type checker's Instances set before treating `[...]` as type arguments.
	// The callee X is mapped first, then the bracketed type args, matching the
	// positional cursor's left-to-right order.
	calleeAst := expr.Fun
	var typeParams *java.Container[java.Expression]
	var fun java.Expression
	switch f := expr.Fun.(type) {
	case *ast.IndexExpr:
		if ctx.isGenericInstantiation(f.X) {
			calleeAst = f.X
			fun = ctx.mapExpr(f.X)
			typeParams = ctx.mapTypeArgsSingle(f)
		} else {
			fun = ctx.mapExpr(expr.Fun)
		}
	case *ast.IndexListExpr:
		// Multiple bracketed args are only ever a generic instantiation; `a[i, j]`
		// is not valid Go indexing.
		calleeAst = f.X
		fun = ctx.mapExpr(f.X)
		typeParams = ctx.mapTypeArgsMulti(f)
	default:
		fun = ctx.mapExpr(expr.Fun)
	}

	var sel *java.RightPadded[java.Expression]
	var name *java.Identifier

	switch f := fun.(type) {
	case *java.FieldAccess:
		selRP := java.RightPadded[java.Expression]{Element: f.Target, After: f.Name.Before}
		sel = &selRP
		name = f.Name.Element
	case *java.Identifier:
		name = f
	default:
		// Callee is a complex expression (func literal, parenthesized, etc.)
		// Store it as Select with empty Name
		selRP := java.RightPadded[java.Expression]{Element: fun}
		sel = &selRP
		name = &java.Identifier{ID: uuid.New()}
	}

	argsBefore := ctx.prefix(expr.Lparen)
	ctx.skip(1) // "("

	var argElements []java.RightPadded[java.Expression]
	for i, arg := range expr.Args {
		mapped := ctx.mapExpr(arg)
		// Handle variadic spread: last arg with `...`
		if expr.Ellipsis.IsValid() && i == len(expr.Args)-1 {
			ellipsisPrefix := ctx.prefix(expr.Ellipsis)
			ctx.skip(3) // "..."
			mapped = &golang.Variadic{
				ID:      uuid.New(),
				Element: mapped,
				Dots:    ellipsisPrefix,
				Postfix: true,
			}
		}
		after := java.EmptySpace
		if i < len(expr.Args)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		}
		argElements = append(argElements, java.RightPadded[java.Expression]{Element: mapped, After: after})
	}

	// Check for trailing comma before closing paren
	var markers java.Markers
	if len(argElements) > 0 {
		// Look for a comma between current position and closing paren
		trailingCommaOff := ctx.findNextBefore(',', int(expr.Rparen)-ctx.file.Base())
		if trailingCommaOff >= 0 {
			commaBefore := ctx.prefix(ctx.file.Pos(trailingCommaOff))
			ctx.skip(1) // ","
			commaAfter := ctx.prefix(expr.Rparen)
			ctx.skip(1) // ")"
			markers = java.Markers{
				ID: uuid.New(),
				Entries: []java.Marker{golang.TrailingComma{
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
		closePrefix := ctx.prefix(expr.Rparen)
		ctx.skip(1) // ")"
		if len(closePrefix.Comments) > 0 {
			argElements = append(argElements, java.RightPadded[java.Expression]{
				Element: &java.Empty{ID: uuid.New()},
				After:   closePrefix,
			})
		}
	}

	mi := &java.MethodInvocation{
		ID:             uuid.New(),
		Prefix:         java.EmptySpace,
		Markers:        markers,
		Select:         sel,
		TypeParameters: typeParams,
		Name:           name,
		Arguments:      java.Container[java.Expression]{Before: argsBefore, Elements: argElements},
	}

	// Type attribution for method invocation. For a generic call `Map[int](...)`
	// calleeAst is the underlying callee (`Map`), not the IndexExpr.
	if selExpr, ok := calleeAst.(*ast.SelectorExpr); ok {
		if selection, ok := ctx.typeInfo.Selections[selExpr]; ok {
			mi.MethodType = ctx.mapper.mapSelectionToMethod(selection)
		} else if obj, ok := ctx.typeInfo.Uses[selExpr.Sel]; ok {
			// Qualified identifier (pkg.Func) — not a selection, but Sel is in Uses
			if fn, ok := obj.(*types.Func); ok {
				mi.MethodType = ctx.mapper.mapMethodObject(fn)
			}
		}
	} else if ident, ok := calleeAst.(*ast.Ident); ok {
		if obj, ok := ctx.typeInfo.Uses[ident]; ok {
			if fn, ok := obj.(*types.Func); ok {
				mi.MethodType = ctx.mapper.mapMethodObject(fn)
			}
		}
	}

	return mi
}

// isGenericInstantiation reports whether x — the operand of an *ast.IndexExpr
// used as a call target — denotes a generic function or type being instantiated
// with explicit type arguments (e.g. `Map` in `Map[int](42)`), as opposed to a
// value being indexed (e.g. `funcs` in `funcs[0]()`). It relies on the type
// checker's Instances set, which records exactly those generic-instantiation
// identifiers.
func (ctx *parseContext) isGenericInstantiation(x ast.Expr) bool {
	id := instanceIdent(x)
	if id == nil {
		return false
	}
	_, ok := ctx.typeInfo.Instances[id]
	return ok
}

// instanceIdent returns the identifier that types.Info.Instances would key on
// for a generic-instantiation operand: the identifier itself for a bare name
// (`Map`), or the selector's field for a qualified name (`pkg.Map`).
func instanceIdent(x ast.Expr) *ast.Ident {
	switch e := x.(type) {
	case *ast.Ident:
		return e
	case *ast.SelectorExpr:
		return e.Sel
	default:
		return nil
	}
}

// mapSelectorExpr maps a selector expression (e.g., pkg.Name).
func (ctx *parseContext) mapSelectorExpr(expr *ast.SelectorExpr) *java.FieldAccess {
	target := ctx.mapExpr(expr.X)

	dotOffset := ctx.findNext('.')
	var dotPrefix java.Space
	if dotOffset >= 0 {
		dotPrefix = ctx.prefix(ctx.file.Pos(dotOffset))
		ctx.skip(1) // "."
	}

	sel := ctx.mapIdent(expr.Sel)

	fa := &java.FieldAccess{
		ID:     uuid.New(),
		Target: target,
		Name:   java.LeftPadded[*java.Identifier]{Before: dotPrefix, Element: sel},
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
func (ctx *parseContext) mapUnaryExpr(expr *ast.UnaryExpr) java.Expression {
	prefix := ctx.prefix(expr.OpPos)
	ctx.skip(len(expr.Op.String()))
	operand := ctx.mapExpr(expr.X)

	// Go-specific operators (&, *, <-) have no J.Unary.Type equivalent, so they
	// map to golang.Unary; the rest stay as java.Unary so recipes can treat them
	// uniformly with other languages.
	switch expr.Op {
	case token.MUL:
		return &golang.Unary{
			ID:         uuid.New(),
			Prefix:     prefix,
			Operator:   java.LeftPadded[golang.UnaryOperator]{Element: golang.Indirection},
			Expression: operand,
		}
	case token.AND:
		return &golang.Unary{
			ID:         uuid.New(),
			Prefix:     prefix,
			Operator:   java.LeftPadded[golang.UnaryOperator]{Element: golang.AddressOf},
			Expression: operand,
		}
	case token.ARROW:
		return &golang.Unary{
			ID:         uuid.New(),
			Prefix:     prefix,
			Operator:   java.LeftPadded[golang.UnaryOperator]{Element: golang.Receive},
			Expression: operand,
		}
	}

	var op java.UnaryOperator
	switch expr.Op {
	case token.SUB:
		op = java.Negate
	case token.NOT:
		op = java.Not
	case token.XOR:
		op = java.BitwiseNot
	case token.ADD:
		op = java.Positive
	case token.TILDE:
		op = java.Tilde
	}

	return &java.Unary{
		ID:       uuid.New(),
		Prefix:   prefix,
		Operator: java.LeftPadded[java.UnaryOperator]{Element: op},
		Operand:  operand,
	}
}

// mapCompositeLit maps a composite literal (e.g., Type{elem1, elem2}).
func (ctx *parseContext) mapCompositeLit(expr *ast.CompositeLit) java.Expression {
	var typeExpr java.Expression
	if expr.Type != nil {
		typeExpr = ctx.mapTypeExpr(expr.Type)
	}
	lbracePrefix := ctx.prefix(expr.Lbrace)
	ctx.skip(1) // "{"

	var elements []java.RightPadded[java.Expression]
	for i, elt := range expr.Elts {
		mapped := ctx.mapExpr(elt)
		var after java.Space
		if i < len(expr.Elts)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		}
		elements = append(elements, java.RightPadded[java.Expression]{Element: mapped, After: after})
	}

	// Check for trailing comma before closing brace
	var compMarkers java.Markers
	if len(elements) > 0 {
		trailingCommaOff := ctx.findNextBefore(',', int(expr.Rbrace)-ctx.file.Base())
		if trailingCommaOff >= 0 {
			commaBefore := ctx.prefix(ctx.file.Pos(trailingCommaOff))
			ctx.skip(1) // ","
			commaAfter := ctx.prefix(expr.Rbrace)
			ctx.skip(1) // "}"
			compMarkers = java.Markers{
				ID: uuid.New(),
				Entries: []java.Marker{golang.TrailingComma{
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
		closePrefix := ctx.prefix(expr.Rbrace)
		ctx.skip(1) // "}"
		if len(closePrefix.Comments) > 0 {
			elements = append(elements, java.RightPadded[java.Expression]{
				Element: &java.Empty{ID: uuid.New()},
				After:   closePrefix,
			})
		}
	}

	return &golang.Composite{
		ID:       uuid.New(),
		Markers:  compMarkers,
		TypeExpr: typeExpr,
		Elements: java.Container[java.Expression]{Before: lbracePrefix, Elements: elements},
	}
}

// mapParenExpr maps a parenthesized expression.
func (ctx *parseContext) mapParenExpr(expr *ast.ParenExpr) java.Expression {
	prefix := ctx.prefix(expr.Lparen)
	ctx.skip(1) // "("
	inner := ctx.mapExpr(expr.X)
	closePrefix := ctx.prefix(expr.Rparen)
	ctx.skip(1) // ")"

	return &java.Parentheses{
		ID:     uuid.New(),
		Prefix: prefix,
		Tree:   java.RightPadded[java.Expression]{Element: inner, After: closePrefix},
	}
}

// mapStarExpr maps a star expression in value context as a pointer dereference
// (`*p`). Go's `*` has no J.Unary.Type equivalent, so it maps to golang.Unary.
func (ctx *parseContext) mapStarExpr(expr *ast.StarExpr) java.Expression {
	prefix := ctx.prefix(expr.Star)
	ctx.skip(1) // "*"
	operand := ctx.mapExpr(expr.X)

	return &golang.Unary{
		ID:         uuid.New(),
		Prefix:     prefix,
		Operator:   java.LeftPadded[golang.UnaryOperator]{Element: golang.Indirection},
		Expression: operand,
	}
}

// mapPointerType maps a star expression in a type context as a pointer type.
func (ctx *parseContext) mapPointerType(expr *ast.StarExpr) java.Expression {
	prefix := ctx.prefix(expr.Star)
	ctx.skip(1) // "*"
	elem := ctx.mapTypeExpr(expr.X)

	return &golang.PointerType{
		ID:     uuid.New(),
		Prefix: prefix,
		Elem:   elem,
	}
}

// mapTypeExpr maps an expression that is known to be in a type position.
// It delegates to mapExpr but overrides StarExpr to produce PointerType
// and IndexExpr to produce ParameterizedType (generic instantiation).
func (ctx *parseContext) mapTypeExpr(expr ast.Expr) java.Expression {
	switch e := expr.(type) {
	case *ast.StarExpr:
		return ctx.mapPointerType(e)
	case *ast.IndexExpr:
		return ctx.mapParameterizedType(e)
	case *ast.IndexListExpr:
		return ctx.mapParameterizedTypeMulti(e)
	case *ast.BinaryExpr:
		// A `|` in a type position is a type-set union constraint
		// (e.g. `~int | ~int8`), not a bitwise-or value expression.
		if e.Op == token.OR {
			return ctx.mapUnionType(e)
		}
		return ctx.mapExpr(expr)
	case *ast.UnaryExpr:
		// `~T` is an approximation element; `~` only ever appears in a
		// type-constraint position in Go.
		if e.Op == token.TILDE {
			return ctx.mapUnderlyingType(e)
		}
		return ctx.mapExpr(expr)
	default:
		return ctx.mapExpr(expr)
	}
}

// mapUnionType maps a type-set union constraint such as `~int | ~int8 | ~int16`
// to a golang.Union. Go parses `a | b | c` as a left-associative tree
// `((a | b) | c)`; this flattens it into source-ordered terms, recording
// the space before each `|` as the preceding term's After.
func (ctx *parseContext) mapUnionType(expr *ast.BinaryExpr) *golang.Union {
	var terms []java.RightPadded[java.Expression]
	ctx.appendUnionTerms(expr, &terms)
	return &golang.Union{ID: uuid.New(), Types: terms}
}

func (ctx *parseContext) appendUnionTerms(expr ast.Expr, terms *[]java.RightPadded[java.Expression]) {
	if be, ok := expr.(*ast.BinaryExpr); ok && be.Op == token.OR {
		ctx.appendUnionTerms(be.X, terms)
		// Space before this `|` belongs to the preceding term's After.
		opPrefix := ctx.prefix(be.OpPos)
		ctx.skip(1) // "|"
		(*terms)[len(*terms)-1].After = opPrefix
		*terms = append(*terms, java.RightPadded[java.Expression]{Element: ctx.mapTypeExpr(be.Y)})
		return
	}
	*terms = append(*terms, java.RightPadded[java.Expression]{Element: ctx.mapTypeExpr(expr)})
}

// mapUnderlyingType maps an approximation element `~T` to a
// golang.UnderlyingType. The space before `~` is the node's Prefix; any
// space between `~` and the type is carried by the element's own Prefix.
func (ctx *parseContext) mapUnderlyingType(expr *ast.UnaryExpr) *golang.UnderlyingType {
	prefix := ctx.prefix(expr.OpPos)
	ctx.skip(1) // "~"
	return &golang.UnderlyingType{
		ID:      uuid.New(),
		Prefix:  prefix,
		Element: ctx.mapTypeExpr(expr.X),
	}
}

// mapArrayType maps an array/slice type expression like `[]string` or `[5]int`.
func (ctx *parseContext) mapArrayType(expr *ast.ArrayType) java.Expression {
	prefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["

	var length java.Expression
	if expr.Len != nil {
		length = ctx.mapExpr(expr.Len)
	}

	// Find the `]`
	var closePrefix java.Space
	rbrackOff := ctx.findNext(']')
	if rbrackOff >= 0 {
		closePrefix = ctx.prefix(ctx.file.Pos(rbrackOff))
		ctx.skip(1) // "]"
	}

	elt := ctx.mapTypeExpr(expr.Elt)

	// Fixed-size arrays `[N]T` carry an inline length expression that
	// java.ArrayType (mirroring J.ArrayType) cannot hold; use golang.ArrayType.
	// Slices `[]T` have no length and map to java.ArrayType.
	if length != nil {
		return &golang.ArrayType{
			ID:          uuid.New(),
			Prefix:      prefix,
			Length:      java.RightPadded[java.Expression]{Element: length, After: closePrefix},
			ElementType: elt,
		}
	}

	return &java.ArrayType{
		ID:          uuid.New(),
		Prefix:      prefix,
		Dimension:   java.LeftPadded[java.Space]{Element: closePrefix},
		ElementType: elt,
	}
}

// mapParameterizedType maps a single-type-arg generic instantiation in a type position,
// e.g. `JSONArray[string]`, producing a J.ParameterizedType.
func (ctx *parseContext) mapParameterizedType(expr *ast.IndexExpr) java.Expression {
	target := ctx.mapExpr(expr.X)
	return &java.ParameterizedType{
		ID:             uuid.New(),
		Clazz:          target,
		TypeParameters: ctx.mapTypeArgsSingle(expr),
	}
}

// mapTypeArgsSingle consumes the `[T]` of a single-type-arg generic
// instantiation and returns the type-argument container. The caller must have
// already consumed everything up to (but not including) the `[`.
func (ctx *parseContext) mapTypeArgsSingle(expr *ast.IndexExpr) *java.Container[java.Expression] {
	lbrackPrefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["
	typeArg := ctx.mapTypeExpr(expr.Index)
	rbrackPrefix := ctx.prefix(expr.Rbrack)
	ctx.skip(1) // "]"
	return &java.Container[java.Expression]{
		Before:   lbrackPrefix,
		Elements: []java.RightPadded[java.Expression]{{Element: typeArg, After: rbrackPrefix}},
	}
}

// mapParameterizedTypeMulti maps a multi-type-arg generic instantiation in a type position,
// e.g. `Store[string, any]`, producing a J.ParameterizedType.
func (ctx *parseContext) mapParameterizedTypeMulti(expr *ast.IndexListExpr) java.Expression {
	target := ctx.mapExpr(expr.X)
	return &java.ParameterizedType{
		ID:             uuid.New(),
		Clazz:          target,
		TypeParameters: ctx.mapTypeArgsMulti(expr),
	}
}

// mapTypeArgsMulti consumes the `[T, U, ...]` of a multi-type-arg generic
// instantiation and returns the type-argument container. The caller must have
// already consumed everything up to (but not including) the `[`.
func (ctx *parseContext) mapTypeArgsMulti(expr *ast.IndexListExpr) *java.Container[java.Expression] {
	lbrackPrefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["

	var elements []java.RightPadded[java.Expression]
	for i, idx := range expr.Indices {
		mapped := ctx.mapTypeExpr(idx)
		var after java.Space
		if i < len(expr.Indices)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		} else {
			after = ctx.prefix(expr.Rbrack)
		}
		elements = append(elements, java.RightPadded[java.Expression]{Element: mapped, After: after})
	}
	ctx.skip(1) // "]"

	return &java.Container[java.Expression]{
		Before:   lbrackPrefix,
		Elements: elements,
	}
}

// mapIndexExpr maps an index expression like `a[i]` or `m[key]`.
func (ctx *parseContext) mapIndexExpr(expr *ast.IndexExpr) java.Expression {
	target := ctx.mapExpr(expr.X)
	lbrackPrefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["
	index := ctx.mapExpr(expr.Index)
	rbrackPrefix := ctx.prefix(expr.Rbrack)
	ctx.skip(1) // "]"

	return &java.ArrayAccess{
		ID:      uuid.New(),
		Indexed: target,
		Dimension: &java.ArrayDimension{
			ID:     uuid.New(),
			Prefix: lbrackPrefix,
			Index:  java.RightPadded[java.Expression]{Element: index, After: rbrackPrefix},
		},
	}
}

// mapIndexListExpr maps a multi-index expression like `Map[int, string]` (generic instantiation).
func (ctx *parseContext) mapIndexListExpr(expr *ast.IndexListExpr) java.Expression {
	target := ctx.mapExpr(expr.X)
	lbrackPrefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["

	var elements []java.RightPadded[java.Expression]
	for i, idx := range expr.Indices {
		mapped := ctx.mapExpr(idx)
		var after java.Space
		if i < len(expr.Indices)-1 {
			commaOffset := ctx.findNext(',')
			if commaOffset >= 0 {
				after = ctx.prefix(ctx.file.Pos(commaOffset))
				ctx.skip(1) // ","
			}
		} else {
			after = ctx.prefix(expr.Rbrack)
		}
		elements = append(elements, java.RightPadded[java.Expression]{Element: mapped, After: after})
	}
	ctx.skip(1) // "]"

	return &golang.IndexList{
		ID:      uuid.New(),
		Target:  target,
		Indices: java.Container[java.Expression]{Before: lbrackPrefix, Elements: elements},
	}
}

// mapTypeAssertExpr maps a type assertion `x.(T)`.
func (ctx *parseContext) mapTypeAssertExpr(expr *ast.TypeAssertExpr) java.Expression {
	x := ctx.mapExpr(expr.X)
	// dot before (
	dotOff := ctx.findNext('.')
	var dotPrefix java.Space
	if dotOff >= 0 {
		dotPrefix = ctx.prefix(ctx.file.Pos(dotOff))
		ctx.skip(1) // "."
	}
	lparenPrefix := ctx.prefix(expr.Lparen)
	ctx.skip(1) // "("

	var typeExpr java.Expression
	if expr.Type != nil {
		typeExpr = ctx.mapTypeExpr(expr.Type)
	} else {
		// type switch: x.(type)
		typePrefix := ctx.prefix(expr.Lparen + 1)
		ctx.skip(len("type"))
		typeExpr = &java.Identifier{ID: uuid.New(), Prefix: typePrefix, Name: "type"}
	}

	rparenPrefix := ctx.prefix(expr.Rparen)
	ctx.skip(1) // ")"

	clazz := &java.ControlParentheses{
		ID:     uuid.New(),
		Prefix: lparenPrefix,
		Tree:   java.RightPadded[java.Expression]{Element: typeExpr, After: rparenPrefix},
	}

	_ = dotPrefix // dot is between Expr and Clazz; stored in Expr's suffix isn't ideal
	// We'll use prefix of the type cast for the dot
	return &java.TypeCast{
		ID:     uuid.New(),
		Prefix: dotPrefix,
		Clazz:  clazz,
		Expr:   x,
	}
}

// mapFuncLit maps a function literal (closure).
func (ctx *parseContext) mapFuncLit(expr *ast.FuncLit) java.Expression {
	prefix := ctx.prefixAndSkip(expr.Type.Func, len("func"))
	params := ctx.mapFieldListAsParams(expr.Type.Params)
	returnType := ctx.mapReturnType(expr.Type.Results)
	body := ctx.mapBlockStmt(expr.Body)

	md := &java.MethodDeclaration{
		ID:         uuid.New(),
		Name:       &java.Identifier{ID: uuid.New(), Name: ""},
		Parameters: params,
		ReturnType: returnType,
		Body:       body,
	}
	// Wrap in StatementExpression so the MethodDeclaration (a Statement) can
	// appear in expression contexts like return statements, assignments, and
	// call arguments.
	return &golang.StatementExpression{
		ID:        uuid.New(),
		Prefix:    prefix,
		Statement: md,
	}
}

// mapKeyValueExpr maps a key:value expression in composite literals.
func (ctx *parseContext) mapKeyValueExpr(expr *ast.KeyValueExpr) java.Expression {
	key := ctx.mapExpr(expr.Key)
	colonPrefix := ctx.prefix(expr.Colon)
	ctx.skip(1) // ":"
	value := ctx.mapExpr(expr.Value)
	return &golang.KeyValue{
		ID:    uuid.New(),
		Key:   key,
		Value: java.LeftPadded[java.Expression]{Before: colonPrefix, Element: value},
	}
}

// mapSliceExpr maps a slice expression like `a[low:high]` or `a[low:high:max]`.
func (ctx *parseContext) mapSliceExpr(expr *ast.SliceExpr) java.Expression {
	target := ctx.mapExpr(expr.X)
	lbrackPrefix := ctx.prefix(expr.Lbrack)
	ctx.skip(1) // "["

	var low java.Expression
	if expr.Low != nil {
		low = ctx.mapExpr(expr.Low)
	} else {
		low = &java.Empty{ID: uuid.New()}
	}

	colon1Off := ctx.findNext(':')
	var colon1Prefix java.Space
	if colon1Off >= 0 {
		colon1Prefix = ctx.prefix(ctx.file.Pos(colon1Off))
		ctx.skip(1)
	}

	var high java.Expression
	if expr.High != nil {
		high = ctx.mapExpr(expr.High)
	} else {
		high = &java.Empty{ID: uuid.New()}
	}

	var max java.Expression
	var colon2Prefix java.Space
	if expr.Slice3 {
		colon2Off := ctx.findNext(':')
		if colon2Off >= 0 {
			colon2Prefix = ctx.prefix(ctx.file.Pos(colon2Off))
			ctx.skip(1)
		}
		if expr.Max != nil {
			max = ctx.mapExpr(expr.Max)
		} else {
			max = &java.Empty{ID: uuid.New()}
		}
	}

	rbrackPrefix := ctx.prefix(expr.Rbrack)
	ctx.skip(1) // "]"

	return &golang.Slice{
		ID:           uuid.New(),
		Indexed:      target,
		OpenBracket:  lbrackPrefix,
		Low:          java.RightPadded[java.Expression]{Element: low, After: colon1Prefix},
		High:         java.RightPadded[java.Expression]{Element: high, After: colon2Prefix},
		Max:          max,
		CloseBracket: rbrackPrefix,
	}
}

// mapMapType maps a map type expression like `map[K]V`.
func (ctx *parseContext) mapMapType(expr *ast.MapType) java.Expression {
	prefix := ctx.prefixAndSkip(expr.Map, len("map"))
	lbrackOff := ctx.findNext('[')
	var lbrackPrefix java.Space
	if lbrackOff >= 0 {
		lbrackPrefix = ctx.prefix(ctx.file.Pos(lbrackOff))
		ctx.skip(1) // "["
	} else {
		ctx.skip(1) // "["
	}
	key := ctx.mapTypeExpr(expr.Key)
	rbrackOff := ctx.findNext(']')
	var rbrackPrefix java.Space
	if rbrackOff >= 0 {
		rbrackPrefix = ctx.prefix(ctx.file.Pos(rbrackOff))
		ctx.skip(1)
	}
	value := ctx.mapTypeExpr(expr.Value)

	return &golang.MapType{
		ID:          uuid.New(),
		Prefix:      prefix,
		OpenBracket: lbrackPrefix,
		Key:         java.RightPadded[java.Expression]{Element: key, After: rbrackPrefix},
		Value:       value,
	}
}

// mapChanType maps a channel type expression.
func (ctx *parseContext) mapChanType(expr *ast.ChanType) java.Expression {
	prefix := ctx.prefix(expr.Begin)
	var markers java.Markers

	var dir golang.ChanDir
	switch expr.Dir {
	case ast.SEND:
		dir = golang.ChanSendOnly
		ctx.skip(len("chan"))
		arrowOff := ctx.findNext('<')
		var dirMarkerBefore java.Space
		if arrowOff >= 0 {
			dirMarkerBefore = ctx.prefix(ctx.file.Pos(arrowOff))
			ctx.cursor = arrowOff
		}
		ctx.skip(2) // "<-"
		if !dirMarkerBefore.IsEmpty() {
			markers = java.Markers{
				ID: uuid.New(),
				Entries: []java.Marker{golang.ChanDirMarker{
					Ident:  uuid.New(),
					Before: dirMarkerBefore,
				}},
			}
		}
	case ast.RECV:
		dir = golang.ChanRecvOnly
		ctx.skip(2) // "<-"
		chanOff := ctx.findNext('c')
		var dirMarkerBefore java.Space
		if chanOff >= 0 && chanOff+4 <= len(ctx.src) && string(ctx.src[chanOff:chanOff+4]) == "chan" {
			dirMarkerBefore = ctx.prefix(ctx.file.Pos(chanOff))
			ctx.cursor = chanOff
		}
		ctx.skip(len("chan"))
		if !dirMarkerBefore.IsEmpty() {
			markers = java.Markers{
				ID: uuid.New(),
				Entries: []java.Marker{golang.ChanDirMarker{
					Ident:  uuid.New(),
					Before: dirMarkerBefore,
				}},
			}
		}
	default:
		dir = golang.ChanBidi
		ctx.skip(len("chan"))
	}

	value := ctx.mapTypeExpr(expr.Value)
	return &golang.Channel{
		ID:      uuid.New(),
		Prefix:  prefix,
		Markers: markers,
		Dir:     dir,
		Value:   value,
	}
}

// mapFuncType maps a function type expression like `func(int) string`.
func (ctx *parseContext) mapFuncType(expr *ast.FuncType) java.Expression {
	prefix := ctx.prefixAndSkip(expr.Func, len("func"))
	params := ctx.mapFieldListAsParams(expr.Params)
	returnType := ctx.mapReturnType(expr.Results)

	return &golang.FuncType{
		ID:         uuid.New(),
		Prefix:     prefix,
		Parameters: params,
		ReturnType: returnType,
	}
}

// mapInterfaceType maps an interface type expression: `interface { methods }`.
func (ctx *parseContext) mapInterfaceType(expr *ast.InterfaceType) java.Expression {
	prefix := ctx.prefixAndSkip(expr.Interface, len("interface"))
	body := ctx.mapFieldListAsInterfaceBody(expr.Methods)
	return &golang.InterfaceType{
		ID:     uuid.New(),
		Prefix: prefix,
		Body:   body,
	}
}

// mapStructType maps a struct type expression: `struct { fields }`.
func (ctx *parseContext) mapStructType(expr *ast.StructType) java.Expression {
	prefix := ctx.prefixAndSkip(expr.Struct, len("struct"))
	body := ctx.mapFieldListAsStructBody(expr.Fields)
	return &golang.StructType{
		ID:     uuid.New(),
		Prefix: prefix,
		Body:   body,
	}
}

// mapFieldListAsStructBody maps a struct's field list to a Block.
// Each field becomes a VariableDeclarations statement.
func (ctx *parseContext) mapFieldListAsStructBody(fl *ast.FieldList) *java.Block {
	blockPrefix := ctx.prefix(fl.Opening)
	ctx.skip(1) // "{"

	var stmts []java.RightPadded[java.Statement]
	for _, field := range fl.List {
		if len(field.Names) == 0 {
			// Embedded type (e.g., `io.Reader` in struct)
			typeExpr := ctx.mapTypeExpr(field.Type)
			vd := &java.VariableDeclarations{
				ID:       uuid.New(),
				TypeExpr: typeExpr,
				Variables: []java.RightPadded[*java.VariableDeclarator]{
					{Element: &java.VariableDeclarator{ID: uuid.New(), Name: &java.Identifier{ID: uuid.New()}}},
				},
			}
			// Map struct tag if present
			if field.Tag != nil {
				ctx.mapStructTag(vd, field.Tag)
			}
			stmts = append(stmts, java.RightPadded[java.Statement]{Element: vd})
		} else {
			// Named field(s): `X int` or `X, Y int`
			var vars []java.RightPadded[*java.VariableDeclarator]
			for j, fieldName := range field.Names {
				nameIdent := ctx.mapIdent(fieldName)
				var nameAfter java.Space
				if j < len(field.Names)-1 {
					commaOffset := ctx.findNext(',')
					if commaOffset >= 0 {
						nameAfter = ctx.prefix(ctx.file.Pos(commaOffset))
						ctx.skip(1) // ","
					}
				}
				vars = append(vars, java.RightPadded[*java.VariableDeclarator]{
					Element: &java.VariableDeclarator{ID: uuid.New(), Name: nameIdent},
					After:   nameAfter,
				})
			}
			typeExpr := ctx.mapTypeExpr(field.Type)
			vd := &java.VariableDeclarations{
				ID:        uuid.New(),
				TypeExpr:  typeExpr,
				Variables: vars,
			}
			// Map struct tag if present
			if field.Tag != nil {
				ctx.mapStructTag(vd, field.Tag)
			}
			stmts = append(stmts, java.RightPadded[java.Statement]{Element: vd})
		}
	}

	end := ctx.prefix(fl.Closing)
	ctx.skip(1) // "}"

	return &java.Block{ID: uuid.New(), Prefix: blockPrefix, Statements: stmts, End: end}
}

// mapStructTag parses a struct field tag literal into a sequence of
// Annotations — one per `key:"value"` pair — and attaches them to
// vd.LeadingAnnotations.
//
// Mirrors `reflect.StructTag.Lookup` parsing semantics: leading spaces
// between pairs are skipped (any number), keys run up to a colon,
// values are double-quoted strings respecting Go escape sequences.
//
// Whitespace policy (Option 1, lossy on non-canonical input):
//   - The space between the field type and the opening backtick goes
//     onto the first annotation's Prefix.
//   - Whitespace between two `key:"value"` pairs goes onto the next
//     annotation's Prefix.
//   - Whitespace IMMEDIATELY inside the backticks (e.g.,
//     `  json:"x"  `) is dropped — gofmt produces zero inner padding,
//     so this only affects hand-typed weird input. Roundtrip on
//     gofmt'd input is exact.
func (ctx *parseContext) mapStructTag(vd *java.VariableDeclarations, tag *ast.BasicLit) {
	outerPrefix := ctx.prefix(tag.Pos())
	ctx.skip(len(tag.Value))

	// tag.Value includes the wrapping backticks (or quotes).
	raw := tag.Value
	if len(raw) >= 2 {
		first, last := raw[0], raw[len(raw)-1]
		if (first == '`' && last == '`') || (first == '"' && last == '"') {
			raw = raw[1 : len(raw)-1]
		}
	}

	pairs := parseStructTagPairs(raw)
	if len(pairs) == 0 {
		return
	}

	annotations := make([]*java.Annotation, len(pairs))
	for i, p := range pairs {
		var annPrefix java.Space
		if i == 0 {
			annPrefix = outerPrefix
		} else {
			annPrefix = java.Space{Whitespace: p.PrefixWS}
		}
		annotations[i] = &java.Annotation{
			ID:     uuid.New(),
			Prefix: annPrefix,
			AnnotationType: &java.Identifier{
				ID:   uuid.New(),
				Name: p.Key,
			},
			Arguments: &java.Container[java.Expression]{
				Elements: []java.RightPadded[java.Expression]{
					{Element: &java.Literal{
						ID:     uuid.New(),
						Source: p.QuotedValue,
						Value:  p.UnquotedValue,
						Kind:   java.StringLiteral,
					}},
				},
			},
		}
	}
	vd.LeadingAnnotations = annotations
}

// extractDirectives splits a Space's leading line-comments into Annotation
// nodes when they match Go directive syntax (`//go:NAME [args]`,
// `//lint:NAME [args]`). The returned residual Space holds whatever
// wasn't extracted: the whitespace after the last extracted directive,
// plus any comments past the first non-directive (or block-comment).
//
// Used by the parser at top-level decl entry points (func, type,
// var/const) to populate `LeadingAnnotations` and shrink the decl's
// own Prefix to the whitespace between last directive and keyword.
func extractDirectives(s java.Space) (anns []*java.Annotation, residual java.Space) {
	if len(s.Comments) == 0 {
		return nil, s
	}
	pendingPrefixWS := s.Whitespace
	i := 0
	for i < len(s.Comments) {
		c := s.Comments[i]
		if c.Kind != java.LineComment {
			break
		}
		name, args, ok := parseDirective(c.Text)
		if !ok {
			break
		}
		anns = append(anns, buildDirectiveAnnotation(name, args, java.Space{Whitespace: pendingPrefixWS}))
		pendingPrefixWS = c.Suffix
		i++
	}
	if len(anns) == 0 {
		return nil, s
	}
	residual = java.Space{
		Whitespace: pendingPrefixWS,
		Comments:   s.Comments[i:],
	}
	return anns, residual
}

// parseDirective tries to parse a `//PREFIX:NAME [ARGS]` line into
// (name, args, ok). The full directive name returned is `PREFIX:NAME`
// — preserved exactly as authors write it (`go:noinline`,
// `lint:ignore`). `args` is the trimmed text after the first space (or
// "" when absent).
//
// Recognized prefixes: `go`, `lint`. (Other vendor-specific prefixes
// like `nolint` aren't of the form `PREFIX:NAME` and are left as
// regular comments.)
func parseDirective(text string) (name, args string, ok bool) {
	if !strings.HasPrefix(text, "//") {
		return "", "", false
	}
	inner := text[2:]
	colonIdx := strings.Index(inner, ":")
	if colonIdx <= 0 {
		return "", "", false
	}
	prefix := inner[:colonIdx]
	if !isDirectivePrefix(prefix) {
		return "", "", false
	}
	rest := inner[colonIdx+1:]
	spaceIdx := strings.IndexAny(rest, " \t")
	if spaceIdx < 0 {
		if rest == "" {
			return "", "", false
		}
		return prefix + ":" + rest, "", true
	}
	dirName := rest[:spaceIdx]
	if dirName == "" {
		return "", "", false
	}
	dirArgs := strings.TrimLeft(rest[spaceIdx:], " \t")
	return prefix + ":" + dirName, dirArgs, true
}

func isDirectivePrefix(p string) bool {
	switch p {
	case "go", "lint":
		return true
	}
	return false
}

func buildDirectiveAnnotation(name, args string, prefix java.Space) *java.Annotation {
	ann := &java.Annotation{
		ID:     uuid.New(),
		Prefix: prefix,
		AnnotationType: &java.Identifier{
			ID:   uuid.New(),
			Name: name,
		},
	}
	if args != "" {
		ann.Arguments = &java.Container[java.Expression]{
			Before: java.Space{Whitespace: " "},
			Elements: []java.RightPadded[java.Expression]{
				{Element: &java.Literal{
					ID:     uuid.New(),
					Source: args,
					Value:  args,
					Kind:   java.StringLiteral,
				}},
			},
		}
	}
	return ann
}

// structTagPair is one parsed `key:"value"` pair from a struct tag.
type structTagPair struct {
	PrefixWS      string // whitespace consumed before this pair (used only for non-first pairs)
	Key           string
	QuotedValue   string // the value source including its surrounding quotes (e.g. `"name"`)
	UnquotedValue string // the value contents after Go-string unquoting
}

// parseStructTagPairs scans a struct tag's contents (without the
// surrounding backticks) into a sequence of `key:"value"` pairs.
// Mirrors `reflect.StructTag.Lookup`'s scanning loop:
//   - Skip ASCII whitespace.
//   - Read key (printable, non-quote, non-colon, non-control).
//   - Expect `:"`, read quoted string respecting `\` escapes.
//
// Returns whatever pairs it parsed up to the first malformed section;
// gofmt'd input is always well-formed but defensive scanning matches
// stdlib behavior.
func parseStructTagPairs(tag string) []structTagPair {
	var pairs []structTagPair
	i := 0
	for i < len(tag) {
		// Skip leading whitespace.
		prefStart := i
		for i < len(tag) && (tag[i] == ' ' || tag[i] == '\t' || tag[i] == '\n' || tag[i] == '\r') {
			i++
		}
		prefixWS := tag[prefStart:i]
		if i == len(tag) {
			break
		}
		// Read key.
		keyStart := i
		for i < len(tag) && tag[i] > ' ' && tag[i] != ':' && tag[i] != '"' && tag[i] != 0x7f {
			i++
		}
		if i == keyStart || i+1 >= len(tag) || tag[i] != ':' || tag[i+1] != '"' {
			break
		}
		key := tag[keyStart:i]
		i++ // skip `:`
		// Read quoted value.
		valueStart := i
		i++ // skip opening `"`
		for i < len(tag) && tag[i] != '"' {
			if tag[i] == '\\' {
				i++
			}
			i++
		}
		if i >= len(tag) {
			break
		}
		i++ // skip closing `"`
		quotedValue := tag[valueStart:i]
		unquoted, err := strconv.Unquote(quotedValue)
		if err != nil {
			break
		}
		pairs = append(pairs, structTagPair{
			PrefixWS:      prefixWS,
			Key:           key,
			QuotedValue:   quotedValue,
			UnquotedValue: unquoted,
		})
	}
	return pairs
}

// mapFieldListAsInterfaceBody maps an interface's method list to a Block.
// Each method becomes a MethodDeclaration (no body) or embedded type reference.
func (ctx *parseContext) mapFieldListAsInterfaceBody(fl *ast.FieldList) *java.Block {
	blockPrefix := ctx.prefix(fl.Opening)
	ctx.skip(1) // "{"

	var stmts []java.RightPadded[java.Statement]
	for _, field := range fl.List {
		if len(field.Names) == 0 {
			// Embedded interface type (e.g., `io.Reader`)
			typeExpr := ctx.mapTypeExpr(field.Type)
			vd := &java.VariableDeclarations{
				ID:       uuid.New(),
				TypeExpr: typeExpr,
				Variables: []java.RightPadded[*java.VariableDeclarator]{
					{Element: &java.VariableDeclarator{ID: uuid.New(), Name: &java.Identifier{ID: uuid.New()}}},
				},
			}
			stmts = append(stmts, java.RightPadded[java.Statement]{Element: vd})
		} else {
			// Method signature: `Name(params) returnType`
			name := ctx.mapIdent(field.Names[0])
			funcType := field.Type.(*ast.FuncType)
			params := ctx.mapFieldListAsParams(funcType.Params)
			returnType := ctx.mapReturnType(funcType.Results)

			md := &java.MethodDeclaration{
				ID:   uuid.New(),
				Name: name,
				Markers: java.Markers{
					Entries: []java.Marker{golang.InterfaceMethod{Ident: uuid.New()}},
				},
				Parameters: params,
				ReturnType: returnType,
				// Body is nil — interface method has no body
			}
			stmts = append(stmts, java.RightPadded[java.Statement]{Element: md})
		}
	}

	end := ctx.prefix(fl.Closing)
	ctx.skip(1) // "}"

	return &java.Block{ID: uuid.New(), Prefix: blockPrefix, Statements: stmts, End: end}
}

// mapEllipsis maps `...T` in function parameters (prefix variadic form).
func (ctx *parseContext) mapEllipsis(expr *ast.Ellipsis) java.Expression {
	prefix := ctx.prefix(expr.Ellipsis)
	ctx.skip(3) // "..."
	elt := ctx.mapTypeExpr(expr.Elt)
	return &golang.Variadic{
		ID:      uuid.New(),
		Prefix:  prefix,
		Element: elt,
		Dots:    java.EmptySpace,
		Postfix: false,
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

func mapBinaryOp(op token.Token) java.BinaryOperator {
	switch op {
	case token.ADD:
		return java.Add
	case token.SUB:
		return java.Subtract
	case token.MUL:
		return java.Multiply
	case token.QUO:
		return java.Divide
	case token.REM:
		return java.Modulo
	case token.EQL:
		return java.Equal
	case token.NEQ:
		return java.NotEqual
	case token.LSS:
		return java.LessThan
	case token.LEQ:
		return java.LessThanOrEqual
	case token.GTR:
		return java.GreaterThan
	case token.GEQ:
		return java.GreaterThanOrEqual
	case token.LAND:
		return java.LogicalAnd
	case token.LOR:
		return java.LogicalOr
	case token.AND:
		return java.BitwiseAnd
	case token.OR:
		return java.BitwiseOr
	case token.XOR:
		return java.BitwiseXor
	case token.SHL:
		return java.LeftShift
	case token.SHR:
		return java.RightShift
	case token.AND_NOT:
		return java.AndNot
	default:
		return java.Add
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

// findNextPositionOf is like findNextBefore but skips over Go rune
// literals ('...'), interpreted string literals ("..."), raw string literals
// (`...`), and `//` / `/* */` comments while scanning. A `before` of 0 means
// scan to end of src. Used for syntactic markers like `;` in a `for` header
// that can otherwise hide inside a `';'` rune literal or a `/* ; */` comment.
func (ctx *parseContext) findNextPositionOf(ch byte, before int) int {
	end := len(ctx.src)
	if before > 0 && before < end {
		end = before
	}
	i := ctx.cursor
	for i < end {
		b := ctx.src[i]
		switch {
		case b == '\'' || b == '"':
			quote := b
			i++
			for i < end {
				c := ctx.src[i]
				if c == '\\' && i+1 < end {
					i += 2
					continue
				}
				i++
				if c == quote {
					break
				}
			}
		case b == '`':
			i++
			for i < end && ctx.src[i] != '`' {
				i++
			}
			if i < end {
				i++
			}
		case b == '/' && i+1 < end && ctx.src[i+1] == '/':
			i += 2
			for i < end && ctx.src[i] != '\n' {
				i++
			}
		case b == '/' && i+1 < end && ctx.src[i+1] == '*':
			i += 2
			for i+1 < end && !(ctx.src[i] == '*' && ctx.src[i+1] == '/') {
				i++
			}
			if i+1 < end {
				i += 2
			}
		default:
			if b == ch {
				return i
			}
			i++
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
