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
using System.Collections.Concurrent;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Format;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Marker placed on a <see cref="Block"/> that is a synthetic container for multiple statements
/// produced by a multi-statement template, rather than a real block in the source code.
/// Used by <see cref="TemplateEngine.AutoFormat"/> to format each statement at the parent level,
/// and by <see cref="RoslynFormatter.DeferredFormatVisitor{P}"/> to identify blocks to flatten.
/// </summary>
public sealed class SyntheticBlockContainer : Marker
{
    public static readonly SyntheticBlockContainer Instance = new();
    public Guid Id { get; } = Guid.NewGuid();
}

/// <summary>
/// Controls how template code is wrapped in a scaffold and how the target node is extracted.
/// </summary>
internal enum ScaffoldKind
{
    /// <summary>Wraps as <c>class __T__ { object __v__ = &lt;code&gt;; }</c> and extracts the initializer expression.</summary>
    Expression,

    /// <summary>Wraps as <c>class __T__ { void __M__() { &lt;code&gt;; } }</c> and extracts the statement.</summary>
    Statement,

    /// <summary>Wraps as <c>class __T__ { &lt;code&gt; }</c> and extracts the class body member.</summary>
    ClassMember,

    /// <summary>Wraps as <c>class __T__ { [&lt;code&gt;] void __M__() {} }</c> and extracts the Annotation.</summary>
    Attribute,
}

/// <summary>
/// Core template parsing and caching engine.
/// Wraps template code in a parseable C# scaffold, parses with <see cref="CSharpParser"/>,
/// extracts the target AST node, and caches results.
/// </summary>
internal static class TemplateEngine
{
    private static readonly ConcurrentDictionary<string, J> GlobalCache = new();

    /// <summary>
    /// Parse a template code string into an AST node using the default scaffold
    /// (auto-detects expression vs statement).
    /// </summary>
    internal static J Parse(string code, IReadOnlyDictionary<string, object> captures,
        IReadOnlyList<string> usings, IReadOnlyList<string> context,
        IReadOnlyDictionary<string, string> dependencies)
    {
        return Parse(code, captures, usings, context, dependencies, scaffoldKind: null);
    }

    /// <summary>
    /// Parse a template code string into an AST node.
    /// The code is wrapped in a scaffold controlled by <paramref name="scaffoldKind"/>,
    /// parsed with <see cref="CSharpParser"/>, and the target node extracted.
    /// </summary>
    /// <param name="scaffoldKind">Controls how the code is wrapped and extracted.
    /// When null, uses the legacy Statement scaffold with auto-unwrap of ExpressionStatements.</param>
    /// <param name="dependencies">NuGet package dependencies (package name → version) for type attribution.</param>
    internal static J Parse(string code, IReadOnlyDictionary<string, object> captures,
        IReadOnlyList<string> usings, IReadOnlyList<string> context,
        IReadOnlyDictionary<string, string> dependencies, ScaffoldKind? scaffoldKind)
    {
        // Compute preamble first — it affects the scaffold shape and must be part of the cache key
        var preamble = BuildScaffoldPreamble(captures);
        var cacheKey = BuildCacheKey(code, preamble, usings, context, dependencies, scaffoldKind);
        if (GlobalCache.TryGetValue(cacheKey, out var cached))
            return cached;

        var result = ParseInternal(code, preamble, usings, context, dependencies, scaffoldKind);
        GlobalCache.TryAdd(cacheKey, result);
        return result;
    }

