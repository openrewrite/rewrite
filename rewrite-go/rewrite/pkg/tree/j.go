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

// Identifier represents a name reference in source code.
type Identifier struct {
	ID          uuid.UUID
	Prefix      Space
	Markers     Markers
	Annotations []Tree // Java annotations on this identifier (empty in Go, received from Java)
	Name        string
	Type        JavaType          // the type of this identifier (nullable)
	FieldType   *JavaTypeVariable // the variable type when this identifier refers to a field (nullable)
}

func (*Identifier) isTree()       {}
func (*Identifier) isJ()          {}
func (*Identifier) isExpression() {}

func (n *Identifier) WithPrefix(prefix Space) *Identifier {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Identifier) WithMarkers(markers Markers) *Identifier {
	c := *n
	c.Markers = markers
	return &c
}

func (n *Identifier) WithName(name string) *Identifier {
	if n.Name == name {
		return n
	}
	c := *n
	c.Name = name
	return &c
}

func (n *Identifier) WithType(t JavaType) *Identifier {
	c := *n
	c.Type = t
	return &c
}

func (n *Identifier) WithFieldType(ft *JavaTypeVariable) *Identifier {
	c := *n
	c.FieldType = ft
	return &c
}

// Literal represents a literal value (string, number, boolean, etc.).
type Literal struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Kind    LiteralKind
	Value   any
	Source  string   // the original source text of the literal
	Type    JavaType // the type of this literal (nullable)
}

type LiteralKind int

const (
	BoolLiteral LiteralKind = iota
	IntLiteral
	FloatLiteral
	StringLiteral
	CharLiteral
	NilLiteral
)

func (*Literal) isTree()       {}
func (*Literal) isJ()          {}
func (*Literal) isExpression() {}

func (n *Literal) WithPrefix(prefix Space) *Literal {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Literal) WithMarkers(markers Markers) *Literal {
	c := *n
	c.Markers = markers
	return &c
}

func (n *Literal) WithValue(value any) *Literal {
	c := *n
	c.Value = value
	return &c
}

func (n *Literal) WithSource(source string) *Literal {
	if n.Source == source {
		return n
	}
	c := *n
	c.Source = source
	return &c
}

// BinaryOperator represents the operator in a binary expression.
type BinaryOperator int

const (
	Add BinaryOperator = iota
	Subtract
	Multiply
	Divide
	Modulo
	And
	Or
	BitwiseAnd
	BitwiseOr
	BitwiseXor
	LeftShift
	RightShift
	Equal
	NotEqual
	LessThan
	LessThanOrEqual
	GreaterThan
	GreaterThanOrEqual
	LogicalAnd
	LogicalOr
	AndNot // Go-specific: &^
)

// String returns the Java enum name for this BinaryOperator.
func (op BinaryOperator) String() string {
	switch op {
	case Add:
		return "Addition"
	case Subtract:
		return "Subtraction"
	case Multiply:
		return "Multiplication"
	case Divide:
		return "Division"
	case Modulo:
		return "Modulo"
	case And, LogicalAnd:
		return "And"
	case Or, LogicalOr:
		return "Or"
	case BitwiseAnd:
		return "BitAnd"
	case BitwiseOr:
		return "BitOr"
	case BitwiseXor:
		return "BitXor"
	case LeftShift:
		return "LeftShift"
	case RightShift:
		return "RightShift"
	case Equal:
		return "Equal"
	case NotEqual:
		return "NotEqual"
	case LessThan:
		return "LessThan"
	case LessThanOrEqual:
		return "LessThanOrEqual"
	case GreaterThan:
		return "GreaterThan"
	case GreaterThanOrEqual:
		return "GreaterThanOrEqual"
	case AndNot:
		return "BitAnd" // Go-specific &^ mapped to BitAnd with marker
	default:
		return "Addition"
	}
}

