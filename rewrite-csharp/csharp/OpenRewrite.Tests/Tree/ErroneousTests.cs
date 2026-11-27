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
using OpenRewrite.Test;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class ErroneousTests : RewriteTest
{
    [Fact]
    public void MissingSemicolonInMethodBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        ILGenerator Unknown
                    }
                }
                """
            ).AfterRecipe(cu => Assert.Equal("ILGenerator Unknown", FindFirst<Erroneous>(cu)?.Text))
        );
    }

    [Fact]
    public void MissingSemicolonInTopLevelStatement()
    {
        RewriteRun(
            CSharp(
                """
                ILGenerator Unknown
                class Other { }
                """
            )
        );
    }

    [Fact]
    public void MissingSemicolonInField()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    ILGenerator Unknown
                }
                """
            )
        );
    }

    [Fact]
    public void MissingParenthesisAndSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        foo(
                    }
                }
                """
            ).AfterRecipe(cu => Assert.Equal("foo(", FindFirst<Erroneous>(cu)?.Text))
        );
    }

    [Fact]
    public void OnlyInnermostStatementIsErroneous()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int x = 1;
                        ILGenerator Unknown
                    }
                }
                """
            ).AfterRecipe(cu =>
            {
                Assert.NotNull(FindFirst<MethodDeclaration>(cu));
                Assert.NotNull(FindFirst<VariableDeclarations>(cu));
            })
        );
    }

    [Fact]
    public void MissingSemicolonInSymbolConditionalBranch()
    {
        RewriteRun(
            CSharp(
                """
                #if DEBUG
                ILGenerator Unknown
                #endif
                class Other { }
                """
            )
        );
    }
}
