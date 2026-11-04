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
using OpenRewrite.Java;
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;
using Recipe = OpenRewrite.Core.Recipe;

namespace OpenRewrite.Tests.Core;

public class PreconditionsTest : RewriteTest
{
    /// <summary>
    /// When the precondition Recipe matches, the inner visitor should run.
    /// </summary>
    [Fact]
    public void RecipePreconditionMatches()
    {
        RewriteRun(
            spec => spec.SetRecipe(new PreconditionedRenameRecipe
            {
                PreconditionClassName = "Foo",
                From = "Foo",
                To = "Bar"
            }),
            CSharp(
                "class Foo { }",
                "class Bar { }"
            )
        );
    }

    /// <summary>
    /// When the precondition Recipe does NOT match, the inner visitor should not run.
    /// </summary>
    [Fact]
    public void RecipePreconditionDoesNotMatch()
    {
        RewriteRun(
            spec => spec.SetRecipe(new PreconditionedRenameRecipe
            {
                PreconditionClassName = "Missing",
                From = "Foo",
                To = "Bar"
            }),
            CSharp("class Foo { }")
        );
    }

    /// <summary>
    /// Or matches when any operand matches: the rename should run because
    /// "Foo" matches one of the two precondition class names.
    /// </summary>
    [Fact]
    public void OrMatchesWhenAnyOperandMatches()
    {
        RewriteRun(
            spec => spec.SetRecipe(new OrPreconditionedRenameRecipe
            {
                ClassNames = ["Missing", "Foo"],
                From = "Foo",
                To = "Bar"
            }),
            CSharp(
                "class Foo { }",
                "class Bar { }"
            )
        );
    }

    /// <summary>
    /// Or skips the inner visitor when no operand matches.
    /// </summary>
    [Fact]
    public void OrSkipsWhenNoOperandMatches()
    {
        RewriteRun(
            spec => spec.SetRecipe(new OrPreconditionedRenameRecipe
            {
                ClassNames = ["Missing", "Absent"],
                From = "Foo",
                To = "Bar"
            }),
            CSharp("class Foo { }")
        );
    }

    /// <summary>
    /// Or requires at least two operands; calling with fewer should throw.
    /// </summary>
    [Fact]
    public void OrRequiresAtLeastTwoOperands()
    {
        var recipe = new FindClassRecipe { ClassName = "Foo" };
        Assert.Throws<ArgumentException>(() =>
            OpenRewrite.Core.Preconditions.Or(recipe.GetVisitor()));
    }

    /// <summary>
    /// RecipeRef helpers must NOT fire any RPC at construction time —
    /// they just bundle a recipe name + options for later wire emission.
    /// </summary>
    [Fact]
    public void RecipeRefHelpersAreLazy()
    {
        // None of these calls require an RPC connection; they just
        // construct lightweight placeholders.
        var hasPath = OpenRewrite.Java.Search.Preconditions.HasSourcePath("**/*.cs");
        var usesType = OpenRewrite.Java.Search.Preconditions.UsesType("System.Console");
        var usesMethod = OpenRewrite.Java.Search.Preconditions.UsesMethod("*..* WriteLine(..)");
        var findMethods = OpenRewrite.Java.Search.Preconditions.FindMethods("*..* Write(..)");
        var findTypes = OpenRewrite.Java.Search.Preconditions.FindTypes("System.IO.File");

        Assert.Equal("org.openrewrite.FindSourceFiles", hasPath.RecipeName);
        Assert.Equal("**/*.cs", hasPath.Options["filePattern"]);

        Assert.Equal("org.openrewrite.java.search.HasType", usesType.RecipeName);
        Assert.Equal("System.Console", usesType.Options["fullyQualifiedTypeName"]);

        Assert.Equal("org.openrewrite.java.search.HasMethod", usesMethod.RecipeName);
        Assert.Equal("*..* WriteLine(..)", usesMethod.Options["methodPattern"]);

        Assert.Equal("org.openrewrite.java.search.FindMethods", findMethods.RecipeName);
        Assert.Equal("org.openrewrite.java.search.FindTypes", findTypes.RecipeName);

        // And each is bundled with a native LocalVisitor so unit tests
        // without an active RPC connection still see real filtering.
        Assert.NotNull(hasPath.LocalVisitor);
        Assert.NotNull(usesType.LocalVisitor);
        Assert.NotNull(usesMethod.LocalVisitor);
        Assert.NotNull(findMethods.LocalVisitor);
        Assert.NotNull(findTypes.LocalVisitor);
    }

