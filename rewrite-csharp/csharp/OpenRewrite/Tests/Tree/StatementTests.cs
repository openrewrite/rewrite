using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class ReturnTests : RewriteTest
{
    [Fact]
    public void ReturnNoValue()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        return;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ReturnValue()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar() {
                        return 1;
                    }
                }
                """
            )
        );
    }
}

public class IfTests : RewriteTest
{
    [Fact]
    public void SimpleIf()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        if (true) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void IfWithElse()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        if (true) { } else { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void IfElseIf()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        if (true) { } else if (false) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void IfWithBooleanNegation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(bool x) {
                        if (!x) { }
                    }
                }
                """
            )
        );
    }
}

public class WhileLoopTests : RewriteTest
{
    [Fact]
    public void SimpleWhile()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        while (true) { }
                    }
                }
                """
            )
        );
    }
}

public class DoWhileLoopTests : RewriteTest
{
    [Fact]
    public void SimpleDoWhile()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        do { } while (true);
                    }
                }
                """
            )
        );
    }
}

public class ForLoopTests : RewriteTest
{
    [Fact]
    public void SimpleFor()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        for (int i = 0; i < 10; i++) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ForWithoutInit()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        for (;;) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ForWithTrailingSpace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        for (; true; ) { }
                    }
                }
                """
            )
        );
    }
}

public class ForEachLoopTests : RewriteTest
{
    [Fact]
    public void SimpleForeach()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        foreach (var x in items) { }
                    }
                }
                """
            )
        );
    }
}

public class TryTests : RewriteTest
{
    [Fact]
    public void TryCatch()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        try { } catch (Exception e) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TryFinally()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        try { } finally { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TryCatchFinally()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        try { } catch (Exception e) { } finally { }
                    }
                }
                """
            )
        );
    }
}

public class ThrowTests : RewriteTest
{
    [Fact]
    public void ThrowExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        throw e;
                    }
                }
                """
            )
        );
    }
}

public class BreakContinueTests : RewriteTest
{
    [Fact]
    public void BreakStatement()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        while (true) {
                            break;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ContinueStatement()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        while (true) {
                            continue;
                        }
                    }
                }
                """
            )
        );
    }
}

public class EmptyStatementTests : RewriteTest
{
    [Fact]
    public void EmptyStatement()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        ;
                    }
                }
                """
            )
        );
    }
}
