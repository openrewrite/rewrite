using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class BlockTests : RewriteTest
{
    [Fact]
    public void EmptyBlock()
    {
        RewriteRun(
            CSharp(
                """
                { }
                """
            )
        );
    }

    [Fact]
    public void BlockWithStatement()
    {
        RewriteRun(
            CSharp(
                """
                { var x = 1; }
                """
            )
        );
    }

    [Fact]
    public void BlockWithMultipleStatements()
    {
        RewriteRun(
            CSharp(
                """
                {
                    var x = 1;
                    var y = 2;
                }
                """
            )
        );
    }
}
