using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class UsingDirectiveTests : RewriteTest
{
    [Fact]
    public void SimpleUsing()
    {
        RewriteRun(
            CSharp(
                """
                using System;
                """
            )
        );
    }

    [Fact]
    public void QualifiedUsing()
    {
        RewriteRun(
            CSharp(
                """
                using System.Collections.Generic;
                """
            )
        );
    }

    [Fact]
    public void UsingWithClass()
    {
        RewriteRun(
            CSharp(
                """
                using System;

                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleUsings()
    {
        RewriteRun(
            CSharp(
                """
                using System;
                using System.Collections.Generic;
                using System.Linq;
                """
            )
        );
    }

    [Fact]
    public void UsingStatic()
    {
        RewriteRun(
            CSharp(
                """
                using static System.Math;
                """
            )
        );
    }

    [Fact]
    public void UsingAlias()
    {
        RewriteRun(
            CSharp(
                """
                using Sys = System;
                """
            )
        );
    }

    [Fact]
    public void UsingAliasQualified()
    {
        RewriteRun(
            CSharp(
                """
                using Col = System.Collections.Generic;
                """
            )
        );
    }
}
