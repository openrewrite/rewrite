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

package golang

import (
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// CompilationUnit represents a Go source file.
type CompilationUnit struct {
	ID          uuid.UUID
	Prefix      java.Space
	Markers     java.Markers
	SourcePath  string
	PackageDecl *java.RightPadded[*java.Identifier] // `package main`
	Imports     *java.Container[*java.Import]       // nil if no imports
	Statements  []java.RightPadded[java.Statement]  // top-level declarations
	EOF         java.Space
}

func (*CompilationUnit) IsTree()       {}
func (*CompilationUnit) IsJ()          {}
func (*CompilationUnit) IsSourceFile() {}

func (n *CompilationUnit) WithPrefix(prefix java.Space) *CompilationUnit {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *CompilationUnit) WithMarkers(markers java.Markers) *CompilationUnit {
	c := *n
	c.Markers = markers
	return &c
}

func (n *CompilationUnit) WithStatements(statements []java.RightPadded[java.Statement]) *CompilationUnit {
	c := *n
	c.Statements = statements
	return &c
}

func (n *CompilationUnit) WithPackageDecl(pkg *java.RightPadded[*java.Identifier]) *CompilationUnit {
	c := *n
	c.PackageDecl = pkg
	return &c
}

func (n *CompilationUnit) WithImports(imports *java.Container[*java.Import]) *CompilationUnit {
	c := *n
	c.Imports = imports
	return &c
}

func (n *CompilationUnit) WithEOF(eof java.Space) *CompilationUnit {
	c := *n
	c.EOF = eof
	return &c
}

// Go represents a `go expr` statement (goroutine launch).
type GoStmt struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Expr    java.Expression
}

func (*GoStmt) IsTree()      {}
func (*GoStmt) IsJ()         {}
func (*GoStmt) IsStatement() {}

func (n *GoStmt) WithPrefix(prefix java.Space) *GoStmt {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *GoStmt) WithMarkers(markers java.Markers) *GoStmt {
	c := *n
	c.Markers = markers
	return &c
}

// Defer represents a `defer expr` statement.
type Defer struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Expr    java.Expression
}

func (*Defer) IsTree()      {}
func (*Defer) IsJ()         {}
func (*Defer) IsStatement() {}

func (n *Defer) WithPrefix(prefix java.Space) *Defer {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Defer) WithMarkers(markers java.Markers) *Defer {
	c := *n
	c.Markers = markers
	return &c
}

// Send represents a channel send statement: `ch <- value`.
type Send struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Channel java.Expression
	Arrow   java.LeftPadded[java.Expression] // Before = space before `<-`
}

func (*Send) IsTree()      {}
func (*Send) IsJ()         {}
func (*Send) IsStatement() {}

func (n *Send) WithPrefix(prefix java.Space) *Send {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Send) WithMarkers(markers java.Markers) *Send {
	c := *n
	c.Markers = markers
	return &c
}

// Goto represents a `goto label` statement.
type Goto struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Label   *java.Identifier
}

func (*Goto) IsTree()      {}
func (*Goto) IsJ()         {}
func (*Goto) IsStatement() {}

func (n *Goto) WithPrefix(prefix java.Space) *Goto {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Goto) WithMarkers(markers java.Markers) *Goto {
	c := *n
	c.Markers = markers
	return &c
}

// Fallthrough represents a `fallthrough` statement in a switch case.
type Fallthrough struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
}

func (*Fallthrough) IsTree()      {}
func (*Fallthrough) IsJ()         {}
func (*Fallthrough) IsStatement() {}

func (n *Fallthrough) WithPrefix(prefix java.Space) *Fallthrough {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Fallthrough) WithMarkers(markers java.Markers) *Fallthrough {
	c := *n
	c.Markers = markers
	return &c
}

