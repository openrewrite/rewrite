using Rewrite.Core.Rpc;

namespace Rewrite.Core;

/// <summary>
/// Markers for annotating LST nodes with error, warning, info, or debug messages.
/// </summary>
public abstract record Markup(Guid Id, string Message, string? Detail) : Marker
{
    public sealed record Error(Guid Id, string Message, string? Detail) : Markup(Id, Message, Detail), IRpcCodec<Error>
    {
        public void RpcSend(Error after, RpcSendQueue q)
        {
            q.GetAndSend(after, m => m.Id);
            q.GetAndSend(after, m => m.Message);
            q.GetAndSend(after, m => m.Detail);
        }

        public Error RpcReceive(Error before, RpcReceiveQueue q)
        {
            throw new NotImplementedException("Markup.Error.RpcReceive");
        }
    }

    public sealed record Warn(Guid Id, string Message, string? Detail) : Markup(Id, Message, Detail), IRpcCodec<Warn>
    {
        public void RpcSend(Warn after, RpcSendQueue q)
        {
            q.GetAndSend(after, m => m.Id);
            q.GetAndSend(after, m => m.Message);
            q.GetAndSend(after, m => m.Detail);
        }

        public Warn RpcReceive(Warn before, RpcReceiveQueue q)
        {
            throw new NotImplementedException("Markup.Warn.RpcReceive");
        }
    }

    public sealed record Info(Guid Id, string Message, string? Detail) : Markup(Id, Message, Detail), IRpcCodec<Info>
    {
        public void RpcSend(Info after, RpcSendQueue q)
        {
            q.GetAndSend(after, m => m.Id);
            q.GetAndSend(after, m => m.Message);
            q.GetAndSend(after, m => m.Detail);
        }

        public Info RpcReceive(Info before, RpcReceiveQueue q)
        {
            throw new NotImplementedException("Markup.Info.RpcReceive");
        }
    }

    public sealed record Debug(Guid Id, string Message, string? Detail) : Markup(Id, Message, Detail), IRpcCodec<Debug>
    {
        public void RpcSend(Debug after, RpcSendQueue q)
        {
            q.GetAndSend(after, m => m.Id);
            q.GetAndSend(after, m => m.Message);
            q.GetAndSend(after, m => m.Detail);
        }

        public Debug RpcReceive(Debug before, RpcReceiveQueue q)
        {
            throw new NotImplementedException("Markup.Debug.RpcReceive");
        }
    }
}
