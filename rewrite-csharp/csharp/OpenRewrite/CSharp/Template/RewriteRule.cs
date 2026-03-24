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
/// Rules can be composed with <see cref="AndThen"/> (sequential pipeline) and
/// <see cref="OrElse"/> (fallback).
/// </summary>
public interface IRewriteRule
{
    /// <summary>
    /// Try to apply this rule to the given node. Returns the transformed node if the
    /// rule's pattern matches and all predicates pass, or <c>null</c> if the rule does not apply.
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
}

/// <summary>
/// Configuration for a rewrite rule, returned by the builder function passed to
/// <see cref="RewriteRule.Rewrite(Func{RewriteConfig})"/>.
/// </summary>
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
    /// A dynamic template factory called with the match result, allowing different
    /// templates based on what was captured. Mutually exclusive with <see cref="After"/>.
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
    /// Optional predicate evaluated before any structural matching.
    /// Receives the candidate node and cursor. Return <c>false</c> to skip this rule entirely.
    /// </summary>
    public Func<J, Cursor, bool>? PreMatch { get; set; }

    /// <summary>
    /// Optional predicate evaluated after a successful structural match.
    /// Receives the candidate node, cursor, and the captures from the match.
    /// Return <c>false</c> to reject the match and continue to the next pattern.
    /// </summary>
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
    /// Creates a visitor that, when run as an after-visitor, splices the statements of
    /// <paramref name="block"/> into its parent block. Use from within a visitor method:
    /// <code>
    /// DoAfterVisit(RewriteRule.CreateBlockFlattener&lt;ExecutionContext&gt;(block));
    /// return block;
    /// </code>
    /// </summary>
    public static CSharpVisitor<P> CreateBlockFlattener<P>(Block block) => new BlockFlattener<P>(block);

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

    private sealed class BlockFlattener<P>(Block target) : CSharpVisitor<P>
    {
        public override J VisitBlock(Block block, P ctx)
        {
            block = (Block)base.VisitBlock(block, ctx);

            var statements = block.Statements;
            var newStatements = new List<JRightPadded<Statement>>(statements.Count);
            var changed = false;

            foreach (var stmt in statements)
            {
                if (stmt.Element is Block inner &&
                    (ReferenceEquals(inner, target) || inner.Id == target.Id))
                {
                    // Splice inner block's statements into the parent.
                    // An empty inner block is intentionally dropped — flattening a block
                    // that produced no statements should remove the slot.
                    var innerStmts = inner.Statements;
                    for (var i = 0; i < innerStmts.Count; i++)
                    {
                        var s = innerStmts[i];
                        if (i == 0)
                            s = s.WithElement(J.SetPrefix(s.Element, stmt.Element.Prefix));
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
    }
}
