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
    public void WellFormattedCodeReturnsReferentiallyIdenticalTree()
    {
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
        var formatted = RoslynFormatter.Format(cu);

        // When source is already well-formatted, the formatter should short-circuit
        // and return the exact same object — no parse, no reconcile, no allocation
        Assert.True(ReferenceEquals(cu, formatted),
            "Expected the same object reference when formatting already well-formatted code");
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
        var classDecl = cu.Members[0].Element as ClassDeclaration;
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
    public void FormatSubtreeDirectCall()
    {
        const string source =
            "class Foo\n" +
            "{\n" +
            "    void Bar()\n" +
            "    {\n" +
            "        int x=1+2;\n" +
            "    }\n" +
            "}\n";

        var cu = _parser.Parse(source);

        // Find the method body block
        var classDecl = cu.Members[0].Element as ClassDeclaration;
        var method = classDecl!.Body.Statements[0].Element as MethodDeclaration;
        var body = method!.Body!;

        // Structurally modify the body: strip its prefix to create a new object with the same ID
        var modifiedBody = body.WithPrefix(Space.Empty);

        // FormatSubtree should splice modifiedBody into the CU, format, and extract it
        var formatted = RoslynFormatter.FormatSubtree(cu, body.Id, modifiedBody, stopAfter: null);

        var printed = _printer.Print(formatted);
        Assert.Contains("x = 1 + 2", printed);
    }

    [Fact]
    public void SubtreeAutoFormatWorksAfterStructuralChange()
    {
        // Source has a for loop with no braces
        const string source =
            "class Foo\n" +
            "{\n" +
            "    void Bar()\n" +
            "    {\n" +
            "        for (int i = 0; i < 10; i++)\n" +
            "            DoWork();\n" +
            "    }\n" +
            "    void DoWork() { }\n" +
            "}\n";

        var cu = _parser.Parse(source);

        // Find the for loop
        ForLoop? forLoop = null;
        new ForLoopFinder(f => forLoop = f).Visit(cu, 0);
        Assert.NotNull(forLoop);

        // Structurally modify: wrap body in a Block with empty spaces
        var stmt = forLoop.Body.Element;
        var stmtPadded = new JRightPadded<Statement>(stmt, Space.Empty, Markers.Empty);
        var block = new Block(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
            new List<JRightPadded<Statement>> { stmtPadded },
            Space.Empty
        );
        var modifiedForLoop = forLoop.WithBody(forLoop.Body.WithElement(block));

        // Build cursor: root → CU → forLoop (points to OLD forLoop in old CU)
        var rootCursor = new Cursor(null, Cursor.ROOT_VALUE);
        var cuCursor = new Cursor(rootCursor, cu);
        var forCursor = new Cursor(cuCursor, forLoop);

        // AutoFormat the modified subtree
        var formatted = modifiedForLoop.AutoFormat(forCursor);

        // The result should have properly indented braces
        var printed = _printer.Print(formatted);
        Assert.Contains("{\n", printed);
        Assert.Contains("DoWork();", printed);
        // The block should NOT be left with empty spaces (unformatted)
        Assert.DoesNotContain("{DoWork", printed);
    }

    [Fact]
    public void AutoFormatFromVisitorWithExplicitCursor()
    {
        // Verify AutoFormat works when called from inside a visitor using the
        // extension method with the visitor's cursor, matching JS/Python pattern
        const string source =
            "class Foo\n" +
            "{\n" +
            "    void Bar()\n" +
            "    {\n" +
            "        for (int i = 0; i < 10; i++)\n" +
            "            DoWork();\n" +
            "    }\n" +
            "    void DoWork() { }\n" +
            "}\n";

        var cu = _parser.Parse(source);

        // Run a visitor that wraps the for-loop body in a block and auto-formats
        var visitor = new WrapForBodyInBlock();
        var result = visitor.Visit(cu, 0)!;

        var printed = _printer.Print(result);
        // The block should be properly formatted, not left with empty spaces
        Assert.Contains("{\n", printed);
        Assert.DoesNotContain("{DoWork", printed);
    }

    /// <summary>
    /// Visitor that wraps a for-loop's body in a Block and calls AutoFormat
    /// using the extension method with an explicit cursor — the same pattern
    /// used in JS and Python.
    /// </summary>
    private class WrapForBodyInBlock : CSharpVisitor<int>
    {
        public override J VisitForLoop(ForLoop forLoop, int p)
        {
            var fl = (ForLoop)base.VisitForLoop(forLoop, p);

            // Only wrap if body is not already a block
            if (fl.Body.Element is Block)
                return fl;

            var stmt = fl.Body.Element;
            var stmtPadded = new JRightPadded<Statement>(stmt, Space.Empty, Markers.Empty);
            var block = new Block(
                Guid.NewGuid(),
                Space.Empty,
                Markers.Empty,
                new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
                new List<JRightPadded<Statement>> { stmtPadded },
                Space.Empty
            );
            var modified = fl.WithBody(fl.Body.WithElement(block));

            return modified.AutoFormat(Cursor.ParentTree);
        }
    }

    [Fact]
    public void FormatSubtreeFallsBackWhenSpliceFails()
    {
        const string source =
            "class Foo\n" +
            "{\n" +
            "    void Bar()\n" +
            "    {\n" +
            "        int x = 1;\n" +
            "    }\n" +
            "}\n";

        var cu = _parser.Parse(source);

        // Create a simple block with a brand-new ID that doesn't exist in the CU
        var block = new Block(
            Guid.NewGuid(),
            Space.Empty,
            Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
            new List<JRightPadded<Statement>>(),
            Space.Empty
        );

        // FormatSubtree with a nonexistent nodeToReplaceId — splice will fail
        var result = RoslynFormatter.FormatSubtree(cu, Guid.NewGuid(), block, stopAfter: null);

        // Should not throw — should return the replacement unchanged
        Assert.NotNull(result);
        Assert.Equal(block.Id, result.Id);
    }

    private class ForLoopFinder(Action<ForLoop> onFound) : CSharpVisitor<int>
    {
        public override J VisitForLoop(ForLoop forLoop, int p)
        {
            onFound(forLoop);
            return forLoop;
        }
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
