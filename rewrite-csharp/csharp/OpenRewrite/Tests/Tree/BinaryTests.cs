using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class BinaryTests : RewriteTest
{
    [Theory]
    [InlineData("1 + 2;")]
    [InlineData("5 - 3;")]
    [InlineData("4 * 2;")]
    [InlineData("10 / 2;")]
    [InlineData("10 % 3;")]
    [InlineData("1 < 2;")]
    [InlineData("1 > 2;")]
    [InlineData("1 <= 2;")]
    [InlineData("1 >= 2;")]
    [InlineData("x == y;")]
    [InlineData("x != y;")]
    [InlineData("a && b;")]
    [InlineData("a || b;")]
    [InlineData("a & b;")]
    [InlineData("a | b;")]
    [InlineData("a ^ b;")]
    [InlineData("a << 2;")]
    [InlineData("a >> 2;")]
    public void BinaryOperator(string source)
    {
        RewriteRun(CSharp(source));
    }

    [Fact]
    public void ChainedBinary()
    {
        RewriteRun(
            CSharp(
                """
                1 + 2 + 3;
                """
            )
        );
    }

    [Fact]
    public void BinaryWithIdentifiers()
    {
        RewriteRun(
            CSharp(
                """
                a + b;
                """
            )
        );
    }

    [Fact]
    public void BinaryWithExtraSpaces()
    {
        RewriteRun(
            CSharp(
                """
                1  +  2;
                """
            )
        );
    }
}
