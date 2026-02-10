using Rewrite.Core;
using Rewrite.Core.Rpc;
using Rewrite.CSharp.Rpc;

namespace Rewrite.Java.Search;

/// <summary>
/// Convenience functions for common precondition visitors.
/// When connected to Java via RPC, delegates to Java's implementations.
/// Otherwise falls back to local implementations.
/// </summary>
public static class Preconditions
{
    /// <summary>
    /// Creates a UsesType precondition. If connected to Java via RPC, delegates to
    /// Java's org.openrewrite.java.search.HasType. Otherwise falls back to local implementation.
    /// </summary>
    public static JavaVisitor<Core.ExecutionContext> UsesType(string fullyQualifiedTypeName)
    {
        var rpc = RewriteRpcServer.Current;
        if (rpc != null)
        {
            var response = rpc.PrepareRecipeOnRemote(
                "org.openrewrite.java.search.HasType",
                new Dictionary<string, object?>
                {
                    ["fullyQualifiedType"] = fullyQualifiedTypeName,
                    ["checkAssignability"] = false
                }
            );
            return new RpcVisitor(rpc, response.EditVisitor);
        }

        return new LocalUsesType<Core.ExecutionContext>(fullyQualifiedTypeName);
    }

    /// <summary>
    /// Creates a UsesMethod precondition. If connected to Java via RPC, delegates to
    /// Java's org.openrewrite.java.search.HasMethod.
    /// </summary>
    public static JavaVisitor<Core.ExecutionContext> UsesMethod(string methodPattern)
    {
        var rpc = RewriteRpcServer.Current;
        if (rpc != null)
        {
            var response = rpc.PrepareRecipeOnRemote(
                "org.openrewrite.java.search.HasMethod",
                new Dictionary<string, object?>
                {
                    ["methodPattern"] = methodPattern,
                    ["matchOverrides"] = false
                }
            );
            return new RpcVisitor(rpc, response.EditVisitor);
        }

        throw new InvalidOperationException("UsesMethod requires an RPC connection to Java");
    }
}
