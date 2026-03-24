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
using OpenRewrite.Java;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// A declarative rewrite rule that pairs structural pattern matching with template application.
/// Create rules with <see cref="RewriteRule.Rewrite(TemplateStringHandler, TemplateStringHandler)"/>
/// and compose them with <see cref="AndThen"/> (sequential pipeline) and
/// <see cref="OrElse"/> (fallback).
/// </summary>
/// <example>
/// <code>
/// // Simple: match Console.Write(expr) → replace with Console.WriteLine(expr)
/// var expr = Capture.Expression("expr");
/// var rule = RewriteRule.Rewrite($"Console.Write({expr})", $"Console.WriteLine({expr})");
///
/// // In a visitor method:
/// return rule.TryOn(Cursor, node) ?? node;
///
/// // Composition:
/// var combined = rule1.AndThen(rule2);   // apply rule2 to rule1's output
/// var either   = rule1.OrElse(rule2);    // try rule1 first, fall back to rule2
/// </code>
/// </example>
public interface IRewriteRule
{
    /// <summary>
    /// Try to apply this rule to the given node. Returns the transformed node if the
    /// rule's pattern matches and all predicates pass, or <c>null</c> if the rule does not apply.
    /// The typical calling convention is <c>rule.TryOn(Cursor, node) ?? node</c>.
    /// </summary>
    J? TryOn(Cursor cursor, J node);

    /// <summary>
    /// Chain another rule after this one. The second rule is applied to the first rule's output.
    /// If the first rule doesn't match, the chain produces <c>null</c>.
    /// If the first matches but the second doesn't, the first rule's result is kept.
    /// <para>
    /// Note: the second rule receives the same cursor as the first. If the second rule's pattern
    /// or template inspects cursor ancestry, it will see the original tree context, not the
    /// modified subtree. This is correct for expression-level rules where the surrounding tree
    /// hasn't changed; for rules that depend on parent structure, apply them in separate visitor passes.
    /// </para>
    /// </summary>
    IRewriteRule AndThen(IRewriteRule next) => new RewriteRule.AndThenRule(this, next);

    /// <summary>
    /// Provide a fallback rule. If this rule matches, its result is used.
    /// Otherwise the alternative is tried on the original node.
    /// </summary>
    IRewriteRule OrElse(IRewriteRule alternative) => new RewriteRule.OrElseRule(this, alternative);

    /// <summary>
    /// Create a <see cref="CSharpVisitor{ExecutionContext}"/> that applies this rule to every
    /// visited node via <see cref="TreeVisitor{T,P}.PostVisit"/>. The pattern's fast-reject
    /// in <see cref="CSharpPattern.Match"/> ensures only nodes whose type matches the pattern
    /// root are fully compared, so iterating over all nodes is cheap.
    /// <para>
    /// This is a convenience method for the common case where the visitor only needs to
    /// apply a rule. For more complex scenarios — such as combining rule results with
    /// manual AST construction, using <see cref="RewriteRule.CreateBlockFlattener{P}"/>,
    /// or applying additional logic before/after the rule — create a custom visitor instead.
    /// </para>
    /// </summary>
    /// <example>
    /// <code>
    /// public override JavaVisitor&lt;ExecutionContext&gt; GetVisitor()
    /// {
    ///     var x = Capture.Expression("x");
    ///     return RewriteRule.Rewrite($"{x} == null", $"{x} is null")
    ///         .OrElse(RewriteRule.Rewrite($"{x} != null", $"{x} is not null"))
    ///         .ToVisitor();
    /// }
    /// </code>
    /// </example>
    CSharpVisitor<ExecutionContext> ToVisitor() => new RewriteRule.RewriteRuleVisitor(this);
}

