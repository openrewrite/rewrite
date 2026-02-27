using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class TernaryTests : RewriteTest
{
    [Fact]
    public void SimpleTernary()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = true ? 1 : 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TernaryWithIdentifierCondition()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool b) {
                        var x = b ? "yes" : "no";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TernaryWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = true  ?  1  :  2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TernaryWithComparisonCondition()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int a) {
                        var x = a > 5 ? "big" : "small";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedTernary()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int a) {
                        var x = a > 10 ? "big" : a > 5 ? "medium" : "small";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TernaryWithMethodCalls()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool b) {
                        var x = b ? GetFirst() : GetSecond();
                    }
                    int GetFirst() { return 1; }
                    int GetSecond() { return 2; }
                }
                """
            )
        );
    }

    [Fact]
    public void TernaryInMethodArgument()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool b) {
                        Console.WriteLine(b ? "yes" : "no");
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TernaryInReturn()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar(bool b) {
                        return b ? 1 : 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TernaryWithNullLiterals()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool b) {
                        string? x = b ? "value" : null;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TernaryWithNewExpressions()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool b) {
                        var x = b ? new Foo() : new Foo();
                    }
                }
                """
            )
        );
    }

    // Null-coalescing tests

    [Fact]
    public void SimpleNullCoalescing()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string? s) {
                        var x = s ?? "default";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullCoalescingWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string? s) {
                        var x = s  ??  "default";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullCoalescingWithMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = GetValue() ?? "default";
                    }
                    string? GetValue() { return null; }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedNullCoalescing()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string? a, string? b) {
                        var x = a ?? b ?? "default";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullCoalescingWithNullConditional()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(Foo? f) {
                        var x = f?.ToString() ?? "null";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullCoalescingInReturn()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    string Bar(string? s) {
                        return s ?? "default";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullCoalescingInMethodArgument()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string? s) {
                        Console.WriteLine(s ?? "default");
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullCoalescingWithIndexAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(string[]? arr) {
                        var x = arr?[0] ?? "default";
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MixedTernaryAndNullCoalescing()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool b, string? s) {
                        var x = b ? s ?? "default" : "other";
                    }
                }
                """
            )
        );
    }
}
