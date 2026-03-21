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
///
/// // Template with NuGet dependencies for type attribution
/// var tmpl = CSharpTemplate.Create(
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
    /// Create a template from an interpolated string containing <see cref="Capture{T}"/>
    /// and/or <see cref="Raw"/> placeholders.
    /// </summary>
    /// <param name="handler">The interpolated string handler that extracts captures.</param>
    /// <param name="usings">Optional using directives for the scaffold.</param>
    /// <param name="context">Optional context lines emitted before the scaffold class.</param>
    /// <param name="dependencies">Optional NuGet package dependencies (package name → version)
    /// required for import resolution and type attribution.</param>
    public static CSharpTemplate Create(TemplateStringHandler handler,
        IReadOnlyList<string>? usings = null, IReadOnlyList<string>? context = null,
        IReadOnlyDictionary<string, string>? dependencies = null)
    {
        return new CSharpTemplate(handler.GetCode(), handler.GetCaptures(), usings, context, dependencies);
    }

    /// <summary>
    /// Create a template from a plain string (no captures).
    /// </summary>
    /// <param name="code">The template code string.</param>
    /// <param name="usings">Optional using directives for the scaffold.</param>
    /// <param name="context">Optional context lines emitted before the scaffold class.</param>
    /// <param name="dependencies">Optional NuGet package dependencies (package name → version)
    /// required for import resolution and type attribution.</param>
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
    /// <returns>The generated AST node with substitutions applied.</returns>
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

        // Phase 3: auto-format within the enclosing compilation unit
        tree = TemplateEngine.AutoFormat(tree, cursor);

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
}
