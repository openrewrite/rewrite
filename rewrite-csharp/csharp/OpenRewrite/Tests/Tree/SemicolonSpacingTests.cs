using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class SemicolonSpacingTests : RewriteTest
{
    [Fact]
    public void ExpressionStatementSimple()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        Console.WriteLine();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExpressionStatementSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        Console.WriteLine() ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ReturnSimple()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    int M() {
                        return 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ReturnSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    int M() {
                        return 1 ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void VariableDeclarationLocal()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        int x = 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void VariableDeclarationSpaceBeforeSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        int x = 1 ;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FieldDeclaration()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    int x = 1;
                }
                """
            )
        );
    }

    [Fact]
    public void AbstractMethod()
    {
        RewriteRun(
            CSharp(
                """
                abstract class C {
                    abstract void M();
                }
                """
            )
        );
    }
}