/// <summary>
/// Configuration for a rewrite rule, returned by the builder function passed to
/// <see cref="RewriteRule.Rewrite(Func{RewriteConfig})"/>. Used for advanced scenarios
/// that the simple two-argument <see cref="RewriteRule.Rewrite(TemplateStringHandler, TemplateStringHandler)"/>
/// overload cannot express: multiple before patterns, dynamic template selection,
/// or pre/post-match filtering.
/// </summary>
/// <example>
/// <code>
/// var expr = Capture.Expression("expr");
/// var rule = RewriteRule.Rewrite(() => new RewriteConfig
/// {
///     Before = CSharpPattern.Expression($"Console.Write({expr})"),
///     After = CSharpTemplate.Expression($"Console.WriteLine({expr})"),
///     PreMatch = (_, cursor) =>
///         cursor.FirstEnclosing&lt;MethodDeclaration&gt;()?.Name.SimpleName == "Target"
/// });
/// </code>
/// </example>
public sealed class RewriteConfig
{
    private CSharpPattern? _singleBefore;
    private CSharpPattern[]? _multipleBefores;
    private CSharpTemplate? _staticAfter;
    private Func<MatchResult, CSharpTemplate>? _dynamicAfter;

    /// <summary>
    /// A single pattern to match. Mutually exclusive with <see cref="Befores"/>.
    /// </summary>
    public CSharpPattern Before
    {
        get => _singleBefore ?? throw new InvalidOperationException("Before not set");
        set
        {
            _singleBefore = value;
            _multipleBefores = null;
        }
    }

    /// <summary>
    /// Multiple patterns to try in order. The first successful match is used.
    /// </summary>
    public CSharpPattern[] Befores
    {
        get => _multipleBefores ?? throw new InvalidOperationException("Befores not set");
        set
        {
            _multipleBefores = value;
            _singleBefore = null;
        }
    }

    /// <summary>
    /// A static template applied when a pattern matches.
    /// Mutually exclusive with <see cref="AfterFactory"/>.
    /// </summary>
    public CSharpTemplate After
    {
        get => _staticAfter ?? throw new InvalidOperationException("After not set");
        set
        {
            _staticAfter = value;
            _dynamicAfter = null;
        }
    }

    /// <summary>
    /// A dynamic template factory called with the match result, allowing selection of
    /// different templates based on what was captured. Use instead of <see cref="After"/>
    /// when the replacement depends on the matched values — e.g., choosing between
    /// different method names based on a captured argument type.
    /// Mutually exclusive with <see cref="After"/>.
    /// </summary>
    public Func<MatchResult, CSharpTemplate> AfterFactory
    {
        get => _dynamicAfter ?? throw new InvalidOperationException("AfterFactory not set");
        set
        {
            _dynamicAfter = value;
            _staticAfter = null;
        }
    }

    /// <summary>
    /// Optional predicate evaluated <em>before</em> any structural matching is attempted.
    /// Use for cheap context-based filtering — e.g., "only apply inside methods named X"
    /// or "skip test files." If this returns <c>false</c>, the rule is skipped entirely
    /// without parsing any patterns.
    /// </summary>
    /// <example>
    /// <code>
    /// PreMatch = (_, cursor) =>
    ///     cursor.FirstEnclosing&lt;MethodDeclaration&gt;()?.Name.SimpleName == "Target"
    /// </code>
    /// </example>
    public Func<J, Cursor, bool>? PreMatch { get; set; }

    /// <summary>
    /// Optional predicate evaluated <em>after</em> a structural pattern match succeeds.
    /// Receives the matched captures, allowing semantic validation of what was captured —
    /// e.g., "only apply if the captured right operand is the literal 0."
    /// If this returns <c>false</c>, the match is rejected and the next pattern in
    /// <see cref="Befores"/> is tried.
    /// </summary>
    /// <example>
    /// <code>
    /// PostMatch = (_, _, captures) =>
    ///     captures.Get(right) is Literal { ValueSource: "0" }
    /// </code>
    /// </example>
    public Func<J, Cursor, MatchResult, bool>? PostMatch { get; set; }

    internal CSharpPattern[] GetBeforePatterns()
    {
        if (_multipleBefores != null) return _multipleBefores;
        if (_singleBefore != null) return [_singleBefore];
        throw new InvalidOperationException("RewriteConfig requires at least one Before pattern.");
    }

    internal CSharpTemplate ResolveAfter(MatchResult match)
    {
        if (_dynamicAfter != null) return _dynamicAfter(match);
        if (_staticAfter != null) return _staticAfter;
        throw new InvalidOperationException("RewriteConfig requires an After template.");
    }
}

