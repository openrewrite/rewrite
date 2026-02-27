using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Core;

/// <summary>
/// Marker indicating that a member access uses the pointer dereference operator (<c>-&gt;</c>)
/// instead of the dot operator (<c>.</c>).
/// Applied to <c>PointerDereference</c> markers when the expression uses <c>-&gt;</c>.
/// </summary>
public sealed class PointerMemberAccess(Guid id) : Marker, IRpcCodec<PointerMemberAccess>
{
    public Guid Id { get; } = id;

    public PointerMemberAccess WithId(Guid id) =>
        id == Id ? this : new(id);

    public static PointerMemberAccess Instance { get; } = new(Guid.Empty);

    public void RpcSend(PointerMemberAccess after, RpcSendQueue q) => q.GetAndSend(after, m => m.Id);
    public PointerMemberAccess RpcReceive(PointerMemberAccess before, RpcReceiveQueue q) =>
        before.WithId(q.ReceiveAndGet<Guid, string>(before.Id, Guid.Parse));
}
