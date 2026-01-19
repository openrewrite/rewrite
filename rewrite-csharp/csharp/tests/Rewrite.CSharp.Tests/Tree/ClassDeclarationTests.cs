using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class ClassDeclarationTests : RewriteTest
{
    [Fact]
    public void EmptyClass()
    {
        RewriteRun(
            CSharp(
                """
                class Foo { }
                """
            )
        );
    }

    [Fact]
    public void PublicClass()
    {
        RewriteRun(
            CSharp(
                """
                public class Foo { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithMultipleModifiers()
    {
        RewriteRun(
            CSharp(
                """
                public sealed class Foo { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithField()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    var x = 1;
                }
                """
            )
        );
    }

    [Theory]
    [InlineData("class Foo { }")]
    [InlineData("struct Foo { }")]
    [InlineData("interface Foo { }")]
    [InlineData("record Foo { }")]
    public void TypeDeclarationKinds(string source)
    {
        RewriteRun(CSharp(source));
    }
}
