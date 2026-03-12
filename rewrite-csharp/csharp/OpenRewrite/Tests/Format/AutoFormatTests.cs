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

public class AutoFormatTests
{
    private readonly CSharpParser _parser = new();
    private readonly CSharpPrinter<int> _printer = new();

    [Fact]
    public void FormatsBasicClass()
    {
        const string before = """
            class Foo{
            void Bar(){
            int x=1;
            }
            }
            """;

        var cu = _parser.Parse(before);
        var formatted = RoslynFormatter.Format(cu);

        var result = _printer.Print(formatted);
        // Roslyn should add proper indentation
        Assert.Contains("    void Bar()", result);
        Assert.Contains("    {", result);
    }

    [Fact]
    public void FormatsUsingStatements()
    {
        const string before = """
            using System;
            using System.Collections.Generic;

            namespace Test
            {
                class Foo { }
            }
            """;

        var cu = _parser.Parse(before);
        var formatted = RoslynFormatter.Format(cu);

        // Already well-formatted — should be unchanged
        Assert.Equal(before, _printer.Print(formatted));
    }

    [Fact]
    public void PreservesIdAfterFormat()
    {
        const string source = """
            class Foo
            {
                void Bar() { }
            }
            """;

        var cu = _parser.Parse(source);
        var formatted = RoslynFormatter.Format(cu);

        Assert.Equal(cu.Id, formatted.Id);
    }

    [Fact]
    public void FormatsSpacingAroundOperators()
    {
        const string before = """
            class Foo
            {
                void Bar()
                {
                    int x=1+2;
                }
            }
            """;

        var cu = _parser.Parse(before);
        var formatted = RoslynFormatter.Format(cu);
        var result = _printer.Print(formatted);

        Assert.Contains("int x = 1 + 2;", result);
    }

    [Fact]
    public void HandlesAlreadyFormattedCode()
    {
        const string source = """
            using System;

            namespace Test
            {
                public class Foo
                {
                    public void Bar()
                    {
                        var x = 1;
                    }
                }
            }
            """;

        var cu = _parser.Parse(source);
        var formatted = RoslynFormatter.Format(cu);

        // Should be unchanged
        Assert.Equal(source, _printer.Print(formatted));
    }

    [Fact]
    public void FormatStyleDetectsSpaces()
    {
        const string source = """
            class Foo
            {
                void Bar()
                {
                    int x = 1;
                }
            }
            """;

        var style = FormatStyle.DetectStyle(source);
        Assert.False(style.UseTabs);
        Assert.Equal(4, style.IndentationSize);
    }

    [Fact]
    public void FormatStyleDetectsTabs()
    {
        var source = "class Foo\n{\n\tvoid Bar()\n\t{\n\t\tint x = 1;\n\t}\n}\n";
        var style = FormatStyle.DetectStyle(source);
        Assert.True(style.UseTabs);
    }

    [Fact]
    public void FormatStyleDetectsTwoSpaceIndent()
    {
        const string source = """
            class Foo
            {
              void Bar()
              {
                int x = 1;
              }
            }
            """;

        var style = FormatStyle.DetectStyle(source);
        Assert.False(style.UseTabs);
        Assert.Equal(2, style.IndentationSize);
    }

    [Fact]
    public void RoslynFormatterNormalizesWhitespace()
    {
        const string before = "class Foo{void Bar(){int x=1;}}";
        var style = new FormatStyle(false, 4, "\n");
        var formatted = RoslynFormatter.FormatWithRoslyn(before, style);

        // Roslyn normalizes spacing (adds spaces around braces) but doesn't add line breaks
        Assert.Contains("class Foo", formatted);
        Assert.NotEqual(before, formatted);
    }

    [Fact]
    public void IntegrationAutoFormatVisitorOnIllFormattedSource()
    {
        // A deliberately ill-formatted C# file with multiple issues:
        // - missing spaces around operators and after keywords
        // - wrong indentation throughout
        // - inconsistent brace placement
        // - cramped parameter lists
        const string before =
            "using System;\n" +
            "using System.Collections.Generic;\n" +
            "namespace MyApp{\n" +
            "public class Calculator{\n" +
            "private readonly List<int> _history=new List<int>();\n" +
            "public int Add(int a,int b){\n" +
            "var result=a+b;\n" +
            "_history.Add(result);\n" +
            "return result;\n" +
            "}\n" +
            "public int Subtract(int a,int b){\n" +
            "var result=a-b;\n" +
            "_history.Add(result);\n" +
            "return result;\n" +
            "}\n" +
            "public List<int> GetHistory(){\n" +
            "return _history;\n" +
            "}\n" +
            "}\n" +
            "}\n";

        var cu = _parser.Parse(before);

        // Run through the AutoFormatVisitor, same as a recipe would
        var visitor = new AutoFormatVisitor<int>();
        var result = visitor.Visit(cu, 0);
        Assert.NotNull(result);

        var formatted = _printer.Print(result);

        // Verify the formatter actually changed something
        Assert.NotEqual(before, formatted);

        // Verify structural corrections Roslyn makes:

        // 1. Spaces around operators
        Assert.Contains("a + b", formatted);
        Assert.Contains("a - b", formatted);

        // 2. Spaces after commas in parameter lists
        Assert.Contains("int a, int b", formatted);

        // 3. Space in field initializer
        Assert.Contains("_history = new", formatted);

        // 4. Class members should be indented (Roslyn adds 4-space indent inside namespace + class)
        Assert.Contains("private readonly", formatted);
        Assert.Contains("public int Add", formatted);

        // 5. Verify it still round-trips (reparse + reprint = same)
        var reparsed = _parser.Parse(formatted);
        var reprinted = _printer.Print(reparsed);
        Assert.Equal(formatted, reprinted);

        // 6. Verify the original tree's ID is preserved
        Assert.Equal(cu.Id, ((CompilationUnit)result).Id);
    }

