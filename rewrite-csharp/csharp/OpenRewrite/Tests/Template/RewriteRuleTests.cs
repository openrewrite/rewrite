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

public class RewriteRuleTests : RewriteTest
{
    [Fact]
    public void SimpleBeforeAfterReplacement()
    {
        RewriteRun(
            spec => spec.SetRecipe(new SwapBinaryOperandsRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        var x = 1 + 2;
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        var x = 2 + 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void SimpleReplacementNoMatch()
    {
        RewriteRun(
            spec => spec.SetRecipe(new SwapBinaryOperandsRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        var x = 1 - 2;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleBeforesMatchesFirst()
    {
        RewriteRun(
            spec => spec.SetRecipe(new NormalizeConsoleOutputRecipe()),
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
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleBeforesMatchesSecond()
    {
        RewriteRun(
            spec => spec.SetRecipe(new NormalizeConsoleOutputRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Error.Write(42);
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AndThenAppliesBothRules()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MigrateAndRedirectRecipe()),
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
                """
                class C
                {
                    void M()
                    {
                        Console.Error.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void AndThenFirstDoesNotMatchSkipsSecond()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MigrateAndRedirectRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void OrElseUsesFirstWhenMatches()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MigrateWithFallbackRecipe()),
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
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void OrElseFallsBackToSecond()
    {
        RewriteRun(
            spec => spec.SetRecipe(new MigrateWithFallbackRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Error.Write(42);
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        Console.Error.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void PreMatchFiltersBeforePatternMatching()
    {
        RewriteRun(
            spec => spec.SetRecipe(new PreMatchFilteredRecipe()),
            CSharp(
                """
                class C
                {
                    void Target()
                    {
                        Console.Write(1);
                    }
                    void Other()
                    {
                        Console.Write(2);
                    }
                }
                """,
                """
                class C
                {
                    void Target()
                    {
                        Console.WriteLine(1);
                    }
                    void Other()
                    {
                        Console.Write(2);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CaptureConstraintFiltersOnCapturedValues()
    {
        RewriteRun(
            spec => spec.SetRecipe(new CaptureConstraintFilteredRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        var x = 1 + 2;
                        var y = 1 + 0;
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        var x = 1 + 2;
                        var y = 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void CapturesFlowFromPatternToTemplate()
    {
        RewriteRun(
            spec => spec.SetRecipe(new CaptureFlowRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Console.Write(1 + 2);
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(1 + 2);
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // CSharpTemplate.Rewrite — eliminates the need for a manual Visitor class
    // ===============================================================

    [Fact]
    public void RewriteAppliesRuleToBinaryNodes()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RewriteBinaryRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        var x = 1 + 2;
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        var x = 2 + 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void RewriteAppliesRuleToMethodInvocations()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RewriteMethodInvocationRecipe()),
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
                """
                class C
                {
                    void M()
                    {
                        Console.WriteLine(42);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FallbackWithManualVisitor()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FallbackWithManualVisitorRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        var a = x == null;
                        var b = y != null;
                        var c = z == 1;
                    }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        var a = x is null;
                        var b = y is not null;
                        var c = z == 1;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void RewriteNoMatchLeavesUnchanged()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RewriteBinaryRecipe()),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        var x = 1 - 2;
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // CSharpTemplate.Rewrite with typed captures
    // ===============================================================

    [Fact]
    public void TypedCaptureWithTypeParametersMatchesDictionary()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UseContainsKeyRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Collections.Generic;

                class Test
                {
                    void M()
                    {
                        var dict = new Dictionary<string, int>();
                        bool has = dict.Keys.Contains("key");
                    }
                }
                """,
                """
                using System.Collections.Generic;

                class Test
                {
                    void M()
                    {
                        var dict = new Dictionary<string, int>();
                        bool has = dict.ContainsKey("key");
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void TypedCaptureWithTypeParametersMatchesEnumerable()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UseElementAtRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Linq;
                using System.Collections.Generic;

                class Test
                {
                    void M()
                    {
                        var list = new List<string>();
                        var item = list.ElementAt(0);
                    }
                }
                """,
                """
                using System.Linq;
                using System.Collections.Generic;

                class Test
                {
                    void M()
                    {
                        var list = new List<string>();
                        var item = list[0];
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // NullSafe preservation — ?. marker transfers through Rewrite
    // ===============================================================

    [Fact]
    public void PreservesNullConditionalInRewrite()
    {
        var x = Capture.Expression(type: "IEnumerable<T>", typeParameters: ["T"]);
        var pred = Capture.Expression();

        RewriteRun(
            spec => spec.SetRecipe(new RewriteRecipe(
                CSharpTemplate.Rewrite(
                    CSharpPattern.Expression($"{x}.Where({pred}).First()"),
                    CSharpTemplate.Expression($"{x}.First({pred})"))))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Linq;
                using System.Collections.Generic;
                class Test
                {
                    void M(Dictionary<string, List<int>> dict)
                    {
                        var result = dict["key"]?.Where(x => x > 0).First();
                    }
                }
                """,
                """
                using System.Linq;
                using System.Collections.Generic;
                class Test
                {
                    void M(Dictionary<string, List<int>> dict)
                    {
                        var result = dict["key"]?.First(x => x > 0);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void PreservesNullConditionalOnFieldAccess()
    {
        var x = Capture.Expression();

        RewriteRun(
            spec => spec.SetRecipe(new RewriteRecipe(
                CSharpTemplate.Rewrite(
                    CSharpPattern.Expression($"{x}.Length"),
                    CSharpTemplate.Expression($"{x}.Count")))),
            CSharp(
                """
                class Test
                {
                    void M(string? s)
                    {
                        var n = s?.Length;
                    }
                }
                """,
                """
                class Test
                {
                    void M(string? s)
                    {
                        var n = s?.Count;
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void PreservesNullConditionalOnElementAccess()
    {
        var x = Capture.Expression();
        var i = Capture.Expression();

        RewriteRun(
            spec => spec.SetRecipe(new RewriteRecipe(
                CSharpTemplate.Rewrite(
                    CSharpPattern.Expression($"{x}[{i}]"),
                    CSharpTemplate.Expression($"{x}[{i}]")))),
            CSharp(
                """
                class Test
                {
                    void M(int[]? arr)
                    {
                        var n = arr?[0];
                    }
                }
                """,
                """
                class Test
                {
                    void M(int[]? arr)
                    {
                        var n = arr?[0];
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NullConditionalNotAddedWhenOriginalHasNone()
    {
        var x = Capture.Expression(type: "IEnumerable<T>", typeParameters: ["T"]);
        var pred = Capture.Expression();

        RewriteRun(
            spec => spec.SetRecipe(new RewriteRecipe(
                CSharpTemplate.Rewrite(
                    CSharpPattern.Expression($"{x}.Where({pred}).First()"),
                    CSharpTemplate.Expression($"{x}.First({pred})"))))
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Linq;
                using System.Collections.Generic;
                class Test
                {
                    void M(List<int> list)
                    {
                        var result = list.Where(x => x > 0).First();
                    }
                }
                """,
                """
                using System.Linq;
                using System.Collections.Generic;
                class Test
                {
                    void M(List<int> list)
                    {
                        var result = list.First(x => x > 0);
                    }
                }
                """
            )
        );
    }

    // ===============================================================
    // FlattenBlock — multi-statement template spliced into parent
    // ===============================================================

    [Fact]
    public void FlattenBlockSplicesStatementsIntoParent()
    {
        // return expr;  →  Console.WriteLine(expr); return expr;
        RewriteRun(
            spec => spec.SetRecipe(new LogBeforeReturnRecipe()),
            CSharp(
                """
                class C
                {
                    int M()
                    {
                        return 42;
                    }
                }
                """,
                """
                class C
                {
                    int M()
                    {
                        Console.WriteLine(42);
                        return 42;
                    }
                }
                """
            )
        );
    }
    [Fact]
    public void DeferredFormatDoesNotAffectSurroundingWhitespace()
    {
        // Replacing an attribute should not change indentation of surrounding code
        RewriteRun(
            spec => spec.SetRecipe(new RenameAttributeRecipe()),
            CSharp(
                """
                class C
                {
                    [Foo]
                    public void M()
                    {
                        var x = 1;
                    }
                }
                """,
                """
                class C
                {
                    [Bar]
                    public void M()
                    {
                        var x = 1;
                    }
                }
                """
            )
        );
    }
}

// ===============================================================
// Recipe implementations
// ===============================================================

class SwapBinaryOperandsRecipe : Core.Recipe
{
    public override string DisplayName => "Swap binary operands";
    public override string Description => "Swaps left and right operands of addition.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var left = Capture.Expression("left");
        var right = Capture.Expression("right");
        var pat = CSharpPattern.Expression($"{left} + {right}");
        var tmpl = CSharpTemplate.Expression($"{right} + {left}");

        return new Visitor(pat, tmpl);
    }

    private class Visitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitBinary(Binary binary, ExecutionContext ctx)
        {
            binary = (Binary)base.VisitBinary(binary, ctx);
            var match = pat.Match(binary, Cursor);
            if (match != null)
                return (J)tmpl.Apply(Cursor, values: match)!;
            return binary;
        }
    }
}

class NormalizeConsoleOutputRecipe : Core.Recipe
{
    public override string DisplayName => "Normalize console output";
    public override string Description => "Normalizes Console.Write and Console.Error.Write to Console.WriteLine.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Expression("expr");
        var pat1 = CSharpPattern.Expression($"Console.Write({expr})");
        var pat2 = CSharpPattern.Expression($"Console.Error.Write({expr})");
        var tmpl = CSharpTemplate.Expression($"Console.WriteLine({expr})");

        return new Visitor(pat1, pat2, tmpl);
    }

    private class Visitor(CSharpPattern pat1, CSharpPattern pat2, CSharpTemplate tmpl)
        : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            var match = pat1.Match(mi, Cursor) ?? pat2.Match(mi, Cursor);
            if (match != null)
                return (J)tmpl.Apply(Cursor, values: match)!;
            return mi;
        }
    }
}

class MigrateAndRedirectRecipe : Core.Recipe
{
    public override string DisplayName => "Migrate and redirect";
    public override string Description => "Chains two rules: Write→WriteLine, then WriteLine→Error.WriteLine.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr1 = Capture.Expression("expr");
        var migratePat = CSharpPattern.Expression($"Console.Write({expr1})");
        var migrateTmpl = CSharpTemplate.Expression($"Console.WriteLine({expr1})");

        var expr2 = Capture.Expression("expr");
        var redirectPat = CSharpPattern.Expression($"Console.WriteLine({expr2})");
        var redirectTmpl = CSharpTemplate.Expression($"Console.Error.WriteLine({expr2})");

        return new Visitor(migratePat, migrateTmpl, redirectPat, redirectTmpl);
    }

    private class Visitor(
        CSharpPattern migratePat, CSharpTemplate migrateTmpl,
        CSharpPattern redirectPat, CSharpTemplate redirectTmpl)
        : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            var match = migratePat.Match(mi, Cursor);
            if (match != null)
            {
                var result = (J)migrateTmpl.Apply(Cursor, values: match)!;
                var redirectMatch = redirectPat.Match(result, Cursor);
                if (redirectMatch != null)
                    result = (J)redirectTmpl.Apply(Cursor, values: redirectMatch)!;
                return result;
            }
            return mi;
        }
    }
}

class MigrateWithFallbackRecipe : Core.Recipe
{
    public override string DisplayName => "Migrate with fallback";
    public override string Description => "Tries primary then fallback rule.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr1 = Capture.Expression("expr");
        var primaryPat = CSharpPattern.Expression($"Console.Write({expr1})");
        var primaryTmpl = CSharpTemplate.Expression($"Console.WriteLine({expr1})");

        var expr2 = Capture.Expression("expr");
        var fallbackPat = CSharpPattern.Expression($"Console.Error.Write({expr2})");
        var fallbackTmpl = CSharpTemplate.Expression($"Console.Error.WriteLine({expr2})");

        return new Visitor(primaryPat, primaryTmpl, fallbackPat, fallbackTmpl);
    }

    private class Visitor(
        CSharpPattern primaryPat, CSharpTemplate primaryTmpl,
        CSharpPattern fallbackPat, CSharpTemplate fallbackTmpl)
        : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            var match = primaryPat.Match(mi, Cursor);
            if (match != null)
                return (MethodInvocation)primaryTmpl.Apply(Cursor, values: match)!;
            match = fallbackPat.Match(mi, Cursor);
            if (match != null)
                return (MethodInvocation)fallbackTmpl.Apply(Cursor, values: match)!;
            return mi;
        }
    }
}

class PreMatchFilteredRecipe : Core.Recipe
{
    public override string DisplayName => "PreMatch filtered";
    public override string Description => "Only transforms inside methods named Target.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Expression("expr");
        var pat = CSharpPattern.Expression($"Console.Write({expr})");
        var tmpl = CSharpTemplate.Expression($"Console.WriteLine({expr})");

        return new Visitor(pat, tmpl);
    }

    private class Visitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            if (Cursor.FirstEnclosing<MethodDeclaration>()?.Name.SimpleName != "Target")
                return mi;
            var match = pat.Match(mi, Cursor);
            if (match != null)
                return (MethodInvocation)tmpl.Apply(Cursor, values: match)!;
            return mi;
        }
    }
}

class CaptureConstraintFilteredRecipe : Core.Recipe
{
    public override string DisplayName => "Capture constraint filtered";
    public override string Description => "Simplifies x + 0 to x using a capture constraint.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var left = Capture.Expression("left");
        var right = Capture.Expression("right",
            constraint: (node, _) => node is Literal { ValueSource: "0" });
        var pat = CSharpPattern.Expression($"{left} + {right}");
        var tmpl = CSharpTemplate.Expression($"{left}");

        return new Visitor(pat, tmpl);
    }

    private class Visitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitBinary(Binary binary, ExecutionContext ctx)
        {
            binary = (Binary)base.VisitBinary(binary, ctx);
            var match = pat.Match(binary, Cursor);
            if (match != null)
                return (J)tmpl.Apply(Cursor, values: match)!;
            return binary;
        }
    }
}

class CaptureFlowRecipe : Core.Recipe
{
    public override string DisplayName => "Capture flow";
    public override string Description => "Tests captures flowing from pattern to template.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Expression("expr");
        var pat = CSharpPattern.Expression($"Console.Write({expr})");
        var tmpl = CSharpTemplate.Expression($"Console.WriteLine({expr})");

        return new Visitor(pat, tmpl);
    }

    private class Visitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            var match = pat.Match(mi, Cursor);
            if (match != null)
                return (MethodInvocation)tmpl.Apply(Cursor, values: match)!;
            return mi;
        }
    }
}