// ParseBinaryOperator converts a Java enum name to a BinaryOperator.
func ParseBinaryOperator(s string) BinaryOperator {
	switch s {
	case "Addition":
		return Add
	case "Subtraction":
		return Subtract
	case "Multiplication":
		return Multiply
	case "Division":
		return Divide
	case "Modulo":
		return Modulo
	case "And":
		return And
	case "Or":
		return Or
	case "BitAnd":
		return BitwiseAnd
	case "BitOr":
		return BitwiseOr
	case "BitXor":
		return BitwiseXor
	case "LeftShift":
		return LeftShift
	case "RightShift":
		return RightShift
	case "Equal":
		return Equal
	case "NotEqual":
		return NotEqual
	case "LessThan":
		return LessThan
	case "LessThanOrEqual":
		return LessThanOrEqual
	case "GreaterThan":
		return GreaterThan
	case "GreaterThanOrEqual":
		return GreaterThanOrEqual
	default:
		return Add
	}
}

// Binary represents a binary expression like `a + b`.
type Binary struct {
	ID       uuid.UUID
	Prefix   Space
	Markers  Markers
	Left     Expression
	Operator LeftPadded[BinaryOperator]
	Right    Expression
	Type     JavaType // the result type (nullable)
}

func (*Binary) isTree()       {}
func (*Binary) isJ()          {}
func (*Binary) isExpression() {}

func (n *Binary) WithPrefix(prefix Space) *Binary {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Binary) WithMarkers(markers Markers) *Binary {
	c := *n
	c.Markers = markers
	return &c
}

func (n *Binary) WithLeft(left Expression) *Binary {
	c := *n
	c.Left = left
	return &c
}

func (n *Binary) WithRight(right Expression) *Binary {
	c := *n
	c.Right = right
	return &c
}

// Block represents a brace-delimited block of statements.
type Block struct {
	ID         uuid.UUID
	Prefix     Space
	Markers    Markers
	Statements []RightPadded[Statement]
	End        Space // whitespace before the closing brace
}

func (*Block) isTree()      {}
func (*Block) isJ()         {}
func (*Block) isStatement() {}

func (n *Block) WithPrefix(prefix Space) *Block {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Block) WithMarkers(markers Markers) *Block {
	c := *n
	c.Markers = markers
	return &c
}

func (n *Block) WithStatements(statements []RightPadded[Statement]) *Block {
	c := *n
	c.Statements = statements
	return &c
}

func (n *Block) WithEnd(end Space) *Block {
	c := *n
	c.End = end
	return &c
}

// Return represents a return statement.
type Return struct {
	ID          uuid.UUID
	Prefix      Space
	Markers     Markers
	Expressions []RightPadded[Expression]
}

func (*Return) isTree()      {}
func (*Return) isJ()         {}
func (*Return) isStatement() {}

func (n *Return) WithPrefix(prefix Space) *Return {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Return) WithMarkers(markers Markers) *Return {
	c := *n
	c.Markers = markers
	return &c
}

// If represents an if statement.
// In Go, if can have an init statement: `if init; cond { }`.
type If struct {
	ID        uuid.UUID
	Prefix    Space
	Markers   Markers
	Init      *RightPadded[Statement] // nil if no init; After = space before `;`
	Condition Expression
	Then      *Block
	ElsePart  *RightPadded[J] // nil if no else clause
}

func (*If) isTree()      {}
func (*If) isJ()         {}
func (*If) isStatement() {}

func (n *If) WithPrefix(prefix Space) *If {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *If) WithMarkers(markers Markers) *If {
	c := *n
	c.Markers = markers
	return &c
}

func (n *If) WithCondition(condition Expression) *If {
	c := *n
	c.Condition = condition
	return &c
}

func (n *If) WithThen(then *Block) *If {
	c := *n
	c.Then = then
	return &c
}

// Else represents an else clause of an if statement.
// This matches Java's J.If.Else for RPC wire format compatibility.
type Else struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Body    RightPadded[Statement]
}

func (*Else) isTree() {}
func (*Else) isJ()    {}

func (n *Else) WithPrefix(prefix Space) *Else {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Else) WithMarkers(markers Markers) *Else {
	c := *n
	c.Markers = markers
	return &c
}

// Assignment represents an assignment statement like `x = expr`.
type Assignment struct {
	ID       uuid.UUID
	Prefix   Space
	Markers  Markers
	Variable Expression
	Value    LeftPadded[Expression]
	Type     JavaType // the result type (nullable)
}

func (*Assignment) isTree()       {}
func (*Assignment) isJ()          {}
func (*Assignment) isStatement()  {}
func (*Assignment) isExpression() {}