    [Fact]
    public void IntegrationAutoFormatVisitorPreservesWellFormattedCode()
    {
        // Well-formatted code should pass through unchanged
        const string source = """
            using System;

            namespace MyApp
            {
                public class Greeter
                {
                    private readonly string _name;

                    public Greeter(string name)
                    {
                        _name = name;
                    }

                    public string Greet()
                    {
                        return $"Hello, {_name}!";
                    }
                }
            }
            """;

        var cu = _parser.Parse(source);

        var visitor = new AutoFormatVisitor<int>();
        var result = visitor.Visit(cu, 0)!;

        Assert.Equal(source, _printer.Print(result));
    }

    [Fact]
    public void IntegrationAutoFormatVisitorHandlesProperties()
    {
        const string before = """
            class Person{
            public string Name{get;set;}
            public int Age{get;set;}
            public string FullName=>$"{Name} ({Age})";
            }
            """;

        var cu = _parser.Parse(before);

        var visitor = new AutoFormatVisitor<int>();
        var result = visitor.Visit(cu, 0)!;
        var formatted = _printer.Print(result);

        // Proper indentation of properties
        Assert.Contains("\n    public string Name", formatted);
        Assert.Contains("\n    public int Age", formatted);

        // Spaces around braces in auto-properties
        Assert.Contains("{ get; set; }", formatted);

        // Expression-bodied member spacing
        Assert.Contains("FullName =>", formatted);
    }

    [Fact]
    public void SubtreeAutoFormatReturnsSubtreeNotCompilationUnit()
    {
        // Verify that AutoFormat on a subtree returns the subtree type, not CompilationUnit
        const string source =
            "class Foo{\n" +
            "void Bar(){\n" +
            "int x=1+2;\n" +
            "}\n" +
            "}\n";

        var cu = _parser.Parse(source);

        // Get the class declaration from the CU to use as the subtree target
        var classDecl = cu.Members[0] as ClassDeclaration;
        Assert.NotNull(classDecl);

        // Build a cursor chain: root → CU → classDecl
        var rootCursor = new Cursor(null, Cursor.ROOT_VALUE);
        var cuCursor = new Cursor(rootCursor, cu);
        var classCursor = new Cursor(cuCursor, classDecl);

        // AutoFormat the class declaration subtree
        var formatted = classDecl.AutoFormat(classCursor);

        // The result should be a ClassDeclaration, not a CompilationUnit
        Assert.IsType<ClassDeclaration>(formatted);
        Assert.IsNotType<CompilationUnit>(formatted);

        // Should preserve the original node's ID
        Assert.Equal(classDecl.Id, formatted.Id);

        // Should have formatted whitespace applied
        var printed = _printer.Print(formatted);
        Assert.Contains("x = 1 + 2", printed);
    }

    [Fact]
    public void IntegrationAutoFormatVisitorHandlesControlFlow()
    {
        const string before = """
            class Foo
            {
            void Bar(int x)
            {
            if(x>0){
            Console.WriteLine("positive");
            }else if(x<0){
            Console.WriteLine("negative");
            }else{
            Console.WriteLine("zero");
            }
            for(int i=0;i<x;i++){
            Console.WriteLine(i);
            }
            }
            }
            """;

        var cu = _parser.Parse(before);

        var visitor = new AutoFormatVisitor<int>();
        var result = visitor.Visit(cu, 0)!;
        var formatted = _printer.Print(result);

        // Spaces after keywords
        Assert.Contains("if (x > 0)", formatted);
        Assert.Contains("else if (x < 0)", formatted);
        Assert.Contains("for (int i = 0; i < x; i++)", formatted);

        // Proper indentation of bodies
        Assert.Contains("\n            Console.WriteLine(", formatted);
    }
}
