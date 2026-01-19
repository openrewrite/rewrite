using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class PropertyDeclarationTests : RewriteTest
{
    [Fact]
    public void SimpleAutoProperty()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int X { get; set; }
                }
                """
            )
        );
    }

    [Fact]
    public void GetOnlyAutoProperty()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public string Name { get; }
                }
                """
            )
        );
    }

    [Fact]
    public void PropertyWithPrivateSetter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int Value { get; private set; }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleProperties()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int X { get; set; }
                    public int Y { get; set; }
                }
                """
            )
        );
    }

    [Fact]
    public void PropertyExpressionBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int X => 42;
                }
                """
            )
        );
    }

    [Fact]
    public void PropertyExpressionBodyWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public int X  =>  42;
                }
                """
            )
        );
    }

    [Fact]
    public void AccessorExpressionBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    private int _x;
                    public int X { get => _x; set => _x = value; }
                }
                """
            )
        );
    }

    [Fact]
    public void AccessorExpressionBodyGetOnly()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    private int _x;
                    public int X { get => _x; }
                }
                """
            )
        );
    }
}