    private static J ParseInternal(string code, ScaffoldPreamble preamble,
        IReadOnlyList<string> usings, IReadOnlyList<string> context,
        IReadOnlyDictionary<string, string> dependencies, ScaffoldKind? scaffoldKind)
    {
        var scaffold = BuildScaffold(code, preamble, usings, context, scaffoldKind);
        var parser = new CSharpParser();

        // Always create a semantic model so the scaffold gets type attribution.
        // DependencyWorkspace includes .NET 6+ implicit usings (System,
        // System.Collections.Generic, etc.) via a synthetic global-using source file,
        // so common types resolve even without explicit usings in the pattern.
        // Reference resolution is cached, so repeated calls are cheap.
        var semanticModel = DependencyWorkspace.CreateSemanticModel(scaffold, dependencies);
        var cu = parser.Parse(scaffold, "__template__.cs", semanticModel);

        var result = ExtractTemplateNode(cu, code, scaffoldKind);

        // When the legacy scaffold (scaffoldKind == null) produces a VariableDeclarations,
        // C#'s parser likely misparsed an expression like `a * b` as a pointer declaration.
        // Re-parse using the Expression scaffold where the code appears in an initializer
        // context (`object __v__ = a * b;`) and the expression is unambiguous.
        if (scaffoldKind == null && result is VariableDeclarations)
        {
            return ParseInternal(code, preamble, usings, context, dependencies, ScaffoldKind.Expression);
        }

        return result;
    }

    /// <summary>
    /// Collects field declarations, type parameters, and where clauses from captures
    /// for scaffold generation.
    /// </summary>
    private sealed record ScaffoldPreamble(
        IReadOnlyList<string> Fields,
        IReadOnlyList<string> TypeParameterNames,
        IReadOnlyList<string> WhereClauses);

    /// <summary>
    /// Build the scaffold preamble from captures: field declarations for expression captures,
    /// type parameter names and where clauses from captures with <see cref="ICapture.TypeParameters"/>.
    /// </summary>
    private static ScaffoldPreamble BuildScaffoldPreamble(IReadOnlyDictionary<string, object> captures)
    {
        const System.Reflection.BindingFlags bindingFlags =
            System.Reflection.BindingFlags.Instance |
            System.Reflection.BindingFlags.Public |
            System.Reflection.BindingFlags.NonPublic;

        var fields = new List<string>();
        var typeParamNames = new List<string>();
        var whereClauses = new List<string>();
        // Track bounds per type parameter name for conflict detection
        var typeParamBounds = new Dictionary<string, string?>();

        foreach (var kvp in captures)
        {
            var kind = kvp.Value.GetType().GetProperty("Kind", bindingFlags)?.GetValue(kvp.Value);
            var placeholder = Placeholder.ToPlaceholder(kvp.Key);

            // Collect type parameters from captures that declare them
            if (kvp.Value is ICapture { TypeParameters: { } typeParams })
            {
                foreach (var tp in typeParams)
                {
                    // Each entry is either "TName" (unbounded) or "TName : Bound1, Bound2"
                    var colonIdx = tp.IndexOf(':');
                    string name;
                    string? bounds;
                    if (colonIdx >= 0)
                    {
                        name = tp[..colonIdx].Trim();
                        bounds = tp[(colonIdx + 1)..].Trim();
                    }
                    else
                    {
                        name = tp.Trim();
                        bounds = null;
                    }

                    if (typeParamBounds.TryGetValue(name, out var existingBounds))
                    {
                        // Same name already declared — check for conflicts
                        if (!string.Equals(existingBounds, bounds, StringComparison.Ordinal))
                            throw new InvalidOperationException(
                                $"Conflicting bounds for type parameter '{name}': " +
                                $"'{existingBounds ?? "(none)"}' vs '{bounds ?? "(none)"}'");
                    }
                    else
                    {
                        typeParamBounds[name] = bounds;
                        typeParamNames.Add(name);
                        if (bounds != null)
                            whereClauses.Add($"where {name} : {bounds}");
                    }
                }
            }

            if (kind is CaptureKind captureKind)
            {
                switch (captureKind)
                {
                    case CaptureKind.Expression:
                        var captureType = kvp.Value.GetType().GetProperty("Type")?.GetValue(kvp.Value) as string;
                        // Always emit a field declaration for expression captures so Roslyn
                        // knows the placeholder is a variable, not a type. Without this,
                        // `__plh_x__ * __plh_y__` is misparsed as a pointer declaration.
                        fields.Add($"{(string.IsNullOrEmpty(captureType) ? "object" : captureType)} {placeholder};");
                        break;
                    case CaptureKind.Type:
                        // TODO: emit scaffold that places placeholder in a type position
                        break;
                    case CaptureKind.Name:
                        // No preamble needed — pure identifier substitution
                        break;
                }
            }
            else
            {
                // Fallback for captures without Kind (shouldn't happen, but defensive)
                var captureType = kvp.Value.GetType().GetProperty("Type")?.GetValue(kvp.Value) as string;
                if (!string.IsNullOrEmpty(captureType))
                {
                    fields.Add($"{captureType} {placeholder};");
                }
            }
        }
        return new ScaffoldPreamble(fields, typeParamNames, whereClauses);
    }

