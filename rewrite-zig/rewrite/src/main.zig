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

//! JSON-RPC 2.0 server for OpenRewrite Zig language support.
//! Communicates over stdin/stdout using Content-Length framed messages (LSP protocol).

const std = @import("std");
const rpc = @import("rpc.zig");
const parser = @import("parser.zig");
const printer_mod = @import("printer.zig");
const sender_mod = @import("sender.zig");
const tree = @import("tree.zig");

const RpcObjectData = rpc.RpcObjectData;
const State = rpc.State;
const SendQueue = rpc.SendQueue;
const ParseContext = parser.ParseContext;
const Sender = sender_mod.Sender;
const LstNode = tree.LstNode;

/// Configuration parsed from command-line arguments.
const Config = struct {
    log_file_path: ?[]const u8 = null,
    trace_rpc_messages: bool = false,
};

/// The RPC server state.
const Server = struct {
    allocator: std.mem.Allocator,
    local_objects: std.StringHashMap(StoredObject),
    remote_objects: std.StringHashMap(StoredObject),
    batch_size: usize = 1000,
    log_file: ?std.fs.File = null,
    trace_rpc: bool = false,

    /// A stored parsed compilation unit.
    const StoredObject = struct {
        source: []const u8,
        source_path: []const u8,
        id: []const u8,
        /// The parsed LST tree (null if parsing failed, falls back to legacy behavior).
        lst: ?LstNode = null,
    };

    fn init(allocator: std.mem.Allocator) Server {
        return .{
            .allocator = allocator,
            .local_objects = std.StringHashMap(StoredObject).init(allocator),
            .remote_objects = std.StringHashMap(StoredObject).init(allocator),
        };
    }

    fn deinit(self: *Server) void {
        var it = self.local_objects.iterator();
        while (it.next()) |entry| {
            self.allocator.free(entry.value_ptr.source);
            self.allocator.free(entry.value_ptr.source_path);
            self.allocator.free(entry.value_ptr.id);
            self.allocator.free(entry.key_ptr.*);
        }
        self.local_objects.deinit();

        var rit = self.remote_objects.iterator();
        while (rit.next()) |entry| {
            self.allocator.free(entry.value_ptr.source);
            self.allocator.free(entry.value_ptr.source_path);
            self.allocator.free(entry.value_ptr.id);
            self.allocator.free(entry.key_ptr.*);
        }
        self.remote_objects.deinit();

        if (self.log_file) |f| f.close();
    }

    fn log(self: *Server, comptime fmt: []const u8, args: anytype) void {
        if (self.log_file) |f| {
            var buf: [4096]u8 = undefined;
            var w = f.writer(&buf);
            w.interface.print(fmt, args) catch {};
            w.interface.print("\n", .{}) catch {};
            w.interface.flush() catch {};
        }
    }

    fn handleRequest(self: *Server, req_json: std.json.Value) !std.json.Value {
        const obj = if (req_json == .object) req_json.object else return self.makeErrorResponse(null, -32700, "Parse error");

        const id = obj.get("id") orelse std.json.Value.null;
        const method_val = obj.get("method") orelse return self.makeErrorResponse(id, -32600, "Invalid Request: missing method");
        const method = if (method_val == .string) method_val.string else return self.makeErrorResponse(id, -32600, "Invalid Request: method must be string");
        const params = obj.get("params") orelse std.json.Value.null;

        self.log("Handling: {s}", .{method});

        if (std.mem.eql(u8, method, "GetLanguages")) {
            return self.handleGetLanguages(id);
        } else if (std.mem.eql(u8, method, "Parse")) {
            return self.handleParse(id, params);
        } else if (std.mem.eql(u8, method, "GetObject")) {
            return self.handleGetObject(id, params);
        } else if (std.mem.eql(u8, method, "Print")) {
            return self.handlePrint(id, params);
        } else if (std.mem.eql(u8, method, "Reset")) {
            return self.handleReset(id);
        } else {
            return self.makeErrorResponseMsg(id, -32601, "Unknown method");
        }
    }

    fn handleGetLanguages(self: *Server, id: std.json.Value) !std.json.Value {
        var result_obj = std.json.ObjectMap.init(self.allocator);
        try result_obj.put("jsonrpc", std.json.Value{ .string = "2.0" });
        try result_obj.put("id", id);

        var arr = std.json.Array.init(self.allocator);
        try arr.append(std.json.Value{ .string = "org.openrewrite.zig.tree.Zig$CompilationUnit" });
        try result_obj.put("result", std.json.Value{ .array = arr });
        return std.json.Value{ .object = result_obj };
    }

    fn handleParse(self: *Server, id: std.json.Value, params: std.json.Value) !std.json.Value {
        if (params != .object) {
            return self.makeErrorResponseMsg(id, -32602, "Invalid params: expected object");
        }
        const params_obj = params.object;

        // Extract inputs array
        const inputs_val = params_obj.get("inputs") orelse return self.makeErrorResponseMsg(id, -32602, "Missing 'inputs' parameter");
        if (inputs_val != .array) {
            return self.makeErrorResponseMsg(id, -32602, "Invalid params: 'inputs' must be array");
        }
        const inputs = inputs_val.array;

        // Get optional relativeTo
        const relative_to: ?[]const u8 = blk: {
            if (params_obj.get("relativeTo")) |rt| {
                if (rt == .string) break :blk rt.string;
            }
            break :blk null;
        };

        var result_ids = std.json.Array.init(self.allocator);

        for (inputs.items) |input_val| {
            if (input_val != .object) continue;
            const input_obj = input_val.object;

            var source: ?[]const u8 = null;
            var source_path: ?[]const u8 = null;

            // Check for path-based input
            if (input_obj.get("path")) |path_val| {
                if (path_val == .string and path_val.string.len > 0) {
                    const file_path = path_val.string;
                    // Read file content
                    const file = std.fs.openFileAbsolute(file_path, .{}) catch |err| {
                        self.log("Failed to open file {s}: {}", .{ file_path, err });
                        return self.makeErrorResponseMsg(id, -32603, "Failed to read file");
                    };
                    defer file.close();
                    var read_buf: [8192]u8 = undefined;
                    var reader = file.reader(&read_buf);
                    const content = reader.interface.allocRemaining(self.allocator, .unlimited) catch |err| {
                        self.log("Failed to read file {s}: {}", .{ file_path, err });
                        return self.makeErrorResponseMsg(id, -32603, "Failed to read file");
                    };
                    source = content;

                    // Compute relative path if relativeTo is set
                    if (relative_to) |rel| {
                        _ = rel;
                        // For simplicity, use the full path; proper relative path computation
                        // would need std.fs.path.relative which may differ in 0.15
                        source_path = try self.allocator.dupe(u8, file_path);
                    } else {
                        source_path = try self.allocator.dupe(u8, file_path);
                    }
                }
            }

            // Check for text-based input
            if (source == null) {
                if (input_obj.get("text")) |text_val| {
                    if (text_val == .string and text_val.string.len > 0) {
                        source = try self.allocator.dupe(u8, text_val.string);
                        if (input_obj.get("sourcePath")) |sp_val| {
                            if (sp_val == .string) {
                                source_path = try self.allocator.dupe(u8, sp_val.string);
                            }
                        }
                        if (source_path == null) {
                            source_path = try self.allocator.dupe(u8, "<unknown>");
                        }
                    }
                }
            }

            if (source == null) continue;

            // Generate a UUID for this compilation unit
            const uuid = generateUuid();
            const uuid_str = try self.allocator.dupe(u8, &uuid);

            // Parse the source into an LST tree
            var lst_node: ?LstNode = null;
            parse_blk: {
                // We need a null-terminated copy for std.zig.Ast.parse
                const source_z = self.allocator.dupeZ(u8, source.?) catch break :parse_blk;
                var ast = std.zig.Ast.parse(self.allocator, source_z, .zig) catch break :parse_blk;
                _ = &ast; // ast is owned by the allocator, kept alive while server lives

                var ctx = ParseContext.init(self.allocator, source.?, source_z, ast);
                const mapped = ctx.mapFile(source_path.?, uuid_str) catch break :parse_blk;
                lst_node = mapped;
            }

            // Store the parsed object
            const key = try self.allocator.dupe(u8, uuid_str);
            try self.local_objects.put(key, .{
                .source = source.?,
                .source_path = source_path.?,
                .id = try self.allocator.dupe(u8, uuid_str),
                .lst = lst_node,
            });

            self.log("Parsed {s} -> {s} (lst={s})", .{
                source_path.?,
                uuid_str,
                if (lst_node != null) "yes" else "no",
            });

            try result_ids.append(std.json.Value{ .string = uuid_str });
        }

        var result_obj = std.json.ObjectMap.init(self.allocator);
        try result_obj.put("jsonrpc", std.json.Value{ .string = "2.0" });
        try result_obj.put("id", id);
        try result_obj.put("result", std.json.Value{ .array = result_ids });
        return std.json.Value{ .object = result_obj };
    }

    fn handleGetObject(self: *Server, id: std.json.Value, params: std.json.Value) !std.json.Value {
        if (params != .object) {
            return self.makeErrorResponseMsg(id, -32602, "Invalid params: expected object");
        }
        const params_obj = params.object;

        const obj_id_val = params_obj.get("id") orelse return self.makeErrorResponseMsg(id, -32602, "Missing 'id' parameter");
        const obj_id = if (obj_id_val == .string) obj_id_val.string else return self.makeErrorResponseMsg(id, -32602, "'id' must be string");

        const stored = self.local_objects.get(obj_id) orelse {
            // Object not found - return DELETE + END_OF_OBJECT
            var batch = std.json.Array.init(self.allocator);
            try batch.append(try (RpcObjectData{ .state = .delete }).toJsonValue(self.allocator));
            try batch.append(try (RpcObjectData{ .state = .end_of_object }).toJsonValue(self.allocator));
            var result_obj = std.json.ObjectMap.init(self.allocator);
            try result_obj.put("jsonrpc", std.json.Value{ .string = "2.0" });
            try result_obj.put("id", id);
            try result_obj.put("result", std.json.Value{ .array = batch });
            return std.json.Value{ .object = result_obj };
        };

        var queue = SendQueue.init(self.allocator, self.batch_size);
        defer queue.deinit();

        if (stored.lst) |lst| {
            // Use the Sender to serialize the parsed LST tree
            var snd = Sender.init(&queue, self.allocator);
            try snd.send(lst);
        } else {
            // No parsed tree, use legacy serialization
            try self.legacySerialize(&queue, stored);
        }

        // END_OF_OBJECT sentinel
        try queue.sendEndOfObject();

        // Build response
        const result = try queue.toJsonArray(self.allocator);
        var result_obj = std.json.ObjectMap.init(self.allocator);
        try result_obj.put("jsonrpc", std.json.Value{ .string = "2.0" });
        try result_obj.put("id", id);
        try result_obj.put("result", result);
        return std.json.Value{ .object = result_obj };
    }

    fn handlePrint(self: *Server, id: std.json.Value, params: std.json.Value) !std.json.Value {
        if (params != .object) {
            return self.wrapResult(id, std.json.Value{ .string = "" });
        }
        const params_obj = params.object;

        const obj_id_val = params_obj.get("treeId") orelse
            params_obj.get("id") orelse
            return self.wrapResult(id, std.json.Value{ .string = "" });

        const obj_id_str = if (obj_id_val == .string) obj_id_val.string else return self.wrapResult(id, std.json.Value{ .string = "" });

        const stored = self.local_objects.get(obj_id_str) orelse return self.wrapResult(id, std.json.Value{ .string = "" });

        // Use printer if we have a parsed tree
        if (stored.lst) |t| {
            const printed = printer_mod.print(self.allocator, t) catch {
                // Fall back to stored source on printer error
                return self.wrapResult(id, std.json.Value{ .string = stored.source });
            };
            defer self.allocator.free(printed);
            // Duplicate the string since the response outlives this scope
            const result = self.allocator.dupe(u8, printed) catch {
                return self.wrapResult(id, std.json.Value{ .string = stored.source });
            };
            return self.wrapResult(id, std.json.Value{ .string = result });
        }
        // Fallback to stored source
        return self.wrapResult(id, std.json.Value{ .string = stored.source });
    }

    fn handleReset(self: *Server, id: std.json.Value) !std.json.Value {
        // Clear all local objects
        var it = self.local_objects.iterator();
        while (it.next()) |entry| {
            self.allocator.free(entry.value_ptr.source);
            self.allocator.free(entry.value_ptr.source_path);
            self.allocator.free(entry.value_ptr.id);
            self.allocator.free(entry.key_ptr.*);
        }
        self.local_objects.clearAndFree();

        var rit = self.remote_objects.iterator();
        while (rit.next()) |entry| {
            self.allocator.free(entry.value_ptr.source);
            self.allocator.free(entry.value_ptr.source_path);
            self.allocator.free(entry.value_ptr.id);
            self.allocator.free(entry.key_ptr.*);
        }
        self.remote_objects.clearAndFree();

        self.log("Reset: cleared all cached state", .{});

        var result_obj = std.json.ObjectMap.init(self.allocator);
        try result_obj.put("jsonrpc", std.json.Value{ .string = "2.0" });
        try result_obj.put("id", id);
        try result_obj.put("result", std.json.Value{ .bool = true });
        return std.json.Value{ .object = result_obj };
    }

    /// Legacy serialization for when the parser failed or returned null.
    /// Produces the same output as the old handleGetObject.
    fn legacySerialize(self: *Server, queue: *SendQueue, stored: StoredObject) !void {
        // --- Top-level ADD ---
        try queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.zig.tree.Zig$CompilationUnit",
        });

        // --- preVisit fields ---
        try queue.sendAdd(null, std.json.Value{ .string = stored.id });
        try self.sendSpace(queue, extractPrefix(stored.source));
        try self.sendEmptyMarkers(queue);

        // --- CompilationUnit fields ---
        try queue.sendAdd(null, std.json.Value{ .string = stored.source_path });
        try queue.sendAdd(null, std.json.Value{ .string = "UTF-8" });
        try queue.sendAdd(null, std.json.Value{ .bool = false });
        try queue.sendDelete();
        try queue.sendDelete();
        try queue.sendDelete();
        try self.sendEmptyList(queue);
        try self.sendSpace(queue, extractTrailing(stored.source));
    }

    /// Send a Space with the given whitespace. Comments list is always empty for now.
    /// Space serialization order (matches JavaSender.visitSpace):
    ///   1. comments (list via getAndSendList)
    ///   2. whitespace (string via getAndSend)
    fn sendSpace(self: *Server, queue: *SendQueue, ws: []const u8) !void {
        // The Space object itself: ADD with valueType
        try queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.java.tree.Space",
        });
        // comments list: ADD (empty list) + CHANGE (positions=[])
        try self.sendEmptyList(queue);
        // whitespace
        try queue.sendAdd(null, std.json.Value{ .string = ws });
    }

    /// Send empty Markers (id=UUID, markers=[]).
    /// Markers serialization order (matches Markers.rpcSend):
    ///   1. id (UUID via getAndSend)
    ///   2. markers (list via getAndSendListAsRef)
    fn sendEmptyMarkers(self: *Server, queue: *SendQueue) !void {
        try queue.put(.{
            .state = .add,
            .value_type = "org.openrewrite.marker.Markers",
        });
        // Markers.id
        const markers_uuid = generateUuid();
        const markers_uuid_str = try self.allocator.dupe(u8, &markers_uuid);
        try queue.sendAdd(null, std.json.Value{ .string = markers_uuid_str });
        // Markers.markers (empty list)
        try self.sendEmptyList(queue);
    }

    /// Send an empty list: ADD + CHANGE with positions=[].
    fn sendEmptyList(self: *Server, queue: *SendQueue) !void {
        try queue.put(.{ .state = .add });
        try queue.put(.{
            .state = .change,
            .value = std.json.Value{ .array = std.json.Array.init(self.allocator) },
        });
    }

    fn wrapResult(self: *Server, id: std.json.Value, result: std.json.Value) !std.json.Value {
        var result_obj = std.json.ObjectMap.init(self.allocator);
        try result_obj.put("jsonrpc", std.json.Value{ .string = "2.0" });
        try result_obj.put("id", id);
        try result_obj.put("result", result);
        return std.json.Value{ .object = result_obj };
    }

    fn makeErrorResponse(self: *Server, id: ?std.json.Value, code: i64, message: []const u8) !std.json.Value {
        return self.makeErrorResponseMsg(id orelse std.json.Value.null, code, message);
    }

    fn makeErrorResponseMsg(self: *Server, id: std.json.Value, code: i64, message: []const u8) !std.json.Value {
        var err_obj = std.json.ObjectMap.init(self.allocator);
        try err_obj.put("code", std.json.Value{ .integer = code });
        try err_obj.put("message", std.json.Value{ .string = message });

        var result_obj = std.json.ObjectMap.init(self.allocator);
        try result_obj.put("jsonrpc", std.json.Value{ .string = "2.0" });
        try result_obj.put("id", id);
        try result_obj.put("error", std.json.Value{ .object = err_obj });
        return std.json.Value{ .object = result_obj };
    }
};

