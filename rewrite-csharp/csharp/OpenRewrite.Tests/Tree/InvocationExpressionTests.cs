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

namespace OpenRewrite.Tests.Tree;

public class InvocationExpressionTests : RewriteTest
{
    [Fact]
    public void ChainedInvocation()
    {
        // GetAction()() - the outer invocation has a MethodInvocation as its expression
        // This creates Cs.InvocationExpression wrapping J.MethodInvocation
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        GetAction()();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedInvocationWithArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        GetFunc()(1, 2);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TripleChainedInvocation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        GetFunc()()();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedInvocationWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        GetAction() ( );
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void QualifiedChainedInvocation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        obj.GetAction()();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedInvocation()
    {
        // (GetAction())() - parenthesized method call then invoked
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        (GetAction())();
                    }
                }
                """
            )
        );
    }
}
