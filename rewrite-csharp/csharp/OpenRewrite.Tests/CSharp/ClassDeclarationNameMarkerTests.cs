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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.CSharp;

/// <summary>
/// The printer used to write a class name with <c>p.Append(classDecl.Name.SimpleName)</c> after
/// visiting only its prefix, which skipped the identifier's markers — so a search recipe that named
/// the type it had found produced output identical to a clean run. Every other name in the file goes
/// through <c>VisitIdentifier</c>; this one did not.
/// </summary>
public class ClassDeclarationNameMarkerTests : RewriteTest
{
    [Fact]
    public void MarkerOnClassNameIsPrinted()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MarkClassName()),
            CSharp(
                "class C { }",
                "class /*~~(look here)~~>*/C { }"
            )
        );
    }

    [Fact]
    public void MarkerOnGenericClassNameIsPrinted()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MarkClassName()),
            CSharp(
                "class C<T> { }",
                "class /*~~(look here)~~>*/C<T> { }"
            )
        );
    }

    private class MarkClassName : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Mark class names";
        public override string Description => "Marks the identifier of every class declaration.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitClassDeclaration(ClassDeclaration cd, ExecutionContext ctx)
            {
                cd = (ClassDeclaration)base.VisitClassDeclaration(cd, ctx);
                return cd.WithName(Markup.CreateWarn(cd.Name, "look here"));
            }
        }
    }
}
