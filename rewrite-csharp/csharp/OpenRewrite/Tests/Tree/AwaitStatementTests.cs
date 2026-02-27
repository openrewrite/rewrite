using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class AwaitStatementTests : RewriteTest
{
    [Fact]
    public void AwaitForeach()
    {
        RewriteRun(
            CSharp(
                """
                using System.Collections.Generic;
                using System.Threading.Tasks;
                class C {
                    async Task M(IAsyncEnumerable<int> items) {
                        await foreach (var x in items) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AwaitUsing()
    {
        RewriteRun(
            CSharp(
                """
                using System.IO;
                using System.Threading.Tasks;
                class C {
                    async Task M() {
                        await using (var x = new MemoryStream()) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void RegularForeach()
    {
        RewriteRun(
            CSharp(
                """
                class C {
                    void M(int[] items) {
                        foreach (var x in items) { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void RegularUsing()
    {
        RewriteRun(
            CSharp(
                """
                using System.IO;
                class C {
                    void M() {
                        using (var x = new MemoryStream()) { }
                    }
                }
                """
            )
        );
    }
}