func (n *Assignment) WithPrefix(prefix Space) *Assignment {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Assignment) WithMarkers(markers Markers) *Assignment {
	c := *n
	c.Markers = markers
	return &c
}

func (n *Assignment) WithVariable(variable Expression) *Assignment {
	c := *n
	c.Variable = variable
	return &c
}

// AssignmentOperation represents a compound assignment like `x += expr`.
type AssignmentOperation struct {
	ID         uuid.UUID
	Prefix     Space
	Markers    Markers
	Variable   Expression
	Operator   LeftPadded[AssignmentOperator]
	Assignment Expression
	Type       JavaType // the result type (nullable)
}

type AssignmentOperator int

const (
	AddAssign    AssignmentOperator = iota // +=
	SubAssign                              // -=
	MulAssign                              // *=
	DivAssign                              // /=
	ModAssign                              // %=
	AndAssign                              // &=
	OrAssign                               // |=
	XorAssign                              // ^=
	ShlAssign                              // <<=
	ShrAssign                              // >>=
	AndNotAssign                           // &^= (Go-specific)
)

// String returns the Java enum name for this AssignmentOperator.
func (op AssignmentOperator) String() string {
	switch op {
	case AddAssign:
		return "Addition"
	case SubAssign:
		return "Subtraction"
	case MulAssign:
		return "Multiplication"
	case DivAssign:
		return "Division"
	case ModAssign:
		return "Modulo"
	case AndAssign:
		return "BitAnd"
	case OrAssign:
		return "BitOr"
	case XorAssign:
		return "BitXor"
	case ShlAssign:
		return "LeftShift"
	case ShrAssign:
		return "RightShift"
	case AndNotAssign:
		return "BitAnd" // Go-specific &^= mapped to BitAnd with marker
	default:
		return "Addition"
	}
}

// ParseAssignmentOperator converts a Java enum name to an AssignmentOperator.
func ParseAssignmentOperator(s string) AssignmentOperator {
	switch s {
	case "Addition":
		return AddAssign
	case "Subtraction":
		return SubAssign
	case "Multiplication":
		return MulAssign
	case "Division":
		return DivAssign
	case "Modulo":
		return ModAssign
	case "BitAnd":
		return AndAssign
	case "BitOr":
		return OrAssign
	case "BitXor":
		return XorAssign
	case "LeftShift":
		return ShlAssign
	case "RightShift":
		return ShrAssign
	default:
		return AddAssign
	}
}

func (*AssignmentOperation) isTree()       {}
func (*AssignmentOperation) isJ()          {}
func (*AssignmentOperation) isStatement()  {}
func (*AssignmentOperation) isExpression() {}

func (n *AssignmentOperation) WithPrefix(prefix Space) *AssignmentOperation {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *AssignmentOperation) WithMarkers(markers Markers) *AssignmentOperation {
	c := *n
	c.Markers = markers
	return &c
}

func (n *AssignmentOperation) WithVariable(variable Expression) *AssignmentOperation {
	c := *n
	c.Variable = variable
	return &c
}

// MethodDeclaration represents a function or method declaration.
type MethodDeclaration struct {
	ID         uuid.UUID
	Prefix     Space
	Markers    Markers
	Receiver   *Container[Statement] // nil for free functions; `(r *Type)` receiver
	Name       *Identifier
	Parameters Container[Statement] // parameter list in parentheses
	ReturnType Expression           // nil for void functions; single type or *TypeList for multiple
	Body       *Block               // nil for forward declarations
	MethodType *JavaTypeMethod      // the method type signature (nullable)
}

func (*MethodDeclaration) isTree()       {}
func (*MethodDeclaration) isJ()          {}
func (*MethodDeclaration) isStatement()  {}
func (*MethodDeclaration) isExpression() {} // for function literals

func (n *MethodDeclaration) WithPrefix(prefix Space) *MethodDeclaration {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *MethodDeclaration) WithMarkers(markers Markers) *MethodDeclaration {
	c := *n
	c.Markers = markers
	return &c
}

func (n *MethodDeclaration) WithName(name *Identifier) *MethodDeclaration {
	c := *n
	c.Name = name
	return &c
}

func (n *MethodDeclaration) WithBody(body *Block) *MethodDeclaration {
	c := *n
	c.Body = body
	return &c
}

