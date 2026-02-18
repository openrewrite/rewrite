using OpenRewrite.Core.Rpc;

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
