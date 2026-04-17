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
/// Search-based precondition visitors that delegate to Java's implementations via RPC.
/// Requires an active RPC connection to a Java process.
/// </summary>
public static class Preconditions
{
    /// <summary>
    /// Creates a UsesType precondition that delegates to Java's
    /// org.openrewrite.java.search.HasType via RPC.
    /// </summary>
    /// <exception cref="InvalidOperationException">Thrown when no RPC connection is available.</exception>
    public static ITreeVisitor<ExecutionContext> UsesType(string fullyQualifiedTypeName)
    {
        var rpc = RewriteRpcServer.Current
                  ?? throw new InvalidOperationException("UsesType requires an RPC connection to Java");

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

    /// <summary>
    /// Creates a UsesMethod precondition that delegates to Java's
    /// org.openrewrite.java.search.HasMethod via RPC.
    /// </summary>
    /// <exception cref="InvalidOperationException">Thrown when no RPC connection is available.</exception>
    public static ITreeVisitor<ExecutionContext> UsesMethod(string methodPattern)
    {
        var rpc = RewriteRpcServer.Current
                  ?? throw new InvalidOperationException("UsesMethod requires an RPC connection to Java");

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
}