    /// <summary>
    /// Build a parseable C# source from the template code.
    /// The scaffold shape is controlled by <paramref name="scaffoldKind"/>.
    /// </summary>
    private static string BuildScaffold(string code, ScaffoldPreamble preamble,
        IReadOnlyList<string> usings, IReadOnlyList<string> context, ScaffoldKind? scaffoldKind)
    {
        var sb = new System.Text.StringBuilder();

        foreach (var u in usings)
        {
            if (u.TrimStart().StartsWith("using "))
                sb.AppendLine(u);
            else
                sb.AppendLine($"using {u};");
        }

        foreach (var c in context)
        {
            sb.AppendLine(c);
        }

        // Emit class declaration with type parameters if any captures declare them
        sb.Append("class __T__");
        if (preamble.TypeParameterNames.Count > 0)
        {
            sb.Append('<');
            sb.Append(string.Join(", ", preamble.TypeParameterNames));
            sb.Append('>');
        }
        if (preamble.WhereClauses.Count > 0)
        {
            sb.Append(' ');
            sb.Append(string.Join(" ", preamble.WhereClauses));
        }
        sb.AppendLine(" {");

        // Typed capture declarations as class fields — in scope for all scaffold kinds
        foreach (var decl in preamble.Fields)
        {
            sb.Append("    ");
            sb.AppendLine(decl);
        }

        switch (scaffoldKind)
        {
            case ScaffoldKind.Expression:
                sb.Append("    object __v__ = ");
                sb.Append(code);
                sb.AppendLine(";");
                break;

            case ScaffoldKind.ClassMember:
                sb.Append("    ");
                sb.Append(code);
                var trimmedMember = code.TrimEnd();
                if (trimmedMember.Length > 0 && trimmedMember[^1] != ';' && trimmedMember[^1] != '}')
                    sb.Append(';');
                sb.AppendLine();
                break;

            case ScaffoldKind.Attribute:
                sb.Append("    [");
                sb.Append(code);
                sb.AppendLine("]");
                sb.AppendLine("    void __M__() {}");
                break;

            default: // null or Statement — legacy behavior
                sb.AppendLine("    void __M__() {");
                sb.Append("        ");
                sb.Append(code);
                var trimmed = code.TrimEnd();
                if (trimmed.Length > 0 && trimmed[^1] != ';' && trimmed[^1] != '}')
                    sb.Append(';');
                sb.AppendLine();
                sb.AppendLine("    }");
                break;
        }

        sb.AppendLine("}");

        return sb.ToString();
    }

    /// <summary>
    /// Extract the template AST node from the scaffold's compilation unit.
    /// Navigation strategy depends on <paramref name="scaffoldKind"/>.
    /// </summary>
    private static J ExtractTemplateNode(CompilationUnit cu, string originalCode, ScaffoldKind? scaffoldKind)
    {
        var classDecl = FindFirst<ClassDeclaration>(cu.Members);
        if (classDecl == null)
            throw new InvalidOperationException("Template scaffold did not produce a class declaration");

        switch (scaffoldKind)
        {
            case ScaffoldKind.Expression:
                return ExtractExpression(classDecl);

            case ScaffoldKind.Statement:
                return ExtractStatementStrict(classDecl);

            case ScaffoldKind.ClassMember:
                return ExtractClassMember(classDecl);

            case ScaffoldKind.Attribute:
                return ExtractAttribute(classDecl);

            default: // null — legacy behavior (auto-unwraps ExpressionStatement)
                return ExtractStatement(classDecl);
        }
    }

