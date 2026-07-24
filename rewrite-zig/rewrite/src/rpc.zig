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

const std = @import("std");

/// State represents the state of an RPC object data message.
/// Matches the Java RpcObjectData.State enum.
pub const State = enum {
    no_change,
    add,
    delete,
    change,
    end_of_object,

    pub fn toString(self: State) []const u8 {
        return switch (self) {
            .no_change => "NO_CHANGE",
            .add => "ADD",
            .delete => "DELETE",
            .change => "CHANGE",
            .end_of_object => "END_OF_OBJECT",
        };
    }

    pub fn fromString(s: []const u8) State {
        if (std.mem.eql(u8, s, "NO_CHANGE")) return .no_change;
        if (std.mem.eql(u8, s, "ADD")) return .add;
        if (std.mem.eql(u8, s, "DELETE")) return .delete;
        if (std.mem.eql(u8, s, "CHANGE")) return .change;
        if (std.mem.eql(u8, s, "END_OF_OBJECT")) return .end_of_object;
        return .no_change;
    }
};

/// RpcObjectData is the wire format for RPC messages.
/// Each field of each AST node is sent as a separate RpcObjectData message.
pub const RpcObjectData = struct {
    state: State,
    value_type: ?[]const u8 = null,
    value: ?std.json.Value = null,
    ref: ?i64 = null,

    /// Serialize to a JSON value suitable for inclusion in a JSON array.
    pub fn toJsonValue(self: RpcObjectData, allocator: std.mem.Allocator) !std.json.Value {
        var obj = std.json.ObjectMap.init(allocator);
        try obj.put("state", std.json.Value{ .string = self.state.toString() });

        if (self.value_type) |vt| {
            try obj.put("valueType", std.json.Value{ .string = vt });
        } else {
            try obj.put("valueType", std.json.Value.null);
        }

        if (self.value) |v| {
            try obj.put("value", v);
        } else {
            try obj.put("value", std.json.Value.null);
        }

        if (self.ref) |r| {
            try obj.put("ref", std.json.Value{ .integer = r });
        } else {
            try obj.put("ref", std.json.Value.null);
        }

        return std.json.Value{ .object = obj };
    }

    /// Parse an RpcObjectData from a JSON object.
    pub fn fromJsonValue(val: std.json.Value) RpcObjectData {
        var result = RpcObjectData{ .state = .no_change };

        if (val != .object) return result;
        const obj = val.object;

        if (obj.get("state")) |s| {
            if (s == .string) {
                result.state = State.fromString(s.string);
            }
        }

        if (obj.get("valueType")) |vt| {
            if (vt == .string) {
                result.value_type = vt.string;
            }
        }

        if (obj.get("value")) |v| {
            if (v != .null) {
                result.value = v;
            }
        }

        if (obj.get("ref")) |r| {
            if (r == .integer) {
                result.ref = r.integer;
            }
        }

        return result;
    }
};

/// SendQueue collects RpcObjectData messages into batches for transmission.
pub const SendQueue = struct {
    batch: std.ArrayListUnmanaged(RpcObjectData),
    batch_size: usize,
    allocator: std.mem.Allocator,

    pub fn init(allocator: std.mem.Allocator, batch_size: usize) SendQueue {
        return SendQueue{
            .batch = .{},
            .batch_size = batch_size,
            .allocator = allocator,
        };
    }

    pub fn deinit(self: *SendQueue) void {
        self.batch.deinit(self.allocator);
    }

    /// Add a message to the batch.
    pub fn put(self: *SendQueue, data: RpcObjectData) !void {
        try self.batch.append(self.allocator, data);
    }

    /// Send a simple value with ADD state.
    pub fn sendAdd(self: *SendQueue, value_type: ?[]const u8, value: ?std.json.Value) !void {
        try self.put(.{
            .state = .add,
            .value_type = value_type,
            .value = value,
        });
    }

    /// Send a NO_CHANGE state.
    pub fn sendNoChange(self: *SendQueue) !void {
        try self.put(.{ .state = .no_change });
    }

    /// Send a DELETE state.
    pub fn sendDelete(self: *SendQueue) !void {
        try self.put(.{ .state = .delete });
    }

    /// Send an END_OF_OBJECT sentinel.
    pub fn sendEndOfObject(self: *SendQueue) !void {
        try self.put(.{ .state = .end_of_object });
    }

    /// Get all accumulated messages as a JSON array.
    pub fn toJsonArray(self: *const SendQueue, allocator: std.mem.Allocator) !std.json.Value {
        var arr = std.json.Array.init(allocator);
        for (self.batch.items) |item| {
            const json_val = try item.toJsonValue(allocator);
            try arr.append(json_val);
        }
        return std.json.Value{ .array = arr };
    }

    /// Clear the batch.
    pub fn clear(self: *SendQueue) void {
        self.batch.clearRetainingCapacity();
    }
};

test "State round-trip" {
    const states = [_]State{ .no_change, .add, .delete, .change, .end_of_object };
    for (states) |s| {
        const str = s.toString();
        const parsed = State.fromString(str);
        try std.testing.expectEqual(s, parsed);
    }
}

test "RpcObjectData toJson and fromJson" {
    const allocator = std.testing.allocator;
    const data = RpcObjectData{
        .state = .add,
        .value_type = "org.openrewrite.zig.tree.Zig$CompilationUnit",
        .value = null,
        .ref = 1,
    };
    const json_val = try data.toJsonValue(allocator);
    defer {
        var val = json_val;
        val.object.deinit();
    }
    const parsed = RpcObjectData.fromJsonValue(json_val);
    try std.testing.expectEqual(State.add, parsed.state);
    try std.testing.expectEqualStrings("org.openrewrite.zig.tree.Zig$CompilationUnit", parsed.value_type.?);
    try std.testing.expectEqual(@as(?i64, 1), parsed.ref);
}
