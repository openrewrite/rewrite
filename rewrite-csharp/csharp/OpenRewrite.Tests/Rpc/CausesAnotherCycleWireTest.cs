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
using System.Text.Json;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Older Java hosts reject unknown PrepareRecipe response fields, so <c>causesAnotherCycle</c>
/// must only appear when the host asked for it.
/// </summary>
public class CausesAnotherCycleWireTest
{
    [Fact]
    public async Task OmittedForHostsThatDidNotAsk()
    {
        var wire = await PrepareWire(new CausesAnotherCycleRecipe(), null);
        Assert.False(wire.TryGetProperty("causesAnotherCycle", out _));
    }

    [Fact]
    public async Task OmittedWhenFalse()
    {
        var wire = await PrepareWire(new PlainRecipe(), true);
        Assert.False(wire.TryGetProperty("causesAnotherCycle", out _));
    }

    [Fact]
    public async Task SentWhenRequested()
    {
        var wire = await PrepareWire(new CausesAnotherCycleRecipe(), true);
        Assert.True(wire.GetProperty("causesAnotherCycle").GetBoolean());
    }

    private static async Task<JsonElement> PrepareWire(OpenRewrite.Core.Recipe recipe, bool? accepts)
    {
        var marketplace = new RecipeMarketplace();
        marketplace.Install(recipe);
        var server = new RewriteRpcServer(marketplace);
        var response = await server.PrepareRecipe(new PrepareRecipeRequest
        {
            Id = recipe.Name,
            AcceptsCausesAnotherCycle = accepts
        });
        return JsonSerializer.SerializeToElement(response, RpcJson.Options);
    }

    private class CausesAnotherCycleRecipe : OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Causes another cycle";
        public override string Description => "Causes another cycle.";
        public override bool CausesAnotherCycle => true;
    }

    private class PlainRecipe : OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Plain";
        public override string Description => "Does not cause another cycle.";
    }
}