// Composite represents a composite literal: `Type{elem1, elem2}`.
type Composite struct {
	ID       uuid.UUID
	Prefix   java.Space
	Markers  java.Markers
	TypeExpr java.Expression                 // nil for untyped composite literals
	Elements java.Container[java.Expression] // Before = space before `{`, elements, last After = space before `}`
}

func (*Composite) IsTree()       {}
func (*Composite) IsJ()          {}
func (*Composite) IsExpression() {}

func (n *Composite) WithPrefix(prefix java.Space) *Composite {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Composite) WithMarkers(markers java.Markers) *Composite {
	c := *n
	c.Markers = markers
	return &c
}

// KeyValue represents a `key: value` pair in composite literals.
type KeyValue struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Key     java.Expression
	Value   java.LeftPadded[java.Expression] // Before = space before `:`
}

func (*KeyValue) IsTree()       {}
func (*KeyValue) IsJ()          {}
func (*KeyValue) IsExpression() {}

func (n *KeyValue) WithPrefix(prefix java.Space) *KeyValue {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *KeyValue) WithMarkers(markers java.Markers) *KeyValue {
	c := *n
	c.Markers = markers
	return &c
}

// Slice represents a slice expression: `a[low:high]` or `a[low:high:max]`.
type Slice struct {
	ID           uuid.UUID
	Prefix       java.Space
	Markers      java.Markers
	Indexed      java.Expression
	OpenBracket  java.Space                        // space before `[`
	Low          java.RightPadded[java.Expression] // After = space before first `:`
	High         java.RightPadded[java.Expression] // After = space before second `:` (empty if 2-index)
	Max          java.Expression                   // nil for 2-index slices
	CloseBracket java.Space                        // space before `]`
}

func (*Slice) IsTree()       {}
func (*Slice) IsJ()          {}
func (*Slice) IsExpression() {}

func (n *Slice) WithPrefix(prefix java.Space) *Slice {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Slice) WithMarkers(markers java.Markers) *Slice {
	c := *n
	c.Markers = markers
	return &c
}

// MapType represents a map type expression: `map[K]V`.
type MapType struct {
	ID          uuid.UUID
	Prefix      java.Space
	Markers     java.Markers
	OpenBracket java.Space                        // space before `[`
	Key         java.RightPadded[java.Expression] // After = space before `]`
	Value       java.Expression
}

func (*MapType) IsTree()       {}
func (*MapType) IsJ()          {}
func (*MapType) IsExpression() {}

func (n *MapType) WithPrefix(prefix java.Space) *MapType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *MapType) WithMarkers(markers java.Markers) *MapType {
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

// PointerType represents a pointer type expression: `*T`.
type PointerType struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Elem    java.Expression
}

func (*PointerType) IsTree()       {}
func (*PointerType) IsJ()          {}
func (*PointerType) IsExpression() {}

func (n *PointerType) WithPrefix(prefix java.Space) *PointerType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *PointerType) WithMarkers(markers java.Markers) *PointerType {
	c := *n
	c.Markers = markers
	return &c
}

// Channel represents a channel type expression: `chan T`, `chan<- T`, `<-chan T`.
type Channel struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Dir     ChanDir
	Value   java.Expression
}

func (*Channel) IsTree()       {}
func (*Channel) IsJ()          {}
func (*Channel) IsExpression() {}

func (n *Channel) WithPrefix(prefix java.Space) *Channel {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Channel) WithMarkers(markers java.Markers) *Channel {
	c := *n
	c.Markers = markers
	return &c
}

// FuncType represents a function type expression: `func(int) string`.
type FuncType struct {
	ID         uuid.UUID
	Prefix     java.Space
	Markers    java.Markers
	Parameters java.Container[java.Statement]
	ReturnType java.Expression // nil if no return type
}

func (*FuncType) IsTree()       {}
func (*FuncType) IsJ()          {}
func (*FuncType) IsExpression() {}

func (n *FuncType) WithPrefix(prefix java.Space) *FuncType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *FuncType) WithMarkers(markers java.Markers) *FuncType {
	c := *n
	c.Markers = markers
	return &c
}

