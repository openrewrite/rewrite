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

    [Fact]
    public void IndexFromEndOperator()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        var x = arr[^1];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void IndexFromEndWithVariable()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        var idx = ^3;
                        var x = arr[idx];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void RangeWithIndexFromEnd()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int[] arr) {
                        var x = arr[1..^1];
                    }
                }
                """
            )
        );
    }
}
