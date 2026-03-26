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
/// A parsed and cacheable template for generating C# AST nodes.
/// Templates are created using C# string interpolation with <see cref="Capture{T}"/> placeholders,
/// providing the C# equivalent of JavaScript's tagged template literals.
/// </summary>
/// <example>
/// <code>
/// // Simple template (no captures)
/// var tmpl = CSharpTemplate.Expression($"Console.WriteLine(\"hello\")");
/// var result = tmpl.Apply(cursor);
///
/// // Template with captures from pattern match
/// var expr = Capture.Of&lt;Expression&gt;("expr");
/// var tmpl = CSharpTemplate.Expression($"Console.WriteLine({expr})");
/// var result = tmpl.Apply(cursor, values: match);
///
/// // Template with usings
/// var tmpl = CSharpTemplate.Expression(
///     $"JsonSerializer.Serialize({expr})",
///     usings: ["System.Text.Json"]);
///
/// // Template with NuGet dependencies for type attribution
/// var tmpl = CSharpTemplate.Expression(
///     $"JsonConvert.SerializeObject({expr})",
///     usings: ["Newtonsoft.Json"],
///     dependencies: new Dictionary&lt;string, string&gt; { ["Newtonsoft.Json"] = "13.*" });
/// </code>
/// </example>
public sealed class CSharpTemplate
{
    private readonly string _code;
    private readonly IReadOnlyDictionary<string, object> _captures;
    private readonly IReadOnlyList<string> _usings;
    private readonly IReadOnlyList<string> _context;
    private readonly IReadOnlyDictionary<string, string> _dependencies;
    private readonly ScaffoldKind? _scaffoldKind;
    private J? _cachedTree;

    private CSharpTemplate(string code, Dictionary<string, object> captures,
        IReadOnlyList<string>? usings, IReadOnlyList<string>? context,
        IReadOnlyDictionary<string, string>? dependencies,
        ScaffoldKind? scaffoldKind = null)
    {
        _code = code;
        _captures = captures;
        _usings = usings ?? [];
        _context = context ?? [];
        _dependencies = dependencies ?? new Dictionary<string, string>();
        _scaffoldKind = scaffoldKind;
    }

