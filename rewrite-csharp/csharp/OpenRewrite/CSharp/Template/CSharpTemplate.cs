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
/// A parsed and cacheable template for generating C# AST nodes.
/// Templates are created using C# string interpolation with <see cref="Capture{T}"/> placeholders,
/// providing the C# equivalent of JavaScript's tagged template literals.
/// </summary>
/// <example>
/// <code>
/// // Simple template (no captures)
/// var tmpl = CSharpTemplate.Create($"Console.WriteLine(\"hello\")");
/// var result = tmpl.Apply(cursor);
///
/// // Template with captures from pattern match
/// var expr = Capture.Of&lt;Expression&gt;("expr");
/// var tmpl = CSharpTemplate.Create($"Console.WriteLine({expr})");
/// var result = tmpl.Apply(cursor, values: match);
///
/// // Template with usings
/// var tmpl = CSharpTemplate.Create(
///     $"JsonSerializer.Serialize({expr})",
///     usings: ["System.Text.Json"]);
/// </code>
/// </example>
public sealed class CSharpTemplate
{
    private readonly string _code;
    private readonly IReadOnlyDictionary<string, object> _captures;
    private readonly IReadOnlyList<string> _usings;
    private readonly IReadOnlyList<string> _context;
    private J? _cachedTree;

    private CSharpTemplate(string code, Dictionary<string, object> captures,
        IReadOnlyList<string>? usings, IReadOnlyList<string>? context)
    {
        _code = code;
        _captures = captures;
        _usings = usings ?? [];
        _context = context ?? [];
    }

    /// <summary>
    /// Create a template from an interpolated string containing <see cref="Capture{T}"/>
    /// and/or <see cref="Raw"/> placeholders.
    /// </summary>
    public static CSharpTemplate Create(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null)
    {
        return new CSharpTemplate(handler.GetCode(), handler.GetCaptures(), usings, context);
    }

    /// <summary>
    /// Create a template from a plain string (no captures).
    /// </summary>
    public static CSharpTemplate Create(string code,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null)
    {
        return new CSharpTemplate(code, new Dictionary<string, object>(), usings, context);
    }

    /// <summary>
    /// Get the parsed template tree (cached after first parse).
    /// </summary>
    public J GetTree()
    {
        return _cachedTree ??= TemplateEngine.Parse(_code, _captures, _usings, _context);
    }

    /// <summary>
    /// Apply this template, substituting captured values from a <see cref="MatchResult"/>
    /// and returning the generated AST node.
    /// </summary>
    /// <param name="cursor">The cursor pointing to the current location in the tree.</param>
    /// <param name="values">Optional match result providing values for captures.</param>
    /// <param name="coordinates">Optional coordinates specifying where to apply (defaults to Replace).</param>
    /// <returns>The generated AST node with substitutions applied.</returns>
    public J? Apply(Cursor cursor, MatchResult? values = null,
        CSharpCoordinates? coordinates = null)
    {
        var tree = GetTree();

        // Phase 1: placeholder substitution
        if (values != null)
        {
            tree = TemplateEngine.ApplySubstitutions(tree, values);
        }

        // Phase 2: coordinate application (prefix preservation)
        if (coordinates != null)
        {
            tree = TemplateEngine.ApplyCoordinates(tree, cursor, coordinates);
        }
        else if (cursor.Value is J cursorValue)
        {
            tree = TemplateEngine.ApplyCoordinates(tree, cursor,
                CSharpCoordinates.Replace(cursorValue));
        }

        return tree;
    }
}
