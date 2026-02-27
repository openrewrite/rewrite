using OpenRewrite.Core.Rpc;
using OpenRewrite.Java.Rpc;

namespace OpenRewrite.Core;

/// <summary>
/// Marker indicating a trailing comma after the last element in a separated list
/// (enum members, initializer expressions, switch arms, etc.).
/// The <see cref="Suffix"/> captures the whitespace/comments between the trailing
/// comma and the closing delimiter (e.g., '}').
/// </summary>
public sealed class TrailingComma(Guid id, Space suffix) : Marker, IRpcCodec<TrailingComma>
{
    public Guid Id { get; } = id;
    public Space Suffix { get; } = suffix;

    public TrailingComma WithId(Guid id) => id == Id ? this : new(id, Suffix);
    public TrailingComma WithSuffix(Space suffix) => ReferenceEquals(suffix, Suffix) ? this : new(Id, suffix);

    public void RpcSend(TrailingComma after, RpcSendQueue q)
    {
        q.GetAndSend(after, m => m.Id);
        q.GetAndSend(after, m => m.Suffix, s => new JavaSender().VisitSpace(s, q));
    }

    public TrailingComma RpcReceive(TrailingComma before, RpcReceiveQueue q) =>
        before
            .WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse))
            .WithSuffix(q.Receive(before.Suffix, s => new JavaReceiver().VisitSpace(s, q)));
}
