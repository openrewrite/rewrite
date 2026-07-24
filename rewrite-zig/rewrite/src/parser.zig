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

//! Recursive descent parser that walks std.zig.Ast and maps nodes to
//! OpenRewrite LST types (tree.zig). Uses a cursor-based context to
//! extract whitespace between tokens.

const std = @import("std");
const tree = @import("tree.zig");
const LstNode = tree.LstNode;
const Space = tree.Space;
const RightPadded = tree.RightPadded;
const LeftPadded = tree.LeftPadded;
const Ast = std.zig.Ast;
const Token = std.zig.Token;
const Node = Ast.Node;

/// ParseContext holds the state for walking through the Zig AST
/// and extracting whitespace/prefix information.
pub const ParseContext = struct {
    ast: Ast,
    source: []const u8,
    /// Sentinel-terminated view of source for re-tokenization.
    source_z: [:0]const u8,
    cursor: usize,
    allocator: std.mem.Allocator,

    pub fn init(allocator: std.mem.Allocator, source: []const u8, source_z: [:0]const u8, ast: Ast) ParseContext {
        return .{
            .ast = ast,
            .source = source,
            .source_z = source_z,
            .cursor = 0,
            .allocator = allocator,
        };
    }

    // -----------------------------------------------------------------
    // Cursor / whitespace helpers
    // -----------------------------------------------------------------

    /// Extract whitespace from cursor up to the start of the given token,
    /// then advance cursor past the token.
    pub fn prefix(self: *ParseContext, token_index: Ast.TokenIndex) Space {
        const tok_start = self.ast.tokenStart(token_index);
        const ws = self.extractWhitespace(tok_start);
        // Advance cursor past this token
        const tok_len = self.tokenLength(token_index);
        self.cursor = tok_start + tok_len;
        return .{ .whitespace = ws };
    }

    /// Extract whitespace from cursor to a given position without
    /// advancing past any token.
    fn extractWhitespace(self: *ParseContext, target: usize) []const u8 {
        if (target <= self.cursor) return "";
        return self.source[self.cursor..target];
    }

    /// Get the text of a token.
    pub fn tokenSlice(self: *ParseContext, token_index: Ast.TokenIndex) []const u8 {
        return self.ast.tokenSlice(token_index);
    }

    /// Advance cursor past a token without extracting whitespace prefix.
    pub fn skipToken(self: *ParseContext, token_index: Ast.TokenIndex) void {
        const tok_start = self.ast.tokenStart(token_index);
        const tok_len = self.tokenLength(token_index);
        self.cursor = tok_start + tok_len;
    }

    /// Compute the length of a token. For tokens with known lexemes (keywords,
    /// operators), use the lexeme length. For identifiers, strings, numbers etc.,
    /// re-tokenize to find the end.
    fn tokenLength(self: *ParseContext, token_index: Ast.TokenIndex) usize {
        const tag = self.ast.tokenTag(token_index);
        // If the tag has a known lexeme, use its length directly
        if (tag.lexeme()) |lex| {
            return lex.len;
        }
        // Otherwise re-tokenize from the token start to find the end
        var tokenizer: std.zig.Tokenizer = .{
            .buffer = self.source_z,
            .index = self.ast.tokenStart(token_index),
        };
        const tok = tokenizer.next();
        return tok.loc.end - tok.loc.start;
    }

    /// Generate a v4 UUID string, allocated.
    fn genUuid(self: *ParseContext) []const u8 {
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

    // -----------------------------------------------------------------
    // Top-level: map a file
    // -----------------------------------------------------------------

    /// Parse the full file into a CompilationUnit LstNode.
    /// The `cu_id` parameter sets the CompilationUnit's UUID (must match the stored object key).
    pub fn mapFile(self: *ParseContext, source_path: []const u8, cu_id: []const u8) ParseError!LstNode {
        const root_decls = self.ast.rootDecls();

        // Extract file-level prefix (whitespace before first token)
        const file_prefix = self.filePrefix();

        // Pre-allocate the statements array based on root decl count
        var stmts = try self.allocator.alloc(RightPadded(LstNode), root_decls.len);
        var stmt_count: usize = 0;

        for (root_decls) |decl_node| {
            const mapped = try self.mapNode(decl_node);
            // After mapping the node, extract trailing whitespace until next decl or EOF
            const after = self.trailingWhitespace(decl_node);
            stmts[stmt_count] = .{
                .element = mapped,
                .after = .{ .whitespace = after },
            };
            stmt_count += 1;
        }
        stmts = stmts[0..stmt_count];

        // EOF space: everything from cursor to end
        const eof_ws = if (self.cursor < self.source.len)
            self.source[self.cursor..]
        else
            "";

        const cu = try self.allocator.create(tree.CompilationUnit);
        cu.* = .{
            .id = cu_id,
            .prefix = file_prefix,
            .source_path = source_path,
            .statements = stmts,
            .eof = .{ .whitespace = eof_ws },
        };
        return LstNode{ .compilation_unit = cu };
    }

    /// Extract leading whitespace/comments before the first real token.
    fn filePrefix(self: *ParseContext) Space {
        const root_decls = self.ast.rootDecls();
        if (root_decls.len == 0) {
            // Empty file: all source is prefix
            const ws = self.source;
            self.cursor = self.source.len;
            return .{ .whitespace = ws };
        }
        // Get the first token of the first declaration
        const first_token = self.ast.firstToken(root_decls[0]);
        const first_start = self.ast.tokenStart(first_token);
        const ws = self.source[0..first_start];
        self.cursor = first_start;
        return .{ .whitespace = ws };
    }

    /// Extract whitespace between end of a declaration and start of the next
    /// token (the semicolon after a statement, or just trailing whitespace).
    fn trailingWhitespace(self: *ParseContext, node: Node.Index) []const u8 {
        // Find the last token of this node and advance past it
        const last_tok = self.ast.lastToken(node);
        const last_start = self.ast.tokenStart(last_tok);
        const last_len = self.tokenLength(last_tok);
        const end_pos = last_start + last_len;

        // Check if there's a semicolon immediately after
        const next_tok = last_tok + 1;
        if (next_tok < self.ast.tokens.len) {
            const next_tag = self.ast.tokenTag(next_tok);
            if (next_tag == .semicolon) {
                // Skip the semicolon
                const semi_start = self.ast.tokenStart(next_tok);
                const semi_end = semi_start + 1;
                // The trailing includes everything from current cursor to after the semicolon
                if (self.cursor < semi_end) {
                    const ws = self.source[self.cursor..semi_end];
                    self.cursor = semi_end;
                    return ws;
                }
            }
        }

        // Just advance cursor to end of node
        if (self.cursor < end_pos) {
            const ws = self.source[self.cursor..end_pos];
            self.cursor = end_pos;
            return ws;
        }
        return "";
    }

    // -----------------------------------------------------------------
    // Node dispatch
    // -----------------------------------------------------------------

    pub const ParseError = std.mem.Allocator.Error;

    /// Map a node index to an LstNode, dispatching based on node tag.
    /// Dispatches to specific mappers for recognized node types,
    /// falling back to mapUnknown for unrecognized tags.
    pub fn mapNode(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const tag = self.ast.nodeTag(node);
        return switch (tag) {
            // Phase A: Literals and identifiers
            .identifier, .unreachable_literal, .anyframe_literal,
            => self.mapIdentifierNode(node),
            .enum_literal => self.mapEnumLiteral(node),
            .number_literal, .char_literal => self.mapNumberLiteralNode(node),
            .string_literal, .multiline_string_literal => self.mapStringLiteralNode(node),

            // Phase B: Blocks and return
            .block, .block_semicolon, .block_two, .block_two_semicolon => self.mapBlock(node),
            .@"return" => self.mapReturn(node),

            // Phase C: Declarations
            .fn_decl => self.mapFnDecl(node),
            .simple_var_decl, .global_var_decl, .local_var_decl, .aligned_var_decl,
            => self.mapVarDecl(node),

            // Phase D: Binary operations
            .add, .sub, .mul, .div, .mod,
            .add_wrap, .sub_wrap, .mul_wrap,
            .add_sat, .sub_sat, .mul_sat,
            .equal_equal, .bang_equal,
            .less_than, .greater_than, .less_or_equal, .greater_or_equal,
            .bit_and, .bit_or, .bit_xor,
            .shl, .shr, .shl_sat,
            .bool_and, .bool_or,
            .array_cat, .array_mult,
            .merge_error_sets,
            .@"catch", .@"orelse",
            => self.mapBinary(node),

            // Phase D: Field access
            .field_access => self.mapFieldAccess(node),

            // Phase D: Function calls
            .call_one, .call_one_comma, .call, .call_comma,
            => self.mapCall(node),

            // Phase D: Assignments
            .assign => self.mapAssign(node),
            .assign_add, .assign_sub, .assign_mul, .assign_div, .assign_mod,
            .assign_add_wrap, .assign_sub_wrap, .assign_mul_wrap,
            .assign_add_sat, .assign_sub_sat, .assign_mul_sat,
            .assign_shl, .assign_shl_sat, .assign_shr,
            .assign_bit_and, .assign_bit_xor, .assign_bit_or,
            => self.mapAssignOp(node),

            // Phase D: Builtin calls
            .builtin_call_two, .builtin_call_two_comma, .builtin_call, .builtin_call_comma,
            => self.mapBuiltinCall(node),

            // Phase E: Control flow
            .if_simple, .@"if" => self.mapIf(node),
            .while_simple, .while_cont, .@"while" => self.mapWhile(node),

            // Phase E: Unary operations
            .negation => self.mapUnary(node, "Negative", "-"),
            .bool_not => self.mapUnary(node, "Not", "!"),
            .bit_not => self.mapUnary(node, "Complement", "~"),
            .address_of => self.mapUnary(node, "AddressOf", "&"),
            .negation_wrap => self.mapUnary(node, "Negative", "-%"),
            .@"try" => self.mapUnaryKeyword(node, "try"),
            .deref => self.mapPostfixUnary(node),
            .unwrap_optional => self.mapPostfixDotOp(node),

            // Phase E: Parenthesized expressions and array access
            .grouped_expression => self.mapGrouped(node),
            .array_access => self.mapArrayAccess(node),

            // Phase E: Error union types
            .error_union => self.mapErrorUnion(node),

            // Phase E: Slice expressions
            .slice_open, .slice, .slice_sentinel => self.mapSlice(node),

            // Phase F: Switch expressions
            .@"switch", .switch_comma => self.mapSwitch(node),

            // Phase F: For loops - use ForLoop for Zig-side round-trip,
            // but fall through to J.Unknown for RPC since J.ForEachLoop's Control
            // structure doesn't map cleanly to Zig's for syntax.
            .for_simple, .@"for" => self.mapForLoop(node),

            // Phase F: Zig-specific constructs
            .@"defer" => self.mapDefer(node, false),
            .@"errdefer" => self.mapDefer(node, true),
            .test_decl => self.mapTestDecl(node),
            .@"comptime" => self.mapComptime(node),

            // Everything else falls back to Unknown
            else => self.mapUnknown(node),
        };
    }

    // -----------------------------------------------------------------
    // Function declaration → J.MethodDeclaration
    // -----------------------------------------------------------------

    fn mapFnDecl(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        // fn_decl: data.node_and_node[0] = proto node, [1] = body node
        const data = self.ast.nodeData(node);
        const body_node = data.node_and_node[1];

        // Get the full fn proto
        var proto_buf: [1]Node.Index = undefined;
        const fn_proto = self.ast.fullFnProto(&proto_buf, node) orelse
            return self.mapUnknown(node);

        // The fn keyword prefix
        const fn_first_token = fn_proto.firstToken();
        const fn_prefix = self.prefix(fn_first_token);

        // Capture keyword text (e.g. "fn", "pub fn", "export fn")
        // from the start of the first token through the end of the fn token.
        const fn_token = fn_proto.ast.fn_token;
        const first_tok_start = self.ast.tokenStart(fn_first_token);
        const fn_tok_end = self.ast.tokenStart(fn_token) + self.tokenLength(fn_token);
        const keywords = self.source[first_tok_start..fn_tok_end];

        // Skip tokens between fn keyword and name
        // fn keyword is fn_proto.ast.fn_token
        if (fn_first_token != fn_token) {
            // There are modifier tokens before fn (pub, export, etc.)
            // We already consumed the first token in prefix, now skip to fn
            self.skipToken(fn_token);
        }

        // Function name
        const name_node = if (fn_proto.name_token) |name_tok|
            try self.mapIdentifier(name_tok)
        else
            try self.makeEmptyIdentifier();

        // Capture the parameter list text including parentheses.
        // params_prefix is the whitespace before "("; params_text includes "(" through ")".
        const lparen_tok = fn_proto.lparen;
        const lparen_start = self.ast.tokenStart(lparen_tok);
        const params_ws = self.extractWhitespace(lparen_start);

        // Find the rparen by scanning forward from lparen
        const rparen_tok = self.findRparen(lparen_tok);
        const rparen_end = self.ast.tokenStart(rparen_tok) + self.tokenLength(rparen_tok);

        // params_text is the full source from "(" to ")" inclusive
        const params_text = self.source[lparen_start..rparen_end];

        // Advance cursor past the rparen
        self.cursor = rparen_end;

        // Return type (cursor is now after rparen, so prefix captures space before return type)
        const return_type: ?LstNode = if (fn_proto.ast.return_type != .none) blk: {
            const ret_node = fn_proto.ast.return_type.unwrap().?;
            break :blk try self.mapNode(ret_node);
        } else null;

        // Body
        const body: ?LstNode = try self.mapNode(body_node);

        const md = try self.allocator.create(tree.MethodDeclaration);
        md.* = .{
            .id = self.genUuid(),
            .prefix = fn_prefix,
            .keywords = keywords,
            .return_type = return_type,
            .name = name_node,
            .params_prefix = .{ .whitespace = params_ws },
            .params_text = params_text,
            .body = body,
        };
        return LstNode{ .method_declaration = md };
    }

    /// Find the matching right paren token by scanning forward from lparen.
    fn findRparen(self: *ParseContext, lparen_tok: Ast.TokenIndex) Ast.TokenIndex {
        var depth: usize = 1;
        var tok = lparen_tok + 1;
        while (tok < self.ast.tokens.len) : (tok += 1) {
            const tag = self.ast.tokenTag(tok);
            if (tag == .l_paren) {
                depth += 1;
            } else if (tag == .r_paren) {
                depth -= 1;
                if (depth == 0) return tok;
            }
        }
        // Fallback: return the token after lparen (shouldn't reach here for valid code)
        return lparen_tok + 1;
    }

    fn makeEmptyIdentifier(self: *ParseContext) ParseError!LstNode {
        const ident = try self.allocator.create(tree.Identifier);
        ident.* = .{
            .id = self.genUuid(),
            .prefix = .{ .whitespace = "" },
            .simple_name = "",
        };
        return LstNode{ .identifier = ident };
    }

    // -----------------------------------------------------------------
    // Variable declaration → J.VariableDeclarations
    // -----------------------------------------------------------------

    fn mapVarDecl(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const var_decl = self.ast.fullVarDecl(node) orelse
            return self.mapUnknown(node);

        // The first token (could be pub, export, const, var)
        const first_token = var_decl.firstToken();
        const decl_prefix = self.prefix(first_token);

        // Capture keyword text (e.g. "const", "var", "pub const", "pub var")
        // from the start of the first token through the end of the mut_token.
        const first_tok_start = self.ast.tokenStart(first_token);
        const mut_tok_end = self.ast.tokenStart(var_decl.ast.mut_token) + self.tokenLength(var_decl.ast.mut_token);
        const keyword = self.source[first_tok_start..mut_tok_end];

        // Skip to the mut_token (const/var) if it's not the first token
        if (first_token != var_decl.ast.mut_token) {
            self.skipToken(var_decl.ast.mut_token);
        }

        // The identifier name is the token after the mut_token
        const name_tok = var_decl.ast.mut_token + 1;
        const name_node = try self.mapIdentifier(name_tok);

        // Type expression (after the colon if present)
        var type_expr: ?LstNode = null;
        if (var_decl.ast.type_node != .none) {
            const type_nd = var_decl.ast.type_node.unwrap().?;
            type_expr = try self.mapNode(type_nd);
        }

        // Initializer (after the = sign)
        var init_lp: ?LeftPadded(LstNode) = null;
        if (var_decl.ast.init_node != .none) {
            const init_nd = var_decl.ast.init_node.unwrap().?;
            // Find the = token: it should be somewhere before the init node
            const init_first_tok = self.ast.firstToken(init_nd);
            // The = token is typically right before the init expression
            // We need to find it by scanning backward from init_first_tok
            var eq_tok = init_first_tok;
            if (eq_tok > 0) {
                eq_tok -= 1;
                while (eq_tok > 0 and self.ast.tokenTag(eq_tok) != .equal) {
                    eq_tok -= 1;
                }
            }
            const eq_space = self.prefix(eq_tok);
            const init_node = try self.mapNode(init_nd);
            init_lp = .{
                .before = eq_space,
                .element = init_node,
            };
        }

        // Build the NamedVariable
        const named_var = try self.allocator.create(tree.NamedVariable);
        named_var.* = .{
            .id = self.genUuid(),
            .prefix = .{ .whitespace = "" },
            .name = name_node,
            .initializer = init_lp,
        };

        // For the NamedVariable, we use J.VariableDeclarations.NamedVariable
        // which is serialized as a visit to the "variable" visitor
        // We wrap it in a RightPadded list
        var vars = try self.allocator.alloc(RightPadded(LstNode), 1);
        // We'll use unknown_source temporarily to carry the named variable data.
        // Actually, NamedVariable is not in our LstNode union yet.
        // We need to represent it. For now we'll use the identifier as the variable.
        // Actually, let's handle this properly by extending LstNode.

        // The NamedVariable's declarator is the name identifier
        // For now, we synthesize the NamedVariable by storing it inline
        // Since NamedVariable is visited separately in the sender, we need it in the tree.
        // We'll store the name identifier directly as the variable element
        // and handle the initializer at the sender level by inspecting the tree.

        // Actually, let me reconsider. The J.VariableDeclarations sends:
        //   1. leadingAnnotations (empty list)
        //   2. modifiers (empty list)
        //   3. typeExpression (the type)
        //   4. varargs (null space)
        //   5. variables (list of RightPadded<NamedVariable>)
        //
        // And J.VariableDeclarations.NamedVariable sends:
        //   1. declarator (the name identifier, which is actually an IdentWithAnnotations)
        //   2. dimensionsAfterName (empty list)
        //   3. initializer (JLeftPadded<Expression>)
        //   4. variableType (null)
        //
        // We need to represent this. Let's just use the name directly.

        // We encode: the name_node goes into the variable entry
        // The sender will need to emit NamedVariable format.
        // For simplicity in tree.zig, let's just put the ident as element.
        // But we need the initializer too.

        // Hmm, let me add NamedVariable to LstNode. Actually looking at the tree.zig,
        // I didn't include it. Let me just store it in Unknown format for now and
        // use a "named_variable" tag. But that would violate the principle.

        // Let's use a practical approach: store a custom wrapper.
        // Actually the cleanest approach is: we store name_node as element,
        // and the sender looks up additional data from the VariableDeclarations parent.

        // For now, wrap the whole thing in an Unknown if we can't do NamedVariable.
        // Actually, we CAN handle it: we don't need NamedVariable in LstNode because
        // the sender serializes the whole VariableDeclarations including its variables list.
        // Each variable is serialized inline by the sender as it walks the tree.

        // Let's store the relevant data in the VariableDeclarations node directly.
        // The variables field already stores the identity. For the initializer,
        // we can store it in the VariableDeclarations struct.

        // OK final approach: use NamedVariable stored in a separate list, not as LstNode.
        // The sender knows how to handle VariableDeclarations by reading its fields directly.

        // Keep the vars as a list but use the identifier as the element placeholder.
        // The sender will use the VariableDeclarations.type_expression and .variables
        // to reconstruct the Java-side format.

        // For the named variable we want:
        //   - preVisit: id, prefix, markers
        //   - declarator (the name identifier)
        //   - dimensionsAfterName: empty list
        //   - initializer: JLeftPadded (if present)
        //   - variableType: null

        // So we need a way to carry the init info. Let's encode it in the RightPadded
        // using the `identifier` as element. The sender will walk the VariableDeclarations
        // and for each variable, emit a full NamedVariable.

        // Actually this is getting complicated. Let me just store it inline.
        // The named_variable isn't a separate LST node in the node union.
        // Instead, the sender for VariableDeclarations will handle each variable
        // element by emitting the NamedVariable protocol directly.

        // We need to store: name identifier + optional initializer.
        // Let's use the identifier as the element in the RightPadded list.

        vars[0] = .{
            .element = name_node,
            .after = .{ .whitespace = "" },
        };

        const vd = try self.allocator.create(tree.VariableDeclarations);
        vd.* = .{
            .id = self.genUuid(),
            .prefix = decl_prefix,
            .keyword = keyword,
            .type_expression = type_expr,
            .variables = vars,
            .initializer = init_lp,
        };
        return LstNode{ .variable_declarations = vd };
    }

    // -----------------------------------------------------------------
    // Block → J.Block
    // -----------------------------------------------------------------

    fn mapBlock(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        // main_token is '{', extract its prefix
        const block_prefix = self.prefix(main_token);

        // Get block statements
        var buf: [2]Node.Index = undefined;
        const stmt_nodes = self.ast.blockStatements(&buf, node) orelse &[_]Node.Index{};

        var stmts = try self.allocator.alloc(RightPadded(LstNode), stmt_nodes.len);
        var stmt_count: usize = 0;
        for (stmt_nodes) |stmt_node| {
            const mapped = try self.mapNode(stmt_node);
            // Trailing whitespace after each statement (includes semicolon)
            const after = self.trailingWhitespace(stmt_node);
            stmts[stmt_count] = .{
                .element = mapped,
                .after = .{ .whitespace = after },
            };
            stmt_count += 1;
        }
        stmts = stmts[0..stmt_count];

        // Find the closing brace
        const last_tok = self.ast.lastToken(node);
        // The last token should be '}', extract whitespace before it
        const rbrace_start = self.ast.tokenStart(last_tok);
        const end_ws = if (rbrace_start > self.cursor)
            self.source[self.cursor..rbrace_start]
        else
            "";
        // Advance past the closing brace
        self.skipToken(last_tok);

        const blk = try self.allocator.create(tree.Block);
        blk.* = .{
            .id = self.genUuid(),
            .prefix = block_prefix,
            .statements = stmts,
            .end = .{ .whitespace = end_ws },
        };
        return LstNode{ .block = blk };
    }

    // -----------------------------------------------------------------
    // Return → J.Return
    // -----------------------------------------------------------------

    fn mapReturn(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const ret_prefix = self.prefix(main_token);

        // The return expression is in data.opt_node
        const data = self.ast.nodeData(node);
        const expr: ?LstNode = if (data.opt_node.unwrap()) |expr_node| blk: {
            break :blk try self.mapNode(expr_node);
        } else null;

        const ret = try self.allocator.create(tree.Return);
        ret.* = .{
            .id = self.genUuid(),
            .prefix = ret_prefix,
            .expression = expr,
        };
        return LstNode{ .@"return" = ret };
    }

    // -----------------------------------------------------------------
    // Binary expression → J.Binary
    // All binary ops have data.node_and_node: [0]=lhs, [1]=rhs
    // The main_token is the operator token.
    // -----------------------------------------------------------------

    fn mapBinary(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const data = self.ast.nodeData(node);
        const lhs_node = data.node_and_node[0];
        const rhs_node = data.node_and_node[1];
        const main_token = self.ast.nodeMainToken(node);

        // Map left operand (this consumes cursor up to operator)
        const left = try self.mapNode(lhs_node);

        // Capture the actual operator source text before prefix() consumes the token
        const op_source = self.ast.tokenSlice(main_token);

        // The operator token prefix (whitespace before the operator)
        const op_prefix = self.prefix(main_token);

        // Map the operator to J.Binary.Type enum name
        const tag = self.ast.nodeTag(node);
        const op_name = zigBinaryOpToJava(tag);

        // Map right operand
        const right = try self.mapNode(rhs_node);

        const bin = try self.allocator.create(tree.Binary);
        bin.* = .{
            .id = self.genUuid(),
            .prefix = left.getPrefix(), // Binary prefix is the left operand's prefix
            .left = left,
            .operator = .{
                .before = op_prefix,
                .element = op_name,
            },
            .operator_source = op_source,
            .right = right,
        };
        return LstNode{ .binary = bin };
    }

    /// Map a Zig AST node tag to the Java J.Binary.Type enum name.
    fn zigBinaryOpToJava(tag: Node.Tag) []const u8 {
        return switch (tag) {
            .add, .add_wrap, .add_sat => "Addition",
            .sub, .sub_wrap, .sub_sat => "Subtraction",
            .mul, .mul_wrap, .mul_sat => "Multiplication",
            .div => "Division",
            .mod => "Modulo",
            .equal_equal => "Equal",
            .bang_equal => "NotEqual",
            .less_than => "LessThan",
            .greater_than => "GreaterThan",
            .less_or_equal => "LessThanOrEqual",
            .greater_or_equal => "GreaterThanOrEqual",
            .bit_and => "BitAnd",
            .bit_or => "BitOr",
            .bit_xor => "BitXor",
            .shl, .shl_sat => "LeftShift",
            .shr => "RightShift",
            .bool_and => "And",
            .bool_or => "Or",
            .array_cat => "Addition", // ++ concatenation mapped to Addition
            .array_mult => "Multiplication", // ** mapped to Multiplication
            .merge_error_sets => "BitOr", // || mapped to BitOr
            .@"catch" => "Or", // catch mapped to Or
            .@"orelse" => "Or", // orelse mapped to Or
            else => "Addition", // fallback
        };
    }

    // -----------------------------------------------------------------
    // Field access → J.FieldAccess
    // data.node_and_token: [0]=target node, [1]=field name token
    // main_token is the "." token
    // -----------------------------------------------------------------

    fn mapFieldAccess(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const data = self.ast.nodeData(node);
        const target_node = data.node_and_token[0];
        const field_token = data.node_and_token[1];

        // Map the target expression
        const target = try self.mapNode(target_node);

        // The "." token is the main_token; extract its prefix
        const dot_token = self.ast.nodeMainToken(node);
        const dot_prefix = self.prefix(dot_token);

        // Map the field name identifier
        const field_name = try self.mapIdentifier(field_token);

        const fa = try self.allocator.create(tree.FieldAccess);
        fa.* = .{
            .id = self.genUuid(),
            .prefix = target.getPrefix(), // FieldAccess prefix is the target's prefix
            .target = target,
            .name = .{
                .before = dot_prefix,
                .element = field_name,
            },
        };
        return LstNode{ .field_access = fa };
    }

    // -----------------------------------------------------------------
    // Function call → J.MethodInvocation
    // call_one: data.node_and_opt_node: [0]=fn expr, [1]=first arg (optional)
    // call: data.node_and_extra: [0]=fn expr, [1]=extra range of args
    // main_token is "(" token
    // -----------------------------------------------------------------

    fn mapCall(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const tag = self.ast.nodeTag(node);
        const main_token = self.ast.nodeMainToken(node);

        // Get the function expression and arguments based on tag variant
        var fn_expr_node: Node.Index = undefined;
        var arg_nodes: []const Node.Index = &.{};
        var arg_buf: [1]Node.Index = undefined;

        switch (tag) {
            .call_one, .call_one_comma => {
                const data = self.ast.nodeData(node);
                fn_expr_node = data.node_and_opt_node[0];
                if (data.node_and_opt_node[1].unwrap()) |arg| {
                    arg_buf[0] = arg;
                    arg_nodes = &arg_buf;
                }
            },
            .call, .call_comma => {
                const data = self.ast.nodeData(node);
                fn_expr_node = data.node_and_extra[0];
                // Extra data contains a SubRange with start/end indexes into extra_data
                const extra_start = @intFromEnum(data.node_and_extra[1]);
                const range_start = self.ast.extra_data[extra_start];
                const range_end = self.ast.extra_data[extra_start + 1];
                // The arg node indices are stored between range_start and range_end
                arg_nodes = @as([]const Node.Index, @ptrCast(self.ast.extra_data[range_start..range_end]));
            },
            else => return self.mapUnknown(node),
        }

        // Determine if this is a method-style call (target.name) or direct call (name)
        const fn_tag = self.ast.nodeTag(fn_expr_node);

        var select: ?tree.RightPadded(LstNode) = null;
        var name_node: LstNode = undefined;

        if (fn_tag == .field_access) {
            // Method-style: target.name(args)
            const fa_data = self.ast.nodeData(fn_expr_node);
            const target_nd = fa_data.node_and_token[0];
            const name_tok = fa_data.node_and_token[1];
            const dot_tok = self.ast.nodeMainToken(fn_expr_node);

            const target = try self.mapNode(target_nd);
            const dot_prefix = self.prefix(dot_tok);

            select = .{
                .element = target,
                .after = dot_prefix,
            };
            name_node = try self.mapIdentifier(name_tok);
        } else {
            // Direct call: name(args)
            name_node = try self.mapNode(fn_expr_node);
        }

        // The "(" token prefix
        const lparen_prefix = self.prefix(main_token);

        // Map arguments
        var args = try self.allocator.alloc(tree.RightPadded(LstNode), arg_nodes.len);
        for (arg_nodes, 0..) |arg_nd, i| {
            const mapped_arg = try self.mapNode(arg_nd);
            // After each argument, extract trailing whitespace (including comma)
            const after = self.trailingWhitespace(arg_nd);
            args[i] = .{
                .element = mapped_arg,
                .after = .{ .whitespace = after },
            };
        }

        // Skip to the rparen
        const last_tok = self.ast.lastToken(node);
        const rparen_start = self.ast.tokenStart(last_tok);
        if (rparen_start > self.cursor) {
            self.cursor = rparen_start;
        }
        self.skipToken(last_tok);

        const mi = try self.allocator.create(tree.MethodInvocation);
        mi.* = .{
            .id = self.genUuid(),
            .prefix = name_node.getPrefix(), // MethodInvocation prefix is the name's prefix
            .select = select,
            .name = name_node,
            .args_prefix = lparen_prefix,
            .args = args,
        };
        return LstNode{ .method_invocation = mi };
    }

    // -----------------------------------------------------------------
    // Assignment → J.Assignment
    // data.node_and_node: [0]=lhs, [1]=rhs
    // main_token is "=" token
    // -----------------------------------------------------------------

    fn mapAssign(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const data = self.ast.nodeData(node);
        const lhs_node = data.node_and_node[0];
        const rhs_node = data.node_and_node[1];
        const main_token = self.ast.nodeMainToken(node);

        // Map left side (variable)
        const variable = try self.mapNode(lhs_node);

        // The "=" token prefix
        const eq_prefix = self.prefix(main_token);

        // Map right side (value)
        const value = try self.mapNode(rhs_node);

        const asgn = try self.allocator.create(tree.Assignment);
        asgn.* = .{
            .id = self.genUuid(),
            .prefix = variable.getPrefix(), // Assignment prefix is the variable's prefix
            .variable = variable,
            .assignment = .{
                .before = eq_prefix,
                .element = value,
            },
        };
        return LstNode{ .assignment = asgn };
    }

    // -----------------------------------------------------------------
    // Compound assignment (+=, -=, etc.) → J.AssignmentOperation
    // data.node_and_node: [0]=lhs, [1]=rhs
    // main_token is the operator token (+=, -=, etc.)
    // -----------------------------------------------------------------

    fn mapAssignOp(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const data = self.ast.nodeData(node);
        const lhs_node = data.node_and_node[0];
        const rhs_node = data.node_and_node[1];
        const main_token = self.ast.nodeMainToken(node);

        // Map left side (variable)
        const variable = try self.mapNode(lhs_node);

        // Capture operator source text
        const op_source = self.ast.tokenSlice(main_token);

        // The operator token prefix
        const op_prefix = self.prefix(main_token);

        // Map operator to J.AssignmentOperation.Type enum name
        const tag = self.ast.nodeTag(node);
        const op_name = zigAssignOpToJava(tag);

        // Map right side
        const assignment = try self.mapNode(rhs_node);

        const ao = try self.allocator.create(tree.AssignmentOperation);
        ao.* = .{
            .id = self.genUuid(),
            .prefix = variable.getPrefix(),
            .variable = variable,
            .operator = .{
                .before = op_prefix,
                .element = op_name,
            },
            .operator_source = op_source,
            .assignment = assignment,
        };
        return LstNode{ .assignment_op = ao };
    }

    fn zigAssignOpToJava(tag: Node.Tag) []const u8 {
        return switch (tag) {
            .assign_add, .assign_add_wrap, .assign_add_sat => "Addition",
            .assign_sub, .assign_sub_wrap, .assign_sub_sat => "Subtraction",
            .assign_mul, .assign_mul_wrap, .assign_mul_sat => "Multiplication",
            .assign_div => "Division",
            .assign_mod => "Modulo",
            .assign_shl, .assign_shl_sat => "LeftShift",
            .assign_shr => "RightShift",
            .assign_bit_and => "BitAnd",
            .assign_bit_or => "BitOr",
            .assign_bit_xor => "BitXor",
            else => "Addition", // fallback
        };
    }

    // -----------------------------------------------------------------
    // Builtin call → J.MethodInvocation
    // @import("std"), @intCast(x), etc.
    // builtin_call_two: data.opt_node_and_opt_node: [0]=arg1, [1]=arg2
    // builtin_call: data.extra_range
    // main_token is the builtin token (@import, @intCast, etc.)
    // -----------------------------------------------------------------

    fn mapBuiltinCall(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const tag = self.ast.nodeTag(node);
        const main_token = self.ast.nodeMainToken(node);

        // Extract the builtin name (e.g., "@import")
        const builtin_prefix = self.prefix(main_token);
        const builtin_name = self.ast.tokenSlice(main_token);

        // Create the name identifier with the builtin name
        const name_ident = try self.allocator.create(tree.Identifier);
        name_ident.* = .{
            .id = self.genUuid(),
            .prefix = builtin_prefix,
            .simple_name = builtin_name,
        };
        const name_node = LstNode{ .identifier = name_ident };

        // Get argument nodes
        var arg_nodes_buf: [2]Node.Index = undefined;
        var arg_count: usize = 0;

        switch (tag) {
            .builtin_call_two, .builtin_call_two_comma => {
                const data = self.ast.nodeData(node);
                if (data.opt_node_and_opt_node[0].unwrap()) |a| {
                    arg_nodes_buf[arg_count] = a;
                    arg_count += 1;
                }
                if (data.opt_node_and_opt_node[1].unwrap()) |a| {
                    arg_nodes_buf[arg_count] = a;
                    arg_count += 1;
                }
            },
            .builtin_call, .builtin_call_comma => {
                // For now, fall back to Unknown for multi-arg builtins
                // since they use extra_range which requires more complex handling
                return self.mapUnknown(node);
            },
            else => return self.mapUnknown(node),
        }

        // The "(" token is right after the builtin name
        const lparen_tok = main_token + 1;
        const lparen_prefix = self.prefix(lparen_tok);

        // Map arguments
        var args = try self.allocator.alloc(tree.RightPadded(LstNode), arg_count);
        for (arg_nodes_buf[0..arg_count], 0..) |arg_nd, i| {
            const mapped_arg = try self.mapNode(arg_nd);
            const after = self.trailingWhitespace(arg_nd);
            args[i] = .{
                .element = mapped_arg,
                .after = .{ .whitespace = after },
            };
        }

        // Skip to the rparen
        const last_tok = self.ast.lastToken(node);
        const rparen_start = self.ast.tokenStart(last_tok);
        if (rparen_start > self.cursor) {
            self.cursor = rparen_start;
        }
        self.skipToken(last_tok);

        const mi = try self.allocator.create(tree.MethodInvocation);
        mi.* = .{
            .id = self.genUuid(),
            .prefix = name_node.getPrefix(),
            .name = name_node,
            .args_prefix = lparen_prefix,
            .args = args,
        };
        return LstNode{ .method_invocation = mi };
    }

    // -----------------------------------------------------------------
    // If → J.If
    // if_simple: data.node_and_opt_node: [0]=cond, [1]=then_expr (optional else)
    //            main_token = "if"
    // .@"if":    Uses extra data for full if/else, accessed via fullIf()
    // -----------------------------------------------------------------

    fn mapIf(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const if_prefix = self.prefix(main_token);

        // Use fullIf to access condition, then, else for all if variants
        const full_if = self.ast.fullIf(node) orelse return self.mapUnknown(node);

        // The "(" token: extract whitespace between "if" and "(" as part of
        // the lparen prefix, then advance cursor past "("
        const lparen_tok = main_token + 1;
        const lparen_prefix = self.prefix(lparen_tok);

        // Map the condition expression (cursor is now right after "(")
        const condition = try self.mapNode(full_if.ast.cond_expr);

        // Find and consume the ")" token after the condition
        const cond_last_tok = self.ast.lastToken(full_if.ast.cond_expr);
        var rparen_tok = cond_last_tok + 1;
        while (rparen_tok < self.ast.tokens.len and self.ast.tokenTag(rparen_tok) != .r_paren) {
            rparen_tok += 1;
        }
        const rparen_prefix = self.prefix(rparen_tok);

        // Map the then branch
        const then_part = try self.mapNode(full_if.ast.then_expr);
        const then_after_ws = self.trailingWhitespace(full_if.ast.then_expr);

        // Map the else branch if present
        var else_part: ?*tree.IfElse = null;
        if (full_if.ast.else_expr != .none) {
            const else_expr_node = full_if.ast.else_expr.unwrap().?;
            // Find the "else" keyword token
            const else_first_tok = self.ast.firstToken(else_expr_node);
            var else_keyword_tok = else_first_tok;
            if (else_keyword_tok > 0) {
                else_keyword_tok -= 1;
                while (else_keyword_tok > 0 and self.ast.tokenTag(else_keyword_tok) != .keyword_else) {
                    else_keyword_tok -= 1;
                }
            }
            const else_kw_prefix = self.prefix(else_keyword_tok);

            const else_body = try self.mapNode(else_expr_node);
            const else_body_after = self.trailingWhitespace(else_expr_node);

            const else_node = try self.allocator.create(tree.IfElse);
            else_node.* = .{
                .id = self.genUuid(),
                .prefix = else_kw_prefix,
                .body = else_body,
                .body_after = .{ .whitespace = else_body_after },
            };
            else_part = else_node;
        }

        const if_node = try self.allocator.create(tree.If);
        if_node.* = .{
            .id = self.genUuid(),
            .prefix = if_prefix,
            .lparen_prefix = lparen_prefix,
            .condition = condition,
            .condition_after = rparen_prefix,
            .then_part = then_part,
            .then_after = .{ .whitespace = then_after_ws },
            .else_part = else_part,
        };
        return LstNode{ .@"if" = if_node };
    }

    // -----------------------------------------------------------------
    // While → J.WhileLoop
    // while_simple: data.node_and_node: [0]=cond, [1]=body
    //              main_token = "while"
    // while_cont, .@"while": accessed via fullWhile()
    // -----------------------------------------------------------------

    fn mapWhile(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const while_prefix = self.prefix(main_token);

        // Use fullWhile to access condition and body for all while variants
        const full_while = self.ast.fullWhile(node) orelse return self.mapUnknown(node);

        // The "(" token: extract whitespace between "while" and "("
        const lparen_tok = main_token + 1;
        const lparen_prefix = self.prefix(lparen_tok);

        // Map the condition
        const condition = try self.mapNode(full_while.ast.cond_expr);

        // Find and consume the ")" token
        const cond_last_tok = self.ast.lastToken(full_while.ast.cond_expr);
        var rparen_tok = cond_last_tok + 1;
        while (rparen_tok < self.ast.tokens.len and self.ast.tokenTag(rparen_tok) != .r_paren) {
            rparen_tok += 1;
        }
        const rparen_prefix = self.prefix(rparen_tok);

        // Map the body
        const body = try self.mapNode(full_while.ast.then_expr);
        const body_after = self.trailingWhitespace(full_while.ast.then_expr);

        const wl = try self.allocator.create(tree.WhileLoop);
        wl.* = .{
            .id = self.genUuid(),
            .prefix = while_prefix,
            .lparen_prefix = lparen_prefix,
            .condition = condition,
            .condition_after = rparen_prefix,
            .body = body,
            .body_after = .{ .whitespace = body_after },
        };
        return LstNode{ .while_loop = wl };
    }

    // -----------------------------------------------------------------
    // Unary expression → J.Unary
    // negation: data.node = operand, main_token = "-"
    // bool_not: data.node = operand, main_token = "!"
    // bit_not: data.node = operand, main_token = "~"
    // -----------------------------------------------------------------

    fn mapUnary(self: *ParseContext, node: Node.Index, op_name: []const u8, op_source: []const u8) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const unary_prefix = self.prefix(main_token);

        const data = self.ast.nodeData(node);
        const operand_node = data.node;
        const expression = try self.mapNode(operand_node);

        const u = try self.allocator.create(tree.Unary);
        u.* = .{
            .id = self.genUuid(),
            .prefix = unary_prefix,
            .operator = .{
                .before = .{ .whitespace = "" }, // prefix unary: no space between op and expr
                .element = op_name,
            },
            .operator_source = op_source,
            .expression = expression,
        };
        return LstNode{ .unary = u };
    }

    // -----------------------------------------------------------------
    // Grouped expression → J.Parentheses
    // grouped_expression: data.node_and_token: [0]=expr, [1]=rparen token
    // main_token = "(" token
    // -----------------------------------------------------------------

    fn mapGrouped(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const lparen_prefix = self.prefix(main_token);

        const data = self.ast.nodeData(node);
        const expr_node = data.node_and_token[0];
        const rparen_tok = data.node_and_token[1];

        // Map the inner expression
        const expression = try self.mapNode(expr_node);

        // Whitespace before the ")" token
        const rparen_start = self.ast.tokenStart(rparen_tok);
        const rparen_ws = if (rparen_start > self.cursor)
            self.source[self.cursor..rparen_start]
        else
            "";
        self.skipToken(rparen_tok);

        const p = try self.allocator.create(tree.Parentheses);
        p.* = .{
            .id = self.genUuid(),
            .prefix = lparen_prefix,
            .expression = expression,
            .after = .{ .whitespace = rparen_ws },
        };
        return LstNode{ .parentheses = p };
    }

    // -----------------------------------------------------------------
    // Error union type → Zig.ErrorUnion
    // error_union: data.node_and_node: [0]=error type, [1]=value type
    // main_token = "!" token
    // -----------------------------------------------------------------

    fn mapErrorUnion(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const data = self.ast.nodeData(node);
        const lhs_node = data.node_and_node[0];
        const rhs_node = data.node_and_node[1];
        const main_token = self.ast.nodeMainToken(node);

        // Check if there's an explicit error type before the "!" operator
        const lhs_first_tok = self.ast.firstToken(lhs_node);
        const lhs_tok_start = self.ast.tokenStart(lhs_first_tok);
        const bang_start = self.ast.tokenStart(main_token);

        var error_type: ?LstNode = null;
        var eu_prefix: Space = undefined;

        if (lhs_tok_start < bang_start) {
            // Explicit error type before "!" (e.g. "MyError!u32")
            error_type = try self.mapNode(lhs_node);
            eu_prefix = error_type.?.getPrefix();
        } else {
            // Implicit error set (just "!Type") - the "!" is before the type
            // Extract whitespace before the "!" token
            eu_prefix = .{ .whitespace = self.extractWhitespace(bang_start) };
        }

        // Skip the "!" token
        const bang_len = self.tokenLength(main_token);
        const after_bang = bang_start + bang_len;
        if (after_bang > self.cursor) {
            self.cursor = after_bang;
        }

        // Map the value type (right side)
        const value_type = try self.mapNode(rhs_node);

        const eu = try self.allocator.create(tree.ErrorUnion);
        eu.* = .{
            .id = self.genUuid(),
            .prefix = eu_prefix,
            .error_type = error_type,
            .value_type = .{
                .before = .{ .whitespace = "" }, // No space between "!" and type
                .element = value_type,
            },
        };
        return LstNode{ .zig_error_union = eu };
    }

    // -----------------------------------------------------------------
    // Array access → J.ArrayAccess
    // array_access: data.node_and_node: [0]=target, [1]=index
    // main_token = "[" token
    // -----------------------------------------------------------------

    fn mapArrayAccess(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const data = self.ast.nodeData(node);
        const target_node = data.node_and_node[0];
        const index_node = data.node_and_node[1];
        const main_token = self.ast.nodeMainToken(node);

        // Map the target expression
        const target = try self.mapNode(target_node);

        // The "[" token prefix
        const lbracket_prefix = self.prefix(main_token);

        // Map the index expression
        const index = try self.mapNode(index_node);

        // Find the "]" token
        const last_tok = self.ast.lastToken(node);
        const rbracket_start = self.ast.tokenStart(last_tok);
        const rbracket_ws = if (rbracket_start > self.cursor)
            self.source[self.cursor..rbracket_start]
        else
            "";
        self.skipToken(last_tok);

        const aa = try self.allocator.create(tree.ArrayAccess);
        aa.* = .{
            .id = self.genUuid(),
            .prefix = target.getPrefix(),
            .indexed = target,
            .dimension_prefix = lbracket_prefix,
            .index = index,
            .index_after = .{ .whitespace = rbracket_ws },
        };
        return LstNode{ .array_access = aa };
    }

    // -----------------------------------------------------------------
    // Slice expression → Zig.Slice
    // slice_open: a[start..], data.node_and_node: [0]=target, [1]=start
    // slice: a[start..end], uses extra data
    // slice_sentinel: a[start..end:sentinel]
    // -----------------------------------------------------------------

    fn mapSlice(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const full_slice = self.ast.fullSlice(node) orelse return self.mapUnknown(node);

        // Map the target expression (the thing being sliced)
        const target = try self.mapNode(full_slice.ast.sliced);

        // The "[" token
        const lbracket_tok = full_slice.ast.lbracket;
        const lbracket_prefix = self.prefix(lbracket_tok);

        // Map the start expression
        const start_node = try self.mapNode(full_slice.ast.start);
        // After the start expression, there are ".." tokens
        // The trailing includes everything up to the end or "]"
        const start_after = self.trailingWhitespace(full_slice.ast.start);

        // Map the end expression if present
        var end_node: ?LstNode = null;
        if (full_slice.ast.end != .none) {
            const end_nd = full_slice.ast.end.unwrap().?;
            end_node = try self.mapNode(end_nd);
        }

        // Find the "]" token
        const last_tok = self.ast.lastToken(node);
        const rbracket_start = self.ast.tokenStart(last_tok);
        const rbracket_ws = if (rbracket_start > self.cursor)
            self.source[self.cursor..rbracket_start]
        else
            "";
        self.skipToken(last_tok);

        const s = try self.allocator.create(tree.Slice);
        s.* = .{
            .id = self.genUuid(),
            .prefix = target.getPrefix(),
            .target = target,
            .open_bracket = lbracket_prefix,
            .start = .{
                .element = start_node,
                .after = .{ .whitespace = start_after },
            },
            .end = end_node,
            .close_bracket = .{ .whitespace = rbracket_ws },
        };
        return LstNode{ .zig_slice = s };
    }

    // -----------------------------------------------------------------
    // Postfix deref (expr.*) → J.FieldAccess
    // deref: data.node = operand, main_token = "*"
    // We map this as J.FieldAccess with field name "*"
    // -----------------------------------------------------------------

    fn mapPostfixUnary(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const data = self.ast.nodeData(node);
        const operand_node = data.node;
        const main_token = self.ast.nodeMainToken(node);

        // Map the operand
        const target = try self.mapNode(operand_node);

        // The main_token is the `.*` (period_asterisk) token which represents both "." and "*"
        // Extract whitespace before this combined token
        const tok_start = self.ast.tokenStart(main_token);
        const dot_prefix_ws = self.extractWhitespace(tok_start);
        // Advance cursor past the entire token (which includes both "." and "*")
        const tok_len = self.tokenLength(main_token);
        self.cursor = tok_start + tok_len;

        // Create a name identifier for "*" with empty prefix
        // (the "." is part of the combined token, printed by FieldAccess)
        const star_ident = try self.allocator.create(tree.Identifier);
        star_ident.* = .{
            .id = self.genUuid(),
            .prefix = .{ .whitespace = "" },
            .simple_name = "*",
        };

        const fa = try self.allocator.create(tree.FieldAccess);
        fa.* = .{
            .id = self.genUuid(),
            .prefix = target.getPrefix(),
            .target = target,
            .name = .{
                .before = .{ .whitespace = dot_prefix_ws },
                .element = LstNode{ .identifier = star_ident },
            },
        };
        return LstNode{ .field_access = fa };
    }

    // -----------------------------------------------------------------
    // Postfix unwrap optional (expr.?) → J.FieldAccess
    // unwrap_optional: data.node_and_token: [0]=operand, [1]="?" token
    // main_token = "." token
    // -----------------------------------------------------------------

    fn mapPostfixDotOp(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const data = self.ast.nodeData(node);
        const operand_node = data.node_and_token[0];
        const question_token = data.node_and_token[1];

        // Map the operand
        const target = try self.mapNode(operand_node);

        // The "." token is the main_token. Extract whitespace before it.
        const dot_token = self.ast.nodeMainToken(node);
        const dot_start = self.ast.tokenStart(dot_token);
        const dot_ws = self.extractWhitespace(dot_start);
        // Advance cursor past the "." token
        self.skipToken(dot_token);

        // Map "?" as a name identifier
        const question_name = try self.mapIdentifier(question_token);

        const fa = try self.allocator.create(tree.FieldAccess);
        fa.* = .{
            .id = self.genUuid(),
            .prefix = target.getPrefix(),
            .target = target,
            .name = .{
                .before = .{ .whitespace = dot_ws },
                .element = question_name,
            },
        };
        return LstNode{ .field_access = fa };
    }

    // -----------------------------------------------------------------
    // Keyword-style unary (try expr) → Zig.Comptime-like or J.Unary
    // The keyword is the main_token, data.node = the operand.
    // We map "try" as Zig.Comptime-like: just keyword + expression.
    // However, since we have Comptime already, let's just use J.Unary
    // with the keyword text as operator_source.
    // -----------------------------------------------------------------

    fn mapUnaryKeyword(self: *ParseContext, node: Node.Index, keyword: []const u8) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const unary_prefix = self.prefix(main_token);

        const data = self.ast.nodeData(node);
        const operand_node = data.node;
        const expression = try self.mapNode(operand_node);

        const u = try self.allocator.create(tree.Unary);
        u.* = .{
            .id = self.genUuid(),
            .prefix = unary_prefix,
            .operator = .{
                .before = .{ .whitespace = "" },
                .element = "Not", // Map try to an existing J.Unary.Type enum
            },
            .operator_source = keyword,
            .expression = expression,
        };
        return LstNode{ .unary = u };
    }

    // -----------------------------------------------------------------
    // Switch → Zig.Switch (mapped to J.Switch on Java side)
    // .@"switch", .switch_comma: fullSwitch() gives condition + case nodes
    // Each case is a switch_case_one/switch_case/switch_case_inline_one/switch_case_inline
    // -----------------------------------------------------------------

    fn mapSwitch(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const full_switch = self.ast.fullSwitch(node) orelse return self.mapUnknown(node);

        // "switch" keyword prefix
        const switch_prefix = self.prefix(full_switch.ast.switch_token);

        // The "(" token comes right after "switch"
        const lparen_tok = full_switch.ast.switch_token + 1;
        const lparen_prefix = self.prefix(lparen_tok);

        // Map the condition expression
        const condition = try self.mapNode(full_switch.ast.condition);

        // Find and consume the ")" token after the condition
        const cond_last_tok = self.ast.lastToken(full_switch.ast.condition);
        var rparen_tok = cond_last_tok + 1;
        while (rparen_tok < self.ast.tokens.len and self.ast.tokenTag(rparen_tok) != .r_paren) {
            rparen_tok += 1;
        }
        const condition_after = self.prefix(rparen_tok);

        // Find and consume the "{" token
        var lbrace_tok = rparen_tok + 1;
        while (lbrace_tok < self.ast.tokens.len and self.ast.tokenTag(lbrace_tok) != .l_brace) {
            lbrace_tok += 1;
        }
        const lbrace_prefix = self.prefix(lbrace_tok);

        // Map each switch case/prong
        const case_nodes = full_switch.ast.cases;
        var prongs = try self.allocator.alloc(RightPadded(LstNode), case_nodes.len);
        for (case_nodes, 0..) |case_node, i| {
            const prong = try self.mapSwitchProng(case_node);
            // After each prong, consume trailing comma if present.
            // In switch blocks, prongs are separated by commas, not semicolons.
            const last_tok = self.ast.lastToken(case_node);
            const next_tok = last_tok + 1;
            var after: []const u8 = "";
            if (next_tok < self.ast.tokens.len and self.ast.tokenTag(next_tok) == .comma) {
                // Include everything from cursor through the comma
                const comma_end = self.ast.tokenStart(next_tok) + self.tokenLength(next_tok);
                if (self.cursor < comma_end) {
                    after = self.source[self.cursor..comma_end];
                    self.cursor = comma_end;
                }
            }
            prongs[i] = .{
                .element = prong,
                .after = .{ .whitespace = after },
            };
        }

        // Find the closing "}" token
        const last_tok = self.ast.lastToken(node);
        const rbrace_start = self.ast.tokenStart(last_tok);
        const end_ws = if (rbrace_start > self.cursor)
            self.source[self.cursor..rbrace_start]
        else
            "";
        self.skipToken(last_tok);

        const sw = try self.allocator.create(tree.Switch);
        sw.* = .{
            .id = self.genUuid(),
            .prefix = switch_prefix,
            .lparen_prefix = lparen_prefix,
            .condition = condition,
            .condition_after = condition_after,
            .lbrace_prefix = lbrace_prefix,
            .prongs = prongs,
            .end = .{ .whitespace = end_ws },
        };
        return LstNode{ .zig_switch = sw };
    }

    fn mapSwitchProng(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const full_case = self.ast.fullSwitchCase(node) orelse return self.mapUnknown(node);

        // Map the case values. Empty values = else case.
        const values = full_case.ast.values;

        var cases: []RightPadded(LstNode) = undefined;
        // Prong prefix is empty -- the whitespace before the prong is captured
        // by the first case value's prefix (or the "else" identifier's prefix).
        const prong_prefix: Space = .{ .whitespace = "" };
        if (values.len == 0) {
            // This is an "else" case. Create an identifier for the "else" keyword.
            // The first token is the "else" keyword (or "inline" if inline else).
            const first_tok = self.ast.firstToken(node);
            var else_tok = first_tok;
            if (full_case.inline_token) |_| {
                // Skip "inline" token, find the actual "else" keyword
                else_tok = first_tok + 1;
                // Consume the "inline" keyword prefix
                _ = self.prefix(first_tok);
            }

            // Consume the "else" keyword, getting its prefix (whitespace before it)
            const else_ws = self.prefix(else_tok);

            cases = try self.allocator.alloc(RightPadded(LstNode), 1);
            const else_ident = try self.allocator.create(tree.Identifier);
            else_ident.* = .{
                .id = self.genUuid(),
                .prefix = else_ws,
                .simple_name = "else",
            };
            cases[0] = .{
                .element = LstNode{ .identifier = else_ident },
                .after = .{ .whitespace = "" },
            };
        } else {
            // For value cases, let mapNode handle the prefix extraction for each value.
            // The first value's prefix will capture the whitespace before the prong.
            cases = try self.allocator.alloc(RightPadded(LstNode), values.len);
            for (values, 0..) |val_node, i| {
                const val = try self.mapNode(val_node);
                const after = self.trailingWhitespace(val_node);
                cases[i] = .{
                    .element = val,
                    .after = .{ .whitespace = after },
                };
            }
        }

        // Handle payload if present (|val|)
        var payload: ?LstNode = null;
        if (full_case.payload_token) |payload_tok| {
            // payload_token points to the first identifier after the "|"
            // We need to capture from the "|" through to the closing "|"
            // The "|" token is payload_tok - 1
            const pipe_tok = payload_tok - 1;
            const pipe_start = self.ast.tokenStart(pipe_tok);
            const pipe_ws = if (pipe_start > self.cursor)
                self.source[self.cursor..pipe_start]
            else
                "";

            // Find the closing "|" by scanning forward
            var close_pipe = payload_tok;
            while (close_pipe < self.ast.tokens.len and self.ast.tokenTag(close_pipe) != .pipe) {
                close_pipe += 1;
            }
            // Capture the full payload text from "|" through "|"
            const close_pipe_end = self.ast.tokenStart(close_pipe) + self.tokenLength(close_pipe);
            const payload_text = self.source[pipe_start..close_pipe_end];
            self.cursor = close_pipe_end;

            // Create an identifier node with the full payload text
            const payload_ident = try self.allocator.create(tree.Identifier);
            payload_ident.* = .{
                .id = self.genUuid(),
                .prefix = .{ .whitespace = pipe_ws },
                .simple_name = payload_text,
            };
            payload = LstNode{ .identifier = payload_ident };
        }

        // The arrow token ("=>") - extract its prefix
        const arrow_tok = full_case.ast.arrow_token;
        const arrow_prefix = self.prefix(arrow_tok);

        // Map the target expression (the body of this prong)
        const target_expr = try self.mapNode(full_case.ast.target_expr);

        const sp = try self.allocator.create(tree.SwitchProng);
        sp.* = .{
            .id = self.genUuid(),
            .prefix = prong_prefix,
            .cases_prefix = .{ .whitespace = "" }, // prefix is already in prong_prefix
            .cases = cases,
            .payload = payload,
            .arrow = .{
                .before = arrow_prefix,
                .element = target_expr,
            },
        };
        return LstNode{ .zig_switch_prong = sp };
    }

    // -----------------------------------------------------------------
    // For loop → Zig.ForLoop
    // for_simple: data.node_and_node: [0]=iterable, [1]=body
    //             main_token = "for"
    // .@"for": accessed via fullFor()
    // -----------------------------------------------------------------

    fn mapForLoop(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const full_for = self.ast.fullFor(node) orelse return self.mapUnknown(node);

        // "for" keyword prefix
        const for_prefix = self.prefix(full_for.ast.for_token);

        // Capture the iterable section text: from "(" through ")"
        // The "(" token is right after "for"
        const lparen_tok = full_for.ast.for_token + 1;
        const lparen_start = self.ast.tokenStart(lparen_tok);
        const lparen_ws = self.extractWhitespace(lparen_start);

        // Find the last token of the last input, then scan for ")"
        const last_input = full_for.ast.inputs[full_for.ast.inputs.len - 1];
        const last_input_tok = self.ast.lastToken(last_input);
        var rparen_tok = last_input_tok + 1;
        while (rparen_tok < self.ast.tokens.len and self.ast.tokenTag(rparen_tok) != .r_paren) {
            rparen_tok += 1;
        }
        const rparen_end = self.ast.tokenStart(rparen_tok) + self.tokenLength(rparen_tok);
        const iterable_text = self.source[lparen_start..rparen_end];
        self.cursor = rparen_end;

        // Capture the payload text: include whitespace between ")" and "|",
        // plus from first "|" through closing "|".
        // This captures " |item|" or " |item, index|" faithfully.
        const payload_tok = full_for.payload_token;
        const payload_text_start = self.cursor; // right after ")"

        // Find the closing "|" by scanning forward from payload_token
        var close_pipe = payload_tok;
        while (close_pipe < self.ast.tokens.len and self.ast.tokenTag(close_pipe) != .pipe) {
            close_pipe += 1;
        }
        const close_pipe_end = self.ast.tokenStart(close_pipe) + self.tokenLength(close_pipe);
        const payload_text = self.source[payload_text_start..close_pipe_end];
        self.cursor = close_pipe_end;

        // Map the body
        const body = try self.mapNode(full_for.ast.then_expr);
        const body_after = self.trailingWhitespace(full_for.ast.then_expr);

        // Map the else branch if present
        var else_body: ?*tree.ForLoopElse = null;
        if (full_for.ast.else_expr != .none) {
            const else_expr_node = full_for.ast.else_expr.unwrap().?;
            // Find the "else" keyword token
            const else_first_tok = self.ast.firstToken(else_expr_node);
            var else_keyword_tok = else_first_tok;
            if (else_keyword_tok > 0) {
                else_keyword_tok -= 1;
                while (else_keyword_tok > 0 and self.ast.tokenTag(else_keyword_tok) != .keyword_else) {
                    else_keyword_tok -= 1;
                }
            }
            const else_kw_prefix = self.prefix(else_keyword_tok);

            const else_expr = try self.mapNode(else_expr_node);
            const else_body_after = self.trailingWhitespace(else_expr_node);

            const else_node = try self.allocator.create(tree.ForLoopElse);
            else_node.* = .{
                .id = self.genUuid(),
                .prefix = else_kw_prefix,
                .body = else_expr,
                .body_after = .{ .whitespace = else_body_after },
            };
            else_body = else_node;
        }

        const fl = try self.allocator.create(tree.ForLoop);
        fl.* = .{
            .id = self.genUuid(),
            .prefix = for_prefix,
            .lparen_prefix = .{ .whitespace = lparen_ws },
            .iterable_text = iterable_text,
            .payload_text = payload_text,
            .body = body,
            .body_after = .{ .whitespace = body_after },
            .else_body = else_body,
        };
        return LstNode{ .zig_for_loop = fl };
    }

    // -----------------------------------------------------------------
    // Defer / Errdefer → Zig.Defer
    // defer: data.node = the deferred expression, main_token = "defer"
    // errdefer: data.opt_token_and_node: [0]=payload token (optional), [1]=expr node
    //           main_token = "errdefer"
    // -----------------------------------------------------------------

    fn mapDefer(self: *ParseContext, node: Node.Index, is_errdefer: bool) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const defer_prefix = self.prefix(main_token);

        const payload: ?LstNode = null;
        var expr_node: Node.Index = undefined;

        if (is_errdefer) {
            // errdefer: data is opt_token_and_node
            const data = self.ast.nodeData(node);
            expr_node = data.opt_token_and_node[1];
            // Payload token if present (|err|)
            if (data.opt_token_and_node[0].unwrap()) |_| {
                // For now, skip payload mapping (would need Zig.Payload type)
                // Just advance cursor past payload tokens
            }
        } else {
            // defer: data is node
            const data = self.ast.nodeData(node);
            expr_node = data.node;
        }

        const expression = try self.mapNode(expr_node);

        const d = try self.allocator.create(tree.Defer);
        d.* = .{
            .id = self.genUuid(),
            .prefix = defer_prefix,
            .is_errdefer = is_errdefer,
            .payload = payload,
            .expression = expression,
        };
        return LstNode{ .zig_defer = d };
    }

    // -----------------------------------------------------------------
    // Test declaration → Zig.TestDecl
    // test_decl: data.opt_token_and_node: [0]=name token (optional), [1]=body node
    // main_token = "test"
    // -----------------------------------------------------------------

    fn mapTestDecl(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const test_prefix = self.prefix(main_token);

        const data = self.ast.nodeData(node);
        const body_node = data.opt_token_and_node[1];

        // Name token is optional (test "name" {...} or test {...})
        var name: ?LstNode = null;
        if (data.opt_token_and_node[0].unwrap()) |name_tok| {
            // The name is a string literal token
            const name_prefix = self.prefix(name_tok);
            const name_text = self.ast.tokenSlice(name_tok);

            const lit = try self.allocator.create(tree.Literal);
            lit.* = .{
                .id = self.genUuid(),
                .prefix = name_prefix,
                .value = .{ .string = name_text },
                .value_source = name_text,
            };
            name = LstNode{ .literal = lit };
        }

        const body = try self.mapNode(body_node);

        const td = try self.allocator.create(tree.TestDecl);
        td.* = .{
            .id = self.genUuid(),
            .prefix = test_prefix,
            .name = name,
            .body = body,
        };
        return LstNode{ .zig_test_decl = td };
    }

    // -----------------------------------------------------------------
    // Comptime → Zig.Comptime
    // comptime: data.node = the expression, main_token = "comptime"
    // -----------------------------------------------------------------

    fn mapComptime(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const comptime_prefix = self.prefix(main_token);

        const data = self.ast.nodeData(node);
        const expr_node = data.node;
        const expression = try self.mapNode(expr_node);

        const ct = try self.allocator.create(tree.Comptime);
        ct.* = .{
            .id = self.genUuid(),
            .prefix = comptime_prefix,
            .expression = expression,
        };
        return LstNode{ .zig_comptime = ct };
    }

    // -----------------------------------------------------------------
    // Identifier (from token) → J.Identifier
    // -----------------------------------------------------------------

    pub fn mapIdentifier(self: *ParseContext, token_index: Ast.TokenIndex) ParseError!LstNode {
        const ident_prefix = self.prefix(token_index);
        const name = self.ast.tokenSlice(token_index);
        const ident = try self.allocator.create(tree.Identifier);
        ident.* = .{
            .id = self.genUuid(),
            .prefix = ident_prefix,
            .simple_name = name,
        };
        return LstNode{ .identifier = ident };
    }

    /// Map an identifier node (as opposed to a token index).
    fn mapIdentifierNode(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        return self.mapIdentifier(main_token);
    }

    /// Map an enum literal (.foo) to an identifier with the dot prefix included.
    fn mapEnumLiteral(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        // The main_token is the identifier after the dot
        // But we need to include the preceding dot as part of the prefix
        const main_token = self.ast.nodeMainToken(node);
        // The dot token is main_token - 1
        const dot_token = main_token - 1;

        // Extract whitespace up to the dot, then consume both dot and identifier
        const dot_start = self.ast.tokenStart(dot_token);
        const ws = if (dot_start > self.cursor) self.source[self.cursor..dot_start] else "";

        // Now consume the dot and identifier tokens
        const ident_start = self.ast.tokenStart(main_token);
        const ident_len = self.tokenLength(main_token);
        self.cursor = ident_start + ident_len;

        // The full name including the dot prefix
        const dot_text = self.source[dot_start .. ident_start + ident_len];

        const ident = try self.allocator.create(tree.Identifier);
        ident.* = .{
            .id = self.genUuid(),
            .prefix = .{ .whitespace = ws },
            .simple_name = dot_text,
        };
        return LstNode{ .identifier = ident };
    }

    // -----------------------------------------------------------------
    // Number literal → J.Literal
    // -----------------------------------------------------------------

    fn mapNumberLiteralNode(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        return self.mapNumberLiteral(main_token);
    }

    pub fn mapNumberLiteral(self: *ParseContext, token_index: Ast.TokenIndex) ParseError!LstNode {
        const lit_prefix = self.prefix(token_index);
        const value_source = self.ast.tokenSlice(token_index);

        // Try to parse the number for the value field
        var value: ?tree.LiteralValue = null;
        if (std.fmt.parseInt(i64, value_source, 0)) |v| {
            value = .{ .int = v };
        } else |_| {
            if (std.fmt.parseFloat(f64, value_source)) |v| {
                value = .{ .float = v };
            } else |_| {}
        }

        const lit = try self.allocator.create(tree.Literal);
        lit.* = .{
            .id = self.genUuid(),
            .prefix = lit_prefix,
            .value = value,
            .value_source = value_source,
        };
        return LstNode{ .literal = lit };
    }

    // -----------------------------------------------------------------
    // String literal → J.Literal
    // -----------------------------------------------------------------

    fn mapStringLiteralNode(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const main_token = self.ast.nodeMainToken(node);
        const lit_prefix = self.prefix(main_token);
        const value_source = self.ast.tokenSlice(main_token);

        const lit = try self.allocator.create(tree.Literal);
        lit.* = .{
            .id = self.genUuid(),
            .prefix = lit_prefix,
            .value = .{ .string = value_source },
            .value_source = value_source,
        };
        return LstNode{ .literal = lit };
    }

    // -----------------------------------------------------------------
    // Unknown (fallback) → J.Unknown
    // -----------------------------------------------------------------

    fn mapUnknown(self: *ParseContext, node: Node.Index) ParseError!LstNode {
        const first_tok = self.ast.firstToken(node);
        const unknown_prefix = self.prefix(first_tok);

        // Extract the full source text of this node
        const last_tok = self.ast.lastToken(node);
        const node_start = self.ast.tokenStart(first_tok);
        const last_start = self.ast.tokenStart(last_tok);
        const last_len = self.tokenLength(last_tok);
        const node_end = last_start + last_len;

        // The text starts after the first token (which was consumed for prefix)
        // Actually, we need the full text including the first token
        const text = self.source[node_start..node_end];
        self.cursor = node_end;

        const source_node = try self.allocator.create(tree.UnknownSource);
        source_node.* = .{
            .id = self.genUuid(),
            .prefix = .{ .whitespace = "" },
            .text = text,
        };

        const unknown = try self.allocator.create(tree.Unknown);
        unknown.* = .{
            .id = self.genUuid(),
            .prefix = unknown_prefix,
            .source = LstNode{ .unknown_source = source_node },
        };
        return LstNode{ .unknown = unknown };
    }
};

