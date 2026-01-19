using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class SwitchTests : RewriteTest
{
    [Fact]
    public void SimpleSwitch()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case 1:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithDefault()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case 1:
                                break;
                            default:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchMultipleCases()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case 1:
                                DoOne();
                                break;
                            case 2:
                                DoTwo();
                                break;
                            case 3:
                                DoThree();
                                break;
                            default:
                                DoDefault();
                                break;
                        }
                    }
                    void DoOne() { }
                    void DoTwo() { }
                    void DoThree() { }
                    void DoDefault() { }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithReturn()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar(int x) {
                        switch (x) {
                            case 1:
                                return 10;
                            case 2:
                                return 20;
                            default:
                                return 0;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch  ( x )  {
                            case  1 :
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithStringCases()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string s) {
                        switch (s) {
                            case "hello":
                                break;
                            case "world":
                                break;
                            default:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchCaseFallthrough()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case 1:
                            case 2:
                                DoSomething();
                                break;
                            default:
                                break;
                        }
                    }
                    void DoSomething() { }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithMultipleStatements()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case 1:
                                var a = 1;
                                var b = 2;
                                DoSomething(a + b);
                                break;
                        }
                    }
                    void DoSomething(int n) { }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchOnExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x, int y) {
                        switch (x + y) {
                            case 0:
                                break;
                            default:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchOnMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        switch (GetValue()) {
                            case 1:
                                break;
                        }
                    }
                    int GetValue() { return 1; }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedSwitch()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x, int y) {
                        switch (x) {
                            case 1:
                                switch (y) {
                                    case 10:
                                        break;
                                }
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithTypePattern()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(object x) {
                        switch (x) {
                            case int i:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithTypePatternAndDiscard()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(object x) {
                        switch (x) {
                            case int _:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithMultipleTypePatterns()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(object x) {
                        switch (x) {
                            case int i:
                                break;
                            case string s:
                                break;
                            default:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithRelationalPattern()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case > 5:
                                break;
                            case < 0:
                                break;
                            default:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithRelationalPatternAllOperators()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case > 10:
                                break;
                            case >= 5:
                                break;
                            case < 0:
                                break;
                            case <= -5:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithOrPattern()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case 1 or 2:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithAndPattern()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case > 0 and < 100:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithComplexBinaryPattern()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) {
                        switch (x) {
                            case > 0 and < 100 or > 200:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithNotNullPattern()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(object? x) {
                        switch (x) {
                            case not null:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithNotCombinedPattern()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(object x) {
                        switch (x) {
                            case not (int or string):
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithPropertyPattern()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string s) {
                        switch (s) {
                            case { Length: > 5 }:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithMultiplePropertyPatterns()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string s) {
                        switch (s) {
                            case { Length: > 5, Length: < 10 }:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithPropertyPatternConstant()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string s) {
                        switch (s) {
                            case { Length: 5 }:
                                break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SwitchWithPropertyPatternWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string s) {
                        switch (s) {
                            case { Length : > 5 }:
                                break;
                        }
                    }
                }
                """
            )
        );
    }
}
