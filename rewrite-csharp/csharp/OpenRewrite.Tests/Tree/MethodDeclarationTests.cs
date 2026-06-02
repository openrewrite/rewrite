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
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class MethodDeclarationTests : RewriteTest
{
    [Fact]
    public void SimpleVoidMethod()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithReturnType()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void PublicMethod()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public void Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithMultipleModifiers()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    public static void Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithOneParameter()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x) { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithMultipleParameters()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar(int x, string y) { }
                }
                """
            )
        );
    }

    [Fact]
    public void MethodWithBody()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar() {
                        var x = 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExplicitInterfaceImplementationVoid()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo { void Bar(); }
                class Foo : IFoo {
                    void IFoo.Bar() { }
                }
                """
            )
        );
    }

    [Fact]
    public void ExplicitInterfaceImplementationWithReturnType()
    {
        RewriteRun(
            CSharp(
                """
                using System.Collections;
                public class MyCollection : IEnumerable
                {
                    IEnumerator IEnumerable.GetEnumerator()
                    {
                        return null;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ExpressionBodiedMethod()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    int Bar() => 42;
                }
                """
            )
        );
    }

    /// <summary>
    /// Verifies that the printer does not crash with an InvalidCastException when a recipe
    /// transforms the Return statement inside an expression-bodied method into a different
    /// statement type (e.g., If). The printer should fall back to block-body printing.
    /// </summary>
    [Fact]
    public void PrinterHandlesExpressionBodiedMethodWithNonReturnBody()
    {
        var parser = new CSharpParser();

        // Parse an expression-bodied method (contains a Return in the body)
        var cu = (CompilationUnit)parser.Parse("class C { int M() => 42; }");

        // Parse a source with an If statement to transplant
        var ifSource = (CompilationUnit)parser.Parse("class D { void N() { if (true) { return 1; } } }");
        var ifClass = (ClassDeclaration)ifSource.Members[0].Element;
        var ifMethod = (MethodDeclaration)ifClass.Body.Statements[0].Element;
        var ifStmt = (If)ifMethod.Body!.Statements[0].Element;

        // Replace the Return in the expression-bodied method's body with the If
        var visitor = new ReplaceReturnWithIfVisitor(ifStmt);
        var result = visitor.Visit(cu, new OpenRewrite.Core.ExecutionContext());

        // The printer should not throw — it should fall back to block-body printing
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(result!);

        // The output should contain the if statement, not the => syntax
        Assert.Contains("if", printed);
        Assert.DoesNotContain("=>", printed);
    }

    private class ReplaceReturnWithIfVisitor(If replacement) : CSharpVisitor<OpenRewrite.Core.ExecutionContext>
    {
        public override J VisitReturn(Return ret, OpenRewrite.Core.ExecutionContext ctx)
        {
            return replacement.WithPrefix(ret.Prefix);
        }
    }
}
