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

namespace OpenRewrite.Tests.Tree;

/// <summary>
/// A conditional directive whose condition names no symbol, such as <c>#if true</c>, is a directive like any other,
/// not text in the whitespace around the code it encloses.
/// </summary>
public class ConstantConditionalDirectiveTests : RewriteTest
{
    [Fact]
    public void IfTrue()
    {
        RewriteRun(
            CSharp(
                """
                #if true
                class C { }
                #endif

                """
            )
        );
    }

    [Fact]
    public void IfFalseElse()
    {
        RewriteRun(
            CSharp(
                """
                #if false
                class Inactive { }
                #else
                class C { }
                #endif

                """
            )
        );
    }

    [Fact]
    public void ElifTrueInAMethod()
    {
        RewriteRun(
            CSharp(
                """
                class C
                {
                    void M()
                    {
                #if false
                        M();
                #elif true
                        M();
                        M();
                #endif
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void IfFalseWithoutElseKeepsItsCode()
    {
        RewriteRun(
            CSharp(
                """
                #if false
                class Inactive { }
                #endif
                class C { }

                """
            )
        );
    }

    [Fact]
    public void IfFalseWithoutElseInAMethodKeepsItsCode()
    {
        RewriteRun(
            CSharp(
                """
                class C
                {
                    void M()
                    {
                #if false
                        M();
                #endif
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void IfFalseKeepsItsCodeAlongsideAConditionOnASymbol()
    {
        RewriteRun(
            CSharp(
                """
                #if DEBUG
                class Debug { }
                #endif
                #if false
                class Inactive { }
                #endif
                class C { }

                """
            )
        );
    }

    [Fact]
    public void ARecipeChangesTheCodeUnderIfFalseAsUnderAnyCondition()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls()),
            CSharp(
                """
                #if false
                class Inactive { void M() { Foo(); } }
                #endif
                class C { void Foo() { } void Bar() { } void M() { Foo(); } }

                """,
                """
                #if false
                class Inactive { void M() { Bar(); } }
                #endif
                class C { void Foo() { } void Bar() { } void M() { Bar(); } }

                """
            )
        );
    }

    [Fact]
    public void TheDirectiveIsInTheTree()
    {
        RewriteRun(
            CSharp(
                """
                #if !false
                class C { }
                #endif

                """
            ).AfterRecipe(cu => Assert.NotNull(FindFirst<ConditionalDirective>(cu)))
        );
    }

    [Fact]
    public void ARecipeLeavesTheActiveBranchAlone()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls()),
            CSharp(
                """
                #if false
                class Inactive { }
                #else
                class C { void Foo() { } void M() { Bar(); } void Bar() { } }
                #endif

                """
            )
        );
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
