using Rewrite.Core.Rpc;
using Rewrite.Java;

namespace Rewrite.CSharp.Rpc;

public class CSharpRpcCodec : DynamicDispatchRpcCodec
{
    public override string SourceFileType => "org.openrewrite.csharp.tree.Cs$CompilationUnit";

    public override Type MatchType => typeof(J);

    public override void RpcSend(object after, RpcSendQueue q)
    {
        new CSharpSender().Visit((J)after, q);
    }

    public override object RpcReceive(object before, RpcReceiveQueue q)
    {
        // Receive-side not yet implemented
        throw new NotImplementedException("CSharpRpcCodec.RpcReceive");
    }
}
