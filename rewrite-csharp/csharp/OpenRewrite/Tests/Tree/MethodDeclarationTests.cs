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

public class MethodDeclarationTests : RewriteTest
{
    [Fact]
    public void SimpleVoidMethod()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithReturnType()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void PublicMethod()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public void Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithMultipleModifiers()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public static void Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithOneParameter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithMultipleParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x, string y) { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar() {
                        var x = 1;
                    }
                }
                """
            )
        );
    }
}