/// <summary>
/// Expands "return expr" into "Console.WriteLine(expr); return expr;" — two statements.
/// Exercises multi-statement templates with synthetic block flattening.
/// </summary>
class LogBeforeReturnRecipe : Core.Recipe
{
    public override string DisplayName => "Log before return";
    public override string Description => "Adds Console.WriteLine before return statements.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Expression("expr");
        var pat = CSharpPattern.Statement($"return {expr}");
        var tmpl = CSharpTemplate.Statement($"Console.WriteLine({expr});\nreturn {expr};");

        return new Visitor(pat, tmpl);
    }

    private class Visitor(CSharpPattern pat, CSharpTemplate tmpl) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitReturn(Return ret, ExecutionContext ctx)
        {
            ret = (Return)base.VisitReturn(ret, ctx);
            var match = pat.Match(ret, Cursor);
            if (match != null)
            {
                var result = tmpl.Apply(Cursor, values: match);
                return result != null ? AutoFormat(result, ctx, Cursor) : ret;
            }
            return ret;
        }
    }
}

class RewriteBinaryRecipe : Core.Recipe
{
    public override string DisplayName => "Rewrite binary swap";
    public override string Description => "Swaps binary operands using CSharpTemplate.Rewrite().";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var left = Capture.Expression("left");
        var right = Capture.Expression("right");
        return CSharpTemplate.Rewrite(
            CSharpPattern.Expression($"{left} + {right}"),
            CSharpTemplate.Expression($"{right} + {left}"));
    }
}

