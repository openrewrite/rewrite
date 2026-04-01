/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.Core;

/// <summary>
/// Markers for annotating LST nodes with error, warning, info, or debug messages.
/// </summary>
public abstract class Markup(Guid id, string message, string? detail) : Marker, IEquatable<Markup>
{
    public Guid Id { get; } = id;
    public string Message { get; } = message;
    public string? Detail { get; } = detail;

    public bool Equals(Markup? other) => other is not null && Id == other.Id;
    public override bool Equals(object? obj) => Equals(obj as Markup);
    public override int GetHashCode() => Id.GetHashCode();

    private static T AddMarker<T>(T tree, Markup marker) where T : J
    {
        var newMarkers = tree.Markers.Add(marker);
        var withMarkers = tree.GetType().GetMethod("WithMarkers", [typeof(Markers)]);
        return withMarkers != null ? (T)withMarkers.Invoke(tree, [newMarkers])! : tree;
    }

    /// <summary>
    /// Adds an Error markup marker to the given tree node.
    /// </summary>
    public static T CreateError<T>(T tree, string message, string? detail = null) where T : J
        => AddMarker(tree, new Error(Guid.NewGuid(), message, detail));

    /// <summary>
    /// Adds a Warn markup marker to the given tree node.
    /// </summary>
    public static T CreateWarn<T>(T tree, string message, string? detail = null) where T : J
        => AddMarker(tree, new Warn(Guid.NewGuid(), message, detail));

    /// <summary>
    /// Adds an Info markup marker to the given tree node.
    /// </summary>
    public static T CreateInfo<T>(T tree, string message, string? detail = null) where T : J
        => AddMarker(tree, new Info(Guid.NewGuid(), message, detail));

    /// <summary>
    /// Adds a Debug markup marker to the given tree node.
    /// </summary>
    public static T CreateDebug<T>(T tree, string message, string? detail = null) where T : J
        => AddMarker(tree, new Debug(Guid.NewGuid(), message, detail));

    public sealed class Error(Guid id, string message, string? detail) : Markup(id, message, detail), IRpcCodec<Error>
    {
        public void RpcSend(Error after, RpcSendQueue q)
        {
            q.GetAndSend(after, m => m.Id);
            q.GetAndSend(after, m => m.Message);
            q.GetAndSend(after, m => m.Detail);
        }

        public Error RpcReceive(Error before, RpcReceiveQueue q)
        {
            var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
            var message = q.Receive(before.Message);
            var detail = q.Receive(before.Detail);
            return new Error(id, message!, detail);
        }
    }

    public sealed class Warn(Guid id, string message, string? detail) : Markup(id, message, detail), IRpcCodec<Warn>
    {
        public void RpcSend(Warn after, RpcSendQueue q)
        {
            q.GetAndSend(after, m => m.Id);
            q.GetAndSend(after, m => m.Message);
            q.GetAndSend(after, m => m.Detail);
        }

        public Warn RpcReceive(Warn before, RpcReceiveQueue q)
        {
            var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
            var message = q.Receive(before.Message);
            var detail = q.Receive(before.Detail);
            return new Warn(id, message!, detail);
        }
    }

    public sealed class Info(Guid id, string message, string? detail) : Markup(id, message, detail), IRpcCodec<Info>
    {
        public void RpcSend(Info after, RpcSendQueue q)
        {
            q.GetAndSend(after, m => m.Id);
            q.GetAndSend(after, m => m.Message);
            q.GetAndSend(after, m => m.Detail);
        }

        public Info RpcReceive(Info before, RpcReceiveQueue q)
        {
            var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
            var message = q.Receive(before.Message);
            var detail = q.Receive(before.Detail);
            return new Info(id, message!, detail);
        }
    }

    public sealed class Debug(Guid id, string message, string? detail) : Markup(id, message, detail), IRpcCodec<Debug>
    {
        public void RpcSend(Debug after, RpcSendQueue q)
        {
            q.GetAndSend(after, m => m.Id);
            q.GetAndSend(after, m => m.Message);
            q.GetAndSend(after, m => m.Detail);
        }

        public Debug RpcReceive(Debug before, RpcReceiveQueue q)
        {
            var id = q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse);
            var message = q.Receive(before.Message);
            var detail = q.Receive(before.Detail);
            return new Debug(id, message!, detail);
        }
    }
}
