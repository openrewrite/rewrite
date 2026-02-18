using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class AttributeListTests : RewriteTest
{
    [Fact]
    public void SimpleAttribute()
    {
        RewriteRun(
            CSharp(
                """
                [Serializable]
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void AttributeWithArgument()
    {
        RewriteRun(
            CSharp(
                """
                [Obsolete("This is deprecated")]
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleAttributes()
    {
        RewriteRun(
            CSharp(
                """
                [Serializable]
                [Obsolete("deprecated")]
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void AttributesOnSameLine()
    {
        RewriteRun(
            CSharp(
                """
                [Serializable, Obsolete("msg")]
                class Foo {
                }
                """
            )
        );
    }
}