test "ParseContext basic parsing" {
    // Use arena allocator to avoid leaking tree nodes
    var arena = std.heap.ArenaAllocator.init(std.testing.allocator);
    defer arena.deinit();
    const allocator = arena.allocator();

    const source = "const x = 42;\n";
    const source_z: [:0]const u8 = try allocator.dupeZ(u8, source);

    var ast = try std.zig.Ast.parse(allocator, source_z, .zig);
    defer ast.deinit(allocator);

    var ctx = ParseContext.init(allocator, source, source_z, ast);
    const result = try ctx.mapFile("test.zig", "test-uuid-1234");

    // Verify we got a compilation_unit
    switch (result) {
        .compilation_unit => |cu| {
            try std.testing.expectEqualStrings("test.zig", cu.source_path);
            try std.testing.expect(cu.statements.len > 0);
        },
        else => return error.TestUnexpectedResult,
    }
}

test "ParseContext for loop round-trip" {
    const printer = @import("printer.zig");
    var arena = std.heap.ArenaAllocator.init(std.testing.allocator);
    defer arena.deinit();
    const allocator = arena.allocator();

    const source =
        \\fn sum(items: []const i32) i32 {
        \\    var total: i32 = 0;
        \\    for (items) |item| {
        \\        total += item;
        \\    }
        \\    return total;
        \\}
        \\
    ;
    const source_z: [:0]const u8 = try allocator.dupeZ(u8, source);
    var ast = try std.zig.Ast.parse(allocator, source_z, .zig);
    defer ast.deinit(allocator);

    var ctx = ParseContext.init(allocator, source, source_z, ast);
    const result = try ctx.mapFile("test.zig", "test-uuid-1234");

    const output = try printer.print(allocator, result);
    defer allocator.free(output);

    try std.testing.expectEqualStrings(source, output);
}

test "ParseContext switch expression round-trip" {
    const printer = @import("printer.zig");
    var arena = std.heap.ArenaAllocator.init(std.testing.allocator);
    defer arena.deinit();
    const allocator = arena.allocator();

    const source =
        \\fn toStr(x: u8) []const u8 {
        \\    return switch (x) {
        \\        0 => "zero",
        \\        1 => "one",
        \\        else => "other",
        \\    };
        \\}
        \\
    ;
    const source_z: [:0]const u8 = try allocator.dupeZ(u8, source);
    var ast = try std.zig.Ast.parse(allocator, source_z, .zig);
    defer ast.deinit(allocator);

    var ctx = ParseContext.init(allocator, source, source_z, ast);
    const result = try ctx.mapFile("test.zig", "test-uuid-1234");

    const output = try printer.print(allocator, result);
    defer allocator.free(output);

    try std.testing.expectEqualStrings(source, output);
}
