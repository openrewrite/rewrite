// Copyright 2025 the original author or authors.
//
// Licensed under the Moderne Source Available License (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://docs.moderne.io/licensing/moderne-source-available-license
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Zig-side LST node types.
//! Each variant is a struct holding the data needed for RPC serialization:
//! an id (UUID string), prefix (whitespace string), and type-specific fields.

const std = @import("std");

/// A unique identifier string (UUID v4, 36 chars).
pub const Uuid = []const u8;

/// Represents whitespace/formatting before a syntax element.
pub const Space = struct {
    whitespace: []const u8,
};

/// Right-padded element: element + whitespace after it.
pub fn RightPadded(comptime T: type) type {
    return struct {
        element: T,
        after: Space,
    };
}

/// Left-padded element: whitespace before + element.
pub fn LeftPadded(comptime T: type) type {
    return struct {
        before: Space,
        element: T,
    };
}

/// The top-level tagged union for all LST node types.
pub const LstNode = union(enum) {
    compilation_unit: *CompilationUnit,
    method_declaration: *MethodDeclaration,
    method_invocation: *MethodInvocation,
    variable_declarations: *VariableDeclarations,
    identifier: *Identifier,
    literal: *Literal,
    binary: *Binary,
    block: *Block,
    field_access: *FieldAccess,
    @"return": *Return,
    assignment: *Assignment,
    // Control flow
    @"if": *If,
    while_loop: *WhileLoop,
    unary: *Unary,
    parentheses: *Parentheses,
    array_access: *ArrayAccess,
    assignment_op: *AssignmentOperation,
    // Zig-specific types
    zig_defer: *Defer,
    zig_comptime: *Comptime,
    zig_test_decl: *TestDecl,
    zig_slice: *Slice,
    zig_error_union: *ErrorUnion,
    zig_switch: *Switch,
    zig_switch_prong: *SwitchProng,
    zig_for_loop: *ForLoop,
    // Fallback types
    unknown: *Unknown,
    unknown_source: *UnknownSource,
    empty: *Empty,

    /// Get the id of any LST node.
    pub fn getId(self: LstNode) Uuid {
        return switch (self) {
            inline else => |node| node.id,
        };
    }

    /// Get the prefix of any LST node.
    pub fn getPrefix(self: LstNode) Space {
        return switch (self) {
            inline else => |node| node.prefix,
        };
    }
};

/// Zig.CompilationUnit - the root of a Zig source file.
pub const CompilationUnit = struct {
    id: Uuid,
    prefix: Space,
    source_path: []const u8,
    charset: []const u8 = "UTF-8",
    charset_bom_marked: bool = false,
    statements: []RightPadded(LstNode),
    eof: Space,
};

/// J.MethodDeclaration - maps to Zig fn declarations.
pub const MethodDeclaration = struct {
    id: Uuid,
    prefix: Space,
    /// The keyword text from the start of this declaration up to (not including)
    /// the function name. E.g. "fn", "pub fn", "export fn".
    /// This text is consumed by the parser cursor but not stored in any Space field.
    keywords: []const u8 = "fn",
    /// Return type expression (e.g. "void", "i32")
    return_type: ?LstNode = null,
    /// Function name
    name: LstNode,
    /// Parameter list container prefix (the "(" token prefix)
    params_prefix: Space,
    /// Source text of the parameter list including parentheses, e.g. "(a: i32, b: i32)".
    /// The parser skips parameter details; this text enables faithful printing.
    params_text: []const u8 = "()",
    /// The function body block
    body: ?LstNode = null,
};

/// J.VariableDeclarations - maps to Zig const/var declarations.
pub const VariableDeclarations = struct {
    id: Uuid,
    prefix: Space,
    /// The keyword text from the start of this declaration up to (not including)
    /// the variable name. E.g. "const", "var", "pub const".
    /// This text is consumed by the parser cursor but not stored in any Space field.
    keyword: []const u8 = "const",
    /// Type expression (e.g., "u32")
    type_expression: ?LstNode = null,
    /// The named variables (typically one for Zig)
    variables: []RightPadded(LstNode),
    /// The initializer expression for the first variable (left-padded by "=" whitespace).
    /// In Zig, each const/var has exactly one variable, so this is stored here directly.
    initializer: ?LeftPadded(LstNode) = null,
};

