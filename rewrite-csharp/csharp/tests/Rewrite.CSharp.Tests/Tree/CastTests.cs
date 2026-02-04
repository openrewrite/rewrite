using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class CastTests : RewriteTest
{
    [Fact]
    public void SimpleCast()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = 42;
                        int x = (int)o;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastToString()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = "hello";
                        string s = (string)o;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = 42;
                        int x = ( int )o;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastInExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = 42;
                        int x = (int)o + 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedCast()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        object o = 42;
                        long x = (long)(int)o;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastMethodResult()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    object GetValue() { return 42; }
                    void Bar() {
                        int x = (int)GetValue();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CastToDouble()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int i = 42;
                        double d = (double)i;
                    }
                }
                """
            )
        );
    }
}
