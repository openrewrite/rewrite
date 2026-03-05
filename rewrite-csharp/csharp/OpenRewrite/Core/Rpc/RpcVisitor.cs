using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.Core.Rpc;

/// <summary>
/// A visitor that delegates to a named visitor on the Java RPC peer.
/// Mirrors TypeScript's RpcVisitor — calls StopAfterPreVisit in PreVisit,
/// then sends the tree to Java via Visit RPC.
/// </summary>
public class RpcVisitor : JavaVisitor<ExecutionContext>
{
    private readonly RewriteRpcServer _rpc;
    private readonly string _visitorName;

    public RpcVisitor(RewriteRpcServer rpc, string visitorName)
    {
        _rpc = rpc;
        _visitorName = visitorName;
    }

    public override J? PreVisit(J tree, ExecutionContext ctx)
    {
        StopAfterPreVisit();

        if (tree is not SourceFile sf)
            return tree;

        var treeId = sf.Id.ToString();
        _rpc.StoreLocalObject(treeId, sf);

        var result = _rpc.VisitOnRemote(_visitorName, treeId, "Cs");
        return result as J;
    }
}
