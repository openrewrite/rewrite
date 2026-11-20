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

namespace OpenRewrite.Test;

/// <summary>
/// A recipe of the Java peer of an <see cref="RpcRewriteTest"/>, by its name and options, such as one that a test runs
/// before or after the recipe under test with <see cref="RecipeSpec.SetRecipes"/>. It runs the recipe's edit visitor
/// on the Java peer, so it takes a recipe that neither scans nor lists other recipes.
/// </summary>
public sealed class RemoteRecipe(string javaRecipeName, Dictionary<string, object?>? options = null)
    : Recipe, IDelegatesTo
{
    private readonly Dictionary<string, object?> _options = options ?? new();

    public override string DisplayName => javaRecipeName;

    public override string Description => $"Runs {javaRecipeName} on the Java peer.";

    public string JavaRecipeName => javaRecipeName;

    public Dictionary<string, object?> Options => new(_options);

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var rpc = RewriteRpcServer.Current ?? throw new InvalidOperationException(
            $"{javaRecipeName} runs on the Java peer, and there is no RPC connection. Use RpcFixture to start one.");
        var prepared = rpc.PrepareRecipeOnRemote(javaRecipeName, Options);
        if (prepared.ScanVisitor != null || prepared.RecipeList.Count > 0)
        {
            throw new ArgumentException(
                $"{javaRecipeName} {(prepared.ScanVisitor != null ? "scans" : "lists other recipes")}, and a " +
                "RemoteRecipe runs only the edit visitor of a recipe.");
        }
        return new RpcVisitor(rpc, prepared.EditVisitor);
    }
}