    /// <summary>
    /// Legacy extraction: navigate into method body, auto-unwrap ExpressionStatement.
    /// Falls back to Expression scaffold when the statement is a VariableDeclarations,
    /// which can happen when Roslyn misparsed an expression like <c>a * b</c> as a
    /// pointer declaration.
    /// </summary>
    private static J ExtractStatement(ClassDeclaration classDecl)
    {
        var methodDecl = FindFirst<MethodDeclaration>(classDecl.Body.Statements
            .Select(s => s.Element).ToList());
        if (methodDecl?.Body == null)
            throw new InvalidOperationException("Template scaffold did not produce a method declaration");

        var statements = methodDecl.Body.Statements;
        if (statements.Count == 0)
            throw new InvalidOperationException("Template code did not produce any statements");

        if (statements.Count == 1)
        {
            var stmt = statements[0].Element;
            // Unwrap ExpressionStatement to get the inner expression
            if (stmt is ExpressionStatement es)
                return StripPrefix(es.Expression);

            return StripPrefix(stmt);
        }

        // Multiple statements: return them as a Block
        return methodDecl.Body.WithMarkers(
            methodDecl.Body.Markers.Add(SyntheticBlockContainer.Instance));
    }

    /// <summary>
    /// Explicit statement extraction: does NOT unwrap ExpressionStatement.
    /// </summary>
    private static J ExtractStatementStrict(ClassDeclaration classDecl)
    {
        var methodDecl = FindFirst<MethodDeclaration>(classDecl.Body.Statements
            .Select(s => s.Element).ToList());
        if (methodDecl?.Body == null)
            throw new InvalidOperationException("Template scaffold did not produce a method declaration");

        var statements = methodDecl.Body.Statements;
        if (statements.Count == 0)
            throw new InvalidOperationException("Template code did not produce any statements");

        if (statements.Count == 1)
            return StripPrefix(statements[0].Element);

        return methodDecl.Body.WithMarkers(
            methodDecl.Body.Markers.Add(SyntheticBlockContainer.Instance));
    }

    /// <summary>
    /// Extract the initializer expression from <c>object __v__ = &lt;code&gt;;</c>.
    /// </summary>
    private static J ExtractExpression(ClassDeclaration classDecl)
    {
        // Find the scaffold field specifically: object __v__ = <code>;
        // Must skip preamble fields (typed capture declarations like "int __plh_x__;")
        foreach (var padded in classDecl.Body.Statements)
        {
            if (padded.Element is VariableDeclarations varDecl)
            {
                var namedVar = varDecl.Variables.FirstOrDefault()?.Element;
                if (namedVar?.Name.SimpleName == "__v__")
                {
                    var initializer = namedVar.Initializer;
                    if (initializer == null)
                        throw new InvalidOperationException("Template scaffold field __v__ has no initializer");
                    return StripPrefix(initializer.Element);
                }
            }
        }

        throw new InvalidOperationException("Template scaffold did not produce the __v__ field declaration");
    }

    /// <summary>
    /// Extract class body members from <c>class __T__ { &lt;code&gt; }</c>,
    /// filtering out preamble fields (typed capture declarations).
    /// Returns a single member if there is one, or the body block if there are multiple.
    /// </summary>
    private static J ExtractClassMember(ClassDeclaration classDecl)
    {
        var members = classDecl.Body.Statements
            .Where(s => !IsPreambleField(s.Element))
            .ToList();

        if (members.Count == 0)
            throw new InvalidOperationException("Template code did not produce any class members");

        if (members.Count == 1)
            return StripPrefix(members[0].Element);

        // Multiple members: return the class body with preamble fields removed
        return classDecl.Body.WithStatements(members);
    }

