using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class MemberReferenceTests : RewriteTest
{
    [Fact]
    public void GenericMethodGroupReference()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M() {
                        var x = items.Select<string>;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SimpleMethodGroupReference()
    {
        RewriteRun(
            CSharp(
                """
                using System;
                class C {
                    void M() {
                        Action a = Console.WriteLine;
                    }
                }
                """
            )
        );
    }
}
