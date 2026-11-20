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

namespace OpenRewrite.Tests.Core;

/// <summary>
/// A source the test skips is code the sources the recipe runs on use, which compiles with them and which the recipe
/// doesn't see.
/// </summary>
public class SkipTests : RewriteTest
{
    private const string Library =
        """
        public static class Library
        {
            public static int Foo() => 1;
            public static int Bar() => Foo();
        }
        """;

    [Fact]
    public void TheRecipeDoesNotSeeASkippedSourceThatTheOthersCompileWith()
    {
        var typed = false;
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls()).ExpectInputCompiles().ExpectOutputCompiles(),
            CSharp(Library, sourcePath: "Library.cs").Skip(),
            CSharp(
                "class C { int M() => Library.Foo(); }",
                "class C { int M() => Library.Bar(); }",
                "C.cs"
            ).AfterRecipe(cu => typed = FindFirst<MethodInvocation>(cu)!.MethodType != null)
        );

        Assert.True(typed);
    }

    [Fact]
    public void ASkippedSourceCannotExpectAChange()
    {
        Assert.Throws<ArgumentException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCalls()),
            CSharp(Library, Library.Replace("=> Foo()", "=> Bar()")).Skip()
        ));
    }

    private class RenameCalls : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename calls";
        public override string Description => "Renames the calls to Foo to Bar.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var call = (MethodInvocation)base.VisitMethodInvocation(method, ctx);
                return call.Name.SimpleName == "Foo" ? call.WithName(call.Name.WithSimpleName("Bar")) : call;
            }
        }
    }
}
