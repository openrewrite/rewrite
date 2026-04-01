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

package tree

import "github.com/google/uuid"

// CompilationUnit represents a Go source file.
type CompilationUnit struct {
	ID          uuid.UUID
	Prefix      Space
	Markers     Markers
	SourcePath  string
	PackageDecl *RightPadded[*Identifier] // `package main`
	Imports     *Container[*Import]       // nil if no imports
	Statements  []RightPadded[Statement]  // top-level declarations
	EOF         Space
}

func (*CompilationUnit) isTree()       {}
func (*CompilationUnit) isJ()          {}
func (*CompilationUnit) isSourceFile() {}

func (n *CompilationUnit) WithPrefix(prefix Space) *CompilationUnit {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *CompilationUnit) WithMarkers(markers Markers) *CompilationUnit {
	c := *n
	c.Markers = markers
	return &c
}

func (n *CompilationUnit) WithStatements(statements []RightPadded[Statement]) *CompilationUnit {
	c := *n
	c.Statements = statements
	return &c
}

func (n *CompilationUnit) WithPackageDecl(pkg *RightPadded[*Identifier]) *CompilationUnit {
	c := *n
	c.PackageDecl = pkg
	return &c
}

func (n *CompilationUnit) WithImports(imports *Container[*Import]) *CompilationUnit {
	c := *n
	c.Imports = imports
	return &c
}

func (n *CompilationUnit) WithEOF(eof Space) *CompilationUnit {
	c := *n
	c.EOF = eof
	return &c
}

// Go represents a `go expr` statement (goroutine launch).
type GoStmt struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Expr    Expression
}

func (*GoStmt) isTree()      {}
func (*GoStmt) isJ()         {}
func (*GoStmt) isStatement() {}

func (n *GoStmt) WithPrefix(prefix Space) *GoStmt {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *GoStmt) WithMarkers(markers Markers) *GoStmt {
	c := *n
	c.Markers = markers
	return &c
}

// Defer represents a `defer expr` statement.
type Defer struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Expr    Expression
}

func (*Defer) isTree()      {}
func (*Defer) isJ()         {}
func (*Defer) isStatement() {}

func (n *Defer) WithPrefix(prefix Space) *Defer {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Defer) WithMarkers(markers Markers) *Defer {
	c := *n
	c.Markers = markers
	return &c
}

// Send represents a channel send statement: `ch <- value`.
type Send struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Channel Expression
	Arrow   LeftPadded[Expression] // Before = space before `<-`
}

func (*Send) isTree()      {}
func (*Send) isJ()         {}
func (*Send) isStatement() {}

func (n *Send) WithPrefix(prefix Space) *Send {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Send) WithMarkers(markers Markers) *Send {
	c := *n
	c.Markers = markers
	return &c
}

// Goto represents a `goto label` statement.
type Goto struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Label   *Identifier
}

func (*Goto) isTree()      {}
func (*Goto) isJ()         {}
func (*Goto) isStatement() {}

func (n *Goto) WithPrefix(prefix Space) *Goto {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Goto) WithMarkers(markers Markers) *Goto {
	c := *n
	c.Markers = markers
	return &c
}

// Fallthrough represents a `fallthrough` statement in a switch case.
type Fallthrough struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
}

func (*Fallthrough) isTree()      {}
func (*Fallthrough) isJ()         {}
func (*Fallthrough) isStatement() {}

func (n *Fallthrough) WithPrefix(prefix Space) *Fallthrough {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Fallthrough) WithMarkers(markers Markers) *Fallthrough {
	c := *n
	c.Markers = markers
	return &c
}

// Composite represents a composite literal: `Type{elem1, elem2}`.
type Composite struct {
	ID       uuid.UUID
	Prefix   Space
	Markers  Markers
	TypeExpr Expression            // nil for untyped composite literals
	Elements Container[Expression] // Before = space before `{`, elements, last After = space before `}`
}

func (*Composite) isTree()       {}
func (*Composite) isJ()          {}
func (*Composite) isExpression() {}

func (n *Composite) WithPrefix(prefix Space) *Composite {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Composite) WithMarkers(markers Markers) *Composite {
	c := *n
	c.Markers = markers
	return &c
}

// KeyValue represents a `key: value` pair in composite literals.
type KeyValue struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Key     Expression
	Value   LeftPadded[Expression] // Before = space before `:`
}

func (*KeyValue) isTree()       {}
func (*KeyValue) isJ()          {}
func (*KeyValue) isExpression() {}

func (n *KeyValue) WithPrefix(prefix Space) *KeyValue {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *KeyValue) WithMarkers(markers Markers) *KeyValue {
	c := *n
	c.Markers = markers
	return &c
}

// Slice represents a slice expression: `a[low:high]` or `a[low:high:max]`.
type Slice struct {
	ID           uuid.UUID
	Prefix       Space
	Markers      Markers
	Indexed      Expression
	OpenBracket  Space                   // space before `[`
	Low          RightPadded[Expression] // After = space before first `:`
	High         RightPadded[Expression] // After = space before second `:` (empty if 2-index)
	Max          Expression              // nil for 2-index slices
	CloseBracket Space                   // space before `]`
}