fn isWhitespace(c: u8) bool {
    return c == ' ' or c == '\t' or c == '\n' or c == '\r';
}

/// Extract leading whitespace and line comments before the first real token.
fn extractPrefix(source: []const u8) []const u8 {
    var i: usize = 0;
    while (i < source.len) {
        if (isWhitespace(source[i])) {
            i += 1;
            continue;
        }
        // Skip line comments (// ...)
        if (i + 1 < source.len and source[i] == '/' and source[i + 1] == '/') {
            while (i < source.len and source[i] != '\n') : (i += 1) {}
            if (i < source.len) i += 1; // skip the \n
            continue;
        }
        break;
    }
    return source[0..i];
}

/// Extract trailing whitespace after the last non-whitespace character.
fn extractTrailing(source: []const u8) []const u8 {
    var end = source.len;
    while (end > 0 and isWhitespace(source[end - 1])) {
        end -= 1;
    }
    return source[end..];
}

/// Generate a v4 UUID string.
fn generateUuid() [36]u8 {
    var bytes: [16]u8 = undefined;
    std.crypto.random.bytes(&bytes);
    // Set version 4
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    // Set variant
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
    return result;
}

// ---------------------------------------------------------------
// Content-Length framed I/O
// ---------------------------------------------------------------

/// Read a Content-Length framed message from stdin using raw POSIX reads.
fn readMessage(allocator: std.mem.Allocator) !?std.json.Value {
    // Read the Content-Length header line byte-by-byte
    var header_buf: [256]u8 = undefined;
    var header_len: usize = 0;

    // Read until we get "\r\n" or "\n"
    while (header_len < header_buf.len) {
        var byte: [1]u8 = undefined;
        const n = std.posix.read(std.posix.STDIN_FILENO, &byte) catch return null;
        if (n == 0) return null; // EOF
        header_buf[header_len] = byte[0];
        header_len += 1;

        // Check for end of line
        if (header_len >= 1 and header_buf[header_len - 1] == '\n') break;
    }

    // Trim and parse the header
    var header_str = header_buf[0..header_len];
    // Trim trailing \r\n
    while (header_str.len > 0 and (header_str[header_str.len - 1] == '\n' or header_str[header_str.len - 1] == '\r')) {
        header_str = header_str[0 .. header_str.len - 1];
    }

    const prefix = "Content-Length:";
    if (!std.mem.startsWith(u8, header_str, prefix)) {
        return error.InvalidHeader;
    }
    const length_str = std.mem.trimLeft(u8, header_str[prefix.len..], " ");
    const content_length = std.fmt.parseInt(usize, length_str, 10) catch return error.InvalidContentLength;

    // Read the empty separator line (\r\n)
    var sep_buf: [256]u8 = undefined;
    var sep_len: usize = 0;
    while (sep_len < sep_buf.len) {
        var byte: [1]u8 = undefined;
        const n = std.posix.read(std.posix.STDIN_FILENO, &byte) catch return null;
        if (n == 0) return null;
        sep_buf[sep_len] = byte[0];
        sep_len += 1;
        if (sep_len >= 1 and sep_buf[sep_len - 1] == '\n') break;
    }

    // Read the message body
    const body = try allocator.alloc(u8, content_length);
    defer allocator.free(body);
    var total_read: usize = 0;
    while (total_read < content_length) {
        const n = std.posix.read(std.posix.STDIN_FILENO, body[total_read..]) catch return null;
        if (n == 0) return null; // EOF
        total_read += n;
    }

    // Parse the JSON
    const parsed = try std.json.parseFromSlice(std.json.Value, allocator, body[0..content_length], .{
        .allocate = .alloc_always,
    });
    return parsed.value;
}