/// J.VariableDeclarations.NamedVariable - a single variable within a declaration.
pub const NamedVariable = struct {
    id: Uuid,
    prefix: Space,
    /// The variable name identifier (as the declarator)
    name: LstNode,
    /// The initializer expression, if present (left-padded with "=" space)
    initializer: ?LeftPadded(LstNode) = null,
};

/// J.Identifier - a simple name reference.
pub const Identifier = struct {
    id: Uuid,
    prefix: Space,
    simple_name: []const u8,
};

/// J.Literal - a literal value (number, string, etc.).
pub const Literal = struct {
    id: Uuid,
    prefix: Space,
    /// The value as a JSON-compatible value (null for now since Zig doesn't have Java types)
    value: ?LiteralValue = null,
    /// The original source text of the literal
    value_source: ?[]const u8 = null,
};

/// Possible literal values for serialization.
pub const LiteralValue = union(enum) {
    int: i64,
    float: f64,
    string: []const u8,
    boolean: bool,
};

/// J.Block - a block of statements delimited by braces.
pub const Block = struct {
    id: Uuid,
    prefix: Space,
    /// Whether this is a static block (always false for Zig)
    is_static: bool = false,
    /// The prefix for the static keyword (empty for Zig)
    static_prefix: Space = .{ .whitespace = "" },
    /// Statements within the block
    statements: []RightPadded(LstNode),
    /// Whitespace before the closing brace
    end: Space,
};

/// J.Return - a return statement.
pub const Return = struct {
    id: Uuid,
    prefix: Space,
    /// The expression being returned (null if bare return)
    expression: ?LstNode = null,
};

/// J.Unknown - fallback for unmapped syntax elements.
pub const Unknown = struct {
    id: Uuid,
    prefix: Space,
    /// The source text wrapped in an UnknownSource
    source: LstNode,
};

/// J.Unknown.Source - the source text of an unknown element.
pub const UnknownSource = struct {
    id: Uuid,
    prefix: Space,
    text: []const u8,
};

/// J.Binary - a binary expression (a + b, a == b, etc.).
pub const Binary = struct {
    id: Uuid,
    prefix: Space,
    /// Left operand
    left: LstNode,
    /// Operator (left-padded with the operator token text as a string enum)
    operator: LeftPadded([]const u8),
    /// The actual source text of the operator token (e.g. "+", "==", "++").
    /// The `operator.element` field stores the Java enum name for RPC, while
    /// this field stores the Zig source text for faithful printing.
    operator_source: []const u8 = "+",
    /// Right operand
    right: LstNode,
};

/// J.FieldAccess - a field access expression (a.b).
pub const FieldAccess = struct {
    id: Uuid,
    prefix: Space,
    /// The target expression
    target: LstNode,
    /// The field name (left-padded with the "." token space)
    name: LeftPadded(LstNode),
};

/// J.MethodInvocation - a function/method call (a(b, c)).
pub const MethodInvocation = struct {
    id: Uuid,
    prefix: Space,
    /// The select expression (the target before the method name, if a.foo() style)
    /// For Zig, this is null for simple calls like foo(x).
    select: ?RightPadded(LstNode) = null,
    /// The function name identifier
    name: LstNode,
    /// The argument list container prefix (the "(" token prefix)
    args_prefix: Space,
    /// The arguments
    args: []RightPadded(LstNode),
};

/// J.Assignment - an assignment expression (a = b).
pub const Assignment = struct {
    id: Uuid,
    prefix: Space,
    /// The variable (left side)
    variable: LstNode,
    /// The assignment expression (right side, left-padded with "=" space)
    assignment: LeftPadded(LstNode),
};

/// J.If - an if/else statement.
/// JavaSender.visitIf:
///   1. ifCondition (ControlParentheses - visit)
///   2. thenPart (RightPadded<Statement>)
///   3. elsePart (If.Else - nullable, visit)
pub const If = struct {
    id: Uuid,
    prefix: Space,
    /// Whitespace before the "(" token (between "if" and "(")
    lparen_prefix: Space,
    /// The condition expression (inside parens in Zig: if (cond))
    condition: LstNode,
    /// Whitespace before the ")" token (between condition and ")")
    condition_after: Space,
    /// The "then" branch statement
    then_part: LstNode,
    /// Whitespace after the then part (inside RightPadded)
    then_after: Space,
    /// The "else" part (null if no else)
    else_part: ?*IfElse = null,
};