func (*Slice) isTree()       {}
func (*Slice) isJ()          {}
func (*Slice) isExpression() {}

func (n *Slice) WithPrefix(prefix Space) *Slice {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Slice) WithMarkers(markers Markers) *Slice {
	c := *n
	c.Markers = markers
	return &c
}

// MapType represents a map type expression: `map[K]V`.
type MapType struct {
	ID          uuid.UUID
	Prefix      Space
	Markers     Markers
	OpenBracket Space                   // space before `[`
	Key         RightPadded[Expression] // After = space before `]`
	Value       Expression
}

func (*MapType) isTree()       {}
func (*MapType) isJ()          {}
func (*MapType) isExpression() {}

func (n *MapType) WithPrefix(prefix Space) *MapType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *MapType) WithMarkers(markers Markers) *MapType {
	c := *n
	c.Markers = markers
	return &c
}

// ChanDir represents the direction of a channel type.
type ChanDir int

const (
	ChanBidi     ChanDir = iota // chan T
	ChanSendOnly                // chan<- T
	ChanRecvOnly                // <-chan T
)

// Channel represents a channel type expression: `chan T`, `chan<- T`, `<-chan T`.
type Channel struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Dir     ChanDir
	Value   Expression
}

func (*Channel) isTree()       {}
func (*Channel) isJ()          {}
func (*Channel) isExpression() {}

func (n *Channel) WithPrefix(prefix Space) *Channel {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Channel) WithMarkers(markers Markers) *Channel {
	c := *n
	c.Markers = markers
	return &c
}

// FuncType represents a function type expression: `func(int) string`.
type FuncType struct {
	ID         uuid.UUID
	Prefix     Space
	Markers    Markers
	Parameters Container[Statement]
	ReturnType Expression // nil if no return type
}

func (*FuncType) isTree()       {}
func (*FuncType) isJ()          {}
func (*FuncType) isExpression() {}

func (n *FuncType) WithPrefix(prefix Space) *FuncType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *FuncType) WithMarkers(markers Markers) *FuncType {
	c := *n
	c.Markers = markers
	return &c
}

// StructType represents a struct type expression: `struct { fields }`.
type StructType struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Body    *Block // contains VariableDeclarations for fields
}

func (*StructType) isTree()       {}
func (*StructType) isJ()          {}
func (*StructType) isExpression() {}

func (n *StructType) WithPrefix(prefix Space) *StructType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *StructType) WithMarkers(markers Markers) *StructType {
	c := *n
	c.Markers = markers
	return &c
}

// InterfaceType represents an interface type expression: `interface { methods }`.
type InterfaceType struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Body    *Block // contains MethodDeclaration (no body) or type refs for embedded interfaces
}

func (*InterfaceType) isTree()       {}
func (*InterfaceType) isJ()          {}
func (*InterfaceType) isExpression() {}

func (n *InterfaceType) WithPrefix(prefix Space) *InterfaceType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *InterfaceType) WithMarkers(markers Markers) *InterfaceType {
	c := *n
	c.Markers = markers
	return &c
}

// TypeList represents a parenthesized list of return types: `(int, error)` or `(n int, err error)`.
// Used for multiple (or single parenthesized) return values in function signatures.
// Elements are VariableDeclarations, each with optional name and type.
type TypeList struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Types   Container[Statement] // Before = space before `(`, last After = space before `)`
}

func (*TypeList) isTree()       {}
func (*TypeList) isJ()          {}
func (*TypeList) isExpression() {}

func (n *TypeList) WithPrefix(prefix Space) *TypeList {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *TypeList) WithMarkers(markers Markers) *TypeList {
	c := *n
	c.Markers = markers
	return &c
}

// TypeDecl represents a `type Name Type` declaration.
// Covers: `type Foo struct{...}`, `type Foo interface{...}`, `type Foo int`, `type Foo = Bar`.
// For grouped declarations `type ( ... )`, Specs is non-nil and Name/Definition are unused.
type TypeDecl struct {
	ID         uuid.UUID
	Prefix     Space
	Markers    Markers
	Name       *Identifier
	Assign     *LeftPadded[Space]    // non-nil for `type Foo = Bar`; Before = space before `=`
	Definition Expression            // the type expression (nil for grouped)
	Specs      *Container[Statement] // non-nil for grouped `type ( ... )`; Before = space before `(`
}

func (*TypeDecl) isTree()      {}
func (*TypeDecl) isJ()         {}
func (*TypeDecl) isStatement() {}

func (n *TypeDecl) WithPrefix(prefix Space) *TypeDecl {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *TypeDecl) WithMarkers(markers Markers) *TypeDecl {
	c := *n
	c.Markers = markers
	return &c
}

// ShortVarDecl is a marker on Assignment indicating `:=` instead of `=`.
type ShortVarDecl struct {
	Ident uuid.UUID
}

func (s ShortVarDecl) ID() uuid.UUID { return s.Ident }