// ForLoop represents a classic for loop: `for init; cond; update { body }`
// Also covers condition-only `for cond {}` and infinite `for {}`.
type ForLoop struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Control ForControl
	Body    *Block
}

func (*ForLoop) isTree()      {}
func (*ForLoop) isJ()         {}
func (*ForLoop) isStatement() {}

func (n *ForLoop) WithPrefix(prefix Space) *ForLoop {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *ForLoop) WithMarkers(markers Markers) *ForLoop {
	c := *n
	c.Markers = markers
	return &c
}

func (n *ForLoop) WithBody(body *Block) *ForLoop {
	c := *n
	c.Body = body
	return &c
}

// ForControl holds the init, condition, and update of a for loop.
// When Init is non-nil, this is a 3-clause loop with semicolons;
// Init.After and Condition.After capture whitespace around semicolons.
// When Init is nil and Condition is non-nil, it's a condition-only loop (no semicolons).
// When both are nil, it's an infinite loop.
type ForControl struct {
	ID        uuid.UUID
	Prefix    Space
	Markers   Markers
	Init      *RightPadded[Statement]  // nil for condition-only or infinite loops
	Condition *RightPadded[Expression] // nil for infinite loops
	Update    *RightPadded[Statement]  // nil when no update clause
}

func (*ForControl) isTree() {}
func (*ForControl) isJ()    {}

func (n *ForControl) WithPrefix(prefix Space) *ForControl {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *ForControl) WithMarkers(markers Markers) *ForControl {
	c := *n
	c.Markers = markers
	return &c
}

// ForEachLoop represents a for-range loop: `for k, v := range expr { body }`
type ForEachLoop struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Control ForEachControl
	Body    *Block
}

func (*ForEachLoop) isTree()      {}
func (*ForEachLoop) isJ()         {}
func (*ForEachLoop) isStatement() {}

func (n *ForEachLoop) WithPrefix(prefix Space) *ForEachLoop {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *ForEachLoop) WithMarkers(markers Markers) *ForEachLoop {
	c := *n
	c.Markers = markers
	return &c
}

func (n *ForEachLoop) WithBody(body *Block) *ForEachLoop {
	c := *n
	c.Body = body
	return &c
}

// ForEachControl holds the variable and iterable of a for-range loop.
// The "range" keyword is implicit (always present).
//
// Structure: `for` [key] [`,` value] [`:=`|`=`] `range` iterable
//   - Key and Value may be nil (e.g., `for range expr {}`)
//   - When Key is non-nil, Operator contains `:=` or `=`
type ForEachControl struct {
	ID       uuid.UUID
	Prefix   Space
	Markers  Markers
	Key      *RightPadded[Expression] // nil for `for range expr`; After = space after key (including comma)
	Value    *RightPadded[Expression] // nil when no value; After = space before operator
	Operator LeftPadded[AssignOp]     // `:=` or `=`; Before = space before operator. Unused when Key is nil
	Iterable Expression              // expression after "range" keyword
}

// AssignOp distinguishes := from = in assignment contexts.
type AssignOp int

const (
	AssignOpEquals AssignOp = iota // =
	AssignOpDefine                 // :=
)

// String returns a string representation of AssignOp for RPC serialization.
func (op AssignOp) String() string {
	switch op {
	case AssignOpEquals:
		return "Equals"
	case AssignOpDefine:
		return "Define"
	default:
		return "Equals"
	}
}

// ParseAssignOp converts a string to an AssignOp.
func ParseAssignOp(s string) AssignOp {
	switch s {
	case "Define":
		return AssignOpDefine
	default:
		return AssignOpEquals
	}
}

func (*ForEachControl) isTree() {}
func (*ForEachControl) isJ()    {}

func (n *ForEachControl) WithPrefix(prefix Space) *ForEachControl {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *ForEachControl) WithMarkers(markers Markers) *ForEachControl {
	c := *n
	c.Markers = markers
	return &c
}

// Switch represents a switch statement.
type Switch struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Init    *RightPadded[Statement]  // optional init statement; After = space after semicolon
	Tag     *RightPadded[Expression] // optional tag expression; After = space before {
	Body    *Block                   // contains Case statements
}