    /// <summary>
    /// Extract the Annotation from <c>class __T__ { [&lt;code&gt;] void __M__() {} }</c>.
    /// The parser wraps the annotated method in <see cref="AnnotatedStatement"/>.
    /// </summary>
    private static J ExtractAttribute(ClassDeclaration classDecl)
    {
        // Find the AnnotatedStatement wrapping __M__
        foreach (var padded in classDecl.Body.Statements)
        {
            if (padded.Element is AnnotatedStatement annotated)
            {
                if (annotated.AttributeLists.Count == 0)
                    throw new InvalidOperationException(
                        "Template scaffold produced an annotated method with no attribute lists");

                var attrList = annotated.AttributeLists[0];
                if (attrList.Attributes.Count == 0)
                    throw new InvalidOperationException(
                        "Template scaffold produced an attribute list with no attributes");

                return StripPrefix(attrList.Attributes[0].Element);
            }
        }

        throw new InvalidOperationException(
            "Template scaffold did not produce an annotated method. " +
            "Ensure the attribute syntax is valid C# (e.g., \"Test\" not \"[Test]\").");
    }

    /// <summary>
    /// Extract the J element from a JRightPadded or JLeftPadded wrapper via reflection.
    /// Returns null if the value is not a padding wrapper or doesn't contain a J element.
    /// </summary>
    private static J? UnwrapPaddingElement(object? value)
    {
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

    /// <summary>
    /// Check if a statement is a preamble field (typed capture declaration with placeholder name).
    /// </summary>
    private static bool IsPreambleField(Statement stmt)
    {
        if (stmt is VariableDeclarations varDecl)
        {
            var name = varDecl.Variables.FirstOrDefault()?.Element.Name;
            if (name != null && Placeholder.FromPlaceholder(name.SimpleName) != null)
                return true;
        }
        return false;
    }

    private static T? FindFirst<T>(IList<Statement> statements) where T : Statement
    {
        foreach (var stmt in statements)
        {
            if (stmt is T t) return t;
        }
        return default;
    }

    private static T? FindFirst<T>(IList<JRightPadded<Statement>> statements) where T : Statement
    {
        foreach (var padded in statements)
        {
            if (padded.Element is T t) return t;
        }
        return default;
    }

    /// <summary>
    /// Strip the scaffold-induced prefix from the extracted node,
    /// replacing it with an empty prefix.
    /// </summary>
    private static J StripPrefix(J node)
    {
        // ExpressionStatement delegates prefix to its inner expression
        if (node is ExpressionStatement es)
            return es.WithExpression((Expression)StripPrefix(es.Expression));

        return J.SetPrefix(node, Space.Empty);
    }

    /// <summary>
    /// Apply placeholder substitutions, replacing placeholder identifiers
    /// with the corresponding captured AST nodes.
    /// </summary>
    internal static J ApplySubstitutions(J tree, MatchResult values)
    {
        var visitor = new SubstitutionVisitor(values);
        visitor.Cursor = new Cursor(null, Cursor.ROOT_VALUE);
        tree = visitor.VisitNonNull(tree, 0);

        // Auto-parenthesize substituted expressions that may need parens in their
        // new context within the template (e.g., a || b substituted into && position)
        var parenthesizer = new CSharpParenthesizeVisitor<int>();
        parenthesizer.Cursor = new Cursor(null, Cursor.ROOT_VALUE);
        tree = parenthesizer.VisitNonNull(tree, 0);

        return tree;
    }

    /// <summary>
    /// Apply coordinate-based insertion, preserving the original node's prefix.
    /// </summary>
    internal static J ApplyCoordinates(J tree, Cursor cursor, CSharpCoordinates coords)
    {
        return coords.Mode switch
        {
            CoordinateMode.Replace => PreservePrefix(tree, coords.Tree),
            // Before/After modes require more complex tree surgery
            // that depends on the parent context. For now, just return the tree.
            _ => tree
        };
    }

    /// <summary>
    /// Transfer the prefix from the original node to the replacement node.
    /// </summary>
    private static J PreservePrefix(J replacement, J original)
    {
        return J.SetPrefix(replacement, original.Prefix);
    }

    /// <summary>
    /// Auto-format the template result within the enclosing compilation unit.
    /// Delegates to <see cref="RoslynFormatter.FormatSubtree"/> for the splice→format→extract pipeline.
    /// The prefix set in the coordinates phase is preserved — the formatter only affects
    /// internal whitespace (argument spacing, etc.), matching the JS/TS approach.
    /// </summary>
    internal static J AutoFormat(J tree, Cursor cursor)
    {
        var cu = cursor.FirstEnclosing<CompilationUnit>();
        if (cu == null)
            return tree;

        // The cursor's value is the original node being replaced.
        // Unwrap JRightPadded/JLeftPadded if the cursor points to a padding wrapper.
        var original = cursor.Value as J ?? UnwrapPaddingElement(cursor.Value);
        if (original == null)
            return tree;

        // Synthetic block containers hold multiple statements that will be spliced
        // into the parent block. Format each statement individually at the parent level
        // so Roslyn sees them as direct siblings, not block-internal children.
        if (tree is Block blk && blk.Markers.FindFirst<SyntheticBlockContainer>() != null)
            return AutoFormatSyntheticBlock(blk, cu, original);

        return AutoFormatSingle(tree, cu, original);
    }

    private static J AutoFormatSingle(J tree, CompilationUnit cu, J original)
    {
        // Save the prefix set by ApplyCoordinates — the formatter may override it
        var preservedPrefix = tree.Prefix;

        // Assign a fresh ID so FindById targets exactly this instance,
        // not a stale node from a prior application of the same template
        tree = J.SetId(tree, Guid.NewGuid());

        var formatted = RoslynFormatter.FormatSubtree(cu, original.Id, tree, stopAfter: null);

        // Restore the coordinate-set prefix. The formatter handles internal whitespace;
        // the prefix (indentation/newlines before the node) comes from the original tree.
        return J.SetPrefix(formatted, preservedPrefix);
    }

    /// <summary>
    /// Format each statement in a synthetic block individually, splicing each one
    /// into the original node's position so Roslyn formats it at the parent level.
    /// </summary>
    private static Block AutoFormatSyntheticBlock(Block blk, CompilationUnit cu, J original)
    {
        var preservedBlockPrefix = blk.Prefix;
        var formattedStmts = new List<JRightPadded<Statement>>(blk.Statements.Count);

        foreach (var s in blk.Statements)
        {
            var stmt = (J)s.Element;
            stmt = J.SetId(stmt, Guid.NewGuid());
            var formatted = RoslynFormatter.FormatSubtree(cu, original.Id, stmt, stopAfter: null);
            formattedStmts.Add(s.WithElement((Statement)formatted));
        }

        return blk.WithStatements(formattedStmts).WithPrefix(preservedBlockPrefix);
    }

    private static string BuildCacheKey(string code, ScaffoldPreamble preamble,
        IReadOnlyList<string> usings, IReadOnlyList<string> context,
        IReadOnlyDictionary<string, string> dependencies, ScaffoldKind? scaffoldKind = null)
    {
        var sb = new System.Text.StringBuilder();
        if (scaffoldKind != null)
        {
            sb.Append("scaffold:");
            sb.Append(scaffoldKind);
            sb.Append('|');
        }
        sb.Append("code:");
        sb.Append(code);

        if (preamble.Fields.Count > 0)
        {
            sb.Append("|preamble:");
            sb.Append(string.Join(",", preamble.Fields));
        }

        if (preamble.TypeParameterNames.Count > 0)
        {
            sb.Append("|typeParams:");
            sb.Append(string.Join(",", preamble.TypeParameterNames));
        }

        if (preamble.WhereClauses.Count > 0)
        {
            sb.Append("|where:");
            sb.Append(string.Join(",", preamble.WhereClauses));
        }

        if (usings.Count > 0)
        {
            sb.Append("|usings:");
            sb.Append(string.Join(",", usings));
        }
        if (context.Count > 0)
        {
            sb.Append("|context:");
            sb.Append(string.Join(",", context));
        }
        if (dependencies.Count > 0)
        {
            sb.Append("|deps:");
            sb.Append(string.Join(",", dependencies.OrderBy(d => d.Key)
                .Select(d => $"{d.Key}={d.Value}")));
        }
        return sb.ToString();
    }
}

/// <summary>
/// Visitor that replaces placeholder identifiers with captured AST nodes.
/// </summary>
internal class SubstitutionVisitor : CSharpVisitor<int>
{
    private readonly MatchResult _values;

