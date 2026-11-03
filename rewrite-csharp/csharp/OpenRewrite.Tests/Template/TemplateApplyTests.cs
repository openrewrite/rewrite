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

public class TemplateApplyTests : RewriteTest
{
    // ===============================================================
    // Pattern match → template apply
    // ===============================================================

    [Fact]
    public void PatternMatchThenTemplateApply()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.Write({expr})",
                $"Console.WriteLine({expr})")),
            CSharp(
                "class C { void M() { Console.Write(42); } }",
                "class C { void M() { Console.WriteLine(42); } }"
            )
        );
    }

    [Fact]
    public void NoChangeWhenPatternDoesNotMatch()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.Write({expr})",
                $"Console.WriteLine({expr})")),
            CSharp(
                "class C { void M() { Console.WriteLine(42); } }"
            )
        );
    }

    [Fact]
    public void ReplacesMultipleOccurrences()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.Write({expr})",
                $"Console.WriteLine({expr})")),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Write(1);
                        Console.WriteLine(2);
                        Console.Write(3);
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(1);
                        Console.WriteLine(2);
                        Console.WriteLine(3);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TemplateWithoutCapturesProducesFixedAst()
    {
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                "Console.Write(42)",
                "Console.WriteLine(\"hello\")")),
            CSharp(
                "class C { void M() { Console.Write(42); } }",
                "class C { void M() { Console.WriteLine(\"hello\"); } }"
            )
        );
    }

    [Fact]
    public void TemplateWithRawCodeSplice()
    {
        var expr = Capture.Of<Expression>("expr");
        var methodName = "Info";
        RewriteRun(
            spec => spec.SetRecipe(new RawSpliceRecipe(expr, methodName)),
            CSharp(
                "class C { void M() { logger.Debug(\"msg\"); } }",
                "class C { void M() { logger.Info(\"msg\"); } }"
            )
        );
    }

    /// <summary>
    /// Substituting an IsPattern (e.g., x is null) into an expression-position capture
    /// must preserve its Pattern field (JLeftPadded&lt;Pattern&gt;). A second recipe that
    /// pattern-matches the resulting IsPattern should not crash.
    /// </summary>
    [Fact]
    public void SubstitutingIsPatternPreservesPatternField()
    {
        var left = Capture.Expression("left");
        var right = Capture.Expression("right");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Binary>(
                $"{left} && {right}",
                $"{left} || {right}")),
            CSharp(
                "class C { void M(object x, bool b) { var r = b && x is null; } }",
                "class C { void M(object x, bool b) { var r = b || x is null; } }"
            )
        );
    }

    /// <summary>
    /// A composite recipe where recipe 1 captures an IsPattern and substitutes it
    /// into a new binary, and recipe 2 pattern-matches the resulting IsPattern.
    /// This verifies the Pattern field survives multi-recipe pipelines.
    /// </summary>
    [Fact]
    public void IsPatternSurvivesCompositeRecipePipeline()
    {
        RewriteRun(
            spec => spec.SetRecipe(new IsPatternCompositeRecipe()),
            CSharp(
                """
                class C
                {
                    void M(object x, bool b)
                    {
                        var r = b && x is null;
                    }
                }
                """,
                """
                class C
                {
                    void M(object x, bool b)
                    {
                        var r = b || x == null;
                    }
                }
                """
            )
        );
    }

    /// <summary>
    /// Directly verifies that template substitution preserves the
    /// <see cref="IsPattern.Pattern"/> field when an IsPattern is substituted into
    /// an expression capture position.
    /// </summary>
    [Fact]
    public void ApplySubstitutionsPreservesIsPatternFields()
    {
        // Parse "x is null" to get an IsPattern node
        var parser = new CSharpParser();
        var cu = parser.Parse("class C { void M(object x) { var r = x is null; } }");
        var isPattern = FindFirst<IsPattern>(cu)!;
        Assert.NotNull(isPattern);
        Assert.NotNull(isPattern.Pattern);

        // Parse a "b" identifier to use as the left operand
        var cu2 = parser.Parse("class C { void M(bool b) { var r = b; } }");
        var bIdent = FindFirst<Identifier>(cu2)!;

        // Build template and substitute
        var left = Capture.Expression("left");
        var right = Capture.Expression("right");
        var tmpl = CSharpTemplate.Expression($"{left} && {right}");

        var values = new MatchResult(
            new Dictionary<string, object>
            {
                ["left"] = bIdent,
                ["right"] = isPattern,
            }, null);

        var cursor = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), cu);
        var result = tmpl.Apply(cursor, values: values);

        Assert.NotNull(result);
        Assert.IsType<Binary>(result);
        var binary = (Binary)result;
        Assert.IsType<IsPattern>(binary.Right);
        var resultIsPattern = (IsPattern)binary.Right;

        // This is the key assertion: Pattern must NOT be null
        Assert.NotNull(resultIsPattern.Pattern);
        Assert.IsType<ConstantPattern>(resultIsPattern.Pattern.Element);
    }

    /// <summary>
    /// Cross-type matching: a pattern like <c>x == null</c> (Binary) should match
    /// a candidate <c>x is null</c> (IsPattern) via PatternMatchingComparator.
    /// This exercises the MatchBinaryPatternToIsNullCandidate code path.
    /// </summary>
    [Fact]
    public void CrossTypeMatchIsPatternToBinaryEqNull()
    {
        var x = Capture.Expression("x");
        RewriteRun(
            spec => spec.SetRecipe(new RewriteVisitorRecipe(
                CSharpTemplate.Rewrite(
                    CSharpPattern.Expression($"{x} == null"),
                    CSharpTemplate.Expression($"{x} == null")))),
            CSharp(
                "class C { void M(object o) { var r = o is null; } }",
                "class C { void M(object o) { var r = o == null; } }"
            )
        );
    }

    /// <summary>
    /// The parser reads <c>x is Foo</c> as a type test and <c>x is "abc"</c> as a constant pattern,
    /// so a capture after <c>is</c> builds whichever the printed text parses to.
    /// </summary>
    [Theory]
    [InlineData("\"abc\"")]
    [InlineData("-1")]
    [InlineData("null")]
    [InlineData("(1)")]
    [InlineData("int.MaxValue")]
    [InlineData("Foo")]
    [InlineData("Foo.Bar")]
    public void CaptureAfterIsMatchesFreshParse(string captured)
    {
        var expr = Capture.Expression("expr");
        var value = Capture.Expression("value");
        var tmpl = CSharpTemplate.Expression($"{expr} is {value}");
        var values = new MatchResult(
            new Dictionary<string, object>
            {
                ["expr"] = ParseExpression("x"),
                ["value"] = ParseExpression(captured),
            }, null);

        var result = tmpl.Apply(new Cursor(null, Cursor.ROOT_VALUE), values: values)!;

        var printed = new CSharpPrinter<int>().Print(result);
        Assert.Equal($"x is {captured}", printed);
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(result, ParseExpression(printed)));
    }

    /// <summary>
    /// A type test binds as a relational operator, and a pattern takes a shift expression.
    /// </summary>
    [Theory]
    [InlineData("", " is Foo", "a == b", "(a == b) is Foo")]
    [InlineData("", " is Foo", "a ?? b", "(a ?? b) is Foo")]
    [InlineData("", " is Foo", "c ? a : b", "(c ? a : b) is Foo")]
    [InlineData("", " is Foo", "c = a", "(c = a) is Foo")]
    [InlineData("", " is Foo", "a + b", "a + b is Foo")]
    [InlineData("", " is Foo", "a < b", "a < b is Foo")]
    [InlineData("", " is Foo", "a as string", "a as string is Foo")]
    [InlineData("", " is Foo", "x is Bar", "x is Bar is Foo")]
    [InlineData("x is ", "", "a == b", "x is (a == b)")]
    [InlineData("x is ", "", "a & b", "x is (a & b)")]
    [InlineData("x is ", "", "c ? a : b", "x is (c ? a : b)")]
    [InlineData("x is ", "", "a << b", "x is a << b")]
    [InlineData("x is ", "", "a + b", "x is a + b")]
    [InlineData("x is < ", "", "a == b", "x is < (a == b)")]
    [InlineData("x is < ", "", "a + b", "x is < a + b")]
    [InlineData("!", "", "x is Foo", "!(x is Foo)")]
    [InlineData("", " == y", "x is Foo", "x is Foo == y")]
    [InlineData("", ".ToString()", "x is Foo", "(x is Foo).ToString()")]
    public void CaptureInTypeTestMatchesFreshParse(string before, string after, string captured, string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{after}"), captured, expected);
    }

    /// <summary>
    /// Each application builds nodes of its own, as Java's templates do, so two replacements share no id.
    /// </summary>
    [Fact]
    public void EachApplicationHasItsOwnIds()
    {
        var a = Capture.Expression("a");
        var tmpl = CSharpTemplate.Expression($"Console.WriteLine({a})");
        var values = new MatchResult(new Dictionary<string, object> { ["a"] = ParseExpression("1") }, null);

        var first = tmpl.Apply(new Cursor(null, Cursor.ROOT_VALUE), values: values)!;
        var second = tmpl.Apply(new Cursor(null, Cursor.ROOT_VALUE), values: values)!;

        Assert.Empty(IdsOf(first).Intersect(IdsOf(second)));
    }

    /// <summary>
    /// A capture keeps its id where it moves from the replaced code, and only once.
    /// </summary>
    [Fact]
    public void CaptureKeepsItsIdOnceWhenItComesFromTheReplacedCode()
    {
        var a = Capture.Expression("a");
        var cu = Parse("class C { void M(int x) { Console.Write(x); } }");
        var write = FindFirst<MethodInvocation>(cu)!;
        var x = write.Arguments.Elements[0].Element;
        var match = CSharpPattern.Expression($"Console.Write({a})")
            .Match(write, new Cursor(new Cursor(null, Cursor.ROOT_VALUE), cu))!;

        var result = CSharpTemplate.Expression($"Math.Max({a}, {a})")
            .Apply(new Cursor(new Cursor(new Cursor(null, Cursor.ROOT_VALUE), cu), write), values: match)!;

        var arguments = ((MethodInvocation)result).Arguments.Elements;
        Assert.Equal(x.Id, arguments[0].Element.Id);
        Assert.NotEqual(x.Id, arguments[1].Element.Id);
        Assert.Equal(IdsOf(result).Count, IdsOf(result).Distinct().Count());
    }

    /// <summary>
    /// Only the code the coordinates replace leaves the tree, so a capture from code that stays gets an id of its own.
    /// </summary>
    [Fact]
    public void CaptureKeepsItsIdOnlyWhereTheCoordinatesReplaceItsCode()
    {
        var a = Capture.Expression("a");
        var cu = Parse("class C { void M(int x) { Console.Write(x); Console.Write(1); } }");
        var writes = FindFirst<MethodDeclaration>(cu)!.Body!.Statements
            .Select(J (s) => s.Element is ExpressionStatement es ? es.Expression : s.Element)
            .Cast<MethodInvocation>().ToList();
        var x = writes[0].Arguments.Elements[0].Element;
        var cursor = new Cursor(new Cursor(new Cursor(null, Cursor.ROOT_VALUE), cu), writes[0]);
        var match = CSharpPattern.Expression($"Console.Write({a})").Match(writes[0], cursor.Parent!)!;
        var tmpl = CSharpTemplate.Expression($"Console.WriteLine({a})");

        MethodInvocation ApplyAt(CSharpCoordinates coordinates) =>
            (MethodInvocation)tmpl.Apply(cursor, values: match, coordinates: coordinates)!;

        Assert.Equal(x.Id, ApplyAt(CSharpCoordinates.Replace(writes[0])).Arguments.Elements[0].Element.Id);
        Assert.NotEqual(x.Id, ApplyAt(CSharpCoordinates.Replace(writes[1])).Arguments.Elements[0].Element.Id);
        Assert.NotEqual(x.Id, ApplyAt(CSharpCoordinates.Before(writes[0])).Arguments.Elements[0].Element.Id);
    }

    /// <summary>
    /// The parenthesizer finds a capture's slot by its id, so each copy of a capture spliced into two slots is
    /// parenthesized for its own.
    /// </summary>
    [Theory]
    [InlineData("", " ?? ", "", "b ?? c", "(b ?? c) ?? b ?? c")]
    [InlineData("", " - ", "", "x - y", "x - y - (x - y)")]
    [InlineData("", " + ", "", "x - y", "x - y + (x - y)")]
    [InlineData("", " ? ", " : z", "c ? d : e", "(c ? d : e) ? c ? d : e : z")]
    public void CaptureInTwoSlotsMatchesFreshParse(string before, string middle, string after, string captured,
        string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{middle}{a}{after}"), captured, expected);
    }

    [Fact]
    public void RecipeBuiltNullCoalescingInTwoSlotsKeepsItsParentheses()
    {
        var a = Capture.Expression("a");
        var coalescing = new CsBinary(Guid.NewGuid(), Space.Empty, Markers.Empty,
            new Identifier(Guid.NewGuid(), Space.Empty, Markers.Empty, [], "b", null, null),
            new JLeftPadded<CsBinary.OperatorType>(Space.SingleSpace, CsBinary.OperatorType.NullCoalescing),
            new Identifier(Guid.NewGuid(), Space.SingleSpace, Markers.Empty, [], "c", null, null), null);
        var values = new MatchResult(new Dictionary<string, object> { ["a"] = coalescing }, null);

        var result = CSharpTemplate.Expression($"{a} ?? {a}")
            .Apply(new Cursor(null, Cursor.ROOT_VALUE), values: values)!;

        Assert.Equal("(b ?? c) ?? b ?? c", new CSharpPrinter<int>().Print(result));
    }

    private static List<Guid> IdsOf(J tree)
    {
        var ids = new List<Guid>();
        new IdCollector(ids).Visit(tree, 0);
        return ids;
    }

    private class IdCollector(List<Guid> ids) : CSharpVisitor<int>
    {
        public override J? PostVisit(J tree, int p)
        {
            ids.Add(tree.Id);
            return tree;
        }
    }

    [Fact]
    public void ReplacesBinaryExpression()
    {
        var lhs = Capture.Of<Expression>("lhs");
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Binary>(
                $"{lhs} + {rhs}",
                $"{lhs} - {rhs}")),
            CSharp(
                "class C { void M() { var x = 1 + 2; } }",
                "class C { void M() { var x = 1 - 2; } }"
            )
        );
    }

    [Fact]
    public void ReplacesThrowStatement()
    {
        var msg = Capture.Of<Expression>("msg");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Throw>(
                $"throw new Exception({msg})",
                $"throw new ArgumentException({msg})")),
            CSharp(
                "class C { void M() { throw new Exception(\"oops\"); } }",
                "class C { void M() { throw new ArgumentException(\"oops\"); } }"
            )
        );
    }

    [Fact]
    public void ReplacesNewClassExpression()
    {
        var arg = Capture.Of<Expression>("arg");
        RewriteRun(
            spec => spec.SetRecipe(Replace<NewClass>(
                $"new List({arg})",
                $"new ArrayList({arg})")),
            CSharp(
                "class C { void M() { var x = new List(10); } }",
                "class C { void M() { var x = new ArrayList(10); } }"
            )
        );
    }

    [Fact]
    public void PreservesExpressionCapture()
    {
        var expr = Capture.Of<Expression>("expr");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.Write({expr})",
                $"Console.WriteLine({expr})")),
            CSharp(
                "class C { void M() { Console.Write(1 + 2); } }",
                "class C { void M() { Console.WriteLine(1 + 2); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesMethodNameCapture()
    {
        var method = Capture.Of<Identifier>("method");
        var args = Capture.Of<Expression>("args");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Console.{method}({args})",
                $"Trace.{method}({args})")),
            CSharp(
                "class C { void M() { Console.Write(42); } }",
                "class C { void M() { Trace.Write(42); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesVariadicArgs()
    {
        var args = Capture.Expression("args", variadic: new());
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Foo({args})",
                $"Bar({args})")),
            CSharp(
                "class C { void M() { Foo(1, 2, 3); } }",
                "class C { void M() { Bar(1, 2, 3); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesMethodNameAndVariadicArgs()
    {
        var method = Capture.Of<Identifier>("method");
        var args = Capture.Expression("args", variadic: new());
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"new Random().{method}({args})",
                $"Random.Shared.{method}({args})")),
            CSharp(
                "class C { void M() { new Random().Next(10); } }",
                "class C { void M() { Random.Shared.Next(10); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesMethodNameAndVariadicArgsWithZeroArgs()
    {
        var method = Capture.Of<Identifier>("method");
        var args = Capture.Expression("args", variadic: new());
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"new Random().{method}({args})",
                $"Random.Shared.{method}({args})")),
            CSharp(
                "class C { void M() { new Random().NextDouble(); } }",
                "class C { void M() { Random.Shared.NextDouble(); } }"
            )
        );
    }

    [Fact]
    public void SubstitutesVariadicArgsInNonTrailingPosition()
    {
        var args = Capture.Expression("args", variadic: new());
        var last = Capture.Of<Expression>("last");
        RewriteRun(
            spec => spec.SetRecipe(Replace<MethodInvocation>(
                $"Foo({args}, {last})",
                $"Bar({last}, {args})")),
            CSharp(
                "class C { void M() { Foo(1, 2, 3); } }",
                "class C { void M() { Bar(3, 1, 2); } }"
            )
        );
    }

    // ===============================================================
    // Attribute variadic captures
    // ===============================================================

    [Fact]
    public void SubstitutesVariadicArgsInAttribute()
    {
        var args = Capture.Expression("args", variadic: new());
        RewriteRun(
            spec => spec.SetRecipe(ReplaceAnnotation(
                $"Fact({args})",
                $"Test({args})")),
            CSharp(
                """
                class C { [Fact(DisplayName = "test")] void M() {} }
                """,
                """
                class C { [Test(DisplayName = "test")] void M() {} }
                """
            )
        );
    }

    [Fact]
    public void SubstitutesEmptyVariadicArgsInAttribute()
    {
        var args = Capture.Expression("args", variadic: new());
        RewriteRun(
            spec => spec.SetRecipe(ReplaceAnnotation(
                $"Fact({args})",
                $"Test({args})")),
            CSharp(
                """
                class C { [Fact] void M() {} }
                """,
                """
                class C { [Test] void M() {} }
                """
            )
        );
    }

    [Fact]
    public void AttributeRenameViaRewriteRule()
    {
        var args = Capture.Expression("args", variadic: new());
        var visitor = CSharpTemplate.Rewrite(
            CSharpPattern.Attribute($"Fact({args})"),
            CSharpTemplate.Attribute($"Test({args})"));
        RewriteRun(
            spec => spec.SetRecipe(new RewriteVisitorRecipe(visitor)),
            CSharp(
                """
                class C
                {
                    [Fact] void M1() {}
                    [Fact(DisplayName = "test")] void M2() {}
                }
                """,
                """
                class C
                {
                    [Test] void M1() {}
                    [Test(DisplayName = "test")] void M2() {}
                }
                """
            )
        );
    }

    [Fact]
    public void SubstitutesFieldNameCapture()
    {
        var field = Capture.Of<Identifier>("field");
        RewriteRun(
            spec => spec.SetRecipe(Replace<FieldAccess>(
                $"DateTime.{field}",
                $"DateTimeOffset.{field}")),
            CSharp(
                "class C { void M() { var x = DateTime.Now; } }",
                "class C { void M() { var x = DateTimeOffset.Now; } }"
            )
        );
    }

    [Fact]
    public void ReplacesAssignmentWithCompoundAssignment()
    {
        var x = Capture.Of<Expression>("x");
        var y = Capture.Of<Expression>("y");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Assignment>(
                $"{x} = {x} + {y}",
                $"{x} += {y}")),
            CSharp(
                "class C { int x; void M() { x = x + 2; } }",
                "class C { int x; void M() { x += 2; } }"
            )
        );
    }

    [Fact]
    public void RemovesStatementByReturningNull()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveEmptyStatementRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        ;
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ReplacesThrowExWithBareThrow()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UseRethrowRecipe()),
            CSharp(
                """
                using System;
                class C
                {
                    void M()
                    {
                        try { } catch (Exception ex) { throw ex; }
                    }
                }
                """,
                """
                using System;
                class C
                {
                    void M()
                    {
                        try { } catch (Exception ex) { throw; }
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // Auto-parenthesization
    // ===============================================================

    [Fact]
    public void AutoParenthesizesWhenSubstitutionNeedsParens()
    {
        // Replace a + b with a - b; when the result sits inside x * _, it needs parens
        var lhs = Capture.Of<Expression>("lhs");
        var rhs = Capture.Of<Expression>("rhs");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Binary>(
                $"{lhs} + {rhs}",
                $"{lhs} - {rhs}")),
            CSharp(
                "class C { void M() { var x = 2 * (1 + 3); } }",
                "class C { void M() { var x = 2 * (1 - 3); } }"
            )
        );
    }

    [Fact]
    public void AutoParenthesizesCaptureInsideHigherPrecedenceBinary()
    {
        // Pattern: {left} - {right} captures left = 1 + 2
        // Template: {left} * {right} — should produce (1 + 2) * 3, not 1 + 2 * 3
        var left = Capture.Of<Expression>("left");
        var right = Capture.Of<Expression>("right");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Binary>(
                $"{left} - {right}",
                $"{left} * {right}")),
            CSharp(
                "class C { int M() { return 1 + 2 - 3; } }",
                "class C { int M() { return (1 + 2) * 3; } }"
            )
        );
    }

    [Fact]
    public void AutoParenthesizesCaptureOrInsideAnd()
    {
        // {left} || {right} where left = true, right = false — replace with &&
        // Use a custom recipe to only match the outermost || in: true || false || true
        // left = Binary(||, true, false), right = true
        // Template {left} && {right} should produce (true || false) && true
        RewriteRun(
            spec => spec.SetRecipe(new ReplaceOutermostOrWithAndRecipe()).SetCycles(1),
            CSharp(
                "class C { void M() { var x = true || false || true; } }",
                "class C { void M() { var x = (true || false) && true; } }"
            )
        );
    }

    [Fact]
    public void AutoParenthesizesCaptureOnRightOfSameOperator()
    {
        // f || g || h would parse as (f || g) || h
        var cond = Capture.Expression("cond");
        var other = Capture.Expression("other");
        RewriteRun(
            spec => spec.SetRecipe(new RewriteVisitorRecipe(CSharpTemplate.Rewrite(
                CSharpPattern.Expression($"{cond} ? true : {other}"),
                CSharpTemplate.Expression($"{cond} || {other}")))),
            CSharp(
                "class C { bool M(bool f, bool g, bool h) { return f ? true : g || h; } }",
                "class C { bool M(bool f, bool g, bool h) { return f || (g || h); } }"
            )
        );
    }

    [Fact]
    public void CaptureOnRightOfSameOperatorMatchesFreshParse()
    {
        // "x" + 1 + 2 is "x12", where "x" + (1 + 2) is "x3"
        var right = Capture.Expression("right");
        var tmpl = CSharpTemplate.Expression($"\"x\" + {right}");
        var values = new MatchResult(
            new Dictionary<string, object> { ["right"] = ParseExpression("1 + 2") }, null);

        var result = tmpl.Apply(new Cursor(null, Cursor.ROOT_VALUE), values: values)!;

        var printed = new CSharpPrinter<int>().Print(result);
        Assert.Equal("\"x\" + (1 + 2)", printed);
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(result, ParseExpression(printed)));
    }

    /// <summary>
    /// <c>??</c> groups to the right and a conditional's branches take any expression, so another conditional or
    /// <c>??</c> needs parentheses only on the left of <c>??</c> or as the condition of a conditional.
    /// </summary>
    [Theory]
    [InlineData("c ?? ", "", "a ?? b", "c ?? a ?? b")]
    [InlineData("", " ?? c", "a ?? b", "(a ?? b) ?? c")]
    [InlineData("c ?? ", "", "x ? y : z", "c ?? (x ? y : z)")]
    [InlineData("x ? y : ", "", "a ?? b", "x ? y : a ?? b")]
    [InlineData("x ? ", " : z", "a ?? b", "x ? a ?? b : z")]
    [InlineData("", " ? y : z", "a ?? b", "a ?? b ? y : z")]
    [InlineData("x ? y : ", "", "c ? d : e", "x ? y : c ? d : e")]
    [InlineData("x ? ", " : z", "c ? d : e", "x ? c ? d : e : z")]
    [InlineData("", " ? y : z", "c ? d : e", "(c ? d : e) ? y : z")]
    [InlineData("Console.WriteLine(", ")", "a ?? b ?? c", "Console.WriteLine(a ?? b ?? c)")]
    [InlineData("Console.WriteLine(", ")", "c ? a : b ? d : e", "Console.WriteLine(c ? a : b ? d : e)")]
    public void CaptureUnderConditionalOrNullCoalescingMatchesFreshParse(string before, string after,
        string captured, string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{after}"), captured, expected);
    }

    /// <summary>
    /// An assignment, a lambda or a query runs as far right as it can, which in a conditional's branch is the end of
    /// the branch, so only the condition and the operands of <c>??</c> need parentheses around one.
    /// </summary>
    [Theory]
    [InlineData("x ? y : ", "", "b = 1", "x ? y : b = 1")]
    [InlineData("x ? ", " : z", "b += 1", "x ? b += 1 : z")]
    [InlineData("x ? null : ", "", "() => b", "x ? null : () => b")]
    [InlineData("x ? ", " : null", "b => b", "x ? b => b : null")]
    [InlineData("x ? ", " : null", "b => c ? d : e", "x ? b => c ? d : e : null")]
    [InlineData("x ? ", " : ys", "from q in xs select q", "x ? from q in xs select q : ys")]
    [InlineData("", " ? y : z", "b = c", "(b = c) ? y : z")]
    [InlineData("c ?? ", "", "b = 1", "c ?? (b = 1)")]
    [InlineData("", " ?? c", "b = 1", "(b = 1) ?? c")]
    public void CaptureInBranchOfConditionalMatchesFreshParse(string before, string after, string captured,
        string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{after}"), captured, expected);
    }

    /// <summary>
    /// The format of an interpolation starts at its first <c>:</c> that no parentheses enclose.
    /// </summary>
    [Theory]
    [InlineData("$\"{", "}\"", "c ? d : e", "$\"{(c ? d : e)}\"")]
    [InlineData("$\"{", ":F2}\"", "c ? d : e", "$\"{(c ? d : e):F2}\"")]
    [InlineData("$\"{x,", "}\"", "c ? 1 : 2", "$\"{x,(c ? 1 : 2)}\"")]
    [InlineData("$\"{", "}\"", "a ?? b", "$\"{a ?? b}\"")]
    public void CaptureInInterpolationMatchesFreshParse(string before, string after, string captured, string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{after}"), captured, expected);
    }

    /// <summary>
    /// A sign printed right before the same sign lexes as <c>--</c> or <c>++</c>, a decrement or increment.
    /// </summary>
    [Theory]
    [InlineData("-", "-b", "-(-b)")]
    [InlineData("-", "--b", "-(--b)")]
    [InlineData("-", "-1", "-(-1)")]
    [InlineData("+", "+b", "+(+b)")]
    [InlineData("+", "++b", "+(++b)")]
    [InlineData("-", "+b", "-+b")]
    [InlineData("-", "b--", "-b--")]
    [InlineData("- ", "-b", "- -b")]
    [InlineData("x-", "-b", "x-(-b)")]
    [InlineData("x-", "-b * c", "x-(-b * c)")]
    [InlineData("x+", "++b", "x+(++b)")]
    [InlineData("x - ", "-b", "x - -b")]
    public void CaptureAfterSignMatchesFreshParse(string before, string captured, string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}"), captured, expected);
    }

    /// <summary>
    /// A conditional access keeps the members after a <c>!</c> in its chain, which parentheses would take out of it.
    /// </summary>
    [Theory]
    [InlineData("x?.ToString()!.Length")]
    [InlineData("x?.ToString()![0]")]
    [InlineData("x?.ToString()!.Trim()")]
    public void CaptureOfConditionalAccessWithNullForgivingKeepsItsChain(string captured)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"Console.WriteLine({a})"), captured,
            $"Console.WriteLine({captured})");
    }

    /// <summary>
    /// A postfix <c>!</c> takes a primary operand, and a member access after a conditional access that ends in one
    /// would join the conditional access.
    /// </summary>
    [Theory]
    [InlineData("", "!", "b ?? c", "(b ?? c)!")]
    [InlineData("", "!.ToString()", "b ?? c", "(b ?? c)!.ToString()")]
    [InlineData("", "!", "-b", "(-b)!")]
    [InlineData("", "!", "(int)b", "((int)b)!")]
    [InlineData("", "!", "b as string", "(b as string)!")]
    [InlineData("", "!", "b++", "b++!")]
    [InlineData("", "!", "x?.y", "x?.y!")]
    [InlineData("", "!", "x?.y!", "x?.y!!")]
    [InlineData("", ".ToString()", "x?.ToString()!", "(x?.ToString()!).ToString()")]
    [InlineData("", "[0]", "x?.y!", "(x?.y!)[0]")]
    [InlineData("", ".Length", "x?.y!!", "(x?.y!!).Length")]
    [InlineData("", ".Length", "x?[0].y!", "(x?[0].y!).Length")]
    [InlineData("", ".Length", "x!", "x!.Length")]
    [InlineData("", ".Length", "x?.y!.z", "x?.y!.z.Length")]
    [InlineData("(object)", "", "x?.y!", "(object)x?.y!")]
    public void CaptureAsOperandOfNullForgivingMatchesFreshParse(string before, string after, string captured,
        string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{after}"), captured, expected);
    }

    /// <summary>
    /// A cast takes a unary operand, and a member access or element access a primary one.
    /// </summary>
    [Theory]
    [InlineData("(object)", "", "b + c", "(object)(b + c)")]
    [InlineData("(object)", "", "b ? c : d", "(object)(b ? c : d)")]
    [InlineData("(object)", "", "b..c", "(object)(b..c)")]
    [InlineData("(object)", "", "-b", "(object)-b")]
    [InlineData("(object)", "", "(int)b", "(object)(int)b")]
    [InlineData("(object)", "", "b++", "(object)b++")]
    [InlineData("(int)", "", "-b", "(int)-b")]
    [InlineData("(Foo)", "", "-b", "(Foo)(-b)")]
    [InlineData("(Foo.Bar)", "", "-1", "(Foo.Bar)(-1)")]
    [InlineData("(Foo)", "", "!b", "(Foo)!b")]
    [InlineData("", ".ToString()", "b + c", "(b + c).ToString()")]
    [InlineData("", ".ToString()", "-b", "(-b).ToString()")]
    [InlineData("", ".ToString()", "(int)b", "((int)b).ToString()")]
    [InlineData("", ".ToString()", "b ?? c", "(b ?? c).ToString()")]
    [InlineData("", ".ToString()", "b++", "b++.ToString()")]
    [InlineData("", ".ToString()", "b.c[0]", "b.c[0].ToString()")]
    [InlineData("", ".Length", "b as string", "(b as string).Length")]
    [InlineData("", ".Count()", "from x in xs select x", "(from x in xs select x).Count()")]
    [InlineData("", ".Result", "await t", "(await t).Result")]
    [InlineData("(object)", "", "await t", "(object)await t")]
    [InlineData("", "[0]", "b ? c : d", "(b ? c : d)[0]")]
    public void CaptureAsOperandOfCastOrMemberAccessMatchesFreshParse(string before, string after, string captured,
        string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{after}"), captured, expected);
    }

    /// <summary>
    /// <c>await</c> and a range take a unary operand, a switch expression a range, and a with expression a switch
    /// expression.
    /// </summary>
    [Theory]
    [InlineData("", "..y", "x + 1", "(x + 1)..y")]
    [InlineData("x..", "", "y - 1", "x..(y - 1)")]
    [InlineData("x..", "", "^1", "x..^1")]
    [InlineData("", " switch { _ => 0 }", "x + 1", "(x + 1) switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x..y", "x..y switch { _ => 0 }")]
    [InlineData("", " with { P = 1 }", "x ?? y", "(x ?? y) with { P = 1 }")]
    [InlineData("", " switch { _ => 0 }", "x as Foo", "x as Foo switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is Foo", "(x is Foo) switch { _ => 0 }")]
    [InlineData("", " with { P = 1 }", "x as R", "x as R with { P = 1 }")]
    [InlineData("", "..y", "x as Foo", "(x as Foo)..y")]
    public void CaptureAsOperandOfAwaitRangeSwitchOrWithMatchesFreshParse(string before, string after,
        string captured, string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{after}"), captured, expected);
    }

    /// <summary>
    /// A switch after a type test or pattern applies to all of it, and a range after <c>x as T</c> starts with all of
    /// it, unless the test ends in an expression that the switch or the <c>..</c> would continue.
    /// </summary>
    [Theory]
    [InlineData("", " switch { _ => 0 }", "x is string", "x is string switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is int?", "x is int? switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is List<int>", "x is List<int> switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is int i", "x is int i switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is Foo f", "x is Foo f switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is { Length: 1 }", "x is { Length: 1 } switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is [1, 2]", "x is [1, 2] switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is int or string", "x is int or string switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is not string", "x is not string switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is Foo.Bar", "(x is Foo.Bar) switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is null", "(x is null) switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is > 1", "(x is > 1) switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is string or null", "(x is string or null) switch { _ => 0 }")]
    [InlineData("", " switch { _ => 0 }", "x is not Foo", "(x is not Foo) switch { _ => 0 }")]
    [InlineData("", "..y", "x as string", "x as string..y")]
    [InlineData("", "..y", "x as int[]", "x as int[]..y")]
    [InlineData("", "..y", "x as List<int>", "(x as List<int>)..y")]
    [InlineData("", "..y", "x as int?", "(x as int?)..y")]
    [InlineData("y..", "", "x as string", "y..(x as string)")]
    [InlineData("Console.WriteLine(", ")", "x is string switch { true => 0, _ => 1 }",
        "Console.WriteLine(x is string switch { true => 0, _ => 1 })")]
    [InlineData("Console.WriteLine(", ")", "x is int i switch { true => i, _ => 1 }",
        "Console.WriteLine(x is int i switch { true => i, _ => 1 })")]
    [InlineData("Console.WriteLine(", ")", "x as string..", "Console.WriteLine(x as string..)")]
    public void CaptureEndingInTypeOrPatternBeforeSwitchOrRangeMatchesFreshParse(string before, string after,
        string captured, string expected)
    {
        var a = Capture.Expression("a");
        AssertAppliesAsParsed(CSharpTemplate.Expression($"{before}{a}{after}"), captured, expected);
    }

    [Theory]
    [InlineData("b ?? c", "await (b ?? c)")]
    [InlineData("(Task)b", "await (Task)b")]
    [InlineData("b.Result", "await b.Result")]
    public void CaptureAsOperandOfAwaitMatchesFreshParse(string captured, string expected)
    {
        var a = Capture.Expression("a");
        var values = new MatchResult(new Dictionary<string, object> { ["a"] = ParseExpression(captured) }, null);

        var result = CSharpTemplate.Expression($"await {a}")
            .Apply(new Cursor(null, Cursor.ROOT_VALUE), values: values)!;

        var printed = new CSharpPrinter<int>().Print(result);
        Assert.Equal(expected, printed);
        // In a method that isn't async, await (b ?? c) would call a method named await
        var reparsed = FindFirst<NamedVariable>(Parse($"class C {{ async void M() {{ var v = {printed}; }} }}"))!;
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(result, reparsed.Initializer!.Element));
    }

    [Fact]
    public void ReplacementAsReceiverKeepsItsParentheses()
    {
        var a = Capture.Expression("a");
        RewriteRun(
            spec => spec.SetRecipe(new RewriteVisitorRecipe(CSharpTemplate.Rewrite(
                CSharpPattern.Expression($"Twice({a})"),
                CSharpTemplate.Expression($"{a} + {a}")))),
            CSharp(
                "class C { int Twice(int i) => i + i; string M(int b) { return Twice(b).ToString(); } }",
                "class C { int Twice(int i) => i + i; string M(int b) { return (b + b).ToString(); } }"
            )
        );
    }

    [Fact]
    public void ReplacementEndingConditionalAccessAsReceiverKeepsItsParentheses()
    {
        var a = Capture.Expression("a");
        RewriteRun(
            spec => spec.SetRecipe(new RewriteVisitorRecipe(CSharpTemplate.Rewrite(
                CSharpPattern.Expression($"NotNull({a})"),
                CSharpTemplate.Expression($"{a}!")))),
            CSharp(
                "class C { T NotNull<T>(T t) => t; int M(string? s) { return NotNull(s?.Trim()).Length; } }",
                "class C { T NotNull<T>(T t) => t; int M(string? s) { return (s?.Trim()!).Length; } }"
            )
        );
    }

    [Fact]
    public void ConditionalReplacingTheExpressionOfAnInterpolationTakesParentheses()
    {
        var a = Capture.Expression("a");
        RewriteRun(
            spec => spec.SetRecipe(new RewriteVisitorRecipe(CSharpTemplate.Rewrite(
                CSharpPattern.Expression($"YesNo({a})"),
                CSharpTemplate.Expression($"{a} ? \"yes\" : \"no\"")))),
            CSharp(
                "class C { string YesNo(bool b) => \"\"; string M(bool b) => $\"{YesNo(b)}\"; }",
                "class C { string YesNo(bool b) => \"\"; string M(bool b) => $\"{(b ? \"yes\" : \"no\")}\"; }"
            )
        );
    }

    [Fact]
    public void ReplacementAfterSignKeepsItsSign()
    {
        var a = Capture.Expression("a");
        RewriteRun(
            spec => spec.SetRecipe(new RewriteVisitorRecipe(CSharpTemplate.Rewrite(
                CSharpPattern.Expression($"Negate({a})"),
                CSharpTemplate.Expression($"-{a}")))),
            CSharp(
                "class C { int Negate(int i) => -i; int M(int b) { return -Negate(b); } }",
                "class C { int Negate(int i) => -i; int M(int b) { return -(-b); } }"
            )
        );
    }

    [Fact]
    public void NoCaptureParenthesizationWhenUnnecessary()
    {
        // Pattern: {left} - {right} captures left = 2 * 3 (higher prec than -)
        // Template: {left} + {right} — should produce 2 * 3 + 4 (no parens needed)
        var left = Capture.Of<Expression>("left");
        var right = Capture.Of<Expression>("right");
        RewriteRun(
            spec => spec.SetRecipe(Replace<Binary>(
                $"{left} - {right}",
                $"{left} + {right}")),
            CSharp(
                "class C { void M() { var x = 2 * 3 - 4; } }",
                "class C { void M() { var x = 2 * 3 + 4; } }"
            )
        );
    }

    // ===============================================================
    // Auto-formatting
    // ===============================================================

    [Fact]
    public void AutoFormatsTemplateResultIndentation()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ReplaceWithIfBlockRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Write(42);
                    }
                }
                """,
                // Auto-format should fix the internal indentation of the if-block
                // to match the surrounding context (8 spaces for braces, 12 for body)
                """
                class C
                {
                    void M()
                    {
                        if (true)
                        {
                            Console.WriteLine(42);
                        }
                    }
                }
                """
            )
        );
    }

    /// <summary>
    /// A file that leaves the members of its namespace flush gets template results indented as it indents, rather than
    /// a level deeper for the namespace.
    /// </summary>
    [Fact]
    public void AutoFormatsTemplateResultIndentationInNamespaceWithFlushMembers()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ReplaceWithIfBlockRecipe()),
            CSharp(
                """
                namespace N
                {
                class C
                {
                    void M()
                    {
                        Console.Write(42);
                    }
                }
                }
                """,
                """
                namespace N
                {
                class C
                {
                    void M()
                    {
                        if (true)
                        {
                            Console.WriteLine(42);
                        }
                    }
                }
                }
                """
            )
        );
    }

    // ===============================================================
    // Recipe factories
    // ===============================================================

    private static Expression ParseExpression(string code) =>
        FindFirst<NamedVariable>(Parse($"class C {{ object v = {code}; }}"))!.Initializer!.Element;

    /// <summary>
    /// Applies <paramref name="tmpl"/> with its capture <c>a</c> bound to <paramref name="captured"/>, and checks
    /// that the result prints <paramref name="expected"/> and is the tree the parser builds for that text.
    /// </summary>
    private static void AssertAppliesAsParsed(CSharpTemplate tmpl, string captured, string expected)
    {
        var values = new MatchResult(new Dictionary<string, object> { ["a"] = ParseExpression(captured) }, null);

        var result = tmpl.Apply(new Cursor(null, Cursor.ROOT_VALUE), values: values)!;

        var printed = new CSharpPrinter<int>().Print(result);
        Assert.Equal(expected, printed);
        Assert.True(StructuralEqualityComparator.AreStructurallyEqual(result, ParseExpression(printed)));
    }

    private static OpenRewrite.Core.Recipe ReplaceAnnotation(TemplateStringHandler pattern, TemplateStringHandler template) =>
        new PatternReplaceRecipe<Annotation>(CSharpPattern.Attribute(pattern), CSharpTemplate.Attribute(template));