func (*Switch) isTree()      {}
func (*Switch) isJ()         {}
func (*Switch) isStatement() {}

func (n *Switch) WithPrefix(prefix Space) *Switch {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Switch) WithMarkers(markers Markers) *Switch {
	c := *n
	c.Markers = markers
	return &c
}

func (n *Switch) WithBody(body *Block) *Switch {
	c := *n
	c.Body = body
	return &c
}

// Case represents a case or default clause in a switch statement.
type Case struct {
	ID          uuid.UUID
	Prefix      Space
	Markers     Markers
	Expressions Container[Expression]      // empty for default case
	Body        []RightPadded[Statement]   // statements after the colon
}

func (*Case) isTree()      {}
func (*Case) isJ()         {}
func (*Case) isStatement() {}

func (n *Case) WithPrefix(prefix Space) *Case {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Case) WithMarkers(markers Markers) *Case {
	c := *n
	c.Markers = markers
	return &c
}

// Break represents a break statement with optional label.
type Break struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Label   *Identifier // nil if no label
}

func (*Break) isTree()      {}
func (*Break) isJ()         {}
func (*Break) isStatement() {}

func (n *Break) WithPrefix(prefix Space) *Break {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Break) WithMarkers(markers Markers) *Break {
	c := *n
	c.Markers = markers
	return &c
}

// Continue represents a continue statement with optional label.
type Continue struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Label   *Identifier // nil if no label
}

func (*Continue) isTree()      {}
func (*Continue) isJ()         {}
func (*Continue) isStatement() {}

func (n *Continue) WithPrefix(prefix Space) *Continue {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Continue) WithMarkers(markers Markers) *Continue {
	c := *n
	c.Markers = markers
	return &c
}

// Label represents a labeled statement: `label: stmt`.
type Label struct {
	ID        uuid.UUID
	Prefix    Space
	Markers   Markers
	Name      RightPadded[*Identifier] // After = space before ':'
	Statement Statement
}

func (*Label) isTree()      {}
func (*Label) isJ()         {}
func (*Label) isStatement() {}

func (n *Label) WithPrefix(prefix Space) *Label {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Label) WithMarkers(markers Markers) *Label {
	c := *n
	c.Markers = markers
	return &c
}

// Empty represents an empty statement or expression placeholder.
type Empty struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
}

func (*Empty) isTree()       {}
func (*Empty) isJ()          {}
func (*Empty) isStatement()  {}
func (*Empty) isExpression() {}

func (n *Empty) WithPrefix(prefix Space) *Empty {
	c := *n
	c.Prefix = prefix
	return &c
}

// Unary represents a unary expression like `-x` or `!y`.
type Unary struct {
	ID       uuid.UUID
	Prefix   Space
	Markers  Markers
	Operator LeftPadded[UnaryOperator]
	Operand  Expression
	Type     JavaType // the result type (nullable)
}

type UnaryOperator int

const (
	Negate        UnaryOperator = iota // -
	Not                               // !
	BitwiseNot                        // ^
	Deref                             // *
	AddressOf                         // &
	Receive                           // <- (channel receive, Go-specific)
	Positive                          // +
	PostIncrement                     // ++ (postfix)
	PostDecrement                     // -- (postfix)
	Spread                            // ... (variadic prefix, param declaration)
	SpreadPostfix                     // ... (variadic postfix, call site)
	Tilde                             // ~ (approximate type constraint, Go-specific)
)

// String returns the Java enum name for this UnaryOperator.
func (op UnaryOperator) String() string {
	switch op {
	case Negate:
		return "Negative"
	case Not:
		return "Not"
	case BitwiseNot:
		return "Complement"
	case Deref:
		return "Not" // Go-specific * dereference, mapped with marker
	case AddressOf:
		return "Not" // Go-specific & address-of, mapped with marker
	case Receive:
		return "Not" // Go-specific <- receive, mapped with marker
	case Positive:
		return "Positive"
	case PostIncrement:
		return "PostIncrement"
	case PostDecrement:
		return "PostDecrement"
	case Spread:
		return "Not" // Go-specific ..., mapped with marker
	case SpreadPostfix:
		return "Not" // Go-specific ..., mapped with marker
	case Tilde:
		return "Complement" // Go-specific ~, mapped with marker
	default:
		return "Negative"
	}
}

