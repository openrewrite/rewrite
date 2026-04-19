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

public class YieldTests : RewriteTest
{
    [Fact]
    public void YieldReturn()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        yield return 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void YieldBreak()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        yield break;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void YieldReturnVariable()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        int x = 42;
                        yield return x;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void YieldReturnExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        yield return 1 + 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleYieldReturn()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        yield return 1;
                        yield return 2;
                        yield return 3;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void YieldReturnWithBreak()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar(bool condition) {
                        yield return 1;
                        if (condition) {
                            yield break;
                        }
                        yield return 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void YieldReturnWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        yield  return  42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void YieldBreakWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        yield  break;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void YieldReturnMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        yield return GetValue();
                    }
                    int GetValue() { return 42; }
                }
                """
            )
        );
    }

    [Fact]
    public void YieldReturnInLoop()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    System.Collections.Generic.IEnumerable<int> Bar() {
                        for (int i = 0; i < 10; i++) {
                            yield return i;
                        }
                    }
                }
                """
            )
        );
    }
}
