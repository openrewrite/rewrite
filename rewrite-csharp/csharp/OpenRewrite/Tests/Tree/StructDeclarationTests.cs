using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class StructDeclarationTests : RewriteTest
{
    [Fact]
    public void SimpleStruct()
    {
        RewriteRun(
            CSharp(
                """
                struct Point {
                }
                """
            )
        );
    }

    [Fact]
    public void StructWithFields()
    {
        RewriteRun(
            CSharp(
                """
                struct Point {
                    public int X;
                    public int Y;
                }
                """
            )
        );
    }

    [Fact]
    public void StructWithMethod()
    {
        RewriteRun(
            CSharp(
                """
                struct Point {
                    public int X;
                    public int Y;
                    public double Distance() {
                        return 0;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void StructWithProperty()
    {
        RewriteRun(
            CSharp(
                """
                struct Point {
                    public int X { get; set; }
                    public int Y { get; set; }
                }
                """
            )
        );
    }

    [Fact]
    public void StructWithConstructor()
    {
        RewriteRun(
            CSharp(
                """
                struct Point {
                    public int X;
                    public int Y;
                    public Point(int x, int y) {
                        X = x;
                        Y = y;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void PublicStruct()
    {
        RewriteRun(
            CSharp(
                """
                public struct Point {
                    public int X;
                    public int Y;
                }
                """
            )
        );
    }

    [Fact]
    public void ReadonlyStruct()
    {
        RewriteRun(
            CSharp(
                """
                readonly struct Point {
                    public int X { get; }
                    public int Y { get; }
                }
                """
            )
        );
    }

    [Fact]
    public void StructWithInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IPoint {
                    int X { get; }
                }
                struct Point : IPoint {
                    public int X { get; set; }
                }
                """
            )
        );
    }

    [Fact]
    public void GenericStruct()
    {
        RewriteRun(
            CSharp(
                """
                struct Pair<T> {
                    public T First;
                    public T Second;
                }
                """
            )
        );
    }

    [Fact]
    public void StructWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                struct  Point  {
                    public  int  X;
                }
                """
            )
        );
    }
}
