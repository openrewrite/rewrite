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
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Recipe;

public class RecipeTest : RewriteTest
{
    [Fact]
    public void RenameClass()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameClassRecipe { From = "Foo", To = "Bar" }),
            CSharp(
                "class Foo { }",
                "class Bar { }"
            )
        );
    }

    [Fact]
    public void NoChangeWhenClassNotFound()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameClassRecipe { From = "Foo", To = "Bar" }),
            CSharp("class Baz { }")
        );
    }

    [Fact]
    public void RenameClassInNamespace()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameClassRecipe { From = "Foo", To = "Bar" }),
            CSharp(
                """
                namespace MyApp
                {
                    class Foo { }
                }
                """,
                """
                namespace MyApp
                {
                    class Bar { }
                }
                """
            )
        );
    }

    [Fact]
    public void NoOpVisitorPreservesReferenceEquality()
    {
        var parser = new CSharpParser();
        var source = parser.Parse("class C { void M() { var x = 1; } }");
        var visitor = new CSharpVisitor<OpenRewrite.Core.ExecutionContext>();
        var result = visitor.Visit(source, new OpenRewrite.Core.ExecutionContext());
        Assert.True(ReferenceEquals(source, result),
            $"No-op visitor should preserve reference equality. " +
            $"source type={source.GetType().Name}, result type={result?.GetType().Name}");
    }

    [Fact]
    public void RecipeDescriptorHasOptions()
    {
        var recipe = new RenameClassRecipe { From = "Foo", To = "Bar" };
        var descriptor = recipe.GetDescriptor();

        Assert.Equal("Rename class", descriptor.DisplayName);
        Assert.Equal(2, descriptor.Options.Count);
        Assert.Equal("From", descriptor.Options[0].Name);
        Assert.Equal("String", descriptor.Options[0].Type);
        Assert.Equal("The class name to rename from.", descriptor.Options[0].Description);
        Assert.Equal("Foo", descriptor.Options[0].Example);
        Assert.True(descriptor.Options[0].Required);
        Assert.False(descriptor.Options[0].Secret);
        Assert.Equal("Foo", descriptor.Options[0].Value);
        Assert.Equal("To", descriptor.Options[1].Name);
        Assert.Equal("Bar", descriptor.Options[1].Value);
    }

    [Fact]
    public void SecretOptionPropagatesAndCanBeRedacted()
    {
        var recipe = new RecipeWithSecret { ApiToken = "hunter2" };
        var descriptor = recipe.GetDescriptor();

        Assert.Single(descriptor.Options);
        var opt = descriptor.Options[0];
        Assert.Equal("ApiToken", opt.Name);
        Assert.True(opt.Secret);
        // Raw value is preserved on the source-level descriptor so RPC + recipe execution
        // still see it. Persistence boundaries must call WithRedactedSecretValue().
        Assert.Equal("hunter2", opt.Value);

        var redacted = opt.WithRedactedSecretValue();
        Assert.True(redacted.Secret);
        Assert.Null(redacted.Value);
        Assert.Equal("ApiToken", redacted.Name);
        Assert.Equal("API token", redacted.DisplayName);
    }

    [Fact]
    public void WithRedactedSecretValueIsNoOpForNonSecret()
    {
        var recipe = new RenameClassRecipe { From = "Foo", To = "Bar" };
        var opt = recipe.GetDescriptor().Options[0];
        Assert.Same(opt, opt.WithRedactedSecretValue());
    }

}

class RecipeWithSecret : OpenRewrite.Core.Recipe
{
    [Option(DisplayName = "API token", Description = "API token used by the recipe.", Secret = true)]
    public required string ApiToken { get; init; }

    public override string DisplayName => "Recipe with secret";
    public override string Description => "Recipe with secret.";

    public override JavaVisitor<OpenRewrite.Core.ExecutionContext> GetVisitor() =>
        new CSharpVisitor<OpenRewrite.Core.ExecutionContext>();
}

class RenameClassRecipe : OpenRewrite.Core.Recipe
{
    [Option(DisplayName = "From", Description = "The class name to rename from.", Example = "Foo")]
    public required string From { get; init; }

    [Option(DisplayName = "To", Description = "The class name to rename to.", Example = "Bar")]
    public required string To { get; init; }

    public override string DisplayName => "Rename class";
    public override string Description => $"Renames class `{From}` to `{To}`.";

    public override JavaVisitor<OpenRewrite.Core.ExecutionContext> GetVisitor() => new RenameClassVisitor(From, To);

    private class RenameClassVisitor : CSharpVisitor<OpenRewrite.Core.ExecutionContext>
    {
        private readonly string _from;
        private readonly string _to;

        public RenameClassVisitor(string from, string to)
        {
            _from = from;
            _to = to;
        }

        public override J VisitClassDeclaration(ClassDeclaration cd, OpenRewrite.Core.ExecutionContext ctx)
        {
            cd = (ClassDeclaration)base.VisitClassDeclaration(cd, ctx);
            if (cd.Name.SimpleName == _from)
            {
                return cd.WithName(cd.Name.WithSimpleName(_to));
            }

            return cd;
        }
    }
}
