namespace OpenRewrite.Core.Rpc;

public interface IRpcCodec
{
    void RpcSend(object after, RpcSendQueue q);
    object RpcReceive(object before, RpcReceiveQueue q);
}

public interface IRpcCodec<T> : IRpcCodec
{
    void RpcSend(T after, RpcSendQueue q);
    T RpcReceive(T before, RpcReceiveQueue q);

    void IRpcCodec.RpcSend(object after, RpcSendQueue q) => RpcSend((T)after, q);
    object IRpcCodec.RpcReceive(object before, RpcReceiveQueue q) => RpcReceive((T)before, q)!;
}
