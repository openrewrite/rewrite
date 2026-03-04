using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class EnumDeclarationTests : RewriteTest
{
    [Fact]
    public void SimpleEnum()
    {
        RewriteRun(
            CSharp(
                """
                enum Color {
                    Red,
                    Green,
                    Blue
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithExplicitValues()
    {
        RewriteRun(
            CSharp(
                """
                enum Status {
                    Active = 1,
                    Inactive = 0,
                    Pending = 2
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithPublicModifier()
    {
        RewriteRun(
            CSharp(
                """
                public enum Color {
                    Red,
                    Green,
                    Blue
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithUnderlyingType()
    {
        RewriteRun(
            CSharp(
                """
                enum Flags : byte {
                    None = 0,
                    First = 1,
                    Second = 2
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                enum  Color  {
                    Red ,
                    Green ,
                    Blue
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithInternalModifier()
    {
        RewriteRun(
            CSharp(
                """
                internal enum Status {
                    Active,
                    Inactive
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithMixedValues()
    {
        RewriteRun(
            CSharp(
                """
                enum Priority {
                    Low,
                    Medium = 5,
                    High
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithExpressionValues()
    {
        RewriteRun(
            CSharp(
                """
                enum Flags {
                    None = 0,
                    First = 1,
                    Second = 1 << 1,
                    Third = 1 << 2
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithSingleMember()
    {
        RewriteRun(
            CSharp(
                """
                enum Single {
                    Value
                }
                """
            )
        );
    }

    [Fact]
    public void EnumWithLongUnderlyingType()
    {
        RewriteRun(
            CSharp(
                """
                enum BigNumbers : long {
                    Big = 1000000000000,
                    Bigger = 2000000000000
                }
                """
            )
        );
    }
}
