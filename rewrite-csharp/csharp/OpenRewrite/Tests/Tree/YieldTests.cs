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
