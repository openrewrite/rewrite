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
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Format;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Format;

/// <summary>
/// Tests for <see cref="MinimumViableSpacingVisitor"/>.
///
/// Strategy: parse well-formed C#, strip ALL whitespace to Space.Empty
/// (simulating a recipe that synthesizes nodes with empty spacing), then
/// run the MVS visitor and verify the minimum token separation is restored.
/// </summary>
public class MinimumViableSpacingTests
{
    private readonly CSharpParser _parser = new();
    private readonly CSharpPrinter<int> _printer = new();

    /// <summary>
    /// Strips all whitespace from a parsed tree, then runs MVS, then prints.
    /// </summary>
    private string StripAndRestore(string source)
    {
        var cu = _parser.Parse(source);
        var stripped = new WhitespaceStripper().Visit(cu, 0) as CompilationUnit ?? cu;
        var restored = new MinimumViableSpacingVisitor().Visit(stripped, 0) as CompilationUnit ?? stripped;
        return _printer.Print(restored);
    }

    [Fact]
    public void ClassWithModifiers()
    {
        var result = StripAndRestore("public sealed class Foo { }");
        Assert.DoesNotContain("publicsealed", result);
        Assert.DoesNotContain("sealedclass", result);
        Assert.DoesNotContain("classFoo", result);
    }

    [Fact]
    public void MethodWithModifiersAndReturnType()
    {
        var result = StripAndRestore(
            "class Foo { public static void Bar() { } }");
        Assert.DoesNotContain("publicstatic", result);
        Assert.DoesNotContain("staticvoid", result);
        Assert.DoesNotContain("voidBar", result);
    }

    [Fact]
    public void MethodWithNoModifiers()
    {
        var result = StripAndRestore(
            "class Foo { int Bar() { return 42; } }");
        // Return type and method name must not merge
        Assert.DoesNotContain("intBar", result);
    }

    [Fact]
    public void VariableDeclarationWithModifiers()
    {
        var result = StripAndRestore(
            "class Foo { private static int _x; }");
        Assert.DoesNotContain("privatestatic", result);
        Assert.DoesNotContain("staticint", result);
        Assert.DoesNotContain("int_x", result);
    }

    [Fact]
    public void ReturnWithExpression()
    {
        var result = StripAndRestore(
            "class Foo { int Bar() { return 42; } }");
        Assert.DoesNotContain("return42", result);
    }

    [Fact]
    public void ThrowWithExpression()
    {
        var result = StripAndRestore(
            "class Foo { void Bar() { throw new System.Exception(); } }");
        Assert.DoesNotContain("thrownew", result);
    }

    [Fact]
    public void NewClassSpacing()
    {
        var result = StripAndRestore(
            "class Foo { void Bar() { throw new System.Exception(); } }");
        // new keyword must not merge with the type name
        Assert.DoesNotContain("newSystem", result);
    }

    [Fact]
    public void ClassExtends()
    {
        var result = StripAndRestore(
            "class MyException : System.Exception { }");
        // The : must be separated from the class name
        Assert.DoesNotContain("MyException:", result);
    }

    [Fact]
    public void ClassImplementsMultiple()
    {
        var result = StripAndRestore(
            "class Foo : System.IDisposable, System.ICloneable { }");
        Assert.DoesNotContain("Foo:", result);
    }

    [Fact]
    public void ConstructorWithInitializer()
    {
        var result = StripAndRestore("""
            class MyException : System.Exception
            {
                public MyException(string message) : base(message) { }
            }
            """);
        // Modifiers and constructor name must not merge
        Assert.DoesNotContain("publicMyException", result);
    }