#pragma warning disable CS0618
    private static OpenRewrite.Core.Recipe Replace<T>(TemplateStringHandler pattern, TemplateStringHandler template)
        where T : J =>
        new PatternReplaceRecipe<T>(CSharpPattern.Create(pattern), CSharpTemplate.Create(template));

    private static OpenRewrite.Core.Recipe Replace<T>(string pattern, string template) where T : J =>
        new PatternReplaceRecipe<T>(CSharpPattern.Create(pattern), CSharpTemplate.Create(template));
#pragma warning restore CS0618
}

/// <summary>
/// Generic replacement recipe that matches nodes of type <typeparamref name="T"/>
/// against a pattern and replaces them using a template.
/// </summary>
file class PatternReplaceRecipe<T>(CSharpPattern pat, CSharpTemplate tmpl) : OpenRewrite.Core.Recipe where T : J
{
    public override string DisplayName => $"Replace {typeof(T).Name}";
    public override string Description => $"Replaces {typeof(T).Name} matching the pattern with the template.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new ReplaceVisitor(pat, tmpl);

    private class ReplaceVisitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
    {
        public override J? PreVisit(J tree, ExecutionContext ctx)
        {
            if (tree is T t && pat.Match(t, Cursor) is { } match)
            {
                return (J?)tmpl.Apply(Cursor, values: match);
            }
            return tree;
        }
    }
}