/// <summary>
/// Factory methods for creating <see cref="IRewriteRule"/> instances.
/// Intended for use with <c>using static OpenRewrite.CSharp.Template.RewriteRule;</c>
/// so that <see cref="Rewrite(TemplateStringHandler, TemplateStringHandler)"/> reads naturally.
/// </summary>
public static class RewriteRule
{
    /// <summary>
    /// Create a rewrite rule from interpolated strings. Uses <see cref="ScaffoldKind.Expression"/>
    /// scaffolding — suitable for matching within expression-level visitor methods.
    /// </summary>
    /// <example>
    /// <code>
    /// using static OpenRewrite.CSharp.Template.RewriteRule;
    ///
    /// var expr = Capture.Expression("expr");
    /// var rule = Rewrite($"Console.Write({expr})", $"Console.WriteLine({expr})");
    /// return rule.TryOn(cursor, node) ?? node;
    /// </code>
    /// </example>
    public static IRewriteRule Rewrite(TemplateStringHandler before, TemplateStringHandler after) =>
        Rewrite(CSharpPattern.Expression(before), CSharpTemplate.Expression(after));

    /// <summary>
    /// Create a rewrite rule from a <see cref="CSharpPattern"/> and <see cref="CSharpTemplate"/>.
    /// Use this when you need explicit scaffold control (e.g., <see cref="CSharpPattern.Statement"/>).
    /// </summary>
    public static IRewriteRule Rewrite(CSharpPattern before, CSharpTemplate after) =>
        new RewriteRuleImpl([before], _ => after, null, null);

    /// <summary>
    /// Create a rewrite rule from a builder function for advanced scenarios:
    /// multiple before patterns, dynamic after templates, pre/post-match hooks.
    /// </summary>
    public static IRewriteRule Rewrite(Func<RewriteConfig> builder)
    {
        var config = builder();
        return new RewriteRuleImpl(
            config.GetBeforePatterns(),
            config.ResolveAfter,
            config.PreMatch,
            config.PostMatch
        );
    }

    /// <summary>
    /// Wrap a <see cref="Recipe"/> as an <see cref="IRewriteRule"/> for composition with
    /// <see cref="IRewriteRule.AndThen"/> and <see cref="IRewriteRule.OrElse"/>.
    /// </summary>
    public static IRewriteRule FromRecipe(Recipe recipe, ExecutionContext ctx) =>
        new RecipeRuleAdapter(recipe, ctx);

    /// <summary>
    /// Creates a visitor that splices statements from any <see cref="Block"/> marked with
    /// <see cref="SyntheticBlockContainer"/> into its parent block. Register once via
    /// <see cref="TreeVisitor{T,P}.DoAfterVisit"/> — a single instance handles all
    /// synthetic blocks produced during the visit.
    /// </summary>
    /// <example>
    /// <code>
    /// var result = rule.TryOn(Cursor, ret);
    /// if (result is Block block &amp;&amp; block.Markers.FindFirst&lt;SyntheticBlockContainer&gt;() != null)
    /// {
    ///     MaybeDoAfterVisit(RewriteRule.CreateBlockFlattener&lt;ExecutionContext&gt;());
    ///     return block;
    /// }
    /// return result ?? ret;
    /// </code>
    /// </example>
    public static CSharpVisitor<P> CreateBlockFlattener<P>() => new BlockFlattener<P>();

    /// <summary>
    /// Overload accepting a block for backwards compatibility. The block parameter is ignored;
    /// the flattener identifies targets by the <see cref="SyntheticBlockContainer"/> marker.
    /// </summary>
    [Obsolete("Use the parameterless overload. The block parameter is no longer needed.")]
    public static CSharpVisitor<P> CreateBlockFlattener<P>(Block block) => new BlockFlattener<P>();

    // ===============================================================
    // Implementation
    // ===============================================================

    private sealed class RewriteRuleImpl : IRewriteRule
    {
        private readonly CSharpPattern[] _before;
        private readonly Func<MatchResult, CSharpTemplate> _resolveAfter;
        private readonly Func<J, Cursor, bool>? _preMatch;
        private readonly Func<J, Cursor, MatchResult, bool>? _postMatch;

        internal RewriteRuleImpl(
            CSharpPattern[] before,
            Func<MatchResult, CSharpTemplate> resolveAfter,
            Func<J, Cursor, bool>? preMatch,
            Func<J, Cursor, MatchResult, bool>? postMatch)
        {
            _before = before;
            _resolveAfter = resolveAfter;
            _preMatch = preMatch;
            _postMatch = postMatch;
        }

