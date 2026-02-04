using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class ArrayTests : RewriteTest
{
    [Fact]
    public void ArrayTypeDeclaration()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int[] arr;
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayTypeWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int [ ] arr;
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayCreationWithSize()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int[10];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayCreationWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int[] { 1, 2, 3 };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ImplicitArrayCreation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var arr = new[] { 1, 2, 3 };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayCreationWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int [ 10 ];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayInitializerWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] arr = new int[] { 1 , 2 , 3 };
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void StringArrayDeclaration()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    string[] names;
                }
                """
            )
        );
    }

    [Fact]
    public void JaggedArrayType()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int[][] jagged;
                }
                """
            )
        );
    }

    [Fact]
    public void JaggedArrayCreation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[][] jagged = new int[3][];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAsMethodParameter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAsReturnType()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int[] Bar() {
                        return new int[0];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void EmptyArrayCreation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int[] empty = new int[0];
                    }
                }
                """
            )
        );
    }
}
