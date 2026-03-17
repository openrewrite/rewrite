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
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Template;

public class PatternMatchTests : RewriteTest
{
    // ===============================================================
    // Method invocations
    // ===============================================================

    [Fact]
    public void MatchesExactMethodInvocation()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation("Console.WriteLine(\"hello\")")),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }",
                "class C { void M() { /*~~>*/Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDifferentMethodName()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation("Console.Write(\"hello\")")),
            CSharp(
                "class C { void M() { Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void CapturesExpressionInMethodArgument()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Console.WriteLine({expr})")),
            CSharp(
                "class C { void M() { Console.WriteLine(42); } }",
                "class C { void M() { /*~~>*/Console.WriteLine(42); } }"
            )
        );
    }

    [Fact]
    public void CapturesMultipleArgs()
    {
        var a = Capture.Of<Expression>("a");
        var b = Capture.Of<Expression>("b");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Math.Max({a}, {b})")),
            CSharp(
                "class C { void M() { Math.Max(1, 2); } }",
                "class C { void M() { /*~~>*/Math.Max(1, 2); } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDifferentArgCount()
    {
        var a = Capture.Of<Expression>("a");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Math.Max({a})")),
            CSharp(
                "class C { void M() { Math.Max(1, 2); } }"
            )
        );
    }

    [Fact]
    public void MatchesChainedMethodCall()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"{expr}.ToString()")),
            CSharp(
                "class C { void M() { var x = obj.ToString(); } }",
                "class C { void M() { var x = /*~~>*/obj.ToString(); } }"
            )
        );
    }

    [Fact]
    public void MatchesNestedMethodInvocation()
    {
        var inner = Capture.Of<Expression>("inner");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Console.WriteLine({inner}.ToString())")),
            CSharp(
                "class C { void M() { Console.WriteLine(x.ToString()); } }",
                "class C { void M() { /*~~>*/Console.WriteLine(x.ToString()); } }"
            )
        );
    }

    // ===============================================================
    // Field access
    // ===============================================================

    [Fact]
    public void MatchesFieldAccess()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindFieldAccess("Console.Out")),
            CSharp(
                "class C { void M() { var x = Console.Out; } }",
                "class C { void M() { var x = /*~~>*/Console.Out; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDifferentFieldName()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindFieldAccess("Console.Error")),
            CSharp(
                "class C { void M() { var x = Console.Out; } }"
            )
        );
    }

    // ===============================================================
    // Literals (int, string, bool, null)
    // ===============================================================

    [Fact]
    public void MatchesBooleanLiteral()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindLiteral("true")),
            CSharp(
                "class C { void M() { var x = true; } }",
                "class C { void M() { var x = /*~~>*/true; } }"
            )
        );
    }

    [Fact]
    public void MatchesNullLiteral()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindLiteral("null")),
            CSharp(
                "class C { void M() { var x = null; } }",
                "class C { void M() { var x = /*~~>*/null; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDifferentStringLiteral()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation("Console.WriteLine(\"hello\")")),
            CSharp(
                "class C { void M() { Console.WriteLine(\"world\"); } }"
            )
        );
    }

    // ===============================================================
    // Binary operators (+, -, &&, >=, etc.)
    // ===============================================================

    [Fact]
    public void MatchesBinaryExpression()
    {
        var lhs = Capture.Of<Expression>("lhs");
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(FindBinary($"{lhs} + {rhs}")),
            CSharp(
                "class C { void M() { var x = 1 + 2; } }",
                "class C { void M() { var x = /*~~>*/1 + 2; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDifferentOperator()
    {
        var lhs = Capture.Of<Expression>("lhs");
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(FindBinary($"{lhs} + {rhs}")),
            CSharp(
                "class C { void M() { var x = 1 - 2; } }"
            )
        );
    }

    [Fact]
    public void MatchesBooleanAndOperator()
    {
        var lhs = Capture.Of<Expression>("lhs");
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(FindBinary($"{lhs} && {rhs}")),
            CSharp(
                "class C { void M() { var x = true && false; } }",
                "class C { void M() { var x = /*~~>*/true && false; } }"
            )
        );
    }

    [Fact]
    public void MatchesComparisonOperator()
    {
        var lhs = Capture.Of<Expression>("lhs");
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(FindBinary($"{lhs} >= {rhs}")),
            CSharp(
                "class C { void M() { var x = 5 >= 3; } }",
                "class C { void M() { var x = /*~~>*/5 >= 3; } }"
            )
        );
    }

    // ===============================================================
    // Unary operators (-, !)
    // ===============================================================

    [Fact]
    public void MatchesUnaryExpression()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindUnary($"-{expr}")),
            CSharp(
                "class C { void M() { var x = -42; } }",
                "class C { void M() { var x = /*~~>*/-42; } }"
            )
        );
    }

    [Fact]
    public void MatchesLogicalNotUnary()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindUnary($"!{expr}")),
            CSharp(
                "class C { void M() { var x = !true; } }",
                "class C { void M() { var x = /*~~>*/!true; } }"
            )
        );
    }

    // ===============================================================
    // Ternary conditional
    // ===============================================================

    [Fact]
    public void MatchesTernaryWithConcreteCondition()
    {
        var then = Capture.Of<Expression>("tthen");
        var els = Capture.Of<Expression>("tels");
        RewriteRun(
            spec => spec.SetRecipe(FindTernary($"true ? {then} : {els}")),
            CSharp(
                "class C { void M() { var x = true ? 1 : 2; } }",
                "class C { void M() { var x = /*~~>*/true ? 1 : 2; } }"
            )
        );
    }

    // ===============================================================
    // Assignment and compound assignment (=, +=, *=)
    // ===============================================================

    [Fact]
    public void MatchesAssignment()
    {
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(FindAssignment($"x = {rhs}")),
            CSharp(
                "class C { int x; void M() { x = 42; } }",
                "class C { int x; void M() { /*~~>*/x = 42; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDifferentAssignmentTarget()
    {
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(FindAssignment($"x = {rhs}")),
            CSharp(
                "class C { int y; void M() { y = 42; } }"
            )
        );
    }

    [Fact]
    public void MatchesCompoundAssignment()
    {
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(FindAssignmentOperation($"x += {rhs}")),
            CSharp(
                "class C { int x; void M() { x += 1; } }",
                "class C { int x; void M() { /*~~>*/x += 1; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDifferentCompoundOperator()
    {
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(FindAssignmentOperation($"x += {rhs}")),
            CSharp(
                "class C { int x; void M() { x *= 2; } }"
            )
        );
    }

    // ===============================================================
    // NewClass and NewArray
    // ===============================================================

    [Fact]
    public void MatchesNewClassExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindNewClass("new object()")),
            CSharp(
                "class C { void M() { var x = new object(); } }",
                "class C { void M() { var x = /*~~>*/new object(); } }"
            )
        );
    }

    [Fact]
    public void MatchesNewClassWithCapturedArg()
    {
        var arg = Capture.Of<Expression>("arg");
        RewriteRun(
            spec => spec.SetRecipe(FindNewClass($"new Exception({arg})")),
            CSharp(
                "class C { void M() { var x = new Exception(\"oops\"); } }",
                "class C { void M() { var x = /*~~>*/new Exception(\"oops\"); } }"
            )
        );
    }

    [Fact]
    public void MatchesNewArray()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindNewArray("new int[] { 1, 2 }")),
            CSharp(
                "class C { void M() { var x = new int[] { 1, 2 }; } }",
                "class C { void M() { var x = /*~~>*/new int[] { 1, 2 }; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchNewArrayWithDifferentElements()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindNewArray("new int[] { 3, 4 }")),
            CSharp(
                "class C { void M() { var x = new int[] { 1, 2 }; } }"
            )
        );
    }

    // ===============================================================
    // TypeCast — exercises ControlParentheses<TypeTree>
    // ===============================================================

    [Fact]
    public void MatchesTypeCast()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindTypeCast($"(int){expr}")),
            CSharp(
                "class C { void M() { var x = (int)3.14; } }",
                "class C { void M() { var x = /*~~>*/(int)3.14; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDifferentCastType()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindTypeCast($"(long){expr}")),
            CSharp(
                "class C { void M() { var x = (int)3.14; } }"
            )
        );
    }

    // ===============================================================
    // ArrayAccess — exercises ArrayDimension
    // ===============================================================

    [Fact]
    public void MatchesArrayAccess()
    {
        var idx = Capture.Of<Expression>("idx");
        RewriteRun(
            spec => spec.SetRecipe(FindArrayAccess($"arr[{idx}]")),
            CSharp(
                "class C { int[] arr; void M() { var x = arr[0]; } }",
                "class C { int[] arr; void M() { var x = /*~~>*/arr[0]; } }"
            )
        );
    }

    // ===============================================================
    // Return statement
    // ===============================================================

    [Fact]
    public void MatchesReturnStatement()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindReturn($"return {expr}")),
            CSharp(
                "class C { int M() { return 42; } }",
                "class C { int M() { /*~~>*/return 42; } }"
            )
        );
    }

    // ===============================================================
    // Throw statement
    // ===============================================================

    [Fact]
    public void MatchesThrowStatement()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindThrow("throw new Exception()")),
            CSharp(
                "class C { void M() { throw new Exception(); } }",
                "class C { void M() { /*~~>*/throw new Exception(); } }"
            )
        );
    }

    [Fact]
    public void MatchesThrowWithCapturedArg()
    {
        var msg = Capture.Of<Expression>("msg");
        RewriteRun(
            spec => spec.SetRecipe(FindThrow($"throw new Exception({msg})")),
            CSharp(
                "class C { void M() { throw new Exception(\"oops\"); } }",
                "class C { void M() { /*~~>*/throw new Exception(\"oops\"); } }"
            )
        );
    }

    // ===============================================================
    // If statement — exercises ControlParentheses<Expression>
    // ===============================================================

    [Fact]
    public void MatchesIfStatement()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindIf("if (true) { }")),
            CSharp(
                "class C { void M() { if (true) { } } }",
                "class C { void M() { /*~~>*/if (true) { } } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchIfWithDifferentCondition()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindIf("if (false) { }")),
            CSharp(
                "class C { void M() { if (true) { } } }"
            )
        );
    }

    // ===============================================================
    // WhileLoop
    // ===============================================================

    [Fact]
    public void MatchesWhileLoop()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindWhileLoop("while (true) { }")),
            CSharp(
                "class C { void M() { while (true) { } } }",
                "class C { void M() { /*~~>*/while (true) { } } }"
            )
        );
    }

    // ===============================================================
    // DoWhileLoop
    // ===============================================================

    [Fact]
    public void MatchesDoWhileLoop()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindDoWhileLoop("do { } while (true)")),
            CSharp(
                "class C { void M() { do { } while (true); } }",
                "class C { void M() { /*~~>*/do { } while (true); } }"
            )
        );
    }

    // ===============================================================
    // ForEachLoop
    // ===============================================================

    [Fact]
    public void MatchesForEachLoop()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindForEachLoop("foreach (var x in items) { }")),
            CSharp(
                "class C { void M() { foreach (var x in items) { } } }",
                "class C { void M() { /*~~>*/foreach (var x in items) { } } }"
            )
        );
    }

    // ===============================================================
    // Switch statement
    // ===============================================================

    [Fact]
    public void MatchesSwitchStatement()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindSwitch("switch (1) { case 1: break; }")),
            CSharp(
                "class C { void M() { switch (1) { case 1: break; } } }",
                "class C { void M() { /*~~>*/switch (1) { case 1: break; } } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchSwitchWithDifferentSelector()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindSwitch("switch (2) { case 1: break; }")),
            CSharp(
                "class C { void M() { switch (1) { case 1: break; } } }"
            )
        );
    }

    // ===============================================================
    // Try-Catch
    // ===============================================================

    [Fact]
    public void MatchesTryCatch()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindTry("try { } catch (Exception e) { }")),
            CSharp(
                "class C { void M() { try { } catch (Exception e) { } } }",
                "class C { void M() { /*~~>*/try { } catch (Exception e) { } } }"
            )
        );
    }

    // ===============================================================
    // Lambda
    // ===============================================================

    [Fact]
    public void MatchesLambdaExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindLambda("() => 42")),
            CSharp(
                "class C { void M() { var f = () => 42; } }",
                "class C { void M() { var f = /*~~>*/() => 42; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchLambdaWithDifferentBody()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindLambda("() => 99")),
            CSharp(
                "class C { void M() { var f = () => 42; } }"
            )
        );
    }

    // ===============================================================
    // InstanceOf — C# `is` keyword (without pattern variable)
    // ===============================================================

    [Fact]
    public void MatchesIsExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindInstanceOf("1 is int")),
            CSharp(
                "class C { void M() { var x = 1 is int; } }",
                "class C { void M() { var x = /*~~>*/1 is int; } }"
            )
        );
    }

    // ===============================================================
    // C#-specific: DefaultExpression
    // ===============================================================

    [Fact]
    public void MatchesDefaultExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindDefaultExpression("default(int)")),
            CSharp(
                "class C { void M() { var x = default(int); } }",
                "class C { void M() { var x = /*~~>*/default(int); } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchDefaultWithDifferentType()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindDefaultExpression("default(string)")),
            CSharp(
                "class C { void M() { var x = default(int); } }"
            )
        );
    }

    // ===============================================================
    // C#-specific: SizeOf
    // ===============================================================

    [Fact]
    public void MatchesSizeOf()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindSizeOf("sizeof(int)")),
            CSharp(
                "class C { void M() { var x = sizeof(int); } }",
                "class C { void M() { var x = /*~~>*/sizeof(int); } }"
            )
        );
    }

    // ===============================================================
    // C#-specific: SwitchExpression
    // ===============================================================

    [Fact]
    public void MatchesSwitchExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindSwitchExpression("1 switch { 1 => \"one\", _ => \"other\" }")),
            CSharp(
                "class C { void M() { var x = 1 switch { 1 => \"one\", _ => \"other\" }; } }",
                "class C { void M() { var x = /*~~>*/1 switch { 1 => \"one\", _ => \"other\" }; } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchSwitchExpressionWithDifferentArm()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindSwitchExpression("1 switch { 1 => \"ONE\", _ => \"other\" }")),
            CSharp(
                "class C { void M() { var x = 1 switch { 1 => \"one\", _ => \"other\" }; } }"
            )
        );
    }

    // ===============================================================
    // C#-specific: TupleExpression
    // ===============================================================

    [Fact]
    public void MatchesTupleExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindTupleExpression("(1, 2)")),
            CSharp(
                "class C { void M() { var x = (1, 2); } }",
                "class C { void M() { var x = /*~~>*/(1, 2); } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchTupleWithDifferentElements()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindTupleExpression("(1, 3)")),
            CSharp(
                "class C { void M() { var x = (1, 2); } }"
            )
        );
    }

    // ===============================================================
    // C#-specific: NullSafeExpression (null-forgiving !)
    // ===============================================================

    [Fact]
    public void MatchesNullForgivingExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindNullSafeExpression("s!")),
            CSharp(
                "class C { void M() { string? s = null; var x = s!; } }",
                "class C { void M() { string? s = null; var x = /*~~>*/s!; } }"
            )
        );
    }

    // ===============================================================
    // Null-conditional member access (?.)
    // ===============================================================

    [Fact]
    public void MatchesNullConditionalMethodCall()
    {
        var obj = Capture.Of<Expression>("obj");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"{obj}?.ToString()")),
            CSharp(
                "class C { void M() { string s = null; var x = s?.ToString(); } }",
                "class C { void M() { string s = null; var x = /*~~>*/s?.ToString(); } }"
            )
        );
    }

    [Fact]
    public void NullConditionalPatternDoesNotMatchRegularDotAccess()
    {
        var obj = Capture.Of<Expression>("obj");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"{obj}?.ToString()")),
            CSharp(
                // Regular dot access should NOT match a ?. pattern
                "class C { void M() { var x = \"hello\".ToString(); } }"
            )
        );
    }

    [Fact]
    public void RegularDotPatternDoesNotMatchNullConditionalAccess()
    {
        var obj = Capture.Of<Expression>("obj");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"{obj}.ToString()")),
            CSharp(
                // ?. access should NOT match a regular . pattern
                "class C { void M() { string s = null; var x = s?.ToString(); } }"
            )
        );
    }

    [Fact]
    public void MatchesNullConditionalFieldAccess()
    {
        var obj = Capture.Of<Expression>("obj");
        RewriteRun(
            spec => spec.SetRecipe(FindFieldAccess($"{obj}?.Length")),
            CSharp(
                "class C { void M() { string s = null; var x = s?.Length; } }",
                "class C { void M() { string s = null; var x = /*~~>*/s?.Length; } }"
            )
        );
    }

    [Fact]
    public void MatchesExactNullConditionalMethodCall()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation("s?.ToString()")),
            CSharp(
                "class C { void M() { string s = null; var x = s?.ToString(); } }",
                "class C { void M() { string s = null; var x = /*~~>*/s?.ToString(); } }"
            )
        );
    }

    // ===============================================================
    // C#-specific: IsPattern (is with pattern variable)
    // ===============================================================

    [Fact]
    public void MatchesIsPatternExpression()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindIsPattern("o is int n")),
            CSharp(
                "class C { void M() { object o = 1; if (o is int n) {} } }",
                "class C { void M() { object o = 1; if (/*~~>*/o is int n) {} } }"
            )
        );
    }

    // ===============================================================
    // C#-specific: CsBinary (as cast)
    // ===============================================================

    [Fact]
    public void MatchesAsCast()
    {
        RewriteRun(
            spec => spec.SetRecipe(FindCsBinary("o as string")),
            CSharp(
                "class C { void M() { object o = 1; var x = o as string; } }",
                "class C { void M() { object o = 1; var x = /*~~>*/o as string; } }"
            )
        );
    }

    // ===============================================================
    // Capture binding behavior
    // ===============================================================

    [Fact]
    public void VariadicCaptureMatchesZeroArguments()
    {
        var args = Capture.Variadic<Expression>("args");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Foo({args})")),
            CSharp(
                "class C { void M() { Foo(); } }",
                "class C { void M() { /*~~>*/Foo(); } }"
            )
        );
    }

    [Fact]
    public void VariadicCaptureInNonTrailingPosition()
    {
        var args = Capture.Variadic<Expression>("args");
        var last = Capture.Of<Expression>("last");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Foo({args}, {last})")),
            CSharp(
                "class C { void M() { Foo(1, 2, 3); } }",
                "class C { void M() { /*~~>*/Foo(1, 2, 3); } }"
            )
        );
    }

    [Fact]
    public void VariadicCaptureWithMinBoundRejectsFewerArgs()
    {
        var args = Capture.Variadic<Expression>("args", min: 2);
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Foo({args})")),
            CSharp(
                // Only 1 arg — min is 2, should NOT match
                "class C { void M() { Foo(1); } }"
            )
        );
    }

    [Fact]
    public void VariadicCaptureWithMaxBoundRejectsMoreArgs()
    {
        var args = Capture.Variadic<Expression>("args", max: 1);
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Foo({args})")),
            CSharp(
                // 3 args — max is 1, should NOT match
                "class C { void M() { Foo(1, 2, 3); } }"
            )
        );
    }

    [Fact]
    public void ConsistentCaptureBindingMatchesWhenSame()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Math.Max({expr}, {expr})")),
            CSharp(
                "class C { void M() { Math.Max(1, 1); } }",
                "class C { void M() { /*~~>*/Math.Max(1, 1); } }"
            )
        );
    }

    [Fact]
    public void ConsistentCaptureBindingFailsWhenDifferent()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Math.Max({expr}, {expr})")),
            CSharp(
                "class C { void M() { Math.Max(1, 2); } }"
            )
        );
    }

    [Fact]
    public void CaptureBindsEntireSubtree()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation($"Console.WriteLine({expr})")),
            CSharp(
                "class C { void M() { Console.WriteLine(1 + 2); } }",
                "class C { void M() { /*~~>*/Console.WriteLine(1 + 2); } }"
            )
        );
    }

    [Fact]
    public void DoesNotMatchWrongNodeType()
    {
        // Pattern is a method invocation — binary expression should not be marked
        RewriteRun(
            spec => spec.SetRecipe(FindMethodInvocation("Console.WriteLine(42)")),
            CSharp(
                "class C { void M() { var x = 1 + 2; } }"
            )
        );
    }

    // ===============================================================
    // Recipe factories
    // ===============================================================

    private static Core.Recipe Search<T>(TemplateStringHandler handler) where T : J =>
        new PatternSearchRecipe<T>(CSharpPattern.Create(handler));

    private static Core.Recipe Search<T>(string code) where T : J =>
        new PatternSearchRecipe<T>(CSharpPattern.Create(code));

    private static Core.Recipe FindMethodInvocation(TemplateStringHandler h) => Search<MethodInvocation>(h);
    private static Core.Recipe FindMethodInvocation(string c) => Search<MethodInvocation>(c);
    private static Core.Recipe FindFieldAccess(TemplateStringHandler h) => Search<FieldAccess>(h);
    private static Core.Recipe FindFieldAccess(string c) => Search<FieldAccess>(c);
    private static Core.Recipe FindLiteral(string c) => Search<Literal>(c);
    private static Core.Recipe FindBinary(TemplateStringHandler h) => Search<Binary>(h);
    private static Core.Recipe FindUnary(TemplateStringHandler h) => Search<Unary>(h);
    private static Core.Recipe FindTernary(TemplateStringHandler h) => Search<Ternary>(h);
    private static Core.Recipe FindAssignment(TemplateStringHandler h) => Search<Assignment>(h);
    private static Core.Recipe FindAssignmentOperation(TemplateStringHandler h) => Search<OpenRewrite.CSharp.AssignmentOperation>(h);
    private static Core.Recipe FindNewClass(string c) => Search<NewClass>(c);
    private static Core.Recipe FindNewClass(TemplateStringHandler h) => Search<NewClass>(h);
    private static Core.Recipe FindNewArray(string c) => Search<NewArray>(c);
    private static Core.Recipe FindTypeCast(TemplateStringHandler h) => Search<TypeCast>(h);
    private static Core.Recipe FindArrayAccess(TemplateStringHandler h) => Search<ArrayAccess>(h);
    private static Core.Recipe FindReturn(TemplateStringHandler h) => Search<Return>(h);
    private static Core.Recipe FindThrow(string c) => Search<Throw>(c);
    private static Core.Recipe FindThrow(TemplateStringHandler h) => Search<Throw>(h);
    private static Core.Recipe FindIf(string c) => Search<If>(c);
    private static Core.Recipe FindWhileLoop(string c) => Search<WhileLoop>(c);
    private static Core.Recipe FindDoWhileLoop(string c) => Search<DoWhileLoop>(c);
    private static Core.Recipe FindForEachLoop(string c) => Search<ForEachLoop>(c);
    private static Core.Recipe FindSwitch(string c) => Search<Switch>(c);
    private static Core.Recipe FindTry(string c) => Search<Try>(c);
    private static Core.Recipe FindLambda(string c) => Search<Lambda>(c);
    private static Core.Recipe FindInstanceOf(string c) => Search<InstanceOf>(c);
    private static Core.Recipe FindDefaultExpression(string c) => Search<DefaultExpression>(c);
    private static Core.Recipe FindSizeOf(string c) => Search<SizeOf>(c);
    private static Core.Recipe FindSwitchExpression(string c) => Search<OpenRewrite.CSharp.SwitchExpression>(c);
    private static Core.Recipe FindTupleExpression(string c) => Search<TupleExpression>(c);
    private static Core.Recipe FindNullSafeExpression(string c) => Search<NullSafeExpression>(c);
    private static Core.Recipe FindIsPattern(string c) => Search<IsPattern>(c);
    private static Core.Recipe FindCsBinary(string c) => Search<CsBinary>(c);
}

/// <summary>
/// Generic search recipe that visits all nodes of type <typeparamref name="T"/>
/// and marks matches with a <see cref="SearchResult"/> marker.
/// </summary>
file class PatternSearchRecipe<T>(CSharpPattern pat) : Core.Recipe where T : J
{
    public override string DisplayName => $"Find {typeof(T).Name}";
    public override string Description => $"Searches for {typeof(T).Name} matching the pattern.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new SearchVisitor(pat);

    private class SearchVisitor(CSharpPattern pat) : CSharpVisitor<ExecutionContext>
    {
        public override J? PreVisit(J tree, ExecutionContext ctx)
        {
            if (tree is T t)
            {
                return pat.Find(t, Cursor);
            }
            return tree;
        }
    }
}
