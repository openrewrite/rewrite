using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class LiteralTests : RewriteTest
{
    [Fact]
    public void StringLiteral()
    {
        RewriteRun(
            CSharp(
                """
                "hello";
                """
            )
        );
    }

    [Fact]
    public void NumericLiteral()
    {
        RewriteRun(
            CSharp(
                """
                42;
                """
            )
        );
    }

    [Fact]
    public void BooleanLiteral()
    {
        RewriteRun(
            CSharp(
                """
                true;
                """
            )
        );
    }

    [Fact]
    public void NullLiteral()
    {
        RewriteRun(
            CSharp(
                """
                null;
                """
            )
        );
    }

    [Fact]
    public void CharLiteral()
    {
        RewriteRun(
            CSharp(
                """
                'a';
                """
            )
        );
    }

    [Fact]
    public void StringLiteralWithLeadingWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                  "hello";
                """
            )
        );
    }

    [Fact]
    public void MultipleLiterals()
    {
        RewriteRun(
            CSharp(
                """
                "hello";
                42;
                """
            )
        );
    }
}