class RewriteMethodInvocationRecipe : Core.Recipe
{
    public override string DisplayName => "Rewrite method invocation";
    public override string Description => "Replaces Console.Write with Console.WriteLine using CSharpTemplate.Rewrite().";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Expression("expr");
        return CSharpTemplate.Rewrite(
            CSharpPattern.Expression($"Console.Write({expr})"),
            CSharpTemplate.Expression($"Console.WriteLine({expr})"));
    }
}

class UseContainsKeyRecipe : Core.Recipe
{
    public override string DisplayName => "Use ContainsKey";
    public override string Description => "Replace dict.Keys.Contains(key) with dict.ContainsKey(key).";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var dict = Capture.Expression(type: "IDictionary<TKey, TValue>", typeParameters: ["TKey", "TValue"]);
        var key = Capture.Expression();
        return CSharpTemplate.Rewrite(
            CSharpPattern.Expression($"{dict}.Keys.Contains({key})",
                usings: ["System.Collections.Generic"]),
            CSharpTemplate.Expression($"{dict}.ContainsKey({key})"));
    }
}

class UseElementAtRecipe : Core.Recipe
{
    public override string DisplayName => "Use element access";
    public override string Description => "Replace ElementAt with indexer.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Expression("expr", type: "IEnumerable<T>", typeParameters: ["T"]);
        var idx = Capture.Expression("idx", type: "int");
        return CSharpTemplate.Rewrite(
            CSharpPattern.Expression($"{expr}.ElementAt({idx})",
                usings: ["System.Collections.Generic", "System.Linq"]),
            CSharpTemplate.Expression($"{expr}[{idx}]"));
    }
}