    public SubstitutionVisitor(MatchResult values)
    {
        _values = values;
    }

    public override J VisitIdentifier(Identifier identifier, int p)
    {
        var captureName = Placeholder.FromPlaceholder(identifier.SimpleName);
        if (captureName != null && _values.Has(captureName))
        {
            var captured = _values.Get<J>(captureName);
            if (captured != null)
            {
                // Preserve the placeholder's prefix on the captured node
                return J.SetPrefix(captured, identifier.Prefix);
            }
        }
        return base.VisitIdentifier(identifier, p);
    }

    public override J VisitAnnotation(Annotation annotation, int p)
    {
        annotation = (Annotation)base.VisitAnnotation(annotation, p);
        annotation = ExpandVariadicAnnotationArgs(annotation);
        return annotation;
    }

    public override J VisitMethodInvocation(MethodInvocation mi, int p)
    {
        // Check if the select is a capture placeholder BEFORE substitution
        var selectCaptureName = mi.Select?.Element is Identifier selectId
            ? Placeholder.FromPlaceholder(selectId.SimpleName)
            : null;

        mi = (MethodInvocation)base.VisitMethodInvocation(mi, p);

        // Substitute placeholder in method name position
        var namePlaceholder = Placeholder.FromPlaceholder(mi.Name.SimpleName);
        if (namePlaceholder != null && _values.Has(namePlaceholder))
        {
            var captured = _values.Get<Identifier>(namePlaceholder);
            if (captured != null)
            {
                mi = mi.WithName(captured.WithPrefix(mi.Name.Prefix));
            }
        }

        // Transfer NullSafe from matched tree when the capture was a null-conditional select
        if (selectCaptureName != null && mi.Markers.FindFirst<NullSafe>() == null)
        {
            var nullSafe = _values.GetNullSafe(selectCaptureName);
            if (nullSafe != null)
                mi = mi.WithMarkers(mi.Markers.Add(nullSafe));
        }

        // Substitute variadic placeholder in arguments
        mi = ExpandVariadicArgs(mi);

        return mi;
    }

