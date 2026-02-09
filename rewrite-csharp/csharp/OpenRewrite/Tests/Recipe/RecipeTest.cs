using Rewrite.Core;
using Rewrite.CSharp;
using Rewrite.Java;
using Rewrite.Test;
using static Rewrite.Java.J;
using ExecutionContext = Rewrite.Core.ExecutionContext;
using Recipe = Rewrite.Core.Recipe;

namespace Rewrite.CSharp.Tests.Recipes;

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
        Assert.Equal("Foo", descriptor.Options[0].Value);
        Assert.Equal("To", descriptor.Options[1].Name);
        Assert.Equal("Bar", descriptor.Options[1].Value);
    }
}

class RenameClassRecipe : Recipe
{
    [Option(DisplayName = "From", Description = "The class name to rename from.", Example = "Foo")]
    public required string From { get; init; }

    [Option(DisplayName = "To", Description = "The class name to rename to.", Example = "Bar")]
    public required string To { get; init; }

    public override string DisplayName => "Rename class";
    public override string Description => $"Renames class `{From}` to `{To}`.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new RenameClassVisitor(From, To);

    private class RenameClassVisitor : CSharpVisitor<ExecutionContext>
    {
        private readonly string _from;
        private readonly string _to;

        public RenameClassVisitor(string from, string to)
        {
            _from = from;
            _to = to;
        }

        public override J VisitClassDeclaration(ClassDeclaration cd, ExecutionContext ctx)
        {
            cd = (ClassDeclaration)base.VisitClassDeclaration(cd, ctx);
            if (cd.Name.SimpleName == _from)
            {
                return cd with { Name = cd.Name with { SimpleName = _to } };
            }

            return cd;
        }
    }
}
