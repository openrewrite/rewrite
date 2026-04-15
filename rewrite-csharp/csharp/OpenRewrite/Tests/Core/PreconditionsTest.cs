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
