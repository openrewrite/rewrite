using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class ParenthesesTests : RewriteTest
{
    [Fact]
    public void ParenthesizedLiteral()
    {
        RewriteRun(
            CSharp(
                """
                (42);
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedIdentifier()
    {
        RewriteRun(
            CSharp(
                """
                (foo);
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedBinary()
    {
        RewriteRun(
            CSharp(
                """
                (1 + 2);
                """
            )
        );
    }

    [Fact]
    public void NestedParentheses()
    {
        RewriteRun(
            CSharp(
                """
                ((42));
                """
            )
        );
    }

    [Fact]
    public void ParenthesesWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                ( 42 );
                """
            )
        );
    }

    [Fact]
    public void ParenthesesInBinary()
    {
        RewriteRun(
            CSharp(
                """
                (1 + 2) * 3;
                """
            )
        );
    }

    [Fact]
    public void ComplexExpression()
    {
        RewriteRun(
            CSharp(
                """
                (a + b) * (c - d);
                """
            )
        );
    }
}
