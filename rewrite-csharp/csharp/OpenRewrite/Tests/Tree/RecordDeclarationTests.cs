using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class RecordDeclarationTests : RewriteTest
{
    [Fact]
    public void SimpleRecord()
    {
        RewriteRun(
            CSharp(
                """
                record Person {
                }
                """
            )
        );
    }

    [Fact]
    public void RecordWithPrimaryConstructor()
    {
        RewriteRun(
            CSharp(
                """
                record Person(string Name, int Age) {
                }
                """
            )
        );
    }

    [Fact]
    public void PositionalRecord()
    {
        RewriteRun(
            CSharp(
                """
                record Person(string Name, int Age);
                """
            )
        );
    }

    [Fact]
    public void RecordWithBody()
    {
        RewriteRun(
            CSharp(
                """
                record Person {
                    public string Name { get; init; }
                }
                """
            )
        );
    }

    [Fact]
    public void RecordClass()
    {
        RewriteRun(
            CSharp(
                """
                record class Person {
                }
                """
            )
        );
    }

    [Fact]
    public void RecordStruct()
    {
        RewriteRun(
            CSharp(
                """
                record struct Point {
                }
                """
            )
        );
    }

    [Fact]
    public void RecordStructWithPrimaryConstructor()
    {
        RewriteRun(
            CSharp(
                """
                record struct Point(int X, int Y) {
                }
                """
            )
        );
    }

    [Fact]
    public void PositionalRecordStruct()
    {
        RewriteRun(
            CSharp(
                """
                record struct Point(int X, int Y);
                """
            )
        );
    }

    [Fact]
    public void RecordWithInheritance()
    {
        RewriteRun(
            CSharp(
                """
                record Person(string Name);
                record Employee(string Name, string Department) : Person(Name);
                """
            )
        );
    }

    [Fact]
    public void GenericRecord()
    {
        RewriteRun(
            CSharp(
                """
                record Container<T>(T Value) {
                }
                """
            )
        );
    }

    [Fact]
    public void RecordWithModifiers()
    {
        RewriteRun(
            CSharp(
                """
                public sealed record Person(string Name) {
                }
                """
            )
        );
    }
}