    public override J VisitFieldAccess(FieldAccess fieldAccess, int p)
    {
        // Check if the target is a capture placeholder BEFORE substitution
        var targetCaptureName = fieldAccess.Target is Identifier targetId
            ? Placeholder.FromPlaceholder(targetId.SimpleName)
            : null;

        fieldAccess = (FieldAccess)base.VisitFieldAccess(fieldAccess, p);

        // Substitute placeholder in field name position
        var nameIdent = fieldAccess.Name.Element;
        var namePlaceholder = Placeholder.FromPlaceholder(nameIdent.SimpleName);
        if (namePlaceholder != null && _values.Has(namePlaceholder))
        {
            var captured = _values.Get<Identifier>(namePlaceholder);
            if (captured != null)
            {
                fieldAccess = fieldAccess.WithName(
                    fieldAccess.Name.WithElement(captured.WithPrefix(nameIdent.Prefix)));
            }
        }

        // Transfer NullSafe from matched tree when the capture was a null-conditional target
        if (targetCaptureName != null && fieldAccess.Markers.FindFirst<NullSafe>() == null)
        {
            var nullSafe = _values.GetNullSafe(targetCaptureName);
            if (nullSafe != null)
                fieldAccess = fieldAccess.WithMarkers(fieldAccess.Markers.Add(nullSafe));
        }

        return fieldAccess;
    }

