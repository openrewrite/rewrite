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

//! Walks an LstNode tree and emits source text to a buffer.
//! The pattern follows the Go printer: for each node emit prefix whitespace,
//! emit the node's keyword/operator/text, then recurse into children.

const std = @import("std");
const tree = @import("tree.zig");

const LstNode = tree.LstNode;
const Space = tree.Space;
const RightPadded = tree.RightPadded;
const LeftPadded = tree.LeftPadded;

pub const Printer = struct {
    buf: std.ArrayListUnmanaged(u8),
    allocator: std.mem.Allocator,

    pub const PrintError = std.mem.Allocator.Error;

    pub fn create(allocator: std.mem.Allocator) Printer {
        return .{
            .buf = .{},
            .allocator = allocator,
        };
    }

    pub fn deinit(self: *Printer) void {
        self.buf.deinit(self.allocator);
    }

    pub fn getOutput(self: *const Printer) []const u8 {
        return self.buf.items;
    }

    // -----------------------------------------------------------------
    // Entry point: print any LstNode
    // -----------------------------------------------------------------

    pub fn printNode(self: *Printer, node: LstNode) PrintError!void {
        switch (node) {
            .compilation_unit => |cu| try self.printCompilationUnit(cu),
            .method_declaration => |md| try self.printMethodDeclaration(md),
            .variable_declarations => |vd| try self.printVariableDeclarations(vd),
            .identifier => |id| try self.printIdentifier(id),
            .literal => |lit| try self.printLiteral(lit),
            .binary => |bin| try self.printBinary(bin),
            .block => |blk| try self.printBlock(blk),
            .field_access => |fa| try self.printFieldAccess(fa),
            .method_invocation => |mi| try self.printMethodInvocation(mi),
            .@"return" => |ret| try self.printReturn(ret),
            .assignment => |asgn| try self.printAssignment(asgn),
            .@"if" => |i| try self.printIf(i),
            .while_loop => |wl| try self.printWhileLoop(wl),
            .unary => |u| try self.printUnary(u),
            .parentheses => |p| try self.printParentheses(p),
            .array_access => |aa| try self.printArrayAccess(aa),
            .assignment_op => |ao| try self.printAssignmentOp(ao),
            .zig_slice => |s| try self.printSlice(s),
            .zig_error_union => |eu| try self.printErrorUnion(eu),
            .zig_switch => |sw| try self.printSwitch(sw),
            .zig_switch_prong => |sp| try self.printSwitchProng(sp),
            .zig_for_loop => |fl| try self.printForLoop(fl),
            .zig_defer => |d| try self.printDefer(d),
            .zig_comptime => |ct| try self.printComptime(ct),
            .zig_test_decl => |td| try self.printTestDecl(td),
            .unknown => |u| try self.printUnknown(u),
            .unknown_source => |us| try self.printUnknownSource(us),
            .empty => |emp| try self.printEmpty(emp),
        }
    }

    // -----------------------------------------------------------------
    // Helper: emit whitespace/prefix text
    // -----------------------------------------------------------------

    pub fn emit(self: *Printer, text: []const u8) PrintError!void {
        if (text.len > 0) {
            try self.buf.appendSlice(self.allocator, text);
        }
    }

    fn emitByte(self: *Printer, byte: u8) PrintError!void {
        try self.buf.append(self.allocator, byte);
    }

    pub fn printSpace(self: *Printer, space: Space) PrintError!void {
        try self.emit(space.whitespace);
    }

    // -----------------------------------------------------------------
    // CompilationUnit
    // -----------------------------------------------------------------

    fn printCompilationUnit(self: *Printer, cu: *const tree.CompilationUnit) PrintError!void {
        try self.printSpace(cu.prefix);
        for (cu.statements) |stmt| {
            try self.printNode(stmt.element);
            try self.printSpace(stmt.after);
        }
        try self.printSpace(cu.eof);
    }

    // -----------------------------------------------------------------
    // MethodDeclaration
    // prefix + keywords + name + params_prefix + params_text + return_type + body
    // -----------------------------------------------------------------

    fn printMethodDeclaration(self: *Printer, md: *const tree.MethodDeclaration) PrintError!void {
        try self.printSpace(md.prefix);
        // Keywords: "fn", "pub fn", "export fn", etc.
        try self.emit(md.keywords);
        // Function name (with its own prefix for spacing)
        try self.printNode(md.name);
        // Parameter list: prefix space + full params text "(a: i32, b: i32)"
        try self.printSpace(md.params_prefix);
        try self.emit(md.params_text);
        // Return type (if present) -- its prefix captures the space after ")"
        if (md.return_type) |rt| {
            try self.printNode(rt);
        }
        // Body block (if present)
        if (md.body) |body| {
            try self.printNode(body);
        }
    }

    // -----------------------------------------------------------------
    // VariableDeclarations
    // prefix + keyword + variables[0].name + type_expression + initializer
    // -----------------------------------------------------------------

    fn printVariableDeclarations(self: *Printer, vd: *const tree.VariableDeclarations) PrintError!void {
        try self.printSpace(vd.prefix);
        // Keyword: "const", "var", "pub const", "pub var", etc.
        try self.emit(vd.keyword);
        // Variables (typically one for Zig)
        for (vd.variables) |v| {
            // The element is the name identifier
            try self.printNode(v.element);
        }
        // Type expression (prefix includes ": ")
        if (vd.type_expression) |te| {
            try self.printNode(te);
        }
        // Initializer (left-padded by "=" whitespace)
        if (vd.initializer) |initializer| {
            try self.printSpace(initializer.before);
            try self.emitByte('=');
            try self.printNode(initializer.element);
        }
    }

    // -----------------------------------------------------------------
    // Identifier
    // prefix + simple_name
    // -----------------------------------------------------------------

    fn printIdentifier(self: *Printer, id: *const tree.Identifier) PrintError!void {
        try self.printSpace(id.prefix);
        try self.emit(id.simple_name);
    }

    // -----------------------------------------------------------------
    // Literal
    // prefix + value_source
    // -----------------------------------------------------------------

    fn printLiteral(self: *Printer, lit: *const tree.Literal) PrintError!void {
        try self.printSpace(lit.prefix);
        if (lit.value_source) |vs| {
            try self.emit(vs);
        }
    }

    // -----------------------------------------------------------------
    // Binary
    // left + operator.before + operator_source + right
    // (prefix = left's prefix, so we don't emit it separately)
    // -----------------------------------------------------------------

    fn printBinary(self: *Printer, bin: *const tree.Binary) PrintError!void {
        // The binary prefix IS the left operand's prefix (set by parser),
        // so we don't emit bin.prefix separately -- it's emitted when we print left.
        try self.printNode(bin.left);
        try self.printSpace(bin.operator.before);
        try self.emit(bin.operator_source);
        try self.printNode(bin.right);
    }

    // -----------------------------------------------------------------
    // Block
    // prefix + "{" + statements + end + "}"
    // -----------------------------------------------------------------

    fn printBlock(self: *Printer, blk: *const tree.Block) PrintError!void {
        try self.printSpace(blk.prefix);
        try self.emitByte('{');
        for (blk.statements) |stmt| {
            try self.printNode(stmt.element);
            try self.printSpace(stmt.after);
        }
        try self.printSpace(blk.end);
        try self.emitByte('}');
    }

    // -----------------------------------------------------------------
    // Return
    // prefix + "return" + expression
    // -----------------------------------------------------------------

    fn printReturn(self: *Printer, ret: *const tree.Return) PrintError!void {
        try self.printSpace(ret.prefix);
        try self.emit("return");
        if (ret.expression) |expr| {
            try self.printNode(expr);
        }
    }

    // -----------------------------------------------------------------
    // FieldAccess
    // target + name.before + "." + name.element
    // (prefix = target's prefix, so we don't emit it separately)
    // -----------------------------------------------------------------

    fn printFieldAccess(self: *Printer, fa: *const tree.FieldAccess) PrintError!void {
        // The field access prefix IS the target's prefix (set by parser).
        try self.printNode(fa.target);
        try self.printSpace(fa.name.before);
        try self.emitByte('.');
        try self.printNode(fa.name.element);
    }

    // -----------------------------------------------------------------
    // MethodInvocation
    // select + "." + name + args_prefix + "(" + args + ")"
    // (prefix = name's prefix / select's prefix)
    // -----------------------------------------------------------------

    fn printMethodInvocation(self: *Printer, mi: *const tree.MethodInvocation) PrintError!void {
        if (mi.select) |sel| {
            // Method-style call: target.name(args)
            try self.printNode(sel.element);
            try self.printSpace(sel.after);
            try self.emitByte('.');
        }
        try self.printNode(mi.name);
        try self.printSpace(mi.args_prefix);
        try self.emitByte('(');
        for (mi.args) |arg| {
            try self.printNode(arg.element);
            try self.printSpace(arg.after);
        }
        try self.emitByte(')');
    }

    // -----------------------------------------------------------------
    // Assignment
    // variable + assignment.before + "=" + assignment.element
    // (prefix = variable's prefix)
    // -----------------------------------------------------------------

    fn printAssignment(self: *Printer, asgn: *const tree.Assignment) PrintError!void {
        try self.printNode(asgn.variable);
        try self.printSpace(asgn.assignment.before);
        try self.emitByte('=');
        try self.printNode(asgn.assignment.element);
    }

    // -----------------------------------------------------------------
    // If
    // prefix + "if" + " (" + condition + ")" + then + else?
    // -----------------------------------------------------------------

    fn printIf(self: *Printer, i: *const tree.If) PrintError!void {
        try self.printSpace(i.prefix);
        try self.emit("if");
        try self.printSpace(i.lparen_prefix);
        try self.emitByte('(');
        try self.printNode(i.condition);
        try self.printSpace(i.condition_after);
        try self.emitByte(')');
        try self.printNode(i.then_part);
        try self.printSpace(i.then_after);
        if (i.else_part) |else_part| {
            try self.printSpace(else_part.prefix);
            try self.emit("else");
            try self.printNode(else_part.body);
            try self.printSpace(else_part.body_after);
        }
    }

    // -----------------------------------------------------------------
    // WhileLoop
    // prefix + "while" + " (" + condition + ")" + body
    // -----------------------------------------------------------------

    fn printWhileLoop(self: *Printer, wl: *const tree.WhileLoop) PrintError!void {
        try self.printSpace(wl.prefix);
        try self.emit("while");
        try self.printSpace(wl.lparen_prefix);
        try self.emitByte('(');
        try self.printNode(wl.condition);
        try self.printSpace(wl.condition_after);
        try self.emitByte(')');
        try self.printNode(wl.body);
        try self.printSpace(wl.body_after);
    }

    // -----------------------------------------------------------------
    // Unary
    // prefix + operator_source + expression
    // -----------------------------------------------------------------

    fn printUnary(self: *Printer, u: *const tree.Unary) PrintError!void {
        try self.printSpace(u.prefix);
        try self.emit(u.operator_source);
        try self.printNode(u.expression);
    }

    // -----------------------------------------------------------------
    // AssignmentOperation
    // variable + operator + assignment
    // -----------------------------------------------------------------

    fn printAssignmentOp(self: *Printer, ao: *const tree.AssignmentOperation) PrintError!void {
        // prefix IS the variable's prefix (set by parser)
        try self.printNode(ao.variable);
        try self.printSpace(ao.operator.before);
        try self.emit(ao.operator_source);
        try self.printNode(ao.assignment);
    }

    // -----------------------------------------------------------------
    // ArrayAccess
    // target + "[" + index + "]"
    // -----------------------------------------------------------------

    fn printArrayAccess(self: *Printer, aa: *const tree.ArrayAccess) PrintError!void {
        // prefix IS the target's prefix (set by parser)
        try self.printNode(aa.indexed);
        try self.printSpace(aa.dimension_prefix);
        try self.emitByte('[');
        try self.printNode(aa.index);
        try self.printSpace(aa.index_after);
        try self.emitByte(']');
    }

    // -----------------------------------------------------------------
    // Parentheses
    // prefix + "(" + expression + after + ")"
    // -----------------------------------------------------------------

    fn printParentheses(self: *Printer, p: *const tree.Parentheses) PrintError!void {
        try self.printSpace(p.prefix);
        try self.emitByte('(');
        try self.printNode(p.expression);
        try self.printSpace(p.after);
        try self.emitByte(')');
    }

    // -----------------------------------------------------------------
    // Slice
    // target + "[" + start + ".." + end? + "]"
    // -----------------------------------------------------------------

    fn printSlice(self: *Printer, s: *const tree.Slice) PrintError!void {
        // prefix IS the target's prefix (set by parser)
        try self.printNode(s.target);
        try self.printSpace(s.open_bracket);
        try self.emitByte('[');
        try self.printNode(s.start.element);
        try self.printSpace(s.start.after);
        if (s.end) |end| {
            try self.printNode(end);
        }
        try self.printSpace(s.close_bracket);
        try self.emitByte(']');
    }

    // -----------------------------------------------------------------
    // ErrorUnion
    // error_type? + "!" + value_type
    // For implicit error sets: prefix + "!" + value_type
    // For explicit: error_type + "!" + value_type
    // -----------------------------------------------------------------

    fn printErrorUnion(self: *Printer, eu: *const tree.ErrorUnion) PrintError!void {
        if (eu.error_type) |et| {
            try self.printNode(et);
        } else {
            try self.printSpace(eu.prefix);
        }
        try self.emitByte('!');
        try self.printSpace(eu.value_type.before);
        try self.printNode(eu.value_type.element);
    }

    // -----------------------------------------------------------------
    // Switch
    // prefix + "switch" + "(" + condition + ")" + "{" + prongs + "}"
    // -----------------------------------------------------------------

    fn printSwitch(self: *Printer, sw: *const tree.Switch) PrintError!void {
        try self.printSpace(sw.prefix);
        try self.emit("switch");
        try self.printSpace(sw.lparen_prefix);
        try self.emitByte('(');
        try self.printNode(sw.condition);
        try self.printSpace(sw.condition_after);
        try self.emitByte(')');
        try self.printSpace(sw.lbrace_prefix);
        try self.emitByte('{');
        for (sw.prongs) |prong| {
            try self.printNode(prong.element);
            try self.printSpace(prong.after);
        }
        try self.printSpace(sw.end);
        try self.emitByte('}');
    }

    // -----------------------------------------------------------------
    // SwitchProng
    // prefix + cases + payload? + "=>" + arrow expression
    // For else case: cases is empty, prefix contains "else"
    // -----------------------------------------------------------------

    fn printSwitchProng(self: *Printer, sp: *const tree.SwitchProng) PrintError!void {
        try self.printSpace(sp.prefix);
        // Print case values
        try self.printSpace(sp.cases_prefix);
        for (sp.cases) |c| {
            try self.printNode(c.element);
            try self.printSpace(c.after);
        }
        // Print payload if present
        if (sp.payload) |pl| {
            try self.printNode(pl);
        }
        // Print arrow (=> + target expression)
        try self.printSpace(sp.arrow.before);
        try self.emit("=>");
        try self.printNode(sp.arrow.element);
    }

    // -----------------------------------------------------------------
    // ForLoop
    // prefix + "for" + " " + iterable_text + " " + payload_text + body
    // -----------------------------------------------------------------

    fn printForLoop(self: *Printer, fl: *const tree.ForLoop) PrintError!void {
        try self.printSpace(fl.prefix);
        try self.emit("for");
        try self.printSpace(fl.lparen_prefix);
        try self.emit(fl.iterable_text);
        // Payload text already has its leading space baked in
        try self.emit(fl.payload_text);
        try self.printNode(fl.body);
        try self.printSpace(fl.body_after);
        if (fl.else_body) |else_body| {
            try self.printSpace(else_body.prefix);
            try self.emit("else");
            try self.printNode(else_body.body);
            try self.printSpace(else_body.body_after);
        }
    }

    // -----------------------------------------------------------------
    // Defer
    // prefix + "defer"/"errdefer" + expression
    // -----------------------------------------------------------------

    fn printDefer(self: *Printer, d: *const tree.Defer) PrintError!void {
        try self.printSpace(d.prefix);
        if (d.is_errdefer) {
            try self.emit("errdefer");
        } else {
            try self.emit("defer");
        }
        if (d.payload) |p| {
            try self.printNode(p);
        }
        try self.printNode(d.expression);
    }

    // -----------------------------------------------------------------
    // Comptime
    // prefix + "comptime" + expression
    // -----------------------------------------------------------------

    fn printComptime(self: *Printer, ct: *const tree.Comptime) PrintError!void {
        try self.printSpace(ct.prefix);
        try self.emit("comptime");
        try self.printNode(ct.expression);
    }

    // -----------------------------------------------------------------
    // TestDecl
    // prefix + "test" + name + body
    // -----------------------------------------------------------------

    fn printTestDecl(self: *Printer, td: *const tree.TestDecl) PrintError!void {
        try self.printSpace(td.prefix);
        try self.emit("test");
        if (td.name) |name| {
            try self.printNode(name);
        }
        try self.printNode(td.body);
    }

    // -----------------------------------------------------------------
    // Unknown
    // prefix + source
    // -----------------------------------------------------------------

    fn printUnknown(self: *Printer, u: *const tree.Unknown) PrintError!void {
        try self.printSpace(u.prefix);
        try self.printNode(u.source);
    }

    // -----------------------------------------------------------------
    // UnknownSource
    // prefix + text
    // -----------------------------------------------------------------

    fn printUnknownSource(self: *Printer, us: *const tree.UnknownSource) PrintError!void {
        try self.printSpace(us.prefix);
        try self.emit(us.text);
    }

    // -----------------------------------------------------------------
    // Empty
    // prefix only
    // -----------------------------------------------------------------

    fn printEmpty(self: *Printer, emp: *const tree.Empty) PrintError!void {
        try self.printSpace(emp.prefix);
    }
};