// StructType represents a struct type expression: `struct { fields }`.
type StructType struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Body    *java.Block // contains VariableDeclarations for fields
}

func (*StructType) IsTree()       {}
func (*StructType) IsJ()          {}
func (*StructType) IsExpression() {}

func (n *StructType) WithPrefix(prefix java.Space) *StructType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *StructType) WithMarkers(markers java.Markers) *StructType {
	c := *n
	c.Markers = markers
	return &c
}

// InterfaceType represents an interface type expression: `interface { methods }`.
type InterfaceType struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Body    *java.Block // contains MethodDeclaration (no body) or type refs for embedded interfaces
}

func (*InterfaceType) IsTree()       {}
func (*InterfaceType) IsJ()          {}
func (*InterfaceType) IsExpression() {}

func (n *InterfaceType) WithPrefix(prefix java.Space) *InterfaceType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *InterfaceType) WithMarkers(markers java.Markers) *InterfaceType {
	c := *n
	c.Markers = markers
	return &c
}

// TypeList represents a parenthesized list of return types: `(int, error)` or `(n int, err error)`.
// Used for multiple (or single parenthesized) return values in function signatures.
// Elements are VariableDeclarations, each with optional name and type.
type TypeList struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Types   java.Container[java.Statement] // Before = space before `(`, last After = space before `)`
}

func (*TypeList) IsTree()       {}
func (*TypeList) IsJ()          {}
func (*TypeList) IsExpression() {}

func (n *TypeList) WithPrefix(prefix java.Space) *TypeList {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *TypeList) WithMarkers(markers java.Markers) *TypeList {
	c := *n
	c.Markers = markers
	return &c
}

// Union represents a type-set union in a generic constraint, e.g. the
// `~int | ~int8 | ~int16` in `interface { ~int | ~int8 | ~int16 }`.
// Each element is a type term (a plain type name or an UnderlyingType).
// The `|` separators are printed between elements; the space before each
// `|` is the preceding element's After, the space after each `|` is the
// next element's Prefix. Mirrors org.openrewrite.golang.tree.Go$Union
// (modeled after JS.Union).
type Union struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Types   []java.RightPadded[java.Expression]
}

func (*Union) IsTree()       {}
func (*Union) IsJ()          {}
func (*Union) IsExpression() {}

func (n *Union) WithPrefix(prefix java.Space) *Union {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Union) WithMarkers(markers java.Markers) *Union {
	c := *n
	c.Markers = markers
	return &c
}

// UnderlyingType represents an approximation element `~T` in a generic
// type constraint: the set of all types whose underlying type is `T`.
// Go's grammar names this production `UnderlyingType = "~" Type`. The
// `~` is printed immediately after Prefix; Element is the underlying
// type (its own Prefix carries any space between `~` and the type).
// Mirrors org.openrewrite.golang.tree.Go$UnderlyingType.
type UnderlyingType struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Element java.Expression
}

func (*UnderlyingType) IsTree()       {}
func (*UnderlyingType) IsJ()          {}
func (*UnderlyingType) IsExpression() {}

func (n *UnderlyingType) WithPrefix(prefix java.Space) *UnderlyingType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *UnderlyingType) WithMarkers(markers java.Markers) *UnderlyingType {
	c := *n
	c.Markers = markers
	return &c
}

// TypeDecl represents a `type Name Type` declaration.
// Covers: `type Foo struct{...}`, `type Foo interface{...}`, `type Foo int`, `type Foo = Bar`.
// For grouped declarations `type ( ... )`, Specs is non-nil and Name/Definition are unused.
type TypeDecl struct {
	ID                 uuid.UUID
	Prefix             java.Space
	Markers            java.Markers
	LeadingAnnotations []*java.Annotation // `//go:generate ...` etc.
	Name               *java.Identifier
	TypeParameters     *java.TypeParameters            // nil for non-generic types; `[T any]` declaration-site type params
	Assign             *java.LeftPadded[java.Space]    // non-nil for `type Foo = Bar`; Before = space before `=`
	Definition         java.Expression                 // the type expression (nil for grouped)
	Specs              *java.Container[java.Statement] // non-nil for grouped `type ( ... )`; Before = space before `(`
}

