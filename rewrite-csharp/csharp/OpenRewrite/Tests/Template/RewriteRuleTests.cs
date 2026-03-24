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
    // ToVisitor — eliminates the need for a manual Visitor class
    // ===============================================================

    [Fact]
    public void ToVisitorAppliesRuleToBinaryNodes()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ToVisitorBinaryRecipe()),
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
    public void ToVisitorAppliesRuleToMethodInvocations()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ToVisitorMethodInvocationRecipe()),
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
    public void ToVisitorNoMatchLeavesUnchanged()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ToVisitorBinaryRecipe()),
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
        var rule = RewriteRule.Rewrite($"{left} + {right}", $"{right} + {left}");

        return new Visitor(rule);
    }

    private class Visitor(IRewriteRule rule) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitBinary(Binary binary, ExecutionContext ctx)
        {
            binary = (Binary)base.VisitBinary(binary, ctx);
            return rule.TryOn(Cursor, binary) ?? binary;
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
        var migrateRule = RewriteRule.Rewrite(
            $"Console.Write({expr1})", $"Console.WriteLine({expr1})");

        var expr2 = Capture.Expression("expr");
        var redirectRule = RewriteRule.Rewrite(
            $"Console.WriteLine({expr2})", $"Console.Error.WriteLine({expr2})");

        return new Visitor(migrateRule, redirectRule);
    }

    private class Visitor(IRewriteRule migrate, IRewriteRule redirect)
        : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            var result = migrate.TryOn(Cursor, mi);
            if (result != null)
                result = redirect.TryOn(Cursor, result) ?? result;
            return (MethodInvocation)(result ?? mi);
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
        var primaryRule = RewriteRule.Rewrite(
            $"Console.Write({expr1})", $"Console.WriteLine({expr1})");

        var expr2 = Capture.Expression("expr");
        var fallbackRule = RewriteRule.Rewrite(
            $"Console.Error.Write({expr2})", $"Console.Error.WriteLine({expr2})");

        return new Visitor(primaryRule, fallbackRule);
    }

    private class Visitor(IRewriteRule primary, IRewriteRule fallback)
        : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            return (MethodInvocation)(primary.TryOn(Cursor, mi)
                ?? fallback.TryOn(Cursor, mi) ?? mi);
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
        var rule = RewriteRule.Rewrite(
            $"Console.Write({expr})", $"Console.WriteLine({expr})");

        return new Visitor(rule);
    }

    private class Visitor(IRewriteRule rule) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            if (Cursor.FirstEnclosing<MethodDeclaration>()?.Name.SimpleName != "Target")
                return mi;
            return (MethodInvocation)(rule.TryOn(Cursor, mi) ?? mi);
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
        var rule = RewriteRule.Rewrite($"{left} + {right}", $"{left}");

        return new Visitor(rule);
    }

    private class Visitor(IRewriteRule rule) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitBinary(Binary binary, ExecutionContext ctx)
        {
            binary = (Binary)base.VisitBinary(binary, ctx);
            return rule.TryOn(Cursor, binary) ?? binary;
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
        var rule = RewriteRule.Rewrite($"Console.Write({expr})", $"Console.WriteLine({expr})");

        return new Visitor(rule);
    }

    private class Visitor(IRewriteRule rule) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);
            return (MethodInvocation)(rule.TryOn(Cursor, mi) ?? mi);
        }
    }
}

/// <summary>
/// Expands "return expr" into "Console.WriteLine(expr); return expr;" — two statements.
/// Exercises multi-statement templates and RewriteRule.CreateBlockFlattener.
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

        var rule = RewriteRule.Rewrite(pat, tmpl);

        return new Visitor(rule);
    }

    private class Visitor(IRewriteRule rule) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitReturn(Return ret, ExecutionContext ctx)
        {
            ret = (Return)base.VisitReturn(ret, ctx);
            var result = rule.TryOn(Cursor, ret);
            if (result is Block { Markers: var m } block &&
                m.FindFirst<SyntheticBlockContainer>() != null)
            {
                MaybeDoAfterVisit(RewriteRule.CreateBlockFlattener<ExecutionContext>());
                return block;
            }
            return result ?? ret;
        }
    }
}

class ToVisitorBinaryRecipe : Core.Recipe
{
    public override string DisplayName => "ToVisitor binary swap";
    public override string Description => "Swaps binary operands using ToVisitor().";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var left = Capture.Expression("left");
        var right = Capture.Expression("right");
        return RewriteRule.Rewrite($"{left} + {right}", $"{right} + {left}")
            .ToVisitor();
    }
}

class ToVisitorMethodInvocationRecipe : Core.Recipe
{
    public override string DisplayName => "ToVisitor method invocation";
    public override string Description => "Replaces Console.Write with Console.WriteLine using ToVisitor().";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var expr = Capture.Expression("expr");
        return RewriteRule.Rewrite($"Console.Write({expr})", $"Console.WriteLine({expr})")
            .ToVisitor();
    }
}

class FallbackWithManualVisitorRecipe : Core.Recipe
{
    public override string DisplayName => "Fallback with manual visitor";
    public override string Description => "Replaces == null / != null with is null / is not null.";

    public override ITreeVisitor<ExecutionContext> GetVisitor()
    {
        var x = Capture.Expression("x");
        var isNull = RewriteRule.Rewrite($"{x} == null", $"{x} is null");
        var isNotNull = RewriteRule.Rewrite($"{x} != null", $"{x} is not null");

        return new Visitor(isNull, isNotNull);
    }

    private class Visitor(IRewriteRule isNull, IRewriteRule isNotNull)
        : CSharpVisitor<ExecutionContext>
    {
        public override J VisitBinary(Binary binary, ExecutionContext ctx)
        {
            binary = (Binary)base.VisitBinary(binary, ctx);
            return isNull.TryOn(Cursor, binary)
                ?? isNotNull.TryOn(Cursor, binary) ?? binary;
        }
    }
}
