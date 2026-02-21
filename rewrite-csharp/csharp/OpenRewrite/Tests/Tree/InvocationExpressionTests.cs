using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class InvocationExpressionTests : RewriteTest
{
    [Fact]
    public void ChainedInvocation()
    {
        // GetAction()() - the outer invocation has a MethodInvocation as its expression
        // This creates Cs.InvocationExpression wrapping J.MethodInvocation
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        GetAction()();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedInvocationWithArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        GetFunc()(1, 2);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TripleChainedInvocation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        GetFunc()()();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedInvocationWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        GetAction() ( );
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void QualifiedChainedInvocation()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        obj.GetAction()();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesizedInvocation()
    {
        // (GetAction())() - parenthesized method call then invoked
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        (GetAction())();
                    }
                }
                """
            )
        );
    }
}