func (*TypeDecl) IsTree()      {}
func (*TypeDecl) IsJ()         {}
func (*TypeDecl) IsStatement() {}

func (n *TypeDecl) WithPrefix(prefix java.Space) *TypeDecl {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *TypeDecl) WithMarkers(markers java.Markers) *TypeDecl {
	c := *n
	c.Markers = markers
	return &c
}

func (n *TypeDecl) WithLeadingAnnotations(anns []*java.Annotation) *TypeDecl {
	c := *n
	c.LeadingAnnotations = anns
	return &c
}

func (n *TypeDecl) WithTypeParameters(tps *java.TypeParameters) *TypeDecl {
	c := *n
	c.TypeParameters = tps
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
	Before java.Space // whitespace between 'import' and '('
}

func (g GroupedImport) ID() uuid.UUID { return g.Ident }

// ImportBlock is a marker on the first Import of a subsequent import block
// (2nd, 3rd, etc.) in files with multiple import declarations. It carries
// the information needed to print the block boundary.
type ImportBlock struct {
	Ident         uuid.UUID
	ClosePrevious bool       // true if the previous block was grouped (need to print ")")
	Before        java.Space // space before the "import" keyword
	Grouped       bool       // true if this block uses import (...)
	GroupedBefore java.Space // space between "import" and "(" (only if Grouped)
}

func (b ImportBlock) ID() uuid.UUID { return b.Ident }

// ChanDirMarker stores whitespace around the direction operator in a channel type.
// For send channels (`chan <- T`), Before holds the space before `<-`.
// For recv channels (`<- chan T`), Before holds the space before `chan`.
type ChanDirMarker struct {
	Ident  uuid.UUID
	Before java.Space
}

func (c ChanDirMarker) ID() uuid.UUID { return c.Ident }

// MultiAssignment represents a multi-value assignment: `x, y = 1, 2` or `x, y := f()`.
type MultiAssignment struct {
	ID        uuid.UUID
	Prefix    java.Space
	Markers   java.Markers
	Variables []java.RightPadded[java.Expression] // LHS; After = space before comma
	Operator  java.LeftPadded[java.Space]         // Before = space before `=` or `:=`
	Values    []java.RightPadded[java.Expression] // RHS; After = space before comma
}

func (*MultiAssignment) IsTree()      {}
func (*MultiAssignment) IsJ()         {}
func (*MultiAssignment) IsStatement() {}

func (n *MultiAssignment) WithPrefix(prefix java.Space) *MultiAssignment {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *MultiAssignment) WithMarkers(markers java.Markers) *MultiAssignment {
	c := *n
	c.Markers = markers
	return &c
}

// CommClause represents a communication clause in a select statement.
// `case <-ch:` or `case ch <- val:` or `case v := <-ch:` or `default:`.
type CommClause struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Comm    java.Statement                     // nil for default; the comm operation
	Colon   java.Space                         // space before `:`
	Body    []java.RightPadded[java.Statement] // statements after the colon
}

func (*CommClause) IsTree()      {}
func (*CommClause) IsJ()         {}
func (*CommClause) IsStatement() {}

func (n *CommClause) WithPrefix(prefix java.Space) *CommClause {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *CommClause) WithMarkers(markers java.Markers) *CommClause {
	c := *n
	c.Markers = markers
	return &c
}

// StatementExpression wraps a Statement so it can appear in expression contexts.
// Used for Go function literals which are parsed as MethodDeclaration (a Statement)
// but can appear in return statements, assignments, and call arguments.
type StatementExpression struct {
	ID        uuid.UUID
	Prefix    java.Space
	Markers   java.Markers
	Statement java.Statement
}