// GroupedImport is a marker on an import Container indicating that imports
// are parenthesized: import ( ... ). It stores the whitespace between
// 'import' and '('.
type GroupedImport struct {
	Ident  uuid.UUID
	Before Space // whitespace between 'import' and '('
}

func (g GroupedImport) ID() uuid.UUID { return g.Ident }

// ImportBlock is a marker on the first Import of a subsequent import block
// (2nd, 3rd, etc.) in files with multiple import declarations. It carries
// the information needed to print the block boundary.
type ImportBlock struct {
	Ident         uuid.UUID
	ClosePrevious bool  // true if the previous block was grouped (need to print ")")
	Before        Space // space before the "import" keyword
	Grouped       bool  // true if this block uses import (...)
	GroupedBefore Space // space between "import" and "(" (only if Grouped)
}

func (b ImportBlock) ID() uuid.UUID { return b.Ident }

// MultiAssignment represents a multi-value assignment: `x, y = 1, 2` or `x, y := f()`.
type MultiAssignment struct {
	ID        uuid.UUID
	Prefix    Space
	Markers   Markers
	Variables []RightPadded[Expression] // LHS; After = space before comma
	Operator  LeftPadded[Space]         // Before = space before `=` or `:=`
	Values    []RightPadded[Expression] // RHS; After = space before comma
}

func (*MultiAssignment) isTree()      {}
func (*MultiAssignment) isJ()         {}
func (*MultiAssignment) isStatement() {}

func (n *MultiAssignment) WithPrefix(prefix Space) *MultiAssignment {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *MultiAssignment) WithMarkers(markers Markers) *MultiAssignment {
	c := *n
	c.Markers = markers
	return &c
}

// CommClause represents a communication clause in a select statement.
// `case <-ch:` or `case ch <- val:` or `case v := <-ch:` or `default:`.
type CommClause struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Comm    Statement                // nil for default; the comm operation
	Colon   Space                    // space before `:`
	Body    []RightPadded[Statement] // statements after the colon
}

func (*CommClause) isTree()      {}
func (*CommClause) isJ()         {}
func (*CommClause) isStatement() {}

func (n *CommClause) WithPrefix(prefix Space) *CommClause {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *CommClause) WithMarkers(markers Markers) *CommClause {
	c := *n
	c.Markers = markers
	return &c
}

// IndexList represents a multi-index expression like `Map[int, string]` (generic instantiation
// with multiple type parameters). Single-index uses ArrayAccess; this is for 2+ indices.
type IndexList struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Target  Expression
	Indices Container[Expression] // Before = space before `[`, Elements = type args, last After = space before `]`
}

func (*IndexList) isTree()       {}
func (*IndexList) isJ()          {}
func (*IndexList) isExpression() {}

func (n *IndexList) WithPrefix(prefix Space) *IndexList {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *IndexList) WithMarkers(markers Markers) *IndexList {
	c := *n
	c.Markers = markers
	return &c
}

// SelectStmt is a marker on Switch indicating it's a `select` statement instead of `switch`.
type SelectStmt struct {
	Ident uuid.UUID
}

func (s SelectStmt) ID() uuid.UUID { return s.Ident }

// TypeSwitchGuard is a marker on Switch indicating it's a type switch with
// a type assertion guard like `switch x.(type)` or `switch v := x.(type)`.
type TypeSwitchGuard struct {
	Ident uuid.UUID
}

func (t TypeSwitchGuard) ID() uuid.UUID { return t.Ident }

// GroupedSpec is a marker on TypeDecl (or VariableDeclarations) inside a grouped declaration,
// indicating the keyword (type/var/const) should not be printed.
type GroupedSpec struct {
	Ident uuid.UUID
}

func (g GroupedSpec) ID() uuid.UUID { return g.Ident }

// InterfaceMethod is a marker on MethodDeclaration indicating it's an interface method signature
// (no `func` keyword in the source).
type InterfaceMethod struct {
	Ident uuid.UUID
}

func (i InterfaceMethod) ID() uuid.UUID { return i.Ident }

// StructTag is a marker on VariableDeclarations in a struct body, storing the struct field tag.
type StructTag struct {
	Ident uuid.UUID
	Tag   *Literal // the raw tag literal, e.g., `json:"name"`
}

func (s StructTag) ID() uuid.UUID { return s.Ident }

// ConstDecl is a marker on VariableDeclarations indicating `const` instead of `var`.
type ConstDecl struct {
	Ident uuid.UUID
}

func (c ConstDecl) ID() uuid.UUID { return c.Ident }

// VarKeyword is a marker on VariableDeclarations indicating a `var` keyword is present.
type VarKeyword struct {
	Ident uuid.UUID
}

func (v VarKeyword) ID() uuid.UUID { return v.Ident }

// TrailingComma is a marker indicating a trailing comma is present in a list.
// The Before space is the space before the comma; After space is the space after
// the comma (before the closing delimiter).
type TrailingComma struct {
	Ident  uuid.UUID
	Before Space // space before ","
	After  Space // space after "," (before closing delimiter)
}

func (t TrailingComma) ID() uuid.UUID { return t.Ident }