// ParseUnaryOperator converts a Java enum name to a UnaryOperator.
func ParseUnaryOperator(s string) UnaryOperator {
	switch s {
	case "PreIncrement":
		return Positive // Go doesn't have pre-increment
	case "PreDecrement":
		return Negate // Go doesn't have pre-decrement
	case "PostIncrement":
		return PostIncrement
	case "PostDecrement":
		return PostDecrement
	case "Positive":
		return Positive
	case "Negative":
		return Negate
	case "Complement":
		return BitwiseNot
	case "Not":
		return Not
	default:
		return Negate
	}
}

func (*Unary) isTree()       {}
func (*Unary) isJ()          {}
func (*Unary) isExpression() {}
func (*Unary) isStatement()  {}

func (n *Unary) WithPrefix(prefix Space) *Unary {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Unary) WithMarkers(markers Markers) *Unary {
	c := *n
	c.Markers = markers
	return &c
}

func (n *Unary) WithOperand(operand Expression) *Unary {
	c := *n
	c.Operand = operand
	return &c
}

// FieldAccess represents a field or method access like `obj.Field`.
type FieldAccess struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Target  Expression
	Name    LeftPadded[*Identifier]
	Type    JavaType // the result type (nullable)
}

func (*FieldAccess) isTree()       {}
func (*FieldAccess) isJ()          {}
func (*FieldAccess) isExpression() {}

func (n *FieldAccess) WithPrefix(prefix Space) *FieldAccess {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *FieldAccess) WithMarkers(markers Markers) *FieldAccess {
	c := *n
	c.Markers = markers
	return &c
}

func (n *FieldAccess) WithTarget(target Expression) *FieldAccess {
	c := *n
	c.Target = target
	return &c
}

// MethodInvocation represents a function or method call like `f(args)`.
type MethodInvocation struct {
	ID         uuid.UUID
	Prefix     Space
	Markers    Markers
	Select     *RightPadded[Expression] // nil for free functions
	Name       *Identifier
	Arguments  Container[Expression]
	MethodType *JavaTypeMethod // the method type being invoked (nullable)
}

func (*MethodInvocation) isTree()       {}
func (*MethodInvocation) isJ()          {}
func (*MethodInvocation) isExpression() {}
func (*MethodInvocation) isStatement()  {}

func (n *MethodInvocation) WithPrefix(prefix Space) *MethodInvocation {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *MethodInvocation) WithMarkers(markers Markers) *MethodInvocation {
	c := *n
	c.Markers = markers
	return &c
}

func (n *MethodInvocation) WithName(name *Identifier) *MethodInvocation {
	c := *n
	c.Name = name
	return &c
}

// VariableDeclarations represents one or more variable declarations.
// For grouped declarations `var ( ... )` or `const ( ... )`, Specs is non-nil and
// Variables/TypeExpr are unused.
type VariableDeclarations struct {
	ID        uuid.UUID
	Prefix    Space
	Markers   Markers
	TypeExpr  Expression                        // the declared type (nil if inferred)
	Variables []RightPadded[*VariableDeclarator] // the declared variables
	Specs     *Container[Statement]             // non-nil for grouped `var ( ... )`; Before = space before `(`
}

func (*VariableDeclarations) isTree()      {}
func (*VariableDeclarations) isJ()         {}
func (*VariableDeclarations) isStatement() {}

func (n *VariableDeclarations) WithPrefix(prefix Space) *VariableDeclarations {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *VariableDeclarations) WithMarkers(markers Markers) *VariableDeclarations {
	c := *n
	c.Markers = markers
	return &c
}

// VariableDeclarator represents a single variable with optional initializer.
type VariableDeclarator struct {
	ID          uuid.UUID
	Prefix      Space
	Markers     Markers
	Name        *Identifier
	Initializer *LeftPadded[Expression] // nil if no initializer
}

func (*VariableDeclarator) isTree() {}
func (*VariableDeclarator) isJ()    {}

func (n *VariableDeclarator) WithPrefix(prefix Space) *VariableDeclarator {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *VariableDeclarator) WithMarkers(markers Markers) *VariableDeclarator {
	c := *n
	c.Markers = markers
	return &c
}