    /// <summary>
    /// In-process: a bare RecipeRef without a LocalVisitor short-circuits
    /// to "matches" so the wrapped editor still runs (the host
    /// evaluates the gate over the wire when an RPC connection is
    /// available).
    /// </summary>
    [Fact]
    public void BareRecipeRefShortCircuitsToMatchInProcess()
    {
        RewriteRun(
            spec => spec.SetRecipe(new BareRecipeRefGatedRenameRecipe
            {
                From = "Foo",
                To = "Bar"
            }),
            CSharp(
                "class Foo { }",
                "class Bar { }"
            )
        );
    }
}

/// <summary>
/// A recipe that gates rename via a bare RecipeRef (no LocalVisitor).
/// Without an active RPC connection, the in-process gate short-circuits
/// to "matches" so the rename runs.
/// </summary>
file class BareRecipeRefGatedRenameRecipe : OpenRewrite.Core.Recipe
{
    public required string From { get; init; }
    public required string To { get; init; }

    public override string DisplayName => "Bare-RecipeRef-gated rename";
    public override string Description => "Renames a class, gated by a bare RecipeRef without a LocalVisitor.";

    public override OpenRewrite.Core.ITreeVisitor<ExecutionContext> GetVisitor() =>
        OpenRewrite.Core.Preconditions.Check(
            new OpenRewrite.Core.RecipeRef(
                "org.openrewrite.java.search.HasType",
                new Dictionary<string, object?> { ["fullyQualifiedTypeName"] = "Some.Missing.Type" }),
            new RenameClassVisitor(From, To));
}

/// <summary>
/// A recipe that renames a class, but only if any of several precondition
/// recipes (FindClass for each name) matches. Exercises Preconditions.Or.
/// </summary>
file class OrPreconditionedRenameRecipe : OpenRewrite.Core.Recipe
{
    public required string[] ClassNames { get; init; }
    public required string From { get; init; }
    public required string To { get; init; }

    public override string DisplayName => "Or-preconditioned rename class";
    public override string Description => "Renames a class only if any precondition recipe matches.";

    public override OpenRewrite.Core.ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var operands = ClassNames
            .Select(n => (OpenRewrite.Core.ITreeVisitor<ExecutionContext>)new FindClassVisitor(n))
            .ToArray();
        return OpenRewrite.Core.Preconditions.Check(
            OpenRewrite.Core.Preconditions.Or(operands),
            new RenameClassVisitor(From, To));
    }
}

/// <summary>
/// A recipe that renames a class, but only if a precondition recipe (FindClass) matches.
/// Uses Preconditions.Check(Recipe, visitor) overload.
/// </summary>
file class PreconditionedRenameRecipe : OpenRewrite.Core.Recipe
{
    public required string PreconditionClassName { get; init; }
    public required string From { get; init; }
    public required string To { get; init; }

    public override string DisplayName => "Preconditioned rename class";
    public override string Description => "Renames a class only if the precondition recipe matches.";

    public override OpenRewrite.Core.ITreeVisitor<ExecutionContext> GetVisitor()
    {
        return OpenRewrite.Core.Preconditions.Check(
            new FindClassRecipe { ClassName = PreconditionClassName },
            new RenameClassVisitor(From, To));
    }
}

/// <summary>
/// A simple search recipe that marks files containing a specific class name.
/// Used as a precondition.
/// </summary>
file class FindClassRecipe : OpenRewrite.Core.Recipe
{
    public required string ClassName { get; init; }

    public override string DisplayName => "Find class";
    public override string Description => "Finds files containing a specific class.";

    public override OpenRewrite.Core.ITreeVisitor<ExecutionContext> GetVisitor() => new FindClassVisitor(ClassName);
}

file class FindClassVisitor(string className) : CSharpVisitor<ExecutionContext>
{
    public override J VisitClassDeclaration(ClassDeclaration cd, ExecutionContext ctx)
    {
        cd = (ClassDeclaration)base.VisitClassDeclaration(cd, ctx);
        if (cd.Name.SimpleName == className)
        {
            return OpenRewrite.Core.SearchResult.Found(cd);
        }
        return cd;
    }
}

file class RenameClassVisitor(string from, string to) : CSharpVisitor<ExecutionContext>
{
    public override J VisitClassDeclaration(ClassDeclaration cd, ExecutionContext ctx)
    {
        cd = (ClassDeclaration)base.VisitClassDeclaration(cd, ctx);
        if (cd.Name.SimpleName == from)
        {
            return cd.WithName(cd.Name.WithSimpleName(to));
        }
        return cd;
    }
}