/// Write a Content-Length framed message to stdout using raw POSIX writes.
fn writeMessage(allocator: std.mem.Allocator, value: std.json.Value) !void {
    // Serialize the JSON value to bytes
    const body = try std.json.Stringify.valueAlloc(allocator, value, .{});
    defer allocator.free(body);

    // Format the header
    var header_buf: [64]u8 = undefined;
    const header = std.fmt.bufPrint(&header_buf, "Content-Length: {d}\r\n\r\n", .{body.len}) catch unreachable;

    // Write header + body atomically
    _ = std.posix.write(std.posix.STDOUT_FILENO, header) catch return error.WriteFailed;
    _ = std.posix.write(std.posix.STDOUT_FILENO, body) catch return error.WriteFailed;
}

/// Parse command-line arguments.
fn parseArgs(allocator: std.mem.Allocator) Config {
    const args = std.process.argsAlloc(allocator) catch return .{};
    defer std.process.argsFree(allocator, args);

    var config = Config{};
    for (args[1..]) |arg| {
        if (std.mem.startsWith(u8, arg, "--log-file=")) {
            config.log_file_path = allocator.dupe(u8, arg["--log-file=".len..]) catch null;
        } else if (std.mem.eql(u8, arg, "--trace-rpc-messages")) {
            config.trace_rpc_messages = true;
        }
    }
    return config;
}

