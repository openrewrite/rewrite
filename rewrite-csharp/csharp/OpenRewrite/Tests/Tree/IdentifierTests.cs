using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class IdentifierTests : RewriteTest
{
    [Fact]
    public void SimpleIdentifier()
    {
        RewriteRun(
            CSharp(
                """
                foo;
                """
            )
        );
    }

    [Fact]
    public void IdentifierWithLeadingWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                  bar;
                """
            )
        );
    }

    [Fact]
    public void MultipleIdentifiers()
    {
        RewriteRun(
            CSharp(
                """
                foo;
                bar;
                """
            )
        );
    }
}
