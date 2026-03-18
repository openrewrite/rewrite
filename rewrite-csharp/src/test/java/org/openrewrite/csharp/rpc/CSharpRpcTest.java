/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.csharp.tree.Cs;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.test.RewriteTest;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.csharp.Assertions.csharp;

/**
 * Integration tests for C# RPC communication.
 * Uses rewriteRun with csharp() assertions for round-trip verification.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CSharpRpcTest implements RewriteTest {

    // ---- Round-trip parsing tests ----

    @Test
    void parseAndPrintSimpleClass() {
        rewriteRun(csharp(
          """
            namespace Test
            {
                public class HelloWorld
                {
                    public void SayHello()
                    {
                        Console.WriteLine("Hello, World!");
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseClassWithProperties() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Person
                {
                    public string FirstName { get; set; }
                    public string LastName { get; set; }
                    public int Age { get; set; }
            
                    public string FullName => $"{FirstName} {LastName}";
                }
            }
            """
        ));
    }

    @Test
    void parseClassWithUsings() {
        rewriteRun(csharp(
          """
            using System;
            using System.Collections.Generic;
            using System.Linq;
            
            namespace Services
            {
                public class DataService
                {
                    private readonly List<string> _items = new();
            
                    public void AddItem(string item)
                    {
                        _items.Add(item);
                    }
            
                    public IEnumerable<string> GetItems()
                    {
                        return _items.Where(x => !string.IsNullOrEmpty(x));
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseMultipleFiles() {
        rewriteRun(
          csharp(
            """
              namespace Models
              {
                  public class Person
                  {
                      public string Name { get; set; }
                  }
              }
              """
          ),
          csharp(
            """
              namespace Models
              {
                  public class Address
                  {
                      public string Street { get; set; }
                      public string City { get; set; }
                  }
              }
              """
          )
        );
    }

    @Test
    void parseAsyncAwait() {
        rewriteRun(csharp(
          """
            using System.Threading.Tasks;
            
            namespace Services
            {
                public class AsyncService
                {
                    public async Task<string> GetDataAsync()
                    {
                        await Task.Delay(100);
                        return "data";
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseSwitchExpression() {
        rewriteRun(csharp(
          """
            namespace Utils
            {
                public class Formatter
                {
                    public string FormatDay(int day) => day switch
                    {
                        1 => "Monday",
                        2 => "Tuesday",
                        3 => "Wednesday",
                        4 => "Thursday",
                        5 => "Friday",
                        6 => "Saturday",
                        7 => "Sunday",
                        _ => "Unknown"
                    };
                }
            }
            """
        ));
    }

    @Test
    void parsePatternMatching() {
        rewriteRun(csharp(
          """
            namespace Utils
            {
                public class TypeChecker
                {
                    public string Describe(object obj)
                    {
                        if (obj is string s)
                        {
                            return $"String with length {s.Length}";
                        }
                        else if (obj is int n and > 0)
                        {
                            return $"Positive int: {n}";
                        }
                        else if (obj is null)
                        {
                            return "null";
                        }
                        return "unknown";
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseRecord() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public record Point(int X, int Y);
            
                public record Person(string FirstName, string LastName)
                {
                    public string FullName => $"{FirstName} {LastName}";
                }
            }
            """
        ));
    }

    @Test
    void parseRegionDirective() {
        rewriteRun(csharp(
          """
            namespace Test
            {
                public class Foo
                {
                    #region Methods
                    public void A()
                    {
                    }
                    #endregion
                }
            }
            """
        ));
    }

    @Test
    void parsePragmaWarningDirective() {
        rewriteRun(csharp(
          """
            namespace Test
            {
                public class Foo
                {
                    #pragma warning disable CS0168
                    public void A()
                    {
                        int x;
                    }
                    #pragma warning restore CS0168
                }
            }
            """
        ));
    }

    @Test
    void parseNullableDirective() {
        rewriteRun(csharp(
          """
            #nullable enable
            namespace Test
            {
                public class Foo
                {
                    public string? Name { get; set; }
                }
            }
            """
        ));
    }

    @Test
    void parseErrorAndWarningDirectives() {
        rewriteRun(csharp(
          """
            namespace Test
            {
                public class Foo
                {
                    public void A()
                    {
                        #warning This method is not implemented
                        throw new System.NotImplementedException();
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseLineDirective() {
        rewriteRun(csharp(
          """
            namespace Test
            {
                public class Foo
                {
                    public void A()
                    {
                        #line 200
                        int x = 1;
                        #line default
                        int y = 2;
                    }
                }
            }
            """
        ));
    }

    @Test
    void parsePragmaWarningMultipleCodes() {
        rewriteRun(csharp(
          """
            namespace Test
            {
                public class Foo
                {
                    #pragma warning disable CS0168, CS0219
                    public void A()
                    {
                        int x;
                        int y;
                    }
                    #pragma warning restore CS0168, CS0219
                }
            }
            """
        ));
    }

    @Test
    void parseClassWithFields() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Person
                {
                    private readonly string _name;
                    public static int Count = 0;
                    private readonly List<string> _items = new();
                    private const int MaxItems = 100;
                }
            }
            """
        ));
    }

    @Test
    void parseClassWithConstructor() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Animal
                {
                    private readonly string _name;
            
                    public Animal(string name)
                    {
                        _name = name;
                    }
            
                    public string GetName()
                    {
                        return _name;
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseConstructorWithInitializer() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Base
                {
                    public Base(int x)
                    {
                    }
                }
            
                public class Derived : Base
                {
                    public Derived(int x) : base(x)
                    {
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseExpressionBodiedConstructor() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Point
                {
                    private int _x;
            
                    public Point(int x) => _x = x;
                }
            }
            """
        ));
    }

    // ---- Marketplace tests ----

    @Test
    void getMarketplace() {
        RecipeBundle bundle = new RecipeBundle("nuget", "test-recipes",
                null, null, null);
        RecipeMarketplace marketplace = CSharpRewriteRpc.getOrStart().getMarketplace(bundle);
        assertThat(marketplace).isNotNull();
        // XML recipes are registered via XmlRecipeActivator
        assertThat(marketplace.getAllRecipes()).isNotEmpty();
    }

    // ---- Type attribution tests ----

    @Test
    void parseWithTypeAttribution() {
        rewriteRun(csharp(
          """
            using System;
            
            namespace TypeTest
            {
                public class Calculator
                {
                    public int Add(int a, int b)
                    {
                        return a + b;
                    }
            
                    public string Greet(string name)
                    {
                        return "Hello, " + name;
                    }
                }
            }
            """,
          spec -> spec.beforeRecipe(cu -> {
              Statement namespaceMember = cu.getMembers().stream()
                .filter(m -> !(m instanceof Cs.UsingDirective))
                .findFirst()
                .orElseThrow();

              J.ClassDeclaration classDecl = findFirst(namespaceMember, J.ClassDeclaration.class);
              assertThat(classDecl).isNotNull();

              J.MethodDeclaration addMethod = findMethodByName(classDecl, "Add");
              assertThat(addMethod).isNotNull();
              assertThat(addMethod.getMethodType()).isNotNull();
              assertThat(addMethod.getMethodType().getName()).isEqualTo("Add");
              assertThat(addMethod.getMethodType().getReturnType()).isInstanceOf(JavaType.Primitive.class);
              assertThat(addMethod.getMethodType().getParameterNames()).containsExactly("a", "b");
              assertThat(addMethod.getMethodType().getDeclaringType().getFullyQualifiedName())
                .isEqualTo("TypeTest.Calculator");

              J.MethodDeclaration greetMethod = findMethodByName(classDecl, "Greet");
              assertThat(greetMethod).isNotNull();
              assertThat(greetMethod.getMethodType()).isNotNull();
              assertThat(greetMethod.getMethodType().getName()).isEqualTo("Greet");
          })
        ));
    }

    @Test
    void comprehensiveTypeAttribution() {
        rewriteRun(csharp(
          """
            using System;
            
            namespace TypeAttrTest
            {
                public class Animal
                {
                    public string Name { get; set; }
                    public int Age;
            
                    public int GetAge()
                    {
                        return Age;
                    }
            
                    public void Process(int x)
                    {
                        var y = x + x;
                        if (y is int n)
                        {
                        }
                    }
            
                    public static void UseDelegate()
                    {
                        Action a = UseDelegate;
                    }
                }
            
                public enum Color
                {
                    Red,
                    Green
                }
            }
            """,
          spec -> spec.beforeRecipe(cu -> {
              Statement namespaceMember = cu.getMembers().stream()
                .filter(m -> !(m instanceof Cs.UsingDirective))
                .findFirst()
                .orElseThrow();

              J.ClassDeclaration classDecl = findFirst(namespaceMember, J.ClassDeclaration.class);
              assertThat(classDecl).isNotNull();
              assertThat(classDecl.getName().getType())
                .isInstanceOf(JavaType.FullyQualified.class);
              assertThat(((JavaType.FullyQualified) Objects.requireNonNull(classDecl.getName().getType()))
                .getFullyQualifiedName())
                .isEqualTo("TypeAttrTest.Animal");

              J.MethodDeclaration getAgeMethod = findMethodByName(classDecl, "GetAge");
              assertThat(getAgeMethod).isNotNull();
              assertThat(getAgeMethod.getName().getType()).isInstanceOf(JavaType.Method.class);
          })
        ));
    }

    // ---- More round-trip tests ----

    @Test
    void parseBaseExpression() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Animal
                {
                    public virtual string Name { get; set; }
                }
            
                public class Dog : Animal
                {
                    public void Speak()
                    {
                        var name = base.Name;
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseGenericTypes() {
        rewriteRun(csharp(
          """
            using System.Collections.Generic;
            
            namespace Models
            {
                public class Container
                {
                    private List<string> _items = new List<string>();
                    private Dictionary<string, int> _counts = new Dictionary<string, int>();
            
                    public List<string> GetItems()
                    {
                        return _items;
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseEventFieldDeclaration() {
        rewriteRun(csharp(
          """
            using System;
            
            namespace Models
            {
                public class Button
                {
                    public event EventHandler Click;
                    public event EventHandler<string> TextChanged;
                }
            }
            """
        ));
    }

    @Test
    void parseLocalFunction() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Calculator
                {
                    public int Compute(int x)
                    {
                        int Double(int n)
                        {
                            return n * 2;
                        }
            
                        return Double(x);
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseThrowExpression() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Validator
                {
                    public string GetValue(bool valid, string value) =>
                        valid ? value : throw new System.InvalidOperationException();
                }
            }
            """
        ));
    }

    @Test
    void parseTypeOfExpression() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class TypeHelper
                {
                    public System.Type GetIntType()
                    {
                        return typeof(int);
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseSizeOf() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class SizeHelper
                {
                    public int GetIntSize()
                    {
                        return sizeof(int);
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseLabeledStatement() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class LabelHelper
                {
                    public void Process()
                    {
                    start:
                        Console.WriteLine("Hello");
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseUnsafeStatement() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class UnsafeHelper
                {
                    public unsafe int GetSize()
                    {
                        unsafe
                        {
                            return sizeof(int);
                        }
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseDefaultExpression() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class DefaultHelper
                {
                    public int GetDefault()
                    {
                        int x = default(int);
                        string s = default;
                        return x;
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseFixedStatement() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class FixedHelper
                {
                    public unsafe void Process(byte[] data)
                    {
                        fixed (byte* p = data)
                        {
                        }
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseInitializerExpression() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Person
                {
                    public string Name { get; set; }
                    public int Age { get; set; }
                }
            
                public class Factory
                {
                    public Person Create()
                    {
                        return new Person { Name = "John", Age = 25 };
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseDestructorDeclaration() {
        rewriteRun(csharp(
          """
            class Resource
            {
                ~Resource()
                {
                    Cleanup();
                }
            
                void Cleanup() { }
            }
            """
        ));
    }

    @Test
    void parseNullForgivingOperator() {
        rewriteRun(csharp(
          """
            class C
            {
                string x = null!;
                string y = GetValue()!;
            
                static string GetValue() => null!;
            }
            """
        ));
    }

    @Test
    void parseNullableType() {
        rewriteRun(csharp(
          """
            class C
            {
                int? x;
                string? y;
            
                int? Add(int? a, string? b)
                {
                    return a;
                }
            }
            """
        ));
    }

    @Test
    void parseLinqQuery() {
        rewriteRun(csharp(
          """
            using System.Linq;
            class Program
            {
                void M()
                {
                    int[] numbers = { 1, 2, 3, 4, 5 };
                    var result = from n in numbers
                                 where n > 2
                                 orderby n descending
                                 select n * 2;
                }
            }
            """
        ));
    }

    @Test
    void parseSwitchStatement() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Dispatcher
                {
                    public void Handle(int code)
                    {
                        switch (code)
                        {
                            case 1:
                                break;
                            case 2:
                            case 3:
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseUsingStatement() {
        rewriteRun(csharp(
          """
            using System;
            using System.IO;
            namespace App
            {
                public class FileReader
                {
                    public void Read()
                    {
                        using (var stream = new MemoryStream())
                        {
                        }
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseCheckedUnchecked() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Math
                {
                    public int Compute(int x)
                    {
                        checked
                        {
                            return x + 1;
                        }
                    }
            
                    public int Fast(int x) => unchecked(x * 2);
                }
            }
            """
        ));
    }

    @Test
    void parseGotoStatement() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Flow
                {
                    public void Run()
                    {
                        goto end;
                        end:
                        return;
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseEnumDeclaration() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public enum Color
                {
                    Red,
                    Green = 1,
                    Blue = 2
                }
            }
            """
        ));
    }

    @Test
    void parseDelegateDeclaration() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public delegate void Handler(int x);
                public delegate T Factory<T>();
            }
            """
        ));
    }

    @Test
    void parseForEachVariable() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Parser
                {
                    public void Parse()
                    {
                        var pairs = new (int, string)[] { (1, "a"), (2, "b") };
                        foreach (var (num, str) in pairs)
                        {
                        }
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseIndexerDeclaration() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Grid
                {
                    private int[] data = new int[10];
            
                    public int this[int index]
                    {
                        get { return data[index]; }
                        set { data[index] = value; }
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseOperatorDeclaration() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public struct Point
                {
                    public int X;
                    public int Y;
            
                    public static Point operator +(Point a, Point b)
                    {
                        return new Point();
                    }
            
                    public static implicit operator int(Point p) => p.X;
                }
            }
            """
        ));
    }

    @Test
    void parseRangeExpression() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Slicer
                {
                    public void Slice()
                    {
                        var arr = new int[] { 1, 2, 3, 4, 5 };
                        var sub = arr[1..3];
                        var rest = arr[2..];
                        var first = arr[..2];
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseCollectionExpression() {
        rewriteRun(csharp(
          """
            using System.Collections.Generic;
            namespace App
            {
                public class Builder
                {
                    public void Build()
                    {
                        List<int> nums = [1, 2, 3];
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseTypeConstraints() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Factory<T> where T : class, new()
                {
                    public T Create()
                    {
                        return new T();
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseEventDeclaration() {
        rewriteRun(csharp(
          """
            using System;
            namespace App
            {
                public class Button
                {
                    public event EventHandler Clicked
                    {
                        add { }
                        remove { }
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseYieldStatement() {
        rewriteRun(csharp(
          """
            using System.Collections.Generic;
            namespace App
            {
                public class Generator
                {
                    public IEnumerable<int> GetNumbers()
                    {
                        yield return 1;
                        yield return 2;
                        yield break;
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseTupleType() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Data
                {
                    public (int X, int Y) GetPoint()
                    {
                        return (1, 2);
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseStackAllocExpression() {
        rewriteRun(csharp(
          """
            using System;
            namespace App
            {
                public class Memory
                {
                    public void Alloc()
                    {
                        Span<int> nums = stackalloc int[3];
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseAnonymousObjectCreation() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Demo
                {
                    public void Run()
                    {
                        var obj = new { Name = "test", Age = 1 };
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseWithExpression() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public record Person(string Name, int Age);
            
                public class Demo
                {
                    public void Run()
                    {
                        var p = new Person("Alice", 30);
                        var p2 = p with { Name = "Bob" };
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseSpreadElement() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Demo
                {
                    public void Run()
                    {
                        int[] a = [1, 2];
                        int[] b = [..a, 3];
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseAliasQualifiedName() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Demo
                {
                    public void Run()
                    {
                        global::System.Console.WriteLine("hi");
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseAnonymousMethodExpression() {
        rewriteRun(csharp(
          """
            using System;
            
            namespace App
            {
                public class Demo
                {
                    public void Run()
                    {
                        Func<int, int> f = delegate(int x) { return x + 1; };
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseRefType() {
        rewriteRun(csharp(
          """
            namespace App
            {
                public class Demo
                {
                    private int _value;
            
                    public ref int GetRef()
                    {
                        return ref _value;
                    }
                }
            }
            """
        ));
    }

    @Test
    void parseImplicitStackAlloc() {
        rewriteRun(csharp(
          """
            using System;
            
            namespace App
            {
                public class Demo
                {
                    public void Run()
                    {
                        Span<int> s = stackalloc[] { 1, 2, 3 };
                    }
                }
            }
            """
        ));
    }

    // ---- Preprocessor directive tests ----

    @Test
    void parseSimpleIfEndif() {
        rewriteRun(csharp(
          """
            #if DEBUG
            using System.Diagnostics;
            #endif
            namespace Test
            {
                public class Foo
                {
                }
            }
            """
        ));
    }

    @Test
    void parseSimpleIfElse() {
        rewriteRun(csharp(
          """
            namespace Test
            {
            #if DEBUG
                public class DebugFoo
                {
                }
            #else
                public class ReleaseFoo
                {
                }
            #endif
            }
            """
        ));
    }

    @Test
    void parseKeywordSplittingDirective() {
        rewriteRun(csharp(
          """
            public
            #if SOMETHING
            record
            #else
            class
            #endif
            MyObject { }
            """
        ));
    }

    @Test
    void parseNestedDirectives() {
        rewriteRun(csharp(
          """
            namespace Test
            {
            #if A
                public class Outer
                {
            #if B
                    public void InnerMethod() { }
            #endif
                }
            #endif
            }
            """
        ));
    }

    @Test
    void parseElifDirective() {
        rewriteRun(csharp(
          """
            namespace Test
            {
                public class Foo
                {
            #if PLATFORM_A
                    public void PlatformA() { }
            #elif PLATFORM_B
                    public void PlatformB() { }
            #else
                    public void Default() { }
            #endif
                }
            }
            """
        ));
    }

    @Test
    void parseDirectiveWithComplexCondition() {
        rewriteRun(csharp(
          """
            namespace Test
            {
                public class Foo
                {
            #if DEBUG && !TRACE
                    public void DebugOnly() { }
            #endif
                }
            }
            """
        ));
    }

    @Test
    void parseDirectiveChangingBaseClass() {
        rewriteRun(csharp(
          """
            #if(IsSupplyBuildpack)
            public partial class MyBuildpack : SupplyBuildpack
            #elif(IsFinalBuildpack)
            public partial class MyBuildpack : FinalBuildpack
            #elif(IsHttpModuleBuildpack || IsHostedServiceBuildpack)
            public partial class MyBuildpack : PluginInjectorBuildpack
            #endif
            {
            }
            """
        ));
    }

    @Test
    void parsePolyfillConditionalClass() {
        rewriteRun(csharp(
          """
            using System;
            
            namespace Polyfills
            {
            #if !NET6_0_OR_GREATER
                internal static class ArgumentNullException
                {
                    public static void ThrowIfNull(object? argument, string? paramName = null)
                    {
                        if (argument is null)
                            throw new System.ArgumentNullException(paramName);
                    }
                }
            #endif
            }
            """
        ));
    }

    // ---- Complex structure tests ----

    @Test
    void parseClassWithMixedMembersAndBoolProperty() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class NavItem
                {
                    public NavItem(string? type, NavLevel ol)
                        : this(type, false, null, ol)
                    {
                    }
            
                    public NavItem(string? type, bool isHidden, string? head, NavLevel ol)
                    {
                        Type = type;
                        IsHidden = isHidden;
                        Head = head;
                        Ol = ol;
                    }
            
                    public string? Type { get; }
                    public bool IsHidden { get; }
                    public string? Head { get; }
                    public NavLevel Ol { get; }
                }
            
                public class NavLevel
                {
                }
            }
            """
        ));
    }

    @Test
    void parseNullCoalescingOperator() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public class Config
                {
                    private string _name;
            
                    public Config(string? name)
                    {
                        _name = name ?? "default";
                    }
            
                    public string Name => _name;
                }
            }
            """
        ));
    }

    @Test
    void parseClassWithOverrideExpressionBodiedProperty() {
        rewriteRun(csharp(
          """
            namespace Models
            {
                public enum ContentType
                {
                    TEXT,
                    BINARY
                }
            
                public abstract class BaseFile
                {
                    public abstract ContentType FileType { get; }
                }
            
                public class TextFile : BaseFile
                {
                    public TextFile(string content)
                    {
                        Content = content ?? throw new System.ArgumentNullException(nameof(content));
                    }
            
                    public string Content { get; }
            
                    public override ContentType FileType => ContentType.TEXT;
                }
            }
            """
        ));
    }

    @Test
    void parseControlParenthesesVariants() {
        rewriteRun(csharp(
          """
            using System;
            
            namespace ControlFlow
            {
                public class Handler
                {
                    public void Process(object input)
                    {
                        if (input == null)
                        {
                            return;
                        }
            
                        while (input != null)
                        {
                            break;
                        }
            
                        switch (input)
                        {
                            case string s:
                                Console.WriteLine(s);
                                break;
                            default:
                                break;
                        }
            
                        try
                        {
                            var text = (string)input;
                        }
                        catch (InvalidCastException ex)
                        {
                            Console.WriteLine(ex.Message);
                        }
                    }
                }
            }
            """
        ));
    }

    // ---- Helpers ----

    @SuppressWarnings("unchecked")
    private static <T> T findFirst(Object tree, Class<T> type) {
        if (type.isInstance(tree)) {
            return (T) tree;
        }
        if (tree instanceof Cs.CompilationUnit cu) {
            for (Statement member : cu.getMembers()) {
                T result = findFirst(member, type);
                if (result != null) return result;
            }
        }
        if (tree instanceof J.ClassDeclaration cd) {
            for (Statement stmt : cd.getBody().getStatements()) {
                T result = findFirst(stmt, type);
                if (result != null) return result;
            }
        }
        if (tree instanceof Cs.NamespaceDeclaration ns) {
            for (var member : ns.getMembers()) {
                T result = findFirst(member, type);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static J.MethodDeclaration findMethodByName(J.ClassDeclaration classDecl, String name) {
        for (Statement stmt : classDecl.getBody().getStatements()) {
            if (stmt instanceof J.MethodDeclaration md && md.getSimpleName().equals(name)) {
                return md;
            }
        }
        return null;
    }
}