/// <summary>
/// Recipe demonstrating Raw.Code splice — replaces logger.Debug({expr}) with logger.{level}({expr}).
/// </summary>
file class RawSpliceRecipe(Capture<Expression> expr, string level) : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Replace logger method";
    public override string Description => "Replaces logger.Debug with logger.<level> using Raw.Code splice.";

    public override JavaVisitor<ExecutionContext> GetVisitor()
    {
        var pat = CSharpPattern.Expression($"logger.Debug({expr})");
        var tmpl = CSharpTemplate.Expression($"logger.{Raw.Code(level)}({expr})");
        return new ReplaceVisitor(pat, tmpl);
    }

    private class ReplaceVisitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            if (pat.Match(mi, Cursor) is { } match)
            {
                return (J)tmpl.Apply(Cursor, values: match)!;
            }
            return mi;
        }
    }
}

/// <summary>
/// Recipe that removes empty statements (standalone semicolons) from blocks.
/// Tests that VisitBlock properly handles null returns (statement deletion).
/// </summary>
file class RemoveEmptyStatementRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Remove empty statements";
    public override string Description => "Remove standalone semicolons.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitEmpty(Empty empty, ExecutionContext ctx)
        {
            if (Cursor.ParentTree.Value is Block)
                return null!;
            return empty;
        }
    }
}

