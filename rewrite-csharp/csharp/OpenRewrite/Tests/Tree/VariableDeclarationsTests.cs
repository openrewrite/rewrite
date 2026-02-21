using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class VariableDeclarationsTests : RewriteTest
{
    [Fact]
    public void VarWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                var x = 1;
                """
            )
        );
    }

    [Fact]
    public void IntWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                int x = 1;
                """
            )
        );
    }

    [Fact]
    public void StringWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                string s = "hello";
                """
            )
        );
    }

    [Fact]
    public void BoolWithInitializer()
    {
        RewriteRun(
            CSharp(
                """
                bool b = true;
                """
            )
        );
    }

    [Fact]
    public void MultipleVariables()
    {
        RewriteRun(
            CSharp(
                """
                int x = 1, y = 2;
                """
            )
        );
    }

    [Fact]
    public void ConstVariable()
    {
        RewriteRun(
            CSharp(
                """
                const int x = 1;
                """
            )
        );
    }

    [Fact]
    public void VariableWithExpression()
    {
        RewriteRun(
            CSharp(
                """
                var x = 1 + 2;
                """
            )
        );
    }

    [Fact]
    public void VariableWithLeadingWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                  var x = 1;
                """
            )
        );
    }

    [Fact]
    public void MultipleStatements()
    {
        RewriteRun(
            CSharp(
                """
                var x = 1;
                var y = 2;
                """
            )
        );
    }

    [Fact]
    public void VariableWithExtraSpaces()
    {
        RewriteRun(
            CSharp(
                """
                var  x  =  1;
                """
            )
        );
    }
}