func (*StatementExpression) IsTree()       {}
func (*StatementExpression) IsJ()          {}
func (*StatementExpression) IsExpression() {}

func (n *StatementExpression) WithPrefix(prefix java.Space) *StatementExpression {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *StatementExpression) WithMarkers(markers java.Markers) *StatementExpression {
	c := *n
	c.Markers = markers
	return &c
}

// IndexList represents a multi-index expression like `Map[int, string]` (generic instantiation
// with multiple type parameters). Single-index uses ArrayAccess; this is for 2+ indices.
type IndexList struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Target  java.Expression
	Indices java.Container[java.Expression] // Before = space before `[`, Elements = type args, last After = space before `]`
}

func (*IndexList) IsTree()       {}
func (*IndexList) IsJ()          {}
func (*IndexList) IsExpression() {}

func (n *IndexList) WithPrefix(prefix java.Space) *IndexList {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *IndexList) WithMarkers(markers java.Markers) *IndexList {
	c := *n
	c.Markers = markers
	return &c
}

// UnaryOperator is a Go-specific prefix unary operator that has no equivalent
// in java.UnaryOperator / J.Unary.Type. Operators that DO map to J.Unary
// (Negate, Not, BitwiseNot, Positive) stay as java.Unary.
type UnaryOperator int

const (
	AddressOf   UnaryOperator = iota + 1 // &
	Indirection                          // *
	Receive                              // <-
)

// String returns the Java enum-constant name (Go.Unary.Type) for the wire.
// Unlike java.UnaryOperator.String() these are faithful 1:1 mappings, so the
// operator survives a Java round-trip without collapsing to "Not".
func (op UnaryOperator) String() string {
	switch op {
	case AddressOf:
		return "AddressOf"
	case Indirection:
		return "Indirection"
	case Receive:
		return "Receive"
	default:
		return "AddressOf"
	}
}

// ParseUnaryOperator converts a Go.Unary.Type enum name back to the operator.
func ParseUnaryOperator(s string) UnaryOperator {
	switch s {
	case "AddressOf":
		return AddressOf
	case "Indirection":
		return Indirection
	case "Receive":
		return Receive
	default:
		return 0
	}
}

// Unary represents a Go-specific prefix unary expression: `&x`, `*p`, `<-ch`.
type Unary struct {
	ID         uuid.UUID
	Prefix     java.Space
	Markers    java.Markers
	Operator   java.LeftPadded[UnaryOperator] // Before = space before the operator token
	Expression java.Expression
}

func (*Unary) IsTree()       {}
func (*Unary) IsJ()          {}
func (*Unary) IsExpression() {}
func (*Unary) IsStatement()  {}

func (n *Unary) WithPrefix(prefix java.Space) *Unary {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Unary) WithMarkers(markers java.Markers) *Unary {
	c := *n
	c.Markers = markers
	return &c
}

// BinaryOperator is a Go-specific binary operator with no java.BinaryOperator
// / J.Binary.Type equivalent. All other Go binary operators stay as java.Binary.
type BinaryOperator int

const (
	BinAndNot BinaryOperator = iota + 1 // &^
)

// String returns the Java enum-constant name (Go.Binary.Type) for the wire.
func (op BinaryOperator) String() string {
	switch op {
	case BinAndNot:
		return "AndNot"
	default:
		return "AndNot"
	}
}

// ParseBinaryOperator converts a Go.Binary.Type enum name back to the operator.
func ParseBinaryOperator(s string) BinaryOperator {
	switch s {
	case "AndNot":
		return BinAndNot
	default:
		return 0
	}
}

// Binary represents a Go-specific binary expression: `a &^ b`.
type Binary struct {
	ID       uuid.UUID
	Prefix   java.Space
	Markers  java.Markers
	Left     java.Expression
	Operator java.LeftPadded[BinaryOperator] // Before = space before the operator token
	Right    java.Expression
}

