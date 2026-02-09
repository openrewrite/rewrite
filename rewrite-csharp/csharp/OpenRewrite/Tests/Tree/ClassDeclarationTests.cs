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

    [Fact]
    public void ClassWithBaseClass()
    {
        RewriteRun(
            CSharp(
                """
                class Base { }
                class Derived : Base { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                class Foo : IFoo { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithMultipleInterfaces()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                interface IBar { }
                class Foo : IFoo, IBar { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithBaseClassAndInterfaces()
    {
        RewriteRun(
            CSharp(
                """
                class Base { }
                interface IFoo { }
                interface IBar { }
                class Derived : Base, IFoo, IBar { }
                """
            )
        );
    }

    [Fact]
    public void StructWithInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                struct Foo : IFoo { }
                """
            )
        );
    }

    [Fact]
    public void InterfaceExtendingInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                interface IBar : IFoo { }
                """
            )
        );
    }

    [Fact]
    public void InterfaceExtendingMultipleInterfaces()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                interface IBar { }
                interface IBaz : IFoo, IBar { }
                """
            )
        );
    }

    [Fact]
    public void RecordWithInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                record Foo : IFoo { }
                """
            )
        );
    }

    [Fact]
    public void GenericClass()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassMultipleTypeParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T, U> { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassWithInheritance()
    {
        RewriteRun(
            CSharp(
                """
                class Base<T> { }
                class Derived<T> : Base<T> { }
                """
            )
        );
    }

    [Fact]
    public void GenericInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<T> { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithAttribute()
    {
        RewriteRun(
            CSharp(
                """
                [System.Serializable]
                class Foo { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithMultipleAttributes()
    {
        RewriteRun(
            CSharp(
                """
                [System.Serializable]
                [System.Obsolete]
                class Foo { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithAttributeArguments()
    {
        RewriteRun(
            CSharp(
                """
                [System.Obsolete("Use NewClass instead")]
                class Foo { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassWithClassConstraint()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : class { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassWithStructConstraint()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : struct { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassWithNewConstraint()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : new() { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassWithMultipleConstraints()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : class, new() { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassWithBaseTypeConstraint()
    {
        RewriteRun(
            CSharp(
                """
                class Base { }
                class Foo<T> where T : Base { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassWithMultipleTypeParameterConstraints()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T, U> where T : class where U : new() { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithPrimaryConstructor()
    {
        RewriteRun(
            CSharp(
                """
                class Foo(int x, string y) { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithPrimaryConstructorAndBaseCall()
    {
        RewriteRun(
            CSharp(
                """
                class Base(int x) { }
                class Derived(int x) : Base(x) { }
                """
            )
        );
    }

    [Fact]
    public void StructWithPrimaryConstructor()
    {
        RewriteRun(
            CSharp(
                """
                struct Foo(int x, string y) { }
                """
            )
        );
    }
}
