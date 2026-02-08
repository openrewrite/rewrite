namespace Rewrite.Core.Rpc;

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

public static class RpcCodec
{
    private static readonly Dictionary<string, List<DynamicDispatchRpcCodec>> CodecsByType = new();

    public static void Register(DynamicDispatchRpcCodec codec)
    {
        if (!CodecsByType.TryGetValue(codec.SourceFileType, out var list))
        {
            list = [];
            CodecsByType[codec.SourceFileType] = list;
        }
        list.Add(codec);
    }

    public static IRpcCodec? ForInstance(object t, string? sourceFileType)
    {
        if (sourceFileType != null && CodecsByType.TryGetValue(sourceFileType, out var codecs))
        {
            foreach (var codec in codecs)
            {
                if (codec.MatchType.IsAssignableFrom(t.GetType()))
                {
                    return codec;
                }
            }
        }

        if (t is IRpcCodec selfCodec)
        {
            return selfCodec;
        }

        return null;
    }
}

public abstract class DynamicDispatchRpcCodec : IRpcCodec
{
    public abstract string SourceFileType { get; }
    public abstract Type MatchType { get; }
    public abstract void RpcSend(object after, RpcSendQueue q);
    public abstract object RpcReceive(object before, RpcReceiveQueue q);
}
