using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Core;

/// <summary>
/// Marker on a Block indicating that braces should not be printed.
/// Used for single-statement bodies (lock, using, etc.) where the original
/// source had no braces but the AST wraps the statement in a Block.
/// </summary>
public sealed class OmitBraces(Guid id) : Marker, IRpcCodec<OmitBraces>
{
    public Guid Id { get; } = id;

    public OmitBraces WithId(Guid id) =>
        id == Id ? this : new(id);

    public static OmitBraces Instance { get; } = new(Guid.Empty);

    public void RpcSend(OmitBraces after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public OmitBraces RpcReceive(OmitBraces before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));
}
