/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Moderne Source Available License (the "License");
 * See https://docs.moderne.io/licensing/moderne-source-available-license
 */
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

public class PrepareRecipeTreeTest
{
    private sealed class ChildRecipe : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Child";
        public override string Description => "Child recipe.";
        public override JavaVisitor<ExecutionContext> GetVisitor() => new CSharpVisitor<ExecutionContext>();
    }

    private sealed class ParentRecipe : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Parent";
        public override string Description => "Parent composite recipe.";
        public override List<global::OpenRewrite.Core.Recipe> GetRecipeList() => [new ChildRecipe()];
    }

    [Fact]
    public void PrepareRecipe_ReturnsConfiguredChildTree()
    {
        var marketplace = new global::OpenRewrite.Core.RecipeMarketplace();
        marketplace.Install(new ParentRecipe(), new global::OpenRewrite.Core.CategoryDescriptor("Test"));
        var server = new RewriteRpcServer(marketplace);

        var parent = server.PrepareRecipe(new PrepareRecipeRequest
        {
            Id = typeof(ParentRecipe).FullName!
        }).GetAwaiter().GetResult();

        Assert.Single(parent.RecipeList);
        var child = parent.RecipeList[0];
        Assert.Equal(typeof(ChildRecipe).FullName, child.Descriptor.Name);
        Assert.NotEqual(parent.Id, child.Id);
        Assert.False(string.IsNullOrEmpty(child.EditVisitor));
    }

    private sealed class ChildWithRequiredOption : global::OpenRewrite.Core.Recipe
    {
        [global::OpenRewrite.Core.Option(DisplayName = "Required thing", Description = "A required option.")]
        public string? RequiredThing { get; set; }

        public override string DisplayName => "Child with required option";
        public override string Description => "Child recipe with a required option.";
        public override JavaVisitor<ExecutionContext> GetVisitor() => new CSharpVisitor<ExecutionContext>();
    }

    private sealed class ParentMissingChildOption : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Parent missing child option";
        public override string Description => "Composite whose child is missing a required option.";
        public override List<global::OpenRewrite.Core.Recipe> GetRecipeList() => [new ChildWithRequiredOption()];
    }

    [Fact]
    public void PrepareRecipe_ThrowsWhenAChildIsMissingARequiredOption()
    {
        var marketplace = new global::OpenRewrite.Core.RecipeMarketplace();
        marketplace.Install(new ParentMissingChildOption(), new global::OpenRewrite.Core.CategoryDescriptor("Test"));
        var server = new RewriteRpcServer(marketplace);

        var ex = Assert.ThrowsAny<Exception>(() =>
            server.PrepareRecipe(new PrepareRecipeRequest { Id = typeof(ParentMissingChildOption).FullName! })
                .GetAwaiter().GetResult());

        Assert.Contains("RequiredThing", ex.Message);
        Assert.Contains("Missing required option", ex.Message);
    }
}
