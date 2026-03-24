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
/// or <see cref="RewriteRule.Rewrite(CSharpPattern, CSharpTemplate)"/>.
/// </summary>
/// <example>
/// <code>
/// var expr = Capture.Expression("expr");
/// var rule = RewriteRule.Rewrite($"Console.Write({expr})", $"Console.WriteLine({expr})");
///
/// // In a visitor method:
/// return rule.TryOn(Cursor, node) ?? node;
/// </code>
/// </example>
public interface IRewriteRule
{
    /// <summary>
    /// Try to apply this rule to the given node. Returns the transformed node if the
    /// rule's pattern matches, or <c>null</c> if the rule does not apply.
    /// The typical calling convention is <c>rule.TryOn(Cursor, node) ?? node</c>.
    /// </summary>
    J? TryOn(Cursor cursor, J node);

    /// <summary>
    /// Create a <see cref="CSharpVisitor{ExecutionContext}"/> that applies this rule to every
    /// visited node via <see cref="TreeVisitor{T,P}.PostVisit"/>. The pattern's fast-reject
    /// in <see cref="CSharpPattern.Match"/> ensures only nodes whose type matches the pattern
    /// root are fully compared, so iterating over all nodes is cheap.
    /// <para>
    /// This is a convenience method for the common case where a recipe only matches a pattern
    /// and applies a template. For more complex scenarios — such as combining multiple rules,
    /// cursor-based filtering, or using <see cref="RewriteRule.CreateBlockFlattener{P}"/> —
    /// create a custom visitor and call <see cref="TryOn"/> directly.
    /// </para>
    /// </summary>
    /// <example>
    /// <code>
    /// public override ITreeVisitor&lt;ExecutionContext&gt; GetVisitor()
    /// {
    ///     var x = Capture.Expression("x");
    ///     return RewriteRule.Rewrite($"Console.Write({x})", $"Console.WriteLine({x})")
    ///         .ToVisitor();
    /// }
    /// </code>
    /// </example>
    CSharpVisitor<ExecutionContext> ToVisitor() => new RewriteRule.RewriteRuleVisitor(this);
}

/// <summary>
/// Factory methods for creating <see cref="IRewriteRule"/> instances.
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
        new RewriteRuleImpl(before, after);

    /// <summary>
    /// Wrap a <see cref="Recipe"/> as an <see cref="IRewriteRule"/>.
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

    private sealed class RewriteRuleImpl(CSharpPattern before, CSharpTemplate after) : IRewriteRule
    {
        public J? TryOn(Cursor cursor, J node)
        {
            var match = before.Match(node, cursor);
            if (match == null) return null;
            return after.Apply(cursor, values: match);
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