    [Fact]
    public void IdempotentOnWellFormedCode()
    {
        // Use the exact same source as the AutoFormatTests.WellFormattedCodeReturnsReferentiallyIdenticalTree test
        const string source = """
            using System;

            namespace Test
            {
                public class Foo
                {
                    private int _x;

                    public int Add(int a, int b)
                    {
                        var result = a + b;
                        _x = result;
                        return result;
                    }

                    public void DoNothing()
                    {
                    }
                }
            }
            """;

        var cu = _parser.Parse(source);
        var result = new MinimumViableSpacingVisitor().Visit(cu, 0);

        // Well-formed code should be unchanged (same reference)
        Assert.True(ReferenceEquals(cu, result),
            "Expected same object reference when spacing is already present");
    }

    [Fact]
    public void PreservesExistingFormatting()
    {
        const string source = """
            public   sealed   class   Foo
            {
                private   static   int   _x;
            }
            """;

        var cu = _parser.Parse(source);
        var printed1 = _printer.Print(cu);

        var result = new MinimumViableSpacingVisitor().Visit(cu, 0) as CompilationUnit ?? cu;
        var printed2 = _printer.Print(result);

        // Existing non-empty whitespace should not be modified
        Assert.Equal(printed1, printed2);
    }

    [Fact]
    public void IntegrationWithAutoFormat()
    {
        const string source = """
            class MyException : System.Exception
            {
                public MyException(string message) : base(message) { }
            }
            """;

        var cu = _parser.Parse(source);

        // Strip all whitespace (simulating recipe that builds nodes with Space.Empty)
        var stripped = new WhitespaceStripper().Visit(cu, 0) as CompilationUnit ?? cu;

        // Run through AutoFormat — MVS ensures printed output is parseable,
        // then Roslyn formats it, then WhitespaceReconciler maps formatting back
        var formatted = RoslynFormatter.Format(stripped);

        var result = _printer.Print(formatted);

        // Should not have smashed tokens
        Assert.DoesNotContain("publicMyException", result);
    }

    [Fact]
    public void MultipleModifiersOnClass()
    {
        var result = StripAndRestore("public abstract class Foo { }");
        Assert.DoesNotContain("publicabstract", result);
        Assert.DoesNotContain("abstractclass", result);
    }

    [Fact]
    public void AnnotatedClassWithModifier()
    {
        var result = StripAndRestore("""
            [System.Serializable]
            public class Foo { }
            """);
        // After annotation, modifier needs space
        Assert.DoesNotContain("]public", result);
    }

    [Fact]
    public void UsingDirective()
    {
        var result = StripAndRestore("using System;");
        Assert.DoesNotContain("usingSystem", result);
    }

    [Fact]
    public void UsingStaticDirective()
    {
        var result = StripAndRestore("using static System.Math;");
        Assert.DoesNotContain("usingstatic", result);
        Assert.DoesNotContain("staticSystem", result);
    }

    [Fact]
    public void NamespaceDeclaration()
    {
        var result = StripAndRestore("namespace MyApp { class Foo { } }");
        Assert.DoesNotContain("namespaceMyApp", result);
    }

    [Fact]
    public void FileScopedNamespace()
    {
        var result = StripAndRestore("""
            namespace MyApp;
            class Foo { }
            """);
        Assert.DoesNotContain("namespaceMyApp", result);
    }

    [Fact]
    public void PropertyDeclaration()
    {
        var result = StripAndRestore("""
            class Foo { public int Bar { get; set; } }
            """);
        Assert.DoesNotContain("publicint", result);
        Assert.DoesNotContain("intBar", result);
    }

    [Fact]
    public void ForEachLoop()
    {
        var result = StripAndRestore("""
            class Foo { void Bar() { foreach (var x in new int[0]) { } } }
            """);
        Assert.DoesNotContain("innew", result);
    }

    /// <summary>
    /// Visitor that strips all whitespace from the tree, simulating a recipe
    /// that builds nodes with Space.Empty everywhere.
    /// </summary>
    private class WhitespaceStripper : CSharpVisitor<int>
    {
        public override Space VisitSpace(Space space, int p)
        {
            return Space.Empty;
        }
    }
}
