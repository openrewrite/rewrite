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

//! Walks an LstNode tree and calls SendQueue methods to produce
//! RpcObjectData batches. Field ordering MUST match JavaSender.java exactly.

const std = @import("std");
const rpc = @import("rpc.zig");
const tree = @import("tree.zig");

const LstNode = tree.LstNode;
const Space = tree.Space;
const RightPadded = tree.RightPadded;
const LeftPadded = tree.LeftPadded;
const SendQueue = rpc.SendQueue;

/// Serializes an LST tree into RPC batches.
pub const Sender = struct {
    queue: *SendQueue,
    allocator: std.mem.Allocator,

    pub fn init(queue: *SendQueue, allocator: std.mem.Allocator) Sender {
        return .{
            .queue = queue,
            .allocator = allocator,
        };
    }

    // -----------------------------------------------------------------
    // Entry point: serialize any LstNode
    // -----------------------------------------------------------------

    pub const SendError = std.mem.Allocator.Error;

    pub fn send(self: *Sender, node: LstNode) SendError!void {
        switch (node) {
            .compilation_unit => |cu| try self.sendCompilationUnit(cu),
            .method_declaration => |md| try self.sendMethodDeclaration(md),
            .method_invocation => |mi| try self.sendMethodInvocation(mi),
            .variable_declarations => |vd| try self.sendVariableDeclarations(vd),
            .identifier => |id| try self.sendIdentifier(id),
            .literal => |lit| try self.sendLiteral(lit),
            .binary => |bin| try self.sendBinary(bin),
            .block => |blk| try self.sendBlock(blk),
            .field_access => |fa| try self.sendFieldAccess(fa),
            .@"return" => |ret| try self.sendReturn(ret),
            .assignment => |asgn| try self.sendAssignment(asgn),
            .@"if" => |i| try self.sendIf(i),
            .while_loop => |wl| try self.sendWhileLoop(wl),
            .unary => |u| try self.sendUnary(u),
            .parentheses => |p| try self.sendParentheses(p),
            .array_access => |aa| try self.sendArrayAccess(aa),
            .assignment_op => |ao| try self.sendAssignmentOp(ao),
            .zig_slice => |s| try self.sendSlice(s),
            .zig_error_union => |eu| try self.sendErrorUnion(eu),
            .zig_switch => |sw| try self.sendSwitch(sw),
            .zig_switch_prong => |sp| try self.sendSwitchProng(sp),
            .zig_for_loop => |fl| try self.sendForLoop(fl),
            .zig_defer => |d| try self.sendDefer(d),
            .zig_comptime => |ct| try self.sendComptime(ct),
            .zig_test_decl => |td| try self.sendTestDecl(td),
            .unknown => |unk| try self.sendUnknown(unk),
            .unknown_source => |src| try self.sendUnknownSource(src),
            .empty => |emp| try self.sendEmpty(emp),
        }
    }

    // -----------------------------------------------------------------
    // Common: preVisit pattern (sent for EVERY node)
    // Matches JavaSender.preVisit:
    //   1. id (UUID)
    //   2. prefix (Space)
    //   3. markers (Markers)
    // -----------------------------------------------------------------

    fn preVisit(self: *Sender, id: tree.Uuid, pfx: Space) SendError!void {
        // id
        try self.queue.sendAdd(null, std.json.Value{ .string = id });
        // prefix (Space object)
        try self.sendSpace(pfx);
        // markers (empty Markers)
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // Zig.CompilationUnit
    // Matches ZigSender.visitZigCompilationUnit:
    //   1. sourcePath
    //   2. charset
    //   3. charsetBomMarked
    //   4. checksum (null)
    //   5. fileAttributes (null)
    //   6. imports (JContainer - null)
    //   7. statements (list of RightPadded<Statement>)
    //   8. eof (Space)
    // -----------------------------------------------------------------

    fn sendCompilationUnit(self: *Sender, cu: *const tree.CompilationUnit) SendError!void {
        // Top-level ADD
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.zig.tree.Zig$CompilationUnit",
        });

        // preVisit
        try self.preVisit(cu.id, cu.prefix);

        // sourcePath
        try self.queue.sendAdd(null, std.json.Value{ .string = cu.source_path });
        // charset
        try self.queue.sendAdd(null, std.json.Value{ .string = cu.charset });
        // charsetBomMarked
        try self.queue.sendAdd(null, std.json.Value{ .bool = cu.charset_bom_marked });
        // checksum (null)
        try self.queue.sendDelete();
        // fileAttributes (null)
        try self.queue.sendDelete();
        // imports (JContainer<J.Import>) - null
        try self.queue.sendDelete();
        // statements (list of RightPadded<Statement>)
        try self.sendRightPaddedList(cu.statements);
        // eof (Space)
        try self.sendSpace(cu.eof);
    }

    // -----------------------------------------------------------------
    // J.MethodDeclaration
    // Matches JavaSender.visitMethodDeclaration:
    //   1. leadingAnnotations (empty list)
    //   2. modifiers (empty list)
    //   3. typeParameters (null)
    //   4. returnTypeExpression
    //   5. name annotations (empty list, from annotations.getName().getAnnotations())
    //   6. name (Identifier)
    //   7. parameters (JContainer)
    //   8. throws (null JContainer)
    //   9. body (Block)
    //  10. defaultValue (null)
    //  11. methodType (null)
    // -----------------------------------------------------------------

    fn sendMethodDeclaration(self: *Sender, md: *const tree.MethodDeclaration) SendError!void {
        // Top-level ADD
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$MethodDeclaration",
        });

        // preVisit
        try self.preVisit(md.id, md.prefix);

        // 1. leadingAnnotations (empty list)
        try self.sendEmptyList();
        // 2. modifiers (empty list)
        try self.sendEmptyList();
        // 3. typeParameters (null - J.TypeParameters via padding)
        try self.queue.sendDelete();
        // 4. returnTypeExpression
        if (md.return_type) |rt| {
            try self.send(rt);
        } else {
            try self.queue.sendDelete();
        }
        // 5. name annotations (empty list from annotations.getName().getAnnotations())
        try self.sendEmptyList();
        // 6. name (Identifier)
        try self.send(md.name);
        // 7. parameters (JContainer) - send as an empty container for now
        try self.sendEmptyContainer(md.params_prefix);
        // 8. throws (null JContainer)
        try self.queue.sendDelete();
        // 9. body (Block)
        if (md.body) |body| {
            try self.send(body);
        } else {
            try self.queue.sendDelete();
        }
        // 10. defaultValue (null JLeftPadded)
        try self.queue.sendDelete();
        // 11. methodType (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.VariableDeclarations
    // Matches JavaSender.visitVariableDeclarations:
    //   1. leadingAnnotations (empty list)
    //   2. modifiers (empty list)
    //   3. typeExpression
    //   4. varargs (null Space)
    //   5. variables (list of RightPadded<NamedVariable>)
    // -----------------------------------------------------------------

    fn sendVariableDeclarations(self: *Sender, vd: *const tree.VariableDeclarations) SendError!void {
        // Top-level ADD
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$VariableDeclarations",
        });

        // preVisit
        try self.preVisit(vd.id, vd.prefix);

        // 1. leadingAnnotations (empty list)
        try self.sendEmptyList();
        // 2. modifiers (empty list)
        try self.sendEmptyList();
        // 3. typeExpression
        if (vd.type_expression) |te| {
            try self.send(te);
        } else {
            try self.queue.sendDelete();
        }
        // 4. varargs (null Space)
        try self.queue.sendDelete();
        // 5. variables (list of RightPadded<NamedVariable>)
        //    Each element is a NamedVariable, which needs its own serialization.
        try self.sendNamedVariableList(vd.variables);
    }

    /// Send a list of NamedVariables as RightPadded list.
    /// Each NamedVariable is serialized matching JavaSender.visitVariable:
    ///   preVisit: id, prefix, markers
    ///   1. declarator (the name Identifier, visited via visit())
    ///   2. dimensionsAfterName (empty list)
    ///   3. initializer (JLeftPadded<Expression> or null)
    ///   4. variableType (null)
    fn sendNamedVariableList(self: *Sender, vars: []const RightPadded(LstNode)) SendError!void {
        // Send list header: ADD + CHANGE with positions
        try self.queue.put(.{ .state = .add });

        // Positions array
        var positions = std.json.Array.init(self.allocator);
        for (0..vars.len) |_| {
            // -1 = ADDED_LIST_ITEM
            try positions.append(std.json.Value{ .integer = -1 });
        }
        try self.queue.put(.{
            .state = .change,
            .value = std.json.Value{ .array = positions },
        });

        // Each RightPadded<NamedVariable>
        for (vars) |v| {
            // ADD for the RightPadded wrapper
            try self.queue.put(.{
                .state = .add,
                .value_type = "org.openrewrite.java.tree.JRightPadded",
            });

            // visitRightPadded protocol:
            //   1. element (NamedVariable via visit)
            //   2. after (Space)
            //   3. markers (Markers)

            // The element is a NamedVariable (sent as a new ADD typed object)
            try self.queue.put(.{
                .state = .add,
                .value_type = "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable",
            });

            // preVisit for the NamedVariable (uses a fresh UUID separate from the identifier)
            try self.preVisit(
                self.genUuid(),
                .{ .whitespace = "" }, // NamedVariable prefix is empty
            );

            // visitVariable (JavaSender.visitVariable) fields:
            //   1. declarator (the name identifier, visited via visit())
            //   2. dimensionsAfterName (empty list)
            //   3. initializer (JLeftPadded<Expression> or null)
            //   4. variableType (null)
            try self.send(v.element);
            try self.sendEmptyList();
            try self.queue.sendDelete();
            try self.queue.sendDelete();

            // RightPadded: after space
            try self.sendSpace(v.after);
            // RightPadded: markers
            try self.sendEmptyMarkers();
        }
    }

    // -----------------------------------------------------------------
    // J.Identifier
    // Matches JavaSender.visitIdentifier:
    //   1. annotations (empty list)
    //   2. simpleName (string)
    //   3. type (null JavaType)
    //   4. fieldType (null JavaType)
    // -----------------------------------------------------------------

    fn sendIdentifier(self: *Sender, id: *const tree.Identifier) SendError!void {
        // Top-level ADD
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Identifier",
        });

        // preVisit
        try self.preVisit(id.id, id.prefix);

        // 1. annotations (empty list)
        try self.sendEmptyList();
        // 2. simpleName
        try self.queue.sendAdd(null, std.json.Value{ .string = id.simple_name });
        // 3. type (null)
        try self.queue.sendDelete();
        // 4. fieldType (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.Literal
    // Matches JavaSender.visitLiteral:
    //   1. value (the actual value)
    //   2. valueSource (original source text)
    //   3. unicodeEscapes (empty list)
    //   4. type (null JavaType)
    // -----------------------------------------------------------------

    fn sendLiteral(self: *Sender, lit: *const tree.Literal) SendError!void {
        // Top-level ADD
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Literal",
        });

        // preVisit
        try self.preVisit(lit.id, lit.prefix);

        // 1. value
        if (lit.value) |val| {
            switch (val) {
                .int => |i| try self.queue.sendAdd(null, std.json.Value{ .integer = i }),
                .float => |f| try self.queue.sendAdd(null, std.json.Value{ .float = f }),
                .string => |s| try self.queue.sendAdd(null, std.json.Value{ .string = s }),
                .boolean => |b| try self.queue.sendAdd(null, std.json.Value{ .bool = b }),
            }
        } else {
            try self.queue.sendDelete();
        }
        // 2. valueSource
        if (lit.value_source) |vs| {
            try self.queue.sendAdd(null, std.json.Value{ .string = vs });
        } else {
            try self.queue.sendDelete();
        }
        // 3. unicodeEscapes (empty list)
        try self.sendEmptyList();
        // 4. type (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.Block
    // Matches JavaSender.visitBlock:
    //   1. static (RightPadded<Boolean>)
    //   2. statements (list of RightPadded<Statement>)
    //   3. end (Space)
    // -----------------------------------------------------------------

    fn sendBlock(self: *Sender, blk: *const tree.Block) SendError!void {
        // Top-level ADD
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Block",
        });

        // preVisit
        try self.preVisit(blk.id, blk.prefix);

        // 1. static (RightPadded<Boolean>)
        //    The static flag is sent as a RightPadded<Boolean>:
        //      element (boolean value)
        //      after (Space)
        //      markers (Markers)
        try self.sendRightPaddedBool(blk.is_static, blk.static_prefix);

        // 2. statements (list of RightPadded<Statement>)
        try self.sendRightPaddedList(blk.statements);

        // 3. end (Space)
        try self.sendSpace(blk.end);
    }

    // -----------------------------------------------------------------
    // J.Return
    // Matches JavaSender.visitReturn:
    //   1. expression (nullable)
    // -----------------------------------------------------------------

    fn sendReturn(self: *Sender, ret: *const tree.Return) SendError!void {
        // Top-level ADD
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Return",
        });

        // preVisit
        try self.preVisit(ret.id, ret.prefix);

        // 1. expression
        if (ret.expression) |expr| {
            try self.send(expr);
        } else {
            try self.queue.sendDelete();
        }
    }

    // -----------------------------------------------------------------
    // J.Binary
    // Matches JavaSender.visitBinary:
    //   1. left (visit)
    //   2. operator (JLeftPadded<J.Binary.Type> via visitLeftPadded)
    //   3. right (visit)
    //   4. type (null)
    // -----------------------------------------------------------------

    fn sendBinary(self: *Sender, bin: *const tree.Binary) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Binary",
        });

        // preVisit
        try self.preVisit(bin.id, bin.prefix);

        // 1. left
        try self.send(bin.left);
        // 2. operator (LeftPadded<enum value>)
        try self.sendLeftPaddedEnum(bin.operator.before, bin.operator.element);
        // 3. right
        try self.send(bin.right);
        // 4. type (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.FieldAccess
    // Matches JavaSender.visitFieldAccess:
    //   1. target (visit)
    //   2. name (JLeftPadded<J.Identifier> via visitLeftPadded)
    //   3. type (null)
    // -----------------------------------------------------------------

    fn sendFieldAccess(self: *Sender, fa: *const tree.FieldAccess) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$FieldAccess",
        });

        // preVisit
        try self.preVisit(fa.id, fa.prefix);

        // 1. target
        try self.send(fa.target);
        // 2. name (LeftPadded<Identifier>)
        //    LeftPadded protocol: before (Space), element (visit J), markers
        try self.sendLeftPaddedNode(fa.name.before, fa.name.element);
        // 3. type (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.MethodInvocation
    // Matches JavaSender.visitMethodInvocation:
    //   1. select (RightPadded, nullable)
    //   2. typeParameters (JContainer, nullable)
    //   3. name (visit)
    //   4. arguments (JContainer)
    //   5. methodType (null)
    // -----------------------------------------------------------------

    fn sendMethodInvocation(self: *Sender, mi: *const tree.MethodInvocation) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$MethodInvocation",
        });

        // preVisit
        try self.preVisit(mi.id, mi.prefix);

        // 1. select (RightPadded, nullable)
        if (mi.select) |sel| {
            try self.queue.put(.{
                .state = .add,
                .value_type = "org.openrewrite.java.tree.JRightPadded",
            });
            try self.send(sel.element);
            try self.sendSpace(sel.after);
            try self.sendEmptyMarkers();
        } else {
            try self.queue.sendDelete();
        }
        // 2. typeParameters (null JContainer)
        try self.queue.sendDelete();
        // 3. name
        try self.send(mi.name);
        // 4. arguments (JContainer)
        try self.sendContainer(mi.args_prefix, mi.args);
        // 5. methodType (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.Assignment
    // Matches JavaSender.visitAssignment:
    //   1. variable (visit)
    //   2. assignment (JLeftPadded<Expression>)
    //   3. type (null)
    // -----------------------------------------------------------------

    fn sendAssignment(self: *Sender, asgn: *const tree.Assignment) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Assignment",
        });

        // preVisit
        try self.preVisit(asgn.id, asgn.prefix);

        // 1. variable
        try self.send(asgn.variable);
        // 2. assignment (LeftPadded<Expression>)
        try self.sendLeftPaddedNode(asgn.assignment.before, asgn.assignment.element);
        // 3. type (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.If
    // Matches JavaSender.visitIf:
    //   1. ifCondition (ControlParentheses - visit)
    //   2. thenPart (RightPadded<Statement> via visitRightPadded)
    //   3. elsePart (If.Else - nullable, visit)
    //
    // J.ControlParentheses:
    //   preVisit: id, prefix, markers
    //   1. tree (RightPadded<Expression> via visitRightPadded)
    //
    // J.If.Else:
    //   preVisit: id, prefix, markers
    //   1. body (RightPadded<Statement> via visitRightPadded)
    // -----------------------------------------------------------------

    fn sendIf(self: *Sender, i: *const tree.If) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$If",
        });

        // preVisit
        try self.preVisit(i.id, i.prefix);

        // 1. ifCondition (ControlParentheses)
        //    ControlParentheses is a J node, so visit() is called on it.
        //    We need to emit: ADD typed, preVisit, then tree (RightPadded)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$ControlParentheses",
        });
        // ControlParentheses.preVisit: id, prefix (empty - the "(" space is already in If prefix), markers
        try self.preVisit(self.genUuid(), .{ .whitespace = "" });
        // ControlParentheses field: tree (RightPadded<Expression>)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        try self.send(i.condition);
        try self.sendSpace(i.condition_after);
        try self.sendEmptyMarkers();

        // 2. thenPart (RightPadded<Statement>)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        try self.send(i.then_part);
        try self.sendSpace(i.then_after);
        try self.sendEmptyMarkers();

        // 3. elsePart (nullable If.Else)
        if (i.else_part) |else_part| {
            try self.queue.put(.{
                .state = .add,
                .value_type = "org.openrewrite.java.tree.J$If$Else",
            });
            try self.preVisit(else_part.id, else_part.prefix);
            // Else field: body (RightPadded<Statement>)
            try self.queue.put(.{
                .state = .add,
                .value_type = "org.openrewrite.java.tree.JRightPadded",
            });
            try self.send(else_part.body);
            try self.sendSpace(else_part.body_after);
            try self.sendEmptyMarkers();
        } else {
            try self.queue.sendDelete();
        }
    }

    // -----------------------------------------------------------------
    // J.WhileLoop
    // Matches JavaSender.visitWhileLoop:
    //   1. condition (ControlParentheses - visit)
    //   2. body (RightPadded<Statement> via visitRightPadded)
    // -----------------------------------------------------------------

    fn sendWhileLoop(self: *Sender, wl: *const tree.WhileLoop) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$WhileLoop",
        });

        // preVisit
        try self.preVisit(wl.id, wl.prefix);

        // 1. condition (ControlParentheses)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$ControlParentheses",
        });
        try self.preVisit(self.genUuid(), .{ .whitespace = "" });
        // ControlParentheses field: tree (RightPadded<Expression>)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        try self.send(wl.condition);
        try self.sendSpace(wl.condition_after);
        try self.sendEmptyMarkers();

        // 2. body (RightPadded<Statement>)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        try self.send(wl.body);
        try self.sendSpace(wl.body_after);
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // J.Unary
    // Matches JavaSender.visitUnary:
    //   1. operator (JLeftPadded<J.Unary.Type> via visitLeftPadded)
    //   2. expression (visit)
    //   3. type (null JavaType)
    // -----------------------------------------------------------------

    fn sendUnary(self: *Sender, u: *const tree.Unary) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Unary",
        });

        // preVisit
        try self.preVisit(u.id, u.prefix);

        // 1. operator (LeftPadded<enum value>)
        try self.sendLeftPaddedEnum(u.operator.before, u.operator.element);
        // 2. expression
        try self.send(u.expression);
        // 3. type (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.Parentheses
    // Matches JavaSender.visitParentheses:
    //   1. tree (RightPadded via visitRightPadded)
    // -----------------------------------------------------------------

    fn sendParentheses(self: *Sender, p: *const tree.Parentheses) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Parentheses",
        });

        // preVisit
        try self.preVisit(p.id, p.prefix);

        // 1. tree (RightPadded<Expression>)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        try self.send(p.expression);
        try self.sendSpace(p.after);
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // J.AssignmentOperation
    // Matches JavaSender.visitAssignmentOperation:
    //   1. variable (visit)
    //   2. operator (JLeftPadded<J.AssignmentOperation.Type> via visitLeftPadded)
    //   3. assignment (visit)
    //   4. type (null JavaType)
    // -----------------------------------------------------------------

    fn sendAssignmentOp(self: *Sender, ao: *const tree.AssignmentOperation) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$AssignmentOperation",
        });

        // preVisit
        try self.preVisit(ao.id, ao.prefix);

        // 1. variable
        try self.send(ao.variable);
        // 2. operator (LeftPadded<enum value>)
        try self.sendLeftPaddedEnum(ao.operator.before, ao.operator.element);
        // 3. assignment
        try self.send(ao.assignment);
        // 4. type (null)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // J.ArrayAccess
    // Matches JavaSender.visitArrayAccess:
    //   1. indexed (visit)
    //   2. dimension (J.ArrayDimension - visit)
    //
    // J.ArrayDimension:
    //   preVisit: id, prefix, markers
    //   1. index (RightPadded<Expression> via visitRightPadded)
    // -----------------------------------------------------------------

    fn sendArrayAccess(self: *Sender, aa: *const tree.ArrayAccess) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$ArrayAccess",
        });

        // preVisit
        try self.preVisit(aa.id, aa.prefix);

        // 1. indexed (the target expression)
        try self.send(aa.indexed);

        // 2. dimension (J.ArrayDimension)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$ArrayDimension",
        });
        // ArrayDimension preVisit: id, prefix, markers
        try self.preVisit(self.genUuid(), aa.dimension_prefix);
        // ArrayDimension field: index (RightPadded<Expression>)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        try self.send(aa.index);
        try self.sendSpace(aa.index_after);
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // Zig.Slice
    // Matches ZigSender.visitSlice:
    //   1. target (visit)
    //   2. openBracket (Space)
    //   3. start (RightPadded via visitRightPadded)
    //   4. end (nullable visit)
    //   5. closeBracket (Space)
    // -----------------------------------------------------------------

    fn sendSlice(self: *Sender, s: *const tree.Slice) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.zig.tree.Zig$Slice",
        });

        // preVisit
        try self.preVisit(s.id, s.prefix);

        // 1. target
        try self.send(s.target);
        // 2. openBracket (Space)
        try self.sendSpace(s.open_bracket);
        // 3. start (RightPadded)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        try self.send(s.start.element);
        try self.sendSpace(s.start.after);
        try self.sendEmptyMarkers();
        // 4. end (nullable)
        if (s.end) |end| {
            try self.send(end);
        } else {
            try self.queue.sendDelete();
        }
        // 5. closeBracket (Space)
        try self.sendSpace(s.close_bracket);
    }

    // -----------------------------------------------------------------
    // Zig.ErrorUnion
    // Matches ZigSender.visitErrorUnion:
    //   1. errorType (nullable visit)
    //   2. valueType (JLeftPadded via visitLeftPadded)
    // -----------------------------------------------------------------

    fn sendErrorUnion(self: *Sender, eu: *const tree.ErrorUnion) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.zig.tree.Zig$ErrorUnion",
        });

        // preVisit
        try self.preVisit(eu.id, eu.prefix);

        // 1. errorType (nullable)
        if (eu.error_type) |et| {
            try self.send(et);
        } else {
            try self.queue.sendDelete();
        }
        // 2. valueType (LeftPadded)
        try self.sendLeftPaddedNode(eu.value_type.before, eu.value_type.element);
    }

    // -----------------------------------------------------------------
    // Zig.Switch → J.SwitchExpression
    // Matches JavaSender.visitSwitchExpression:
    //   1. selector (ControlParentheses - visit)
    //   2. cases (J.Block - visit)
    //   3. type (nullable JavaType)
    //
    // We use J.SwitchExpression (not J.Switch) because Zig's switch
    // is an expression, and J.Switch is a statement that doesn't
    // implement Expression (can't be used in return, assignment, etc.)
    // -----------------------------------------------------------------

    fn sendSwitch(self: *Sender, sw: *const tree.Switch) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$SwitchExpression",
        });

        // preVisit
        try self.preVisit(sw.id, sw.prefix);

        // 1. selector (ControlParentheses wrapping the condition)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$ControlParentheses",
        });
        try self.preVisit(self.genUuid(), sw.lparen_prefix);
        // ControlParentheses field: tree (RightPadded<Expression>)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        try self.send(sw.condition);
        try self.sendSpace(sw.condition_after);
        try self.sendEmptyMarkers();

        // 2. cases (J.Block containing SwitchProng nodes)
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Block",
        });
        try self.preVisit(self.genUuid(), sw.lbrace_prefix);

        // Block static (RightPadded<Boolean>) - always false
        try self.sendRightPaddedBool(false, .{ .whitespace = "" });

        // Block statements (list of RightPadded<Statement> = prongs)
        try self.sendRightPaddedList(sw.prongs);

        // Block end (Space before "}")
        try self.sendSpace(sw.end);

        // 3. type (null JavaType)
        try self.queue.sendDelete();
    }

    // -----------------------------------------------------------------
    // Zig.SwitchProng
    // Matches ZigSender.visitSwitchProng:
    //   1. cases (JContainer of Expression)
    //   2. payload (nullable, visit)
    //   3. arrow (JLeftPadded<Expression>)
    // -----------------------------------------------------------------

    fn sendSwitchProng(self: *Sender, sp: *const tree.SwitchProng) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.zig.tree.Zig$SwitchProng",
        });

        // preVisit
        try self.preVisit(sp.id, sp.prefix);

        // 1. cases (JContainer of Expression)
        try self.sendContainer(sp.cases_prefix, sp.cases);

        // 2. payload (nullable, visit)
        if (sp.payload) |pl| {
            try self.send(pl);
        } else {
            try self.queue.sendDelete();
        }

        // 3. arrow (JLeftPadded<Expression>)
        try self.sendLeftPaddedNode(sp.arrow.before, sp.arrow.element);
    }

    // -----------------------------------------------------------------
    // Zig.ForLoop
    // Zig for-loops don't map cleanly to J.ForEachLoop because the
    // Control type expects a J.VariableDeclarations and iterable, while
    // Zig uses a different syntax (multiple inputs, payload captures).
    // We send it as J.Unknown for RPC (the Java side only gets a
    // skeleton), but preserve it as Zig.ForLoop on the Zig side for
    // faithful printing. The Zig printer handles the actual output.
    // -----------------------------------------------------------------

    fn sendForLoop(self: *Sender, fl: *const tree.ForLoop) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Unknown",
        });

        // preVisit: the Java side creates a J.Unknown with null source.
        // The Zig printer handles printing via the ForLoop struct.
        try self.preVisit(fl.id, fl.prefix);
    }

    // -----------------------------------------------------------------
    // Zig.Defer
    // Matches ZigSender.visitDefer:
    //   1. isErrdefer (boolean)
    //   2. payload (nullable, visit)
    //   3. expression (visit)
    // -----------------------------------------------------------------

    fn sendDefer(self: *Sender, d: *const tree.Defer) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.zig.tree.Zig$Defer",
        });

        // preVisit
        try self.preVisit(d.id, d.prefix);

        // 1. isErrdefer
        try self.queue.sendAdd(null, std.json.Value{ .bool = d.is_errdefer });
        // 2. payload (nullable)
        if (d.payload) |pl| {
            try self.send(pl);
        } else {
            try self.queue.sendDelete();
        }
        // 3. expression
        try self.send(d.expression);
    }

    // -----------------------------------------------------------------
    // Zig.Comptime
    // Matches ZigSender.visitComptime:
    //   1. expression (visit)
    // -----------------------------------------------------------------

    fn sendComptime(self: *Sender, ct: *const tree.Comptime) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.zig.tree.Zig$Comptime",
        });

        // preVisit
        try self.preVisit(ct.id, ct.prefix);

        // 1. expression
        try self.send(ct.expression);
    }

    // -----------------------------------------------------------------
    // Zig.TestDecl
    // Matches ZigSender.visitTestDecl:
    //   1. name (nullable, visit)
    //   2. body (visit)
    // -----------------------------------------------------------------

    fn sendTestDecl(self: *Sender, td: *const tree.TestDecl) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.zig.tree.Zig$TestDecl",
        });

        // preVisit
        try self.preVisit(td.id, td.prefix);

        // 1. name (nullable)
        if (td.name) |n| {
            try self.send(n);
        } else {
            try self.queue.sendDelete();
        }
        // 2. body
        try self.send(td.body);
    }

    // -----------------------------------------------------------------
    // J.Unknown
    // The default visitor traversal for Unknown visits:
    //   preVisit: id, prefix, markers
    //   Then visits the source child (Unknown.Source)
    // -----------------------------------------------------------------

    fn sendUnknown(self: *Sender, unk: *const tree.Unknown) SendError!void {
        // Top-level ADD
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Unknown",
        });

        // preVisit
        try self.preVisit(unk.id, unk.prefix);

        // The source child is visited via the default visitor traversal (visitAndCast),
        // which calls sendUnknownSource for the Unknown.Source child node.
    }

    // -----------------------------------------------------------------
    // J.Unknown.Source
    // Send id/prefix/markers via preVisit, then send the text field
    // so visitUnknownSource on the Java side can receive it.
    // -----------------------------------------------------------------

    fn sendUnknownSource(self: *Sender, src: *const tree.UnknownSource) SendError!void {
        try self.preVisit(src.id, src.prefix);
        // Send the text field to match ZigSenderDelegate.visitUnknownSource
        try self.queue.sendAdd(null, std.json.Value{ .string = src.text });
    }

    // -----------------------------------------------------------------
    // J.Empty
    // JavaSender.visitEmpty returns empty with no fields beyond preVisit.
    // -----------------------------------------------------------------

    fn sendEmpty(self: *Sender, emp: *const tree.Empty) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.J$Empty",
        });
        try self.preVisit(emp.id, emp.prefix);
    }

    // -----------------------------------------------------------------
    // Helper: send a Space
    // Matches JavaSender.visitSpace:
    //   1. comments (list - getAndSendList)
    //   2. whitespace (string - getAndSend)
    // -----------------------------------------------------------------

    fn sendSpace(self: *Sender, space: Space) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.Space",
        });
        // 1. comments (empty list)
        try self.sendEmptyList();
        // 2. whitespace
        try self.queue.sendAdd(null, std.json.Value{ .string = space.whitespace });
    }

    // -----------------------------------------------------------------
    // Helper: send empty Markers
    // Matches Markers.rpcSend:
    //   1. id (UUID)
    //   2. markers (list as ref - empty)
    // -----------------------------------------------------------------

    fn sendEmptyMarkers(self: *Sender) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.marker.Markers",
        });
        // Markers.id
        try self.queue.sendAdd(null, std.json.Value{ .string = self.genUuid() });
        // Markers.markers (empty list)
        try self.sendEmptyList();
    }

    // -----------------------------------------------------------------
    // Helper: send an empty list
    // Protocol: ADD + CHANGE with positions=[]
    // -----------------------------------------------------------------

    fn sendEmptyList(self: *Sender) SendError!void {
        try self.queue.put(.{ .state = .add });
        try self.queue.put(.{
            .state = .change,
            .value = std.json.Value{ .array = std.json.Array.init(self.allocator) },
        });
    }

    // -----------------------------------------------------------------
    // Helper: send a list of RightPadded<LstNode>
    // Protocol:
    //   ADD (list exists)
    //   CHANGE with positions array
    //   For each element:
    //     visit(element)
    //     sendSpace(after)
    //     sendEmptyMarkers()
    // -----------------------------------------------------------------

    fn sendRightPaddedList(self: *Sender, items: []const RightPadded(LstNode)) SendError!void {
        // List header: ADD + CHANGE with positions
        try self.queue.put(.{ .state = .add });

        var positions = std.json.Array.init(self.allocator);
        for (0..items.len) |_| {
            // -1 = ADDED_LIST_ITEM (all items are new)
            try positions.append(std.json.Value{ .integer = -1 });
        }
        try self.queue.put(.{
            .state = .change,
            .value = std.json.Value{ .array = positions },
        });

        // Each RightPadded element needs its own ADD wrapper with valueType
        for (items) |item| {
            // ADD for the RightPadded wrapper
            try self.queue.put(.{
                .state = .add,
                .value_type = "org.openrewrite.java.tree.JRightPadded",
            });
            // visitRightPadded protocol:
            //   1. element (J node via visit)
            //   2. after (Space)
            //   3. markers (Markers)
            try self.send(item.element);
            try self.sendSpace(item.after);
            try self.sendEmptyMarkers();
        }
    }

    // -----------------------------------------------------------------
    // Helper: send a RightPadded<Boolean>
    // Protocol:
    //   element (boolean - ADD with value)
    //   after (Space)
    //   markers (Markers)
    // -----------------------------------------------------------------

    fn sendRightPaddedBool(self: *Sender, val: bool, after: Space) SendError!void {
        // The RightPadded wrapper itself
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JRightPadded",
        });
        // visitRightPadded protocol for non-J element:
        //   element (boolean - ADD with value, no valueType since java.lang)
        try self.queue.sendAdd(null, std.json.Value{ .bool = val });
        // after (Space)
        try self.sendSpace(after);
        // markers (Markers)
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // Helper: send an empty JContainer with a given before-space
    // Protocol (matches JavaSender.visitContainer):
    //   before (Space)
    //   elements (list of RightPadded - empty)
    //   markers (Markers)
    // -----------------------------------------------------------------

    fn sendEmptyContainer(self: *Sender, before: Space) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JContainer",
        });
        // before
        try self.sendSpace(before);
        // elements (empty list)
        try self.sendEmptyList();
        // markers
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // Helper: send a LeftPadded<enum value> (e.g., Binary operator)
    // Protocol:
    //   ADD with valueType JLeftPadded (the wrapper)
    //   Then visitLeftPadded content:
    //     before (Space)
    //     element (plain value via ADD)
    //     markers (Markers)
    // -----------------------------------------------------------------

    fn sendLeftPaddedEnum(self: *Sender, before: Space, val: []const u8) SendError!void {
        // ADD wrapper for JLeftPadded
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JLeftPadded",
        });
        // before
        try self.sendSpace(before);
        // element (enum value as string)
        try self.queue.sendAdd(null, std.json.Value{ .string = val });
        // markers
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // Helper: send a LeftPadded<J node> (e.g., FieldAccess name)
    // Protocol:
    //   ADD with valueType JLeftPadded (the wrapper)
    //   Then visitLeftPadded content:
    //     before (Space)
    //     element (visit J)
    //     markers (Markers)
    // -----------------------------------------------------------------

    fn sendLeftPaddedNode(self: *Sender, before: Space, element: LstNode) SendError!void {
        // ADD wrapper for JLeftPadded
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JLeftPadded",
        });
        // before
        try self.sendSpace(before);
        // element (J node via visit)
        try self.send(element);
        // markers
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // Helper: send a JContainer with elements
    // Protocol (matches JavaSender.visitContainer):
    //   before (Space)
    //   elements (list of RightPadded)
    //   markers (Markers)
    // -----------------------------------------------------------------

    fn sendContainer(self: *Sender, before: Space, items: []const RightPadded(LstNode)) SendError!void {
        try self.queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.JContainer",
        });
        // before
        try self.sendSpace(before);
        // elements (list of RightPadded)
        try self.sendRightPaddedList(items);
        // markers
        try self.sendEmptyMarkers();
    }

    // -----------------------------------------------------------------
    // UUID generation
    // -----------------------------------------------------------------

    fn genUuid(self: *Sender) []const u8 {
        var bytes: [16]u8 = undefined;
        std.crypto.random.bytes(&bytes);
        bytes[6] = (bytes[6] & 0x0f) | 0x40;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;

        var result: [36]u8 = undefined;
        const hex = "0123456789abcdef";
        var pos: usize = 0;
        for (bytes, 0..) |b, i| {
            if (i == 4 or i == 6 or i == 8 or i == 10) {
                result[pos] = '-';
                pos += 1;
            }
            result[pos] = hex[b >> 4];
            result[pos + 1] = hex[b & 0x0f];
            pos += 2;
        }
        return self.allocator.dupe(u8, &result) catch "00000000-0000-0000-0000-000000000000";
    }
};

test "Sender basic compilation unit" {
    // Use arena allocator to avoid leaking UUIDs generated during send
    var arena = std.heap.ArenaAllocator.init(std.testing.allocator);
    defer arena.deinit();
    const allocator = arena.allocator();

    var queue = SendQueue.init(allocator, 1000);
    defer queue.deinit();

    var cu = tree.CompilationUnit{
        .id = "test-cu-id",
        .prefix = .{ .whitespace = "" },
        .source_path = "test.zig",
        .statements = &.{},
        .eof = .{ .whitespace = "\n" },
    };
    const node = LstNode{ .compilation_unit = &cu };

    var sender = Sender.init(&queue, allocator);
    try sender.send(node);

    // Verify we got some data in the queue
    try std.testing.expect(queue.batch.items.len > 0);

    // First item should be the ADD with valueType for CompilationUnit
    const first = queue.batch.items[0];
    try std.testing.expectEqual(rpc.State.add, first.state);
    try std.testing.expectEqualStrings(
        "org.openrewrite.zig.tree.Zig$CompilationUnit",
        first.value_type.?,
    );
}
