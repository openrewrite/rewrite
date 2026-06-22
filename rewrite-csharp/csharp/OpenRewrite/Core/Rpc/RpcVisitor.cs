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

namespace OpenRewrite.Core.Rpc;

/// <summary>
/// A visitor that delegates to a named visitor on the Java RPC peer.
/// Handles any tree type (C#, XML, etc.) by determining the correct
/// Java source file type from the C# tree's type.
/// </summary>
public class RpcVisitor : TreeVisitor<Tree, ExecutionContext>
{
    private readonly RewriteRpcServer _rpc;
    private readonly string _visitorName;

    public string VisitorName => _visitorName;

    public RpcVisitor(RewriteRpcServer rpc, string visitorName)
    {
        _rpc = rpc;
        _visitorName = visitorName;
    }

    public override Tree? PreVisit(Tree tree, ExecutionContext ctx)
    {
        StopAfterPreVisit();

        if (tree is not SourceFile sf)
            return tree;

        var treeId = sf.Id.ToString();
        _rpc.StoreLocalObject(treeId, sf);

        var ctxId = Guid.NewGuid().ToString();
        _rpc.StoreLocalObject(ctxId, ctx);

        var sourceFileType = RpcSendQueue.ToJavaTypeName(sf.GetType())
                             ?? "org.openrewrite.csharp.tree.Cs$CompilationUnit";
        try
        {
            return _rpc.VisitOnRemote(_visitorName, treeId, sourceFileType, ctxId);
        }
        catch (Exception ex)
        {
            // A Java-side visitor may legitimately be unable to handle this tree
            // type (e.g. a JavaVisitor asked to visit an Xml.Document), in which
            // case skipping it and leaving the tree unchanged is correct. But a
            // serialization/protocol failure also surfaces here, and silently
            // swallowing every exception hides real bugs (it masked a wire-format
            // mismatch as a no-op for some time). Log before continuing so the
            // failure is diagnosable without aborting the whole recipe run.
            Serilog.Log.Debug(ex,
                "RPC visit delegation to {VisitorName} failed for {SourcePath} ({SourceFileType}); leaving the tree unchanged",
                _visitorName, sf.SourcePath, sourceFileType);
            return tree;
        }
    }
}
