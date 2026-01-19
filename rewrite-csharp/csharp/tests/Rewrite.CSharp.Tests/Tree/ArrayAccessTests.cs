using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class ArrayAccessTests : RewriteTest
{
    [Fact]
    public void SimpleArrayAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        var x = arr[0];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAccessWithExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        int i = 2;
                        var x = arr[i];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAccessWithArithmetic()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        var x = arr[1 + 2];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAccessWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        var x = arr [ 0 ];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedArrayAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[][] arr) {
                        var x = arr[0][1];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAccessOnStringIndexer()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var s = "hello";
                        var c = s[0];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedArrayAndMethodAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string[] arr) {
                        var x = arr[0].ToUpper();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAccessInMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        Console.WriteLine(arr[0]);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAccessAssignment()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        arr[0] = 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAccessInReturn()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar(int[] arr) {
                        return arr[0];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ArrayAccessInBinaryExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        var x = arr[0] + arr[1];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultiDimensionalArrayAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[,] matrix) {
                        var x = matrix[0, 1];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultiDimensionalArrayAccessWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[,] matrix) {
                        var x = matrix [ 0 , 1 ];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultiDimensionalArrayAccessWithExpressions()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[,] matrix) {
                        int i = 0;
                        int j = 1;
                        var x = matrix[i, j];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ThreeDimensionalArrayAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[,,] cube) {
                        var x = cube[0, 1, 2];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ThreeDimensionalArrayAccessWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[,,] cube) {
                        var x = cube [ 0 , 1 , 2 ];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultiDimensionalArrayAssignment()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[,] matrix) {
                        matrix[0, 1] = 42;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultiDimensionalInMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[,] matrix) {
                        Console.WriteLine(matrix[0, 1]);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullConditionalIndexAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[]? arr) {
                        var x = arr?[0];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullConditionalIndexAccessWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[]? arr) {
                        var x = arr ?[ 0 ];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedNullConditionalIndexAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[][]? arr) {
                        var x = arr?[0]?[1];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullConditionalIndexThenMethod()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string[]? arr) {
                        var x = arr?[0].ToUpper();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullConditionalIndexThenNullConditionalMethod()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string[]? arr) {
                        var x = arr?[0]?.ToUpper();
                    }
                }
                """
            )
        );
    }
}
