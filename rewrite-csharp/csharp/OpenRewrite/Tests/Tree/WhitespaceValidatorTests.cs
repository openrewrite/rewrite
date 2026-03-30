/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests.Tree;

public class WhitespaceValidatorTests
{
    private static List<WhitespaceViolation> Validate(string code)
    {
        var parser = new CSharpParser();
        var cu = parser.Parse(code);
        var violations = new List<WhitespaceViolation>();
        new WhitespaceValidator().Visit(cu, violations);
        return violations;
    }

    private static void AssertNoViolations(string code)
    {
        var violations = Validate(code);
        Assert.True(violations.Count == 0,
            $"Expected no whitespace violations but found {violations.Count}:\n" +
            string.Join("\n", violations));
    }

    [Fact]
    public void EmptyClass()
    {
        AssertNoViolations("class Foo { }");
    }

    [Fact]
    public void ClassWithModifiers()
    {
        AssertNoViolations("public static class Foo { }");
    }

    [Fact]
    public void ClassWithBaseType()
    {
        AssertNoViolations("class Foo : Bar { }");
    }

    [Fact]
    public void ClassWithMultipleInterfaces()
    {
        AssertNoViolations("class Foo : IBar, IBaz { }");
    }

    [Fact]
    public void ClassWithGenericConstraints()
    {
        AssertNoViolations("class Foo<T> where T : class, new() { }");
    }

    [Fact]
    public void Struct()
    {
        AssertNoViolations("struct Point { public int X; public int Y; }");
    }

    [Fact]
    public void RecordDeclaration()
    {
        AssertNoViolations("record Person(string Name, int Age);");
    }

    [Fact]
    public void Interface()
    {
        AssertNoViolations("interface IFoo { void Bar(); }");
    }

    [Fact]
    public void EnumDeclaration()
    {
        AssertNoViolations("enum Color { Red, Green, Blue }");
    }

    [Fact]
    public void FlagsEnum()
    {
        AssertNoViolations(
            """
            [Flags]
            enum Permissions { Read = 1, Write = 2, Execute = 4 }
            """);
    }

    [Fact]
    public void SimpleAttribute()
    {
        AssertNoViolations(
            """
            [Serializable]
            class Foo { }
            """);
    }

    [Fact]
    public void AttributeWithArgument()
    {
        AssertNoViolations(
            """
            [Obsolete("deprecated")]
            class Foo { }
            """);
    }

    [Fact]
    public void MultipleAttributes()
    {
        AssertNoViolations(
            """
            [Serializable]
            [Obsolete("deprecated")]
            class Foo { }
            """);
    }

    [Fact]
    public void AttributesOnSameLine()
    {
        AssertNoViolations(
            """
            [Serializable, Obsolete("msg")]
            class Foo { }
            """);
    }

    [Fact]
    public void AttributeWithComment()
    {
        AssertNoViolations(
            """
            [Serializable(/*bar*/)]
            class Foo { }
            """);
    }

    [Fact]
    public void AttributeOnStruct()
    {
        AssertNoViolations(
            """
            [Serializable]
            struct Foo { }
            """);
    }

    [Fact]
    public void AttributeOnEnum()
    {
        AssertNoViolations(
            """
            [Flags]
            enum Permissions { Read = 1, Write = 2 }
            """);
    }

    [Fact]
    public void AttributeOnInterface()
    {
        AssertNoViolations(
            """
            [Obsolete]
            interface IFoo { }
            """);
    }

    [Fact]
    public void AttributeOnMethod()
    {
        AssertNoViolations(
            """
            class Foo {
                [Obsolete]
                void Bar() { }
            }
            """);
    }

    [Fact]
    public void AttributeOnProperty()
    {
        AssertNoViolations(
            """
            class Foo {
                [Obsolete]
                public int X { get; set; }
            }
            """);
    }

    [Fact]
    public void AttributeOnField()
    {
        AssertNoViolations(
            """
            class Foo {
                [Obsolete]
                private int _x;
            }
            """);
    }