    public override J VisitArrayAccess(ArrayAccess arrayAccess, int p)
    {
        // Check if the indexed expression is a capture placeholder BEFORE substitution
        var indexedCaptureName = arrayAccess.Indexed is Identifier indexedId
            ? Placeholder.FromPlaceholder(indexedId.SimpleName)
            : null;

        arrayAccess = (ArrayAccess)base.VisitArrayAccess(arrayAccess, p);

        // Transfer NullSafe from matched tree when the capture was a null-conditional indexed expr
        if (indexedCaptureName != null && arrayAccess.Markers.FindFirst<NullSafe>() == null)
        {
            var nullSafe = _values.GetNullSafe(indexedCaptureName);
            if (nullSafe != null)
                arrayAccess = arrayAccess.WithMarkers(arrayAccess.Markers.Add(nullSafe));
        }

        return arrayAccess;
    }

    /// <summary>
    /// If any argument is a placeholder identifier bound to a variadic capture (list),
    /// expand it into the argument list.
    /// </summary>
    private MethodInvocation ExpandVariadicArgs(MethodInvocation mi)
    {
        var elements = mi.Arguments.Elements;
        List<JRightPadded<Expression>>? expanded = null;

        for (int i = 0; i < elements.Count; i++)
        {
            var arg = elements[i].Element;
            if (arg is Identifier ident)
            {
                var captureName = Placeholder.FromPlaceholder(ident.SimpleName);
                if (captureName != null && _values.Has(captureName))
                {
                    var capturedList = _values.GetList<Expression>(captureName);
                    if (expanded == null)
                    {
                        expanded = new List<JRightPadded<Expression>>();
                        for (int k = 0; k < i; k++)
                            expanded.Add(elements[k]);
                    }
                    // Expand captured args (may be empty, removing the placeholder)
                    for (int j = 0; j < capturedList.Count; j++)
                    {
                        var capturedArg = capturedList[j];
                        // First element inherits the placeholder's prefix
                        if (j == 0)
                            capturedArg = J.SetPrefix(capturedArg, ident.Prefix);
                        expanded.Add(new JRightPadded<Expression>(
                            capturedArg, Space.Empty, Markers.Empty));
                    }
                    continue;
                }
            }
            expanded?.Add(elements[i]);
        }

        if (expanded != null)
        {
            mi = mi.WithArguments(mi.Arguments.WithElements(expanded));
        }

        return mi;
    }

    /// <summary>
    /// If any argument in an Annotation is a placeholder identifier bound to a variadic capture,
    /// expand it into the argument list. When all arguments expand to empty, remove the
    /// Arguments container entirely (producing an attribute with no parentheses).
    /// </summary>
    private Annotation ExpandVariadicAnnotationArgs(Annotation annotation)
    {
        if (annotation.Arguments == null)
            return annotation;

        var elements = annotation.Arguments.Elements;
        List<JRightPadded<Expression>>? expanded = null;

        for (int i = 0; i < elements.Count; i++)
        {
            var arg = elements[i].Element;
            if (arg is Identifier ident)
            {
                var captureName = Placeholder.FromPlaceholder(ident.SimpleName);
                if (captureName != null && _values.Has(captureName))
                {
                    var capturedList = _values.GetList<Expression>(captureName);
                    if (expanded == null)
                    {
                        expanded = new List<JRightPadded<Expression>>();
                        for (int k = 0; k < i; k++)
                            expanded.Add(elements[k]);
                    }
                    for (int j = 0; j < capturedList.Count; j++)
                    {
                        var capturedArg = capturedList[j];
                        if (j == 0)
                            capturedArg = J.SetPrefix(capturedArg, ident.Prefix);
                        expanded.Add(new JRightPadded<Expression>(
                            capturedArg, Space.Empty, Markers.Empty));
                    }
                    continue;
                }
            }
            expanded?.Add(elements[i]);
        }

        if (expanded != null)
        {
            if (expanded.Count == 0)
            {
                // All variadic captures were empty — remove parentheses entirely
                annotation = annotation.WithArguments(null);
            }
            else
            {
                annotation = annotation.WithArguments(annotation.Arguments.WithElements(expanded));
            }
        }

        return annotation;
    }
}