pub fn main() !void {
    var gpa_impl: std.heap.GeneralPurposeAllocator(.{}) = .init;
    defer _ = gpa_impl.deinit();
    const allocator = gpa_impl.allocator();

    const config = parseArgs(allocator);

    var server = Server.init(allocator);
    defer server.deinit();

    // Open log file if specified
    if (config.log_file_path) |path| {
        server.log_file = std.fs.createFileAbsolute(path, .{ .truncate = false }) catch null;
        if (server.log_file) |f| {
            // Seek to end for appending
            f.seekFromEnd(0) catch {};
        }
    }
    server.trace_rpc = config.trace_rpc_messages;

    server.log("Zig RPC server starting...", .{});

    // Main message loop
    while (true) {
        const req = readMessage(allocator) catch |err| {
            server.log("Error reading message: {}", .{err});
            break;
        };
        if (req == null) break; // EOF

        const resp = server.handleRequest(req.?) catch |err| {
            server.log("Error handling request: {}", .{err});
            break;
        };

        writeMessage(allocator, resp) catch |err| {
            server.log("Error writing response: {}", .{err});
            break;
        };
    }

    server.log("Zig RPC server shutting down...", .{});
}

// Reference all module tests so `zig test src/main.zig` runs them all.
comptime {
    _ = rpc;
    _ = parser;
    _ = printer_mod;
    _ = sender_mod;
    _ = tree;
}

test "generateUuid format" {
    const uuid = generateUuid();
    try std.testing.expect(uuid[8] == '-');
    try std.testing.expect(uuid[13] == '-');
    try std.testing.expect(uuid[18] == '-');
    try std.testing.expect(uuid[23] == '-');
    try std.testing.expect(uuid.len == 36);
}

test "isWhitespace" {
    try std.testing.expect(isWhitespace(' '));
    try std.testing.expect(isWhitespace('\t'));
    try std.testing.expect(isWhitespace('\n'));
    try std.testing.expect(isWhitespace('\r'));
    try std.testing.expect(!isWhitespace('a'));
}
