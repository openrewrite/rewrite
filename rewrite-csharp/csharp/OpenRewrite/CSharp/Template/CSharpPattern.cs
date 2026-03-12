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
/// var pat = CSharpPattern.Create($"Console.Write({expr})");
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
    private J? _cachedTree;

    private CSharpPattern(string code, Dictionary<string, object> captures,
        IReadOnlyList<string>? usings, IReadOnlyList<string>? context)
    {
        _code = code;
        _captures = captures;
        _usings = usings ?? [];
        _context = context ?? [];
    }

    /// <summary>
    /// Create a pattern from an interpolated string containing <see cref="Capture{T}"/> placeholders.
    /// </summary>
    public static CSharpPattern Create(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null)
    {
        return new CSharpPattern(handler.GetCode(), handler.GetCaptures(), usings, context);
    }

    /// <summary>
    /// Create a pattern from a plain string (no captures — useful for exact matching).
    /// </summary>
    public static CSharpPattern Create(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null)
    {
        return new CSharpPattern(code, new Dictionary<string, object>(), usings, context);
    }

    /// <summary>
    /// Get the parsed pattern tree (cached after first parse).
    /// </summary>
    public J GetTree()
    {
        return _cachedTree ??= TemplateEngine.Parse(_code, _captures, _usings, _context);
    }

    /// <summary>
    /// Match this pattern against an AST node.
    /// Returns a <see cref="MatchResult"/> if matched, null otherwise.
    /// </summary>
    public MatchResult? Match(J tree, Cursor cursor)
    {
        var patternTree = GetTree();
        var comparator = new PatternMatchingComparator(_captures);
        var captured = comparator.Match(patternTree, tree, cursor);
        return captured != null ? new MatchResult(captured) : null;
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