class RewriteRecipe(CSharpVisitor<ExecutionContext> visitor) : Core.Recipe
{
    public override string DisplayName => "Rewrite recipe";
    public override string Description => "Applies a CSharpTemplate.Rewrite visitor.";

    public override ITreeVisitor<ExecutionContext> GetVisitor() => visitor;
}

class FallbackWithManualVisitorRecipe : Core.Recipe
{
    public override string DisplayName => "Fallback with manual visitor";
    public override string Description => "Replaces == null / != null with is null / is not null.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var x = Capture.Expression("x");
        return CSharpTemplate.Rewrite(
            (CSharpPattern.Expression($"{x} == null"), CSharpTemplate.Expression($"{x} is null")),
            (CSharpPattern.Expression($"{x} != null"), CSharpTemplate.Expression($"{x} is not null")));
    }
}

class RenameAttributeRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Rename attribute";
    public override string Description => "Renames [Foo] to [Bar].";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        return CSharpTemplate.Rewrite(
            CSharpPattern.Attribute($"Foo"),
            CSharpTemplate.Attribute($"Bar"));
    }
}

class SwapBinaryWithAutoFormatRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Swap binary with auto-format";
    public override string Description => "Swaps binary operands, using AutoFormat.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var left = Capture.Expression("left");
        var right = Capture.Expression("right");
        return CSharpTemplate.Rewrite(
            CSharpPattern.Expression($"{left} + {right}"),
            CSharpTemplate.Expression($"{right} + {left}"));
    }
}