        public J? TryOn(Cursor cursor, J node)
        {
            if (_preMatch != null && !_preMatch(node, cursor))
                return null;

            foreach (var pattern in _before)
            {
                var match = pattern.Match(node, cursor);
                if (match == null) continue;

                if (_postMatch != null && !_postMatch(node, cursor, match))
                    continue;

                var template = _resolveAfter(match);
                return template.Apply(cursor, values: match);
            }

            return null;
        }

    }

    internal sealed class AndThenRule(IRewriteRule first, IRewriteRule next) : IRewriteRule
    {
        public J? TryOn(Cursor cursor, J node)
        {
            var firstResult = first.TryOn(cursor, node);
            if (firstResult == null) return null;

            // cursor reflects the original tree context, not firstResult's context.
            var secondResult = next.TryOn(cursor, firstResult);
            return secondResult ?? firstResult;
        }
    }

    internal sealed class OrElseRule(IRewriteRule primary, IRewriteRule alternative) : IRewriteRule
    {
        public J? TryOn(Cursor cursor, J node)
        {
            var result = primary.TryOn(cursor, node);
            return result ?? alternative.TryOn(cursor, node);
        }
    }

    private sealed class RecipeRuleAdapter(Recipe recipe, ExecutionContext ctx) : IRewriteRule
    {
        public J? TryOn(Cursor cursor, J node)
        {
            var visitor = recipe.GetVisitor();
            Tree? result;
            if (visitor is TreeVisitor<J, ExecutionContext> tv)
                result = tv.Visit(node, ctx, cursor);
            else
                // Fallback for non-generic visitors: cursor is not propagated.
                // Recipes relying on cursor ancestry will not work through this path.
                result = visitor.Visit(node, ctx);

            if (result is not J j || ReferenceEquals(result, node))
                return null;
            return j;
        }
    }

    internal sealed class RewriteRuleVisitor(IRewriteRule rule) : CSharpVisitor<ExecutionContext>
    {
        public override J? PostVisit(J tree, ExecutionContext ctx)
        {
            return rule.TryOn(Cursor, tree) ?? tree;
        }
    }

    private sealed class BlockFlattener<P> : CSharpVisitor<P>, IEquatable<BlockFlattener<P>>
    {
        public bool Equals(BlockFlattener<P>? other) => other is not null;
        public override bool Equals(object? obj) => obj is BlockFlattener<P>;
        public override int GetHashCode() => typeof(BlockFlattener<P>).GetHashCode();
        public override J VisitBlock(Block block, P ctx)
        {
            block = (Block)base.VisitBlock(block, ctx);

            var statements = block.Statements;
            var newStatements = new List<JRightPadded<Statement>>(statements.Count);
            var changed = false;

            foreach (var stmt in statements)
            {
                if (stmt.Element is Block inner &&
                    inner.Markers.FindFirst<SyntheticBlockContainer>() != null)
                {
                    // Splice inner block's statements into the parent.
                    // An empty inner block is intentionally dropped — flattening a block
                    // that produced no statements should remove the slot.
                    var innerStmts = inner.Statements;
                    for (var i = 0; i < innerStmts.Count; i++)
                    {
                        var s = innerStmts[i];
                        if (i == 0)
                        {
                            // Transfer the original statement's prefix (comments, blank lines)
                            // to the first spliced statement.
                            s = s.WithElement(SetStatementPrefix(s.Element, stmt.Element.Prefix));
                        }
                        newStatements.Add(s);
                    }
                    changed = true;
                }
                else
                {
                    newStatements.Add(stmt);
                }
            }

            return changed ? block.WithStatements(newStatements) : block;
        }

        /// <summary>
        /// Sets the prefix on a statement, handling <see cref="ExpressionStatement"/> which
        /// delegates its prefix to its inner expression and has no <c>WithPrefix</c> method.
        /// </summary>
        private static Statement SetStatementPrefix(Statement stmt, Space prefix)
        {
            if (stmt is ExpressionStatement es)
                return es.WithExpression(J.SetPrefix(es.Expression, prefix));
            return (Statement)J.SetPrefix(stmt, prefix);
        }
    }
}
