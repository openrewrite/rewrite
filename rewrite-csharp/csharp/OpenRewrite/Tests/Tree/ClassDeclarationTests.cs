using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

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

    // ==================== Modifiers ====================

    [Theory]
    [InlineData("public class Foo { }")]
    [InlineData("internal class Foo { }")]
    [InlineData("private class Foo { }")]
    [InlineData("protected class Foo { }")]
    [InlineData("static class Foo { }")]
    [InlineData("abstract class Foo { }")]
    [InlineData("sealed class Foo { }")]
    [InlineData("partial class Foo { }")]
    [InlineData("unsafe class Foo { }")]
    public void SingleModifiers(string source)
    {
        RewriteRun(CSharp(source));
    }

    [Fact]
    public void PublicStaticClass()
    {
        RewriteRun(
            CSharp(
                """
                public static class Foo { }
                """
            )
        );
    }

    [Fact]
    public void PublicAbstractClass()
    {
        RewriteRun(
            CSharp(
                """
                public abstract class Foo { }
                """
            )
        );
    }

    [Fact]
    public void ProtectedInternalClass()
    {
        RewriteRun(
            CSharp(
                """
                class Outer {
                    protected internal class Inner { }
                }
                """
            )
        );
    }

    [Fact]
    public void PrivateProtectedClass()
    {
        RewriteRun(
            CSharp(
                """
                class Outer {
                    private protected class Inner { }
                }
                """
            )
        );
    }

    [Fact]
    public void NewModifierOnNestedClass()
    {
        RewriteRun(
            CSharp(
                """
                class Base {
                    public class Inner { }
                }
                class Derived : Base {
                    public new class Inner { }
                }
                """
            )
        );
    }

    [Fact]
    public void UnsafeClass()
    {
        RewriteRun(
            CSharp(
                """
                public unsafe class Foo { }
                """
            )
        );
    }

    [Fact]
    public void PartialClassMultipleModifiers()
    {
        RewriteRun(
            CSharp(
                """
                public sealed partial class Foo { }
                """
            )
        );
    }

    [Fact]
    public void FileModifier()
    {
        RewriteRun(
            CSharp(
                """
                file class Foo { }
                """
            )
        );
    }

    [Fact]
    public void RequiredMemberInClass()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public required string Name { get; set; }
                }
                """
            )
        );
    }

    // ==================== Nesting ====================

    [Fact]
    public void NestedClasses()
    {
        RewriteRun(
            CSharp(
                """
                class Outer {
                    class Inner { }
                }
                """
            )
        );
    }

    [Fact]
    public void DeeplyNestedClasses()
    {
        RewriteRun(
            CSharp(
                """
                class A {
                    class B {
                        class C { }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ClassInsideStruct()
    {
        RewriteRun(
            CSharp(
                """
                struct Outer {
                    class Inner { }
                }
                """
            )
        );
    }

    [Fact]
    public void ClassInsideInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IOuter {
                    class Inner { }
                }
                """
            )
        );
    }

    // ==================== Generic Variance ====================

    [Fact]
    public void CovariantTypeParameter()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<out T> { }
                """
            )
        );
    }

    [Fact]
    public void ContravariantTypeParameter()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<in T> { }
                """
            )
        );
    }

    [Fact]
    public void MixedVarianceTypeParameters()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<in TKey, out TValue> { }
                """
            )
        );
    }

    // ==================== Generic Constraints (all variations) ====================

    [Fact]
    public void ConstraintClassNullable()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : class? { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintNotNull()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : notnull { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintUnmanaged()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : unmanaged { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintDefault()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : default { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintInterfaceType()
    {
        RewriteRun(
            CSharp(
                """
                interface IDisposable { }
                class Foo<T> where T : IDisposable { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintGenericInterfaceType()
    {
        RewriteRun(
            CSharp(
                """
                interface IComparable<T> { }
                class Foo<T> where T : IComparable<T> { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintMultipleInterfaceTypes()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                interface IBar { }
                class Foo<T> where T : IFoo, IBar { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintBaseClassAndInterface()
    {
        RewriteRun(
            CSharp(
                """
                class Base { }
                interface IFoo { }
                class Foo<T> where T : Base, IFoo { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintClassInterfaceNew()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                class Foo<T> where T : class, IFoo, new() { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintStructNew()
    {
        // struct + new() is valid (new() is implied by struct, but allowed)
        RewriteRun(
            CSharp(
                """
                class Foo<T> where T : struct, new() { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintOnMultipleTypeParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T, U, V> where T : class where U : struct where V : new() { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintTypeParameterDependsOnAnother()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T, U> where T : class where U : T { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintOnGenericInterfaceWithMultipleTypeArgs()
    {
        RewriteRun(
            CSharp(
                """
                interface IDictionary<TKey, TValue> { }
                class Foo<T> where T : IDictionary<string, int> { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintsOnStruct()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                struct Foo<T> where T : IFoo { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintsOnInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<T> where T : class { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintsOnRecord()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                record Foo<T>(T Value) where T : IFoo { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintsOnRecordStruct()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                record struct Foo<T>(T Value) where T : IFoo;
                """
            )
        );
    }

    // ==================== Attributes on Type Parameters ====================

    [Fact]
    public void AttributeOnTypeParameter()
    {
        RewriteRun(
            CSharp(
                """
                using System.Diagnostics.CodeAnalysis;
                class Foo<[MaybeNull] T> { }
                """
            )
        );
    }

    [Fact]
    public void MultipleAttributesOnTypeParameter()
    {
        RewriteRun(
            CSharp(
                """
                using System.Diagnostics.CodeAnalysis;
                class Foo<[MaybeNull] [NotNull] T> { }
                """
            )
        );
    }

    [Fact]
    public void AttributeOnMultipleTypeParameters()
    {
        RewriteRun(
            CSharp(
                """
                using System.Diagnostics.CodeAnalysis;
                class Foo<[MaybeNull] T, [NotNull] U> { }
                """
            )
        );
    }

    [Fact]
    public void AttributeOnCovariantTypeParameter()
    {
        RewriteRun(
            CSharp(
                """
                using System.Diagnostics.CodeAnalysis;
                interface IFoo<[MaybeNull] out T> { }
                """
            )
        );
    }

    // ==================== Primary Constructors ====================

    [Fact]
    public void PrimaryConstructorWithDefaultValues()
    {
        RewriteRun(
            CSharp(
                """
                class Foo(int x = 0, string y = "hello") { }
                """
            )
        );
    }

    [Fact]
    public void PrimaryConstructorWithRefParams()
    {
        RewriteRun(
            CSharp(
                """
                struct Foo(ref int x) { }
                """
            )
        );
    }

    [Fact]
    public void PrimaryConstructorWithParamsArray()
    {
        RewriteRun(
            CSharp(
                """
                class Foo(params int[] values) { }
                """
            )
        );
    }

    [Fact]
    public void PrimaryConstructorSingleParameter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo(int x) { }
                """
            )
        );
    }

    [Fact]
    public void PrimaryConstructorNoParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo() { }
                """
            )
        );
    }

    [Fact]
    public void PrimaryConstructorWithGenericTypeParameter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo<T>(T value) { }
                """
            )
        );
    }

    [Fact]
    public void PrimaryConstructorWithBaseCallMultipleArgs()
    {
        RewriteRun(
            CSharp(
                """
                class Base(int a, int b) { }
                class Derived(int x, int y) : Base(x, y) { }
                """
            )
        );
    }

    [Fact]
    public void RecordWithPrimaryConstructorAndBaseCall()
    {
        RewriteRun(
            CSharp(
                """
                record Base(string Name);
                record Derived(string Name, int Age) : Base(Name);
                """
            )
        );
    }

    [Fact]
    public void PrimaryConstructorWithAttributeOnParameter()
    {
        RewriteRun(
            CSharp(
                """
                using System;
                class Foo([Obsolete] int x) { }
                """
            )
        );
    }

    // ==================== Record Variations ====================

    [Fact]
    public void RecordClassExplicit()
    {
        RewriteRun(
            CSharp(
                """
                record class Foo(string Name) { }
                """
            )
        );
    }

    [Fact]
    public void RecordStructExplicit()
    {
        RewriteRun(
            CSharp(
                """
                record struct Foo(int X, int Y) { }
                """
            )
        );
    }

    [Fact]
    public void ReadonlyRecordStruct()
    {
        RewriteRun(
            CSharp(
                """
                readonly record struct Point(int X, int Y);
                """
            )
        );
    }

    [Fact]
    public void RecordWithSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                record Foo(string Name);
                """
            )
        );
    }

    [Fact]
    public void RecordClassWithSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                record class Foo(string Name);
                """
            )
        );
    }

    [Fact]
    public void RecordStructWithSemicolon()
    {
        RewriteRun(
            CSharp(
                """
                record struct Foo(int X);
                """
            )
        );
    }

    [Fact]
    public void RecordWithBodyAndSemicolon()
    {
        // Record with primary constructor and a body (no semicolon shorthand)
        RewriteRun(
            CSharp(
                """
                record Foo(string Name) {
                    public string Display => Name;
                }
                """
            )
        );
    }

    [Fact]
    public void RecordWithGenericConstraints()
    {
        RewriteRun(
            CSharp(
                """
                record Foo<T>(T Value) where T : class, new() { }
                """
            )
        );
    }

    // ==================== Inheritance Combinations ====================

    [Fact]
    public void ClassWithGenericBaseClass()
    {
        RewriteRun(
            CSharp(
                """
                class Base<T> { }
                class Derived : Base<int> { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithGenericInterfaceImplementation()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<T> { }
                class Foo : IFoo<string> { }
                """
            )
        );
    }

    [Fact]
    public void ClassWithMultipleGenericInterfaceImplementations()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<T> { }
                interface IBar<T, U> { }
                class Foo : IFoo<int>, IBar<string, bool> { }
                """
            )
        );
    }

    [Fact]
    public void GenericClassWithGenericBaseAndInterfaces()
    {
        RewriteRun(
            CSharp(
                """
                class Base<T> { }
                interface IFoo<T> { }
                interface IBar { }
                class Derived<T> : Base<T>, IFoo<T>, IBar where T : class { }
                """
            )
        );
    }

    [Fact]
    public void RecordStructWithInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                record struct Foo(int X) : IFoo;
                """
            )
        );
    }

    // ==================== Struct Variations ====================

    [Fact]
    public void ReadonlyRefStruct()
    {
        RewriteRun(
            CSharp(
                """
                readonly ref struct Foo { }
                """
            )
        );
    }

    [Fact]
    public void RefStruct()
    {
        RewriteRun(
            CSharp(
                """
                ref struct Foo { }
                """
            )
        );
    }

    [Fact]
    public void StructWithPrimaryConstructorAndInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                struct Foo(int x) : IFoo { }
                """
            )
        );
    }

    // ==================== Generic with Complex Constraints ====================

    [Fact]
    public void ThreeTypeParametersWithDifferentConstraints()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                class Base { }
                class Foo<T, U, V> where T : Base, IFoo where U : class, new() where V : struct { }
                """
            )
        );
    }

    [Fact]
    public void ConstraintWithWhitespaceVariations()
    {
        RewriteRun(
            CSharp(
                """
                class Foo< T , U >  where  T  :  class  where  U  :  new() { }
                """
            )
        );
    }

    // ==================== Body Variations ====================

    [Fact]
    public void ClassWithMultipleMemberKinds()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    private int _x;
                    public int X { get; set; }
                    public void Bar() { }
                    public class Inner { }
                }
                """
            )
        );
    }

    [Fact]
    public void EmptyClassWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class  Foo  {  }
                """
            )
        );
    }

    [Fact]
    public void ClassWithEmptyBodyOnSeparateLines()
    {
        RewriteRun(
            CSharp(
                """
                class Foo
                {
                }
                """
            )
        );
    }

    // ==================== Partial Types ====================

    [Fact]
    public void PartialClass()
    {
        RewriteRun(
            CSharp(
                """
                partial class Foo {
                    void Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void PartialStruct()
    {
        RewriteRun(
            CSharp(
                """
                partial struct Foo {
                    public int X;
                }
                """
            )
        );
    }

    [Fact]
    public void PartialInterface()
    {
        RewriteRun(
            CSharp(
                """
                partial interface IFoo {
                    void Bar();
                }
                """
            )
        );
    }

    [Fact]
    public void PartialRecord()
    {
        RewriteRun(
            CSharp(
                """
                partial record Foo(string Name) { }
                """
            )
        );
    }

    // ==================== Sealed and Abstract Interfaces (C# 8+ default interface methods) ====================

    [Fact]
    public void InterfaceWithStaticAbstractMember()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                    static abstract int Bar();
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithStaticVirtualMember()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                    static virtual int Bar() => 0;
                }
                """
            )
        );
    }

    // ==================== Attributes on Type Declarations ====================

    [Fact]
    public void AttributeOnStruct()
    {
        RewriteRun(
            CSharp(
                """
                [System.Serializable]
                struct Foo { }
                """
            )
        );
    }

    [Fact]
    public void AttributeOnInterface()
    {
        RewriteRun(
            CSharp(
                """
                [System.Obsolete]
                interface IFoo { }
                """
            )
        );
    }

    [Fact]
    public void AttributeOnRecord()
    {
        RewriteRun(
            CSharp(
                """
                [System.Serializable]
                record Foo(string Name);
                """
            )
        );
    }

    [Fact]
    public void AttributeWithMultipleArguments()
    {
        RewriteRun(
            CSharp(
                """
                [System.Obsolete("msg", true)]
                class Foo { }
                """
            )
        );
    }

    [Fact]
    public void AttributeOnClassWithModifiers()
    {
        RewriteRun(
            CSharp(
                """
                [System.Serializable]
                public sealed class Foo { }
                """
            )
        );
    }

    [Fact]
    public void MultipleAttributeListsOnClass()
    {
        RewriteRun(
            CSharp(
                """
                [System.Serializable]
                [System.Obsolete("old")]
                [System.ComponentModel.Description("test")]
                public class Foo { }
                """
            )
        );
    }

    // ==================== Nullable Type in Base List ====================

    [Fact]
    public void GenericBaseClassWithNullableTypeArg()
    {
        RewriteRun(
            CSharp(
                """
                class Base<T> { }
                class Derived : Base<string?> { }
                """
            )
        );
    }

    // ==================== Complex Real-World Patterns ====================

    [Fact]
    public void GenericClassAllFeatures()
    {
        RewriteRun(
            CSharp(
                """
                class Base<T> { }
                interface IFoo<T> { }
                [System.Serializable]
                public sealed class Derived<T, U> : Base<T>, IFoo<U> where T : class, new() where U : struct {
                    private T _value;
                }
                """
            )
        );
    }

    [Fact]
    public void RecordAllFeatures()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { }
                [System.Serializable]
                public record Foo<T>(T Value, string Name) : IFoo where T : class;
                """
            )
        );
    }

    [Fact]
    public void NestedGenericTypes()
    {
        RewriteRun(
            CSharp(
                """
                class Outer<T> {
                    class Inner<U> where U : T { }
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithStaticMembers()
    {
        RewriteRun(
            CSharp(
                """
                interface IFactory<T> where T : IFactory<T> {
                    static abstract T Create();
                }
                """
            )
        );
    }

    // ==================== Whitespace Preservation ====================

    [Fact]
    public void ClassWithExtraWhitespaceEverywhere()
    {
        RewriteRun(
            CSharp(
                """
                [  System.Serializable  ]
                public   sealed   class   Foo < T ,  U >  :  Base < T > ,  IFoo  where  T  :  class  {
                }
                class Base<T> { }
                interface IFoo { }
                """
            )
        );
    }

    [Fact]
    public void GenericConstraintWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo < T >   where   T   :   class ,   new()  { }
                """
            )
        );
    }
}
