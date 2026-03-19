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
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.CSharp.Rpc;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Java.Search;

/// <summary>
/// Convenience functions for common precondition visitors.
/// When connected to Java via RPC, delegates to Java's implementations.
/// Otherwise falls back to local implementations.
/// </summary>
public static class Preconditions
{
    /// <summary>
    /// Wraps a visitor with a precondition check. The inner visitor only runs
    /// on files where the precondition matches.
    /// </summary>
    public static ITreeVisitor<ExecutionContext> Check(
        ITreeVisitor<ExecutionContext> precondition,
        ITreeVisitor<ExecutionContext> visitor)
    {
        return new Check(precondition, visitor);
    }

    /// <summary>
    /// Creates a UsesType precondition. If connected to Java via RPC, delegates to
    /// Java's org.openrewrite.java.search.HasType. Otherwise falls back to local implementation.
    /// </summary>
    public static ITreeVisitor<ExecutionContext> UsesType(string fullyQualifiedTypeName)
    {
        var rpc = RewriteRpcServer.Current;
        if (rpc != null)
        {
            var response = rpc.PrepareRecipeOnRemote(
                "org.openrewrite.java.search.HasType",
                new Dictionary<string, object?>
                {
                    ["fullyQualifiedTypeName"] = fullyQualifiedTypeName,
                    ["checkAssignability"] = false
                }
            );
            return new RpcVisitor(rpc, response.EditVisitor);
        }

        return new LocalUsesType<ExecutionContext>(fullyQualifiedTypeName);
    }

    /// <summary>
    /// Creates a UsesMethod precondition. If connected to Java via RPC, delegates to
    /// Java's org.openrewrite.java.search.HasMethod.
    /// </summary>
    public static ITreeVisitor<ExecutionContext> UsesMethod(string methodPattern)
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