/// <summary>
/// Replaces Console.Write(expr) statement with an if-block containing Console.WriteLine(42).
/// The template produces a single-line if statement that auto-format should expand to multi-line
/// with correct indentation.
/// </summary>
file class ReplaceWithIfBlockRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Replace Console.Write with if block";
    public override string Description => "Wraps Console.Write in an if block.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitExpressionStatement(ExpressionStatement es, ExecutionContext ctx)
        {
            es = (ExpressionStatement)base.VisitExpressionStatement(es, ctx);
            if (es.Expression is MethodInvocation mi &&
                mi.Select?.Element is Identifier { SimpleName: "Console" } &&
                mi.Name.SimpleName == "Write")
            {
                // Multi-line template with 0-based indentation.
                // Auto-format should fix internal indentation to match the target context.
                var tmpl = CSharpTemplate.Statement("if (true)\n{\n    Console.WriteLine(42);\n}");
                return AutoFormat((J)tmpl.Apply(Cursor)!, ctx, Cursor);
            }
            return es;
        }
    }
}

/// <summary>
/// Replaces only the outermost || binary with &&, avoiding double-matching of inner || nodes.
/// </summary>
file class ReplaceOutermostOrWithAndRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Replace outermost || with &&";
    public override string Description => "Replaces the outermost || with &&.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitBinary(Binary binary, ExecutionContext ctx)
        {
            binary = (Binary)base.VisitBinary(binary, ctx);
            if (binary.Operator.Element != Binary.OperatorType.Or)
                return binary;

            // Only match the outermost ||
            if (Cursor.ParentTree.Value is Binary parentBin &&
                parentBin.Operator.Element == Binary.OperatorType.Or)
                return binary;

            var left = Capture.Of<Expression>("left");
            var right = Capture.Of<Expression>("right");
            var pat = CSharpPattern.Expression($"{left} || {right}");
            var tmpl = CSharpTemplate.Expression($"{left} && {right}");

            if (pat.Match(binary, Cursor) is { } match)
            {
                return (J)tmpl.Apply(Cursor, values: match)!;
            }
            return binary;
        }
    }
}