    [Fact]
    public void AttributeOnParameter()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar([Obsolete] int x) { }
            }
            """);
    }

    [Fact]
    public void AttributeOnReturnValue()
    {
        AssertNoViolations(
            """
            class Foo {
                [return: Obsolete]
                int Bar() { return 0; }
            }
            """);
    }

    [Fact]
    public void UsingDirectives()
    {
        AssertNoViolations(
            """
            using System;
            using System.Collections.Generic;
            class Foo { }
            """);
    }

    [Fact]
    public void UsingStaticDirective()
    {
        AssertNoViolations(
            """
            using static System.Math;
            class Foo { }
            """);
    }

    [Fact]
    public void UsingAlias()
    {
        AssertNoViolations(
            """
            using Sys = System;
            class Foo { }
            """);
    }

    [Fact]
    public void Namespace()
    {
        AssertNoViolations(
            """
            namespace MyApp {
                class Foo { }
            }
            """);
    }

    [Fact]
    public void FileScopedNamespace()
    {
        AssertNoViolations(
            """
            namespace MyApp;
            class Foo { }
            """);
    }

    [Fact]
    public void PropertyDeclaration()
    {
        AssertNoViolations(
            """
            class Foo {
                public int X { get; set; }
                public string Name { get; }
                public int Y => 42;
            }
            """);
    }

    [Fact]
    public void PropertyWithInitializer()
    {
        AssertNoViolations(
            """
            class Foo {
                public int X { get; set; } = 10;
            }
            """);
    }

    [Fact]
    public void MethodDeclaration()
    {
        AssertNoViolations(
            """
            class Foo {
                public void Bar() { }
                private int Baz(int x, string y) { return x; }
                protected virtual void Qux() { }
            }
            """);
    }

    [Fact]
    public void ExpressionBodiedMethod()
    {
        AssertNoViolations(
            """
            class Foo {
                public int Bar() => 42;
            }
            """);
    }

    [Fact]
    public void IfElse()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    if (true) { }
                    else if (false) { }
                    else { }
                }
            }
            """);
    }

    [Fact]
    public void ForLoop()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    for (int i = 0; i < 10; i++) { }
                }
            }
            """);
    }

    [Fact]
    public void ForEachLoop()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    foreach (var x in new int[] { 1, 2, 3 }) { }
                }
            }
            """);
    }

    [Fact]
    public void WhileLoop()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    while (true) { }
                }
            }
            """);
    }

    [Fact]
    public void DoWhileLoop()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    do { } while (true);
                }
            }
            """);
    }

    [Fact]
    public void TryCatchFinally()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    try { }
                    catch (System.Exception e) { }
                    finally { }
                }
            }
            """);
    }

    [Fact]
    public void SwitchStatement()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar(int x) {
                    switch (x) {
                        case 1:
                            break;
                        default:
                            break;
                    }
                }
            }
            """);
    }

    [Fact]
    public void SwitchExpression()
    {
        AssertNoViolations(
            """
            class Foo {
                string Bar(int x) => x switch {
                    1 => "one",
                    2 => "two",
                    _ => "other"
                };
            }
            """);
    }

    [Fact]
    public void Lambda()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    System.Func<int, int> f = x => x + 1;
                    System.Action a = () => { };
                }
            }
            """);
    }

    [Fact]
    public void Tuple()
    {
        AssertNoViolations(
            """
            class Foo {
                (int, string) Bar() => (1, "hello");
            }
            """);
    }

    [Fact]
    public void InterpolatedString()
    {
        AssertNoViolations(
            """
            class Foo {
                string Bar(int x) => $"Value is {x}";
            }
            """);
    }

    [Fact]
    public void SingleLineComment()
    {
        AssertNoViolations(
            """
            // This is a comment
            class Foo { }
            """);
    }

    [Fact]
    public void MultiLineComment()
    {
        AssertNoViolations(
            """
            /* This is a
               multi-line comment */
            class Foo { }
            """);
    }

    [Fact]
    public void CommentsInsideClass()
    {
        AssertNoViolations(
            """
            class Foo {
                // comment
                int x;
                /* block comment */
                int y;
            }
            """);
    }

    [Fact]
    public void Nullable()
    {
        AssertNoViolations(
            """
            class Foo {
                string? Name { get; set; }
                int? Count { get; set; }
            }
            """);
    }

    [Fact]
    public void GenericClass()
    {
        AssertNoViolations(
            """
            class Foo<T, U> {
                T Value { get; set; }
            }
            """);
    }

    [Fact]
    public void AsyncAwait()
    {
        AssertNoViolations(
            """
            class Foo {
                async System.Threading.Tasks.Task Bar() {
                    await System.Threading.Tasks.Task.Delay(1);
                }
            }
            """);
    }

    [Fact]
    public void PatternMatching()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar(object o) {
                    if (o is int i) { }
                    if (o is string s) { }
                }
            }
            """);
    }

    [Fact]
    public void ArrayDeclaration()
    {
        AssertNoViolations(
            """
            class Foo {
                int[] arr = new int[] { 1, 2, 3 };
                int[,] matrix = new int[2, 3];
            }
            """);
    }

    [Fact]
    public void EventDeclaration()
    {
        AssertNoViolations(
            """
            class Foo {
                event System.EventHandler MyEvent;
            }
            """);
    }

    [Fact]
    public void DelegateDeclaration()
    {
        AssertNoViolations(
            """
            delegate void MyDelegate(int x, string y);
            """);
    }

    [Fact]
    public void YieldReturn()
    {
        AssertNoViolations(
            """
            class Foo {
                System.Collections.Generic.IEnumerable<int> Bar() {
                    yield return 1;
                    yield return 2;
                }
            }
            """);
    }

    [Fact]
    public void UsingStatement()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    using (var x = new System.IO.MemoryStream()) { }
                }
            }
            """);
    }

    [Fact]
    public void LockStatement()
    {
        AssertNoViolations(
            """
            class Foo {
                object _lock = new object();
                void Bar() {
                    lock (_lock) { }
                }
            }
            """);
    }

    [Fact]
    public void ThrowExpression()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    throw new System.Exception("error");
                }
            }
            """);
    }

    [Fact]
    public void ConditionalExpression()
    {
        AssertNoViolations(
            """
            class Foo {
                int Bar(bool b) => b ? 1 : 2;
            }
            """);
    }

    [Fact]
    public void NullCoalescing()
    {
        AssertNoViolations(
            """
            class Foo {
                string Bar(string? s) => s ?? "default";
            }
            """);
    }

    [Fact]
    public void IndexerDeclaration()
    {
        AssertNoViolations(
            """
            class Foo {
                int this[int i] => i;
            }
            """);
    }

    [Fact]
    public void OperatorOverload()
    {
        AssertNoViolations(
            """
            class Foo {
                public static Foo operator +(Foo a, Foo b) => a;
            }
            """);
    }

    [Fact]
    public void ExplicitInterfaceImplementation()
    {
        AssertNoViolations(
            """
            interface IFoo { void Bar(); }
            class Foo : IFoo {
                void IFoo.Bar() { }
            }
            """);
    }

    [Fact]
    public void NestedClass()
    {
        AssertNoViolations(
            """
            class Outer {
                class Inner { }
            }
            """);
    }

    [Fact]
    public void StaticConstructor()
    {
        AssertNoViolations(
            """
            class Foo {
                static Foo() { }
            }
            """);
    }

    [Fact]
    public void Destructor()
    {
        AssertNoViolations(
            """
            class Foo {
                ~Foo() { }
            }
            """);
    }

    [Fact]
    public void CastExpression()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar(object o) {
                    var x = (int)o;
                }
            }
            """);
    }

    [Fact]
    public void ObjectInitializer()
    {
        AssertNoViolations(
            """
            class Foo {
                int X { get; set; }
                void Bar() {
                    var f = new Foo { X = 1 };
                }
            }
            """);
    }

    [Fact]
    public void CollectionInitializer()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    var list = new System.Collections.Generic.List<int> { 1, 2, 3 };
                }
            }
            """);
    }

    [Fact]
    public void NamedArguments()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar(int x, int y) { }
                void Baz() {
                    Bar(x: 1, y: 2);
                }
            }
            """);
    }

    [Fact]
    public void VariableDeclarationWithVar()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    var x = 1;
                    var y = "hello";
                }
            }
            """);
    }

    [Fact]
    public void MultipleVariableDeclarations()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    int x = 1, y = 2, z = 3;
                }
            }
            """);
    }

    [Fact]
    public void Preprocessor()
    {
        AssertNoViolations(
            """
            #if DEBUG
            class DebugOnly { }
            #else
            class ReleaseOnly { }
            #endif
            """);
    }

    [Fact]
    public void Region()
    {
        AssertNoViolations(
            """
            class Foo {
                #region MyRegion
                int x;
                #endregion
            }
            """);
    }

    [Fact]
    public void NullConditionalAccess()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar(string? s) {
                    var len = s?.Length;
                }
            }
            """);
    }

    [Fact]
    public void RangeExpression()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    int[] arr = { 1, 2, 3, 4, 5 };
                    var slice = arr[1..3];
                }
            }
            """);
    }

    [Fact]
    public void WithExpression()
    {
        AssertNoViolations(
            """
            record Point(int X, int Y);
            class Foo {
                void Bar() {
                    var p = new Point(1, 2);
                    var q = p with { X = 3 };
                }
            }
            """);
    }

    [Fact]
    public void DeclarationExpression()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    int.TryParse("1", out var result);
                }
            }
            """);
    }

    [Fact]
    public void DiscardPattern()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar(object o) {
                    _ = o;
                }
            }
            """);
    }

    [Fact]
    public void RefParameters()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar(ref int x, out int y, in int z) { y = 0; }
            }
            """);
    }

    [Fact]
    public void DefaultExpression()
    {
        AssertNoViolations(
            """
            class Foo {
                int Bar() => default;
                int Baz() => default(int);
            }
            """);
    }

    [Fact]
    public void TypeOfSizeOf()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    var t = typeof(int);
                }
            }
            """);
    }

    [Fact]
    public void GotoStatement()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    goto end;
                    end:
                    return;
                }
            }
            """);
    }

    [Fact]
    public void CheckedUnchecked()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    checked { int x = int.MaxValue + 1; }
                    unchecked { int y = int.MaxValue + 1; }
                }
            }
            """);
    }

    [Fact]
    public void ConversionOperator()
    {
        AssertNoViolations(
            """
            class Foo {
                public static implicit operator int(Foo f) => 0;
                public static explicit operator Foo(int i) => new Foo();
            }
            """);
    }

    [Fact]
    public void LinqQuery()
    {
        AssertNoViolations(
            """
            class Foo {
                void Bar() {
                    var nums = new int[] { 1, 2, 3 };
                    var q = from n in nums
                            where n > 1
                            select n;
                }
            }
            """);
    }

    [Fact]
    public void XmlDocComment()
    {
        AssertNoViolations(
            """
            /// <summary>
            /// My class
            /// </summary>
            class Foo { }
            """);
    }
}