/// Top-level print function: create a printer, walk the tree, return owned output.
pub fn print(allocator: std.mem.Allocator, node: LstNode) ![]const u8 {
    var p = Printer.create(allocator);
    defer p.deinit();
    try p.printNode(node);
    return try allocator.dupe(u8, p.getOutput());
}

// -----------------------------------------------------------------
// Tests
// -----------------------------------------------------------------

test "print identifier" {
    const allocator = std.testing.allocator;
    var ident = tree.Identifier{
        .id = "test-uuid",
        .prefix = .{ .whitespace = " " },
        .simple_name = "foo",
    };
    const node = LstNode{ .identifier = &ident };
    const result = try print(allocator, node);
    defer allocator.free(result);
    try std.testing.expectEqualStrings(" foo", result);
}

test "print literal" {
    const allocator = std.testing.allocator;
    var lit = tree.Literal{
        .id = "test-uuid",
        .prefix = .{ .whitespace = " " },
        .value = .{ .int = 42 },
        .value_source = "42",
    };
    const node = LstNode{ .literal = &lit };
    const result = try print(allocator, node);
    defer allocator.free(result);
    try std.testing.expectEqualStrings(" 42", result);
}

test "print block" {
    const allocator = std.testing.allocator;
    var blk = tree.Block{
        .id = "test-uuid",
        .prefix = .{ .whitespace = " " },
        .statements = &.{},
        .end = .{ .whitespace = "" },
    };
    const node = LstNode{ .block = &blk };
    const result = try print(allocator, node);
    defer allocator.free(result);
    try std.testing.expectEqualStrings(" {}", result);
}

test "print unknown source" {
    const allocator = std.testing.allocator;
    var src = tree.UnknownSource{
        .id = "test-uuid",
        .prefix = .{ .whitespace = "" },
        .text = "some_code()",
    };
    var unk = tree.Unknown{
        .id = "test-uuid-2",
        .prefix = .{ .whitespace = "\n" },
        .source = LstNode{ .unknown_source = &src },
    };
    const node = LstNode{ .unknown = &unk };
    const result = try print(allocator, node);
    defer allocator.free(result);
    try std.testing.expectEqualStrings("\nsome_code()", result);
}