file class UseRethrowRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Use rethrow";
    public override string Description => "Replace throw ex with throw.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitThrow(Throw throwStmt, ExecutionContext ctx)
        {
            throwStmt = (Throw)base.VisitThrow(throwStmt, ctx);

            if (throwStmt.Exception is not Identifier thrownId)
                return throwStmt;

            var cursor = Cursor;
            while (cursor.Parent != null)
            {
                cursor = cursor.Parent;
                if (cursor.Value is Try.Catch catchClause)
                {
                    var catchParam = catchClause.Parameter.Tree.Element;
                    if (catchParam is VariableDeclarations vd &&
                        vd.Variables.Count > 0 &&
                        vd.Variables[0].Element.Name.SimpleName == thrownId.SimpleName)
                    {
                        return throwStmt.WithException(
                            new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty));
                    }
                    break;
                }
            }

            return throwStmt;
        }
    }
}

file class RewriteVisitorRecipe(CSharpVisitor<ExecutionContext> visitor) : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Rewrite";
    public override string Description => "Applies a CSharpTemplate.Rewrite() visitor.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => visitor;
}

/// <summary>
/// Composite recipe: (1) swap && to || via capture/substitute, (2) convert 'is null' to '== null'.
/// Tests that IsPattern.Pattern survives template substitution across recipe boundaries.
/// </summary>
file class IsPatternCompositeRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "IsPattern composite";
    public override string Description => "Swap && to || then convert is-null to ==-null.";

    public override List<OpenRewrite.Core.Recipe> GetRecipeList()
    {
        var left = Capture.Expression("left");
        var right = Capture.Expression("right");
        var x = Capture.Expression("x");

        return
        [
            new RewriteVisitorRecipe(CSharpTemplate.Rewrite(
                CSharpPattern.Expression($"{left} && {right}"),
                CSharpTemplate.Expression($"{left} || {right}"))),
            new RewriteVisitorRecipe(CSharpTemplate.Rewrite(
                CSharpPattern.Expression($"{x} is null"),
                CSharpTemplate.Expression($"{x} == null"))),
        ];
    }
}
