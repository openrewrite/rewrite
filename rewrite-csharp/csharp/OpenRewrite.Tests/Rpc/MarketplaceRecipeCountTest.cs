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
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Verifies the marketplace row's RecipeCount is 1 + every transitive sub-recipe, not just the
/// direct children — the host uses it as a marketplace sort key.
/// </summary>
public class MarketplaceRecipeCountTest
{
    private sealed class LeafRecipe : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Leaf";
        public override string Description => "Leaf recipe.";
        public override JavaVisitor<ExecutionContext> GetVisitor() => new CSharpVisitor<ExecutionContext>();
    }

    private sealed class MiddleRecipe : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Middle";
        public override string Description => "Middle composite.";
        public override List<global::OpenRewrite.Core.Recipe> GetRecipeList() => [new LeafRecipe()];
    }

    private sealed class RootRecipe : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Root";
        public override string Description => "Root composite.";
        public override List<global::OpenRewrite.Core.Recipe> GetRecipeList() => [new MiddleRecipe()];
    }

    [Fact]
    public void GetMarketplace_RecipeCountIncludesTransitiveSubRecipes()
    {
        var marketplace = new global::OpenRewrite.Core.RecipeMarketplace();
        marketplace.Install(new RootRecipe(), new global::OpenRewrite.Core.CategoryDescriptor("Test"));
        var server = new RewriteRpcServer(marketplace);

        var rows = server.GetMarketplace().GetAwaiter().GetResult();
        var root = rows.Single(r => r.DisplayName == "Root");

        Assert.Equal(3, root.RecipeCount); // root + middle + leaf

        // Guard: the marketplace holds a RecipeListing (with the precomputed count), not a descriptor.
        global::OpenRewrite.Core.RecipeListing held = marketplace.FindRecipe(typeof(RootRecipe).FullName!)!.Value.Listing;
        Assert.Equal(3, held.RecipeCount);
    }
}