func (*Binary) IsTree()       {}
func (*Binary) IsJ()          {}
func (*Binary) IsExpression() {}

func (n *Binary) WithPrefix(prefix java.Space) *Binary {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Binary) WithMarkers(markers java.Markers) *Binary {
	c := *n
	c.Markers = markers
	return &c
}

// AssignmentOperator is a Go-specific compound-assignment operator with no
// java.AssignmentOperator / J.AssignmentOperation.Type equivalent.
type AssignmentOperator int

const (
	AssignAndNot AssignmentOperator = iota + 1 // &^=
)

// String returns the Java enum-constant name (Go.AssignmentOperation.Type).
func (op AssignmentOperator) String() string {
	switch op {
	case AssignAndNot:
		return "AndNot"
	default:
		return "AndNot"
	}
}

// ParseAssignmentOperator converts a Go.AssignmentOperation.Type enum name back.
func ParseAssignmentOperator(s string) AssignmentOperator {
	switch s {
	case "AndNot":
		return AssignAndNot
	default:
		return 0
	}
}

// AssignmentOperation represents a Go-specific compound assignment: `a &^= b`.
type AssignmentOperation struct {
	ID         uuid.UUID
	Prefix     java.Space
	Markers    java.Markers
	Variable   java.Expression
	Operator   java.LeftPadded[AssignmentOperator] // Before = space before the operator token
	Assignment java.Expression
}

func (*AssignmentOperation) IsTree()       {}
func (*AssignmentOperation) IsJ()          {}
func (*AssignmentOperation) IsExpression() {}
func (*AssignmentOperation) IsStatement()  {}

func (n *AssignmentOperation) WithPrefix(prefix java.Space) *AssignmentOperation {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *AssignmentOperation) WithMarkers(markers java.Markers) *AssignmentOperation {
	c := *n
	c.Markers = markers
	return &c
}

// Variadic represents Go's `...` ellipsis: the `...T` parameter type
// (Postfix=false) and the `args...` call spread (Postfix=true). Dots is the
// whitespace immediately before the `...` token in the postfix form.
type Variadic struct {
	ID      uuid.UUID
	Prefix  java.Space
	Markers java.Markers
	Element java.Expression
	Dots    java.Space
	Postfix bool
}

func (*Variadic) IsTree()       {}
func (*Variadic) IsJ()          {}
func (*Variadic) IsExpression() {}

func (n *Variadic) WithPrefix(prefix java.Space) *Variadic {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Variadic) WithMarkers(markers java.Markers) *Variadic {
	c := *n
	c.Markers = markers
	return &c
}

// RangeLoop represents Go's `for ... range` loop: `for k, v := range expr { body }`.
// Unlike Java's single-variable J.ForEachLoop.Control, Go binds a Key and an
// optional Value (or none, for `for range expr`), joined by `:=` or `=`.
type RangeLoop struct {
	ID       uuid.UUID
	Prefix   java.Space
	Markers  java.Markers
	Key      *java.RightPadded[java.Expression] // nil for `for range expr`; After = space after key (incl. comma)
	Value    *java.RightPadded[java.Expression] // nil when no value; After = space before operator
	Operator java.LeftPadded[java.AssignOp]     // `:=` or `=`; Before = space before `range`. Unused when Key is nil
	Iterable java.Expression
	Body     java.RightPadded[java.Statement] // the loop body block
}

func (*RangeLoop) IsTree()      {}
func (*RangeLoop) IsJ()         {}
func (*RangeLoop) IsStatement() {}

func (n *RangeLoop) WithPrefix(prefix java.Space) *RangeLoop {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *RangeLoop) WithMarkers(markers java.Markers) *RangeLoop {
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
	Tag   *java.Literal // the raw tag literal, e.g., `json:"name"`
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
	Before java.Space // space before ","
	After  java.Space // space after "," (before closing delimiter)
}

func (t TrailingComma) ID() uuid.UUID { return t.Ident }
