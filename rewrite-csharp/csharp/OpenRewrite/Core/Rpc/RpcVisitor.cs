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
