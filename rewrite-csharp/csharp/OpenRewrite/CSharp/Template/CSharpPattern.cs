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

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// A structural pattern for matching against C# AST nodes.
/// Patterns are created using C# string interpolation with <see cref="Capture{T}"/> placeholders.
/// </summary>
/// <example>
/// <code>
/// var expr = Capture.Of&lt;Expression&gt;("expr");
/// var pat = CSharpPattern.Expression($"Console.Write({expr})");
///
/// if (pat.Match(methodInvocation, cursor) is { } match)
/// {
///     var capturedExpr = match.Get(expr);
/// }
/// </code>
/// </example>
public sealed class CSharpPattern
{
    private readonly string _code;
    private readonly IReadOnlyDictionary<string, object> _captures;
    private readonly IReadOnlyList<string> _usings;
    private readonly IReadOnlyList<string> _context;
    private readonly IReadOnlyDictionary<string, string> _dependencies;
    private readonly ScaffoldKind? _scaffoldKind;
    private J? _cachedTree;

    private CSharpPattern(string code, Dictionary<string, object> captures,
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
    /// Create a pattern with auto-detected scaffold kind.
    /// Prefer <see cref="Expression"/>, <see cref="Statement"/>,
    /// <see cref="ClassMember"/>, or <see cref="Attribute"/> for explicit scaffold control.
    /// </summary>
    [Obsolete("Use Expression(), Statement(), ClassMember(), or Attribute() for explicit scaffold control.")]
    public static CSharpPattern Create(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies);
    }

    /// <inheritdoc cref="Create(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    [Obsolete("Use Expression(), Statement(), ClassMember(), or Attribute() for explicit scaffold control.")]
    public static CSharpPattern Create(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(code, new Dictionary<string, object>(), usings, context, dependencies);
    }

    /// <summary>
    /// Create a pattern that matches an expression.
    /// </summary>
    public static CSharpPattern Expression(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies, ScaffoldKind.Expression);
    }

    /// <inheritdoc cref="Expression(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    public static CSharpPattern Expression(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(code, new Dictionary<string, object>(), usings, context, dependencies, ScaffoldKind.Expression);
    }

    /// <summary>
    /// Create a pattern that matches a statement.
    /// </summary>
    public static CSharpPattern Statement(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies, ScaffoldKind.Statement);
    }

    /// <inheritdoc cref="Statement(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    public static CSharpPattern Statement(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(code, new Dictionary<string, object>(), usings, context, dependencies, ScaffoldKind.Statement);
    }

    /// <summary>
    /// Create a pattern that matches a class member (method, field, property, etc.).
    /// </summary>
    public static CSharpPattern ClassMember(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies, ScaffoldKind.ClassMember);
    }

    /// <inheritdoc cref="ClassMember(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    public static CSharpPattern ClassMember(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(code, new Dictionary<string, object>(), usings, context, dependencies, ScaffoldKind.ClassMember);
    }

    /// <summary>
    /// Create a pattern that matches an attribute (C# <c>[Foo]</c>).
    /// </summary>
    public static CSharpPattern Attribute(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies, ScaffoldKind.Attribute);
    }

    /// <inheritdoc cref="Attribute(TemplateStringHandler, IReadOnlyList{string}?, IReadOnlyList{string}?, IReadOnlyDictionary{string, string}?)"/>
    public static CSharpPattern Attribute(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpPattern(code, new Dictionary<string, object>(), usings, context, dependencies, ScaffoldKind.Attribute);
    }

    /// <summary>
    /// Get the parsed pattern tree (cached after first parse).
    /// </summary>
    public J GetTree()
    {
        return _cachedTree ??= TemplateEngine.Parse(_code, _captures, _usings, _context, _dependencies, _scaffoldKind);
    }

    /// <summary>
    /// Match this pattern against an AST node.
    /// Returns a <see cref="MatchResult"/> if matched, null otherwise.
    /// </summary>
    public MatchResult? Match(J tree, Cursor cursor)
    {
        var patternTree = GetTree();

        // Fast reject: if the pattern root is not a capture placeholder and the
        // candidate is a different node type, no match is possible — unless the
        // comparator has a known cross-type equivalence (e.g. Binary ↔ IsPattern).
        // This avoids allocating a PatternMatchingComparator for the common non-matching case.
        if (patternTree.GetType() != tree.GetType()
            && !IsCapturePlaceholder(patternTree)
            && !PatternMatchingComparator.HasCrossTypeEquivalence(patternTree, tree))
            return null;

        var comparator = new PatternMatchingComparator(_captures);
        var captured = comparator.Match(patternTree, tree, cursor);
        return captured != null ? new MatchResult(captured) : null;
    }

    private bool IsCapturePlaceholder(J node)
    {
        return node is Identifier id
               && Placeholder.FromPlaceholder(id.SimpleName) is { } name
               && _captures.ContainsKey(name);
    }

    /// <summary>
    /// Check if this pattern matches (without capturing).
    /// </summary>
    public bool Matches(J tree, Cursor cursor) => Match(tree, cursor) != null;

    /// <summary>
    /// If this pattern matches the given tree node, return the node with a
    /// <see cref="SearchResult"/> marker added. Otherwise return the node unchanged.
    /// This is the search-only equivalent of template apply — it marks found
    /// syntax so that <c>/*~~&gt;*/</c> appears in printed output.
    /// </summary>
    public T Find<T>(T tree, Cursor cursor, string? description = null) where T : J
    {
        return Match(tree, cursor) != null ? SearchResult.Found(tree, description) : tree;
    }
}