func (n *VariableDeclarator) WithName(name *Identifier) *VariableDeclarator {
	c := *n
	c.Name = name
	return &c
}

// ArrayType represents an array or slice type like `[]T`, `[N]T`.
type ArrayType struct {
	ID          uuid.UUID
	Prefix      Space
	Markers     Markers
	Dimension   LeftPadded[Space] // Before = space before `[`, Element = space inside brackets before `]`; for `[N]T`, the length expr is stored separately
	Length      Expression        // nil for slices (`[]T`), non-nil for arrays (`[N]T`)
	ElementType Expression        // the element type
	Type        JavaType          // the array type (nullable)
}

func (*ArrayType) isTree()       {}
func (*ArrayType) isJ()          {}
func (*ArrayType) isExpression() {}

func (n *ArrayType) WithPrefix(prefix Space) *ArrayType {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *ArrayType) WithMarkers(markers Markers) *ArrayType {
	c := *n
	c.Markers = markers
	return &c
}

// Parentheses wraps an expression in parentheses: `(expr)`.
type Parentheses struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Tree    RightPadded[Expression] // Element = inner expr, After = space before `)`
	Type    JavaType                // the result type (nullable)
}

func (*Parentheses) isTree()       {}
func (*Parentheses) isJ()          {}
func (*Parentheses) isExpression() {}

func (n *Parentheses) WithPrefix(prefix Space) *Parentheses {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Parentheses) WithMarkers(markers Markers) *Parentheses {
	c := *n
	c.Markers = markers
	return &c
}

// TypeCast represents a type assertion or type cast: `x.(T)` in Go.
type TypeCast struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Clazz   *ControlParentheses // the type in parentheses
	Expr    Expression          // the expression being cast/asserted
	Type    JavaType            // the result type (nullable)
}

func (*TypeCast) isTree()       {}
func (*TypeCast) isJ()          {}
func (*TypeCast) isExpression() {}

func (n *TypeCast) WithPrefix(prefix Space) *TypeCast {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *TypeCast) WithMarkers(markers Markers) *TypeCast {
	c := *n
	c.Markers = markers
	return &c
}

// ControlParentheses wraps an expression in parentheses for control flow.
type ControlParentheses struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Tree    RightPadded[Expression] // Element = inner, After = space before `)`
}

func (*ControlParentheses) isTree()       {}
func (*ControlParentheses) isJ()          {}
func (*ControlParentheses) isExpression() {}

func (n *ControlParentheses) WithPrefix(prefix Space) *ControlParentheses {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *ControlParentheses) WithMarkers(markers Markers) *ControlParentheses {
	c := *n
	c.Markers = markers
	return &c
}

// ArrayAccess represents an index expression like `a[i]` or `m[key]`.
type ArrayAccess struct {
	ID        uuid.UUID
	Prefix    Space
	Markers   Markers
	Indexed   Expression
	Dimension *ArrayDimension
	Type      JavaType // the result type (nullable)
}

// ArrayDimension represents the `[index]` part of an array access.
type ArrayDimension struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Index   RightPadded[Expression] // Element = index expr, After = space before `]`
}

func (*ArrayAccess) isTree()       {}
func (*ArrayAccess) isJ()          {}
func (*ArrayAccess) isExpression() {}

func (n *ArrayAccess) WithPrefix(prefix Space) *ArrayAccess {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *ArrayAccess) WithMarkers(markers Markers) *ArrayAccess {
	c := *n
	c.Markers = markers
	return &c
}

func (*ArrayDimension) isTree() {}
func (*ArrayDimension) isJ()    {}

func (n *ArrayDimension) WithPrefix(prefix Space) *ArrayDimension {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *ArrayDimension) WithMarkers(markers Markers) *ArrayDimension {
	c := *n
	c.Markers = markers
	return &c
}

// Import represents a single import declaration.
type Import struct {
	ID      uuid.UUID
	Prefix  Space
	Markers Markers
	Alias  *LeftPadded[*Identifier] // nil if no alias
	Qualid Expression               // typically *Literal for Go imports
}

func (*Import) isTree() {}
func (*Import) isJ()    {}

func (n *Import) WithPrefix(prefix Space) *Import {
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *Import) WithMarkers(markers Markers) *Import {
	c := *n
	c.Markers = markers
	return &c
}
