using Rewrite.Test;

namespace Rewrite.CSharp.Tests.Tree;

public class MethodInvocationTests : RewriteTest
{
    [Fact]
    public void SimpleMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething();
                    }
                    void DoSomething() { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodCallWithArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(1, 2, 3);
                    }
                    void DoSomething(int a, int b, int c) { }
                }
                """
            )
        );
    }

    [Fact]
    public void QualifiedMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        Console.WriteLine("hello");
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodCallOnInstance()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = "hello";
                        x.ToString();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodCallWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething ( 1 , 2 );
                    }
                    void DoSomething(int a, int b) { }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedMethodCalls()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        "hello".ToUpper().ToLower();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullConditionalMethodCall()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = "hello";
                        x?.ToString();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ChainedNullConditionalMethodCalls()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = "hello";
                        x?.ToUpper()?.ToLower();
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullConditionalWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        var x = "hello";
                        x ?. ToString ( );
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NamedArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(name: "foo", value: 42);
                    }
                    void DoSomething(string name, int value) { }
                }
                """
            )
        );
    }

    [Fact]
    public void NamedArgumentsReordered()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(value: 42, name: "foo");
                    }
                    void DoSomething(string name, int value) { }
                }
                """
            )
        );
    }

    [Fact]
    public void NamedArgumentsWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething( name : "foo" , value : 42 );
                    }
                    void DoSomething(string name, int value) { }
                }
                """
            )
        );
    }

    [Fact]
    public void MixedNamedAndPositionalArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething("foo", value: 42);
                    }
                    void DoSomething(string name, int value) { }
                }
                """
            )
        );
    }

    [Fact]
    public void RefArgument()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int x = 0;
                        DoSomething(ref x);
                    }
                    void DoSomething(ref int value) { }
                }
                """
            )
        );
    }

    [Fact]
    public void OutArgumentWithExistingVariable()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int result;
                        DoSomething(out result);
                    }
                    void DoSomething(out int value) { value = 42; }
                }
                """
            )
        );
    }

    [Fact]
    public void OutArgumentWithVarDeclaration()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(out var result);
                    }
                    void DoSomething(out int value) { value = 42; }
                }
                """
            )
        );
    }

    [Fact]
    public void OutArgumentWithExplicitType()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(out int result);
                    }
                    void DoSomething(out int value) { value = 42; }
                }
                """
            )
        );
    }

    [Fact]
    public void OutArgumentWithDiscard()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething(out _);
                    }
                    void DoSomething(out int value) { value = 42; }
                }
                """
            )
        );
    }

    [Fact]
    public void InArgument()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int x = 42;
                        DoSomething(in x);
                    }
                    void DoSomething(in int value) { }
                }
                """
            )
        );
    }

    [Fact]
    public void RefArgumentWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int x = 0;
                        DoSomething( ref  x );
                    }
                    void DoSomething(ref int value) { }
                }
                """
            )
        );
    }

    [Fact]
    public void OutVarWithSpaces()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        DoSomething( out  var  result );
                    }
                    void DoSomething(out int value) { value = 42; }
                }
                """
            )
        );
    }

    [Fact]
    public void MixedRefAndPositionalArguments()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        int x = 0;
                        DoSomething("hello", ref x, 42);
                    }
                    void DoSomething(string a, ref int b, int c) { }
                }
                """
            )
        );
    }
}
