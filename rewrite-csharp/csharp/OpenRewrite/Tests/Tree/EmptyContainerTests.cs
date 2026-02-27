using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class EmptyContainerTests : RewriteTest
{
    [Fact]
    public void EmptyPropertyPattern()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    bool M(object o) => o is { };
                }
                """
            )
        );
    }

    [Fact]
    public void EmptyPropertyPatternWithSpace()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    bool M(object o) => o is {  };
                }
                """
            )
        );
    }

    [Fact]
    public void NonEmptyPropertyPattern()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    bool M(string o) => o is { Length: 0 };
                }
                """
            )
        );
    }
}
