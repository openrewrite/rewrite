/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Moderne Source Available License (the "License");
 * See https://docs.moderne.io/licensing/moderne-source-available-license
 */
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

public class DelegatesToTest
{
    // A recipe that delegates entirely to a Java host recipe. The C# server can't run it, so
    // PrepareRecipe answers with delegatesTo{recipeName, options} and the Java host resolves the
    // recipe from its own marketplace instead of wrapping the C# recipe in an RpcRecipe.
    private sealed class DelegatingRecipe : global::OpenRewrite.Core.Recipe, global::OpenRewrite.Core.IDelegatesTo
    {
        public override string DisplayName => "Delegating";
        public override string Description => "Delegates to a Java recipe.";
        public string JavaRecipeName => "org.openrewrite.java.ChangeType";

        public Dictionary<string, object?> Options => new()
        {
            ["oldFullyQualifiedTypeName"] = "a.A",
            ["newFullyQualifiedTypeName"] = "b.B"
        };
    }

    // A composite whose only child delegates to a Java recipe — the cross-ecosystem case the host's
    // whole-tree path resolves from the prepared recipeList.
    private sealed class CompositeWithDelegatingChild : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Composite with delegating child";
        public override string Description => "A composite whose child delegates to a Java recipe.";
        public override List<global::OpenRewrite.Core.Recipe> GetRecipeList() => [new DelegatingRecipe()];
    }

    [Fact]
    public void PrepareRecipe_RootDelegatesToJavaRecipe()
    {
        var marketplace = new global::OpenRewrite.Core.RecipeMarketplace();
        marketplace.Install(new DelegatingRecipe(), new global::OpenRewrite.Core.CategoryDescriptor("Test"));
        var server = new RewriteRpcServer(marketplace);

        var response = server.PrepareRecipe(new PrepareRecipeRequest
        {
            Id = typeof(DelegatingRecipe).FullName!
        }).GetAwaiter().GetResult();

        Assert.NotNull(response.DelegatesTo);
        Assert.Equal("org.openrewrite.java.ChangeType", response.DelegatesTo!.RecipeName);
        Assert.Equal("a.A", response.DelegatesTo.Options["oldFullyQualifiedTypeName"]);
        Assert.Equal("b.B", response.DelegatesTo.Options["newFullyQualifiedTypeName"]);
        // A delegating recipe carries no C# child tree; the host owns it.
        Assert.Empty(response.RecipeList);
    }

    [Fact]
    public void PrepareRecipe_CompositeChildDelegatesToJavaRecipe()
    {
        var marketplace = new global::OpenRewrite.Core.RecipeMarketplace();
        marketplace.Install(new CompositeWithDelegatingChild(), new global::OpenRewrite.Core.CategoryDescriptor("Test"));
        var server = new RewriteRpcServer(marketplace);

        var parent = server.PrepareRecipe(new PrepareRecipeRequest
        {
            Id = typeof(CompositeWithDelegatingChild).FullName!
        }).GetAwaiter().GetResult();

        Assert.Null(parent.DelegatesTo);
        var child = Assert.Single(parent.RecipeList);
        Assert.NotNull(child.DelegatesTo);
        Assert.Equal("org.openrewrite.java.ChangeType", child.DelegatesTo!.RecipeName);
        Assert.Equal("a.A", child.DelegatesTo.Options["oldFullyQualifiedTypeName"]);
    }
}