/// J.If.Else - the else branch of an if statement.
/// JavaSender.visitElse:
///   1. body (RightPadded<Statement>)
pub const IfElse = struct {
    id: Uuid,
    prefix: Space,
    /// The else body statement
    body: LstNode,
    /// Whitespace after the body (inside RightPadded)
    body_after: Space,
};

/// J.WhileLoop - a while loop.
/// JavaSender.visitWhileLoop:
///   1. condition (ControlParentheses - visit)
///   2. body (RightPadded<Statement>)
pub const WhileLoop = struct {
    id: Uuid,
    prefix: Space,
    /// Whitespace before the "(" token (between "while" and "(")
    lparen_prefix: Space,
    /// The condition expression (inside parens: while (cond))
    condition: LstNode,
    /// Whitespace before the ")" token (between condition and ")")
    condition_after: Space,
    /// The loop body
    body: LstNode,
    /// Whitespace after the body (inside RightPadded)
    body_after: Space,
};

/// J.Unary - a unary expression (prefix or postfix operator).
/// JavaSender.visitUnary:
///   1. operator (LeftPadded<J.Unary.Type>)
///   2. expression (visit)
///   3. type (null JavaType)
pub const Unary = struct {
    id: Uuid,
    prefix: Space,
    /// Operator enum name (e.g. "Negative", "Not", "Complement")
    operator: LeftPadded([]const u8),
    /// The actual source text of the operator (e.g. "-", "!", "~")
    operator_source: []const u8,
    /// The operand expression
    expression: LstNode,
};

/// J.AssignmentOperation - compound assignment (a += b, a -= b, etc.).
/// JavaSender.visitAssignmentOperation:
///   1. variable (visit)
///   2. operator (JLeftPadded<J.AssignmentOperation.Type> via visitLeftPadded)
///   3. assignment (visit)
///   4. type (null JavaType)
pub const AssignmentOperation = struct {
    id: Uuid,
    prefix: Space,
    /// Left operand (variable)
    variable: LstNode,
    /// Operator enum name (e.g. "Addition", "Subtraction")
    operator: LeftPadded([]const u8),
    /// The actual source text of the operator token (e.g. "+=", "-=")
    operator_source: []const u8,
    /// Right operand (assignment value)
    assignment: LstNode,
};

/// J.ArrayAccess - array/pointer indexing (lhs[rhs]).
/// JavaSender.visitArrayAccess:
///   1. indexed (visit - the array/pointer expression)
///   2. dimension (J.ArrayDimension - visit)
/// J.ArrayDimension:
///   1. index (RightPadded<Expression> via visitRightPadded)
pub const ArrayAccess = struct {
    id: Uuid,
    prefix: Space,
    /// The indexed expression (the array/pointer)
    indexed: LstNode,
    /// Whitespace before the "[" token
    dimension_prefix: Space,
    /// The index expression
    index: LstNode,
    /// Whitespace before the "]" token
    index_after: Space,
};

/// J.Parentheses - a parenthesized expression (expr).
/// JavaSender.visitParentheses:
///   1. tree (RightPadded<Expression> via visitRightPadded)
pub const Parentheses = struct {
    id: Uuid,
    prefix: Space,
    /// The contained expression
    expression: LstNode,
    /// Whitespace before the ")" token
    after: Space,
};

/// Zig.Defer - a defer or errdefer statement.
pub const Defer = struct {
    id: Uuid,
    prefix: Space,
    /// Whether this is an errdefer (true) or defer (false)
    is_errdefer: bool = false,
    /// The error payload (for errdefer |err| only), null for plain defer
    payload: ?LstNode = null,
    /// The deferred expression
    expression: LstNode,
};

/// Zig.Comptime - a comptime expression/block.
pub const Comptime = struct {
    id: Uuid,
    prefix: Space,
    /// The comptime expression
    expression: LstNode,
};

/// Zig.TestDecl - a test declaration (test "name" { ... }).
pub const TestDecl = struct {
    id: Uuid,
    prefix: Space,
    /// The test name (a string literal, or null for unnamed tests)
    name: ?LstNode = null,
    /// The test body block
    body: LstNode,
};