    /// <summary>
    /// Create a template with auto-detected scaffold kind.
    /// Prefer <see cref="Expression"/>, <see cref="Statement"/>,
    /// <see cref="ClassMember"/>, or <see cref="Attribute"/> for explicit scaffold control.
    /// </summary>
    [Obsolete("Use Expression(), Statement(), ClassMember(), or Attribute() for explicit scaffold control.")]
    public static CSharpTemplate Create(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies);
    }

    /// <inheritdoc cref="Create(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    [Obsolete("Use Expression(), Statement(), ClassMember(), or Attribute() for explicit scaffold control.")]
    public static CSharpTemplate Create(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(code, new Dictionary<string, object>(), usings, context, dependencies);
    }

    /// <summary>
    /// Create a template that produces an expression.
    /// Scaffolds as <c>class __T__ { object __v__ = &lt;code&gt;; }</c> and extracts the initializer.
    /// </summary>
    public static CSharpTemplate Expression(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies, ScaffoldKind.Expression);
    }

    /// <inheritdoc cref="Expression(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    public static CSharpTemplate Expression(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(code, new Dictionary<string, object>(), usings, context, dependencies, ScaffoldKind.Expression);
    }

    /// <summary>
    /// Create a template that produces a statement.
    /// Scaffolds as <c>class __T__ { void __M__() { &lt;code&gt;; } }</c> and extracts the statement.
    /// Unlike <see cref="Create(string, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>,
    /// this does not auto-unwrap ExpressionStatements.
    /// </summary>
    public static CSharpTemplate Statement(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies, ScaffoldKind.Statement);
    }

    /// <inheritdoc cref="Statement(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    public static CSharpTemplate Statement(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(code, new Dictionary<string, object>(), usings, context, dependencies, ScaffoldKind.Statement);
    }

    /// <summary>
    /// Create a template that produces a class member (method, field, property, etc.).
    /// Scaffolds as <c>class __T__ { &lt;code&gt; }</c> and extracts the first body member.
    /// </summary>
    public static CSharpTemplate ClassMember(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies, ScaffoldKind.ClassMember);
    }

    /// <inheritdoc cref="ClassMember(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    public static CSharpTemplate ClassMember(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(code, new Dictionary<string, object>(), usings, context, dependencies, ScaffoldKind.ClassMember);
    }

    /// <summary>
    /// Create a template that produces an attribute (C# <c>[Foo]</c>).
    /// Scaffolds as <c>class __T__ { [&lt;code&gt;] void __M__() {} }</c> and extracts the
    /// <see cref="Annotation"/> node with full type attribution.
    /// </summary>
    public static CSharpTemplate Attribute(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies, ScaffoldKind.Attribute);
    }

    /// <inheritdoc cref="Attribute(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    public static CSharpTemplate Attribute(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(code, new Dictionary<string, object>(), usings, context, dependencies, ScaffoldKind.Attribute);
    }

    /// <summary>
    /// Get the parsed template tree (cached after first parse).
    /// </summary>
    public J GetTree()
    {
        return _cachedTree ??= TemplateEngine.Parse(_code, _captures, _usings, _context, _dependencies, _scaffoldKind);
    }

    /// <summary>
    /// Apply this template, substituting captured values from a <see cref="MatchResult"/>
    /// and returning the generated AST node.
    /// </summary>
    /// <param name="cursor">The cursor pointing to the current location in the tree.</param>
    /// <param name="values">Optional match result providing values for captures.</param>
    /// <param name="coordinates">Optional coordinates specifying where to apply (defaults to Replace).</param>
    /// <returns>The generated AST node with substitutions and prefix applied, but not formatted.
    /// The caller is responsible for formatting, e.g. via
    /// <see cref="CSharpVisitor{P}.AutoFormat{T}"/> (deferred batch) or
    /// <see cref="AutoFormatExtensions.AutoFormat{T}"/> (immediate).</returns>
    public J? Apply(Cursor cursor, MatchResult? values = null,
        CSharpCoordinates? coordinates = null)
    {
        // Unwrap JRightPadded/JLeftPadded to get the J element if the cursor
        // points to a padding wrapper (common when replacing padded children)
        var cursorJ = UnwrapCursorValue(cursor.Value);
        var tree = GetTree();

        // Phase 1: placeholder substitution
        if (values != null)
        {
            tree = TemplateEngine.ApplySubstitutions(tree, values);
        }

        // Phase 1.5: auto-parenthesization after substitution
        if (tree is Expression expr && cursorJ != null)
        {
            tree = CSharpParenthesizeVisitor.MaybeParenthesize(expr, cursor);
        }

        // Phase 2: coordinate application (prefix preservation)
        if (coordinates != null)
        {
            tree = TemplateEngine.ApplyCoordinates(tree, cursor, coordinates);
        }
        else if (cursorJ != null)
        {
            tree = TemplateEngine.ApplyCoordinates(tree, cursor,
                CSharpCoordinates.Replace(cursorJ));
        }

        return tree;
    }

    /// <summary>
    /// Extract the J element from a cursor value, unwrapping JRightPadded/JLeftPadded
    /// wrappers if present. Returns null if the value is not a J or padding wrapper.
    /// </summary>
    private static J? UnwrapCursorValue(object? value)
    {
        if (value is J j) return j;

        // JRightPadded<T> and JLeftPadded<T> are generic, so use reflection
        // to extract the Element property
        var type = value?.GetType();
        if (type is { IsGenericType: true })
        {
            var genericDef = type.GetGenericTypeDefinition();
            if (genericDef == typeof(JRightPadded<>) || genericDef == typeof(JLeftPadded<>))
            {
                return type.GetProperty("Element")?.GetValue(value) as J;
            }
        }

        return null;
    }

    // ===============================================================
    // Rewrite — declarative pattern→template visitor factory
    // ===============================================================

    /// <summary>
    /// Create a <see cref="CSharpVisitor{ExecutionContext}"/> that matches a single pattern
    /// and applies a template via <see cref="TreeVisitor{T,P}.PostVisit"/>. The pattern's
    /// fast-reject ensures only nodes whose type matches the pattern root are fully compared.
    /// </summary>
    /// <example>
    /// <code>
    /// var expr = Capture.Expression();
    /// return CSharpTemplate.Rewrite(
    ///     CSharpPattern.Expression($"Console.Write({expr})"),
    ///     CSharpTemplate.Expression($"Console.WriteLine({expr})"));
    /// </code>
    /// </example>
    public static CSharpVisitor<ExecutionContext> Rewrite(CSharpPattern before, CSharpTemplate after) =>
        new RewriteVisitor([(before, after)]);

    /// <summary>
    /// Create a visitor that tries multiple patterns against a shared template. First match wins.
    /// </summary>
    /// <example>
    /// <code>
    /// var s = Capture.Expression("s", type: "string");
    /// return CSharpTemplate.Rewrite(
    ///     [
    ///         CSharpPattern.Expression($"{s} == null || {s} == \"\""),
    ///         CSharpPattern.Expression($"{s} == null || {s}.Length == 0"),
    ///     ],
    ///     CSharpTemplate.Expression($"string.IsNullOrEmpty({s})"));
    /// </code>
    /// </example>
    public static CSharpVisitor<ExecutionContext> Rewrite(CSharpPattern[] befores, CSharpTemplate after) =>
        new RewriteVisitor(Array.ConvertAll(befores, b => (b, after)));

    /// <summary>
    /// Create a visitor that tries multiple (pattern, template) pairs in order. First match wins.
    /// Use this when different patterns map to different templates.
    /// </summary>
    public static CSharpVisitor<ExecutionContext> Rewrite(
        params (CSharpPattern before, CSharpTemplate after)[] rules) =>
        new RewriteVisitor(rules);

    /// <summary>
    /// Creates a visitor that splices statements from any <see cref="Block"/> marked with
    /// <see cref="SyntheticBlockContainer"/> into its parent block. Register once via
    /// <see cref="TreeVisitor{T,P}.DoAfterVisit"/> — a single instance handles all
    /// synthetic blocks produced during the visit.
    /// </summary>
    /// <example>
    /// <code>
    /// var match = pat.Match(ret, Cursor);
    /// if (match != null)
    /// {
    ///     var result = tmpl.Apply(Cursor, values: match);
    ///     if (result is Block block &amp;&amp; block.Markers.FindFirst&lt;SyntheticBlockContainer&gt;() != null)
    ///     {
    ///         MaybeDoAfterVisit(CSharpTemplate.CreateBlockFlattener&lt;ExecutionContext&gt;());
    ///         return block;
    ///     }
    ///     return result ?? ret;
    /// }
    /// </code>
    /// </example>
    public static CSharpVisitor<P> CreateBlockFlattener<P>() => new BlockFlattener<P>();

    // ===============================================================
    // Implementation
    // ===============================================================

    private sealed class RewriteVisitor((CSharpPattern before, CSharpTemplate after)[] rules)
        : CSharpVisitor<ExecutionContext>
    {
        public override J? PostVisit(J tree, ExecutionContext ctx)
        {
            foreach (var (before, after) in rules)
            {
                var match = before.Match(tree, Cursor);
                if (match != null)
                {
                    var result = after.Apply(Cursor, values: match);
                    return result != null ? AutoFormat(result, ctx, Cursor) : null;
                }
            }
            return tree;
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
