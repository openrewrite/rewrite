using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class NewClassTests : RewriteTest
{
    [Fact]
    public void SimpleConstructor()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = new Foo();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ConstructorWithArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = new Foo(1, "hello");
                    }
                    Foo(int a, string b) { }
                }
                """
            )
        );
    }

    [Fact]
    public void ConstructorWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = new  Foo ( 1 , 2 );
                    }
                    Foo(int a, int b) { }
                }
                """
            )
        );
    }

    [Fact]
    public void GenericConstructor()
    {
        RewriteRun(
            CSharp(
                """
                using System.Collections.Generic;
                class Foo {
                    void Bar() {
                        var x = new List<int>();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedGenericConstructor()
    {
        RewriteRun(
            CSharp(
                """
                using System.Collections.Generic;
                class Foo {
                    void Bar() {
                        var x = new Dictionary<string, List<int>>();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ConstructorAsArgument()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(new Foo());
                    }
                    void DoSomething(Foo f) { }
                }
                """
            )
        );
    }

    [Fact]
    public void ConstructorInReturn()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    Foo Bar() {
                        return new Foo();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ConstructorInBinaryExpression()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = new Foo() == null;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedMethodOnConstructor()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = new Foo().ToString();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ConstructorWithNamedArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = new Foo(value: 42, name: "test");
                    }
                    Foo(string name, int value) { }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedConstructorCalls()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = new Foo(new Foo());
                    }
                    Foo() { }
                    Foo(Foo f) { }
                }
                """
            )
        );
    }

    [Fact]
    public void QualifiedConstructor()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = new System.Text.StringBuilder();
                    }
                }
                """
            )
        );
    }
}
