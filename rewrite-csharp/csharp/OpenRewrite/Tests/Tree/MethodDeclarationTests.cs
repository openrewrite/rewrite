using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

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