/// Zig.ErrorUnion - an error union type (ErrorSet!ValueType or !ValueType).
/// ZigSender.visitErrorUnion:
///   1. errorType (nullable visit)
///   2. valueType (JLeftPadded via visitLeftPadded)
pub const ErrorUnion = struct {
    id: Uuid,
    prefix: Space,
    /// The error set expression (null for implicit error sets like "!T")
    error_type: ?LstNode = null,
    /// The value type, left-padded by the "!" token whitespace
    value_type: LeftPadded(LstNode),
};

/// Zig.Slice - a slice expression (a[start..end]).
/// ZigSender.visitSlice:
///   1. target (visit)
///   2. openBracket (Space)
///   3. start (RightPadded via visitRightPadded)
///   4. end (nullable visit)
///   5. closeBracket (Space)
pub const Slice = struct {
    id: Uuid,
    prefix: Space,
    /// The target expression being sliced
    target: LstNode,
    /// Whitespace before the "[" token
    open_bracket: Space,
    /// The start expression
    start: RightPadded(LstNode),
    /// The end expression (null for open-ended slices)
    end: ?LstNode = null,
    /// Whitespace before the "]" token
    close_bracket: Space,
};

/// Zig.Switch - a switch expression.
/// Maps to J.Switch with a selector (ControlParentheses) and cases (Block of SwitchProng).
/// ZigSender field order:
///   (via J.Switch) selector (ControlParentheses), cases (Block)
pub const Switch = struct {
    id: Uuid,
    prefix: Space,
    /// Whitespace before the "(" token (between "switch" and "(")
    lparen_prefix: Space,
    /// The scrutinee expression (inside parens: switch (expr))
    condition: LstNode,
    /// Whitespace before the ")" token
    condition_after: Space,
    /// Whitespace before the "{" token
    lbrace_prefix: Space,
    /// The switch prongs/cases
    prongs: []RightPadded(LstNode),
    /// Whitespace before the closing "}"
    end: Space,
};

/// Zig.SwitchProng - a single case arm in a switch expression.
/// ZigSender.visitSwitchProng:
///   1. cases (JContainer of Expression)
///   2. payload (nullable visit, Zig.Payload)
///   3. arrow (JLeftPadded<Expression>)
pub const SwitchProng = struct {
    id: Uuid,
    prefix: Space,
    /// The case values (list of expressions, empty = else case)
    /// The container prefix captures the whitespace before the first case value.
    cases_prefix: Space,
    cases: []RightPadded(LstNode),
    /// Optional payload (|val|), null if not present
    payload: ?LstNode = null,
    /// The arrow expression (the target/body of this prong), left-padded by "=>" whitespace
    arrow: LeftPadded(LstNode),
};

/// Zig.ForLoop - a for loop.
/// Mapped as J.ForEachLoop.
/// JavaSender.visitForEachLoop:
///   1. control (J.ForEachLoop.Control - visit)
///   2. body (RightPadded<Statement>)
///
/// The Zig for loop syntax is: for (items) |captures| body
/// We store the full source text for the control section for faithful round-tripping.
pub const ForLoop = struct {
    id: Uuid,
    prefix: Space,
    /// Whitespace before the "(" token
    lparen_prefix: Space,
    /// Full source text of the iterable list including parens, e.g. "(items)"
    iterable_text: []const u8,
    /// Full source text of the payload captures, e.g. "|item|" or "|item, index|"
    payload_text: []const u8,
    /// The loop body
    body: LstNode,
    /// Whitespace after the body (inside RightPadded)
    body_after: Space,
    /// Optional else expression
    else_body: ?*ForLoopElse = null,
};

/// Zig.ForLoop.Else - the else branch of a for loop.
pub const ForLoopElse = struct {
    id: Uuid,
    prefix: Space,
    /// The else body expression
    body: LstNode,
    /// Whitespace after the body
    body_after: Space,
};

/// J.Empty - an empty expression placeholder.
pub const Empty = struct {
    id: Uuid,
    prefix: Space,
};

test "LstNode getId" {
    var ident = Identifier{
        .id = "test-uuid",
        .prefix = .{ .whitespace = "" },
        .simple_name = "foo",
    };
    const node = LstNode{ .identifier = &ident };
    try std.testing.expectEqualStrings("test-uuid", node.getId());
}
