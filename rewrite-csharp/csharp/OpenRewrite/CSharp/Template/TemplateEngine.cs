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
/// Core template parsing and caching engine.
/// Wraps template code in a parseable C# scaffold, parses with <see cref="CSharpParser"/>,
/// extracts the target AST node, and caches results.
/// </summary>
internal static class TemplateEngine
{
    private static readonly ConcurrentDictionary<string, J> GlobalCache = new();

    /// <summary>
    /// Parse a template code string into an AST node.
    /// The code is wrapped in a scaffold to make it parseable, then the inner
    /// expression or statement is extracted.
    /// </summary>
    /// <param name="dependencies">NuGet package dependencies (package name → version) for type attribution.
    /// When provided, a dependency workspace will be created so Roslyn can resolve external types.</param>
    internal static J Parse(string code, IReadOnlyDictionary<string, object> captures,
        IReadOnlyList<string> usings, IReadOnlyList<string> context,
        IReadOnlyDictionary<string, string> dependencies)
    {
        var cacheKey = BuildCacheKey(code, captures, usings, context, dependencies);
        if (GlobalCache.TryGetValue(cacheKey, out var cached))
            return cached;

        var result = ParseInternal(code, captures, usings, context, dependencies);
        GlobalCache.TryAdd(cacheKey, result);
        return result;
    }

    private static J ParseInternal(string code, IReadOnlyDictionary<string, object> captures,
        IReadOnlyList<string> usings, IReadOnlyList<string> context,
        IReadOnlyDictionary<string, string> dependencies)
    {
        var preamble = BuildTypePreamble(captures);
        var scaffold = BuildScaffold(code, preamble, usings, context);
        // TODO: when dependencies are provided, use DependencyWorkspace to create a
        // project with NuGet references so Roslyn can resolve external types
        var parser = new CSharpParser();

        CompilationUnit cu;
        if (dependencies.Count > 0)
        {
            var semanticModel = DependencyWorkspace.CreateSemanticModel(scaffold, dependencies);
            cu = parser.Parse(scaffold, "__template__.cs", semanticModel);
        }
        else
        {
            cu = parser.Parse(scaffold, "__template__.cs");
        }

        return ExtractTemplateNode(cu, code);
    }

    /// <summary>
    /// Build typed field declarations for captures that have a Type.
    /// These are emitted as class fields on the scaffold class so they are in scope
    /// inside the method body. This avoids mixing preamble statements with the template
    /// code, so <see cref="ExtractTemplateNode"/> doesn't need to skip anything.
    /// Dispatches on <see cref="CaptureKind"/> to generate the right scaffold form.
    /// </summary>
    private static List<string> BuildTypePreamble(IReadOnlyDictionary<string, object> captures)
    {
        var preamble = new List<string>();
        foreach (var kvp in captures)
        {
            var kind = kvp.Value.GetType().GetProperty("Kind")?.GetValue(kvp.Value);
            var placeholder = Placeholder.ToPlaceholder(kvp.Key);

            if (kind is CaptureKind captureKind)
            {
                switch (captureKind)
                {
                    case CaptureKind.Expression:
                        var captureType = kvp.Value.GetType().GetProperty("Type")?.GetValue(kvp.Value) as string;
                        if (!string.IsNullOrEmpty(captureType))
                        {
                            preamble.Add($"{captureType} {placeholder};");
                        }
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
                    preamble.Add($"{captureType} {placeholder};");
                }
            }
        }
        return preamble;
    }

    /// <summary>
    /// Build a parseable C# source from the template code.
    /// Typed capture declarations are emitted as class fields (before the method),
    /// so the method body contains only the template code.
    /// </summary>
    private static string BuildScaffold(string code, IReadOnlyList<string> preamble,
        IReadOnlyList<string> usings, IReadOnlyList<string> context)
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

        sb.AppendLine("class __T__ {");

        // Typed capture declarations as class fields — in scope inside the method
        foreach (var decl in preamble)
        {
            sb.Append("    ");
            sb.AppendLine(decl);
        }

        sb.AppendLine("    void __M__() {");
        sb.Append("        ");
        sb.Append(code);
        // Add semicolon if the code doesn't already end with one or with a block
        var trimmed = code.TrimEnd();
        if (trimmed.Length > 0 && trimmed[^1] != ';' && trimmed[^1] != '}')
            sb.Append(';');
        sb.AppendLine();
        sb.AppendLine("    }");
        sb.AppendLine("}");

        return sb.ToString();
    }

    /// <summary>
    /// Extract the template AST node from the scaffold's compilation unit.
    /// Navigates: CompilationUnit → ClassDeclaration → Body → MethodDeclaration → Body → Statement(s)
    /// If the statement is an ExpressionStatement, unwrap to get the Expression.
    /// </summary>
    private static J ExtractTemplateNode(CompilationUnit cu, string originalCode)
    {
        // Navigate into the class body
        var classDecl = FindFirst<ClassDeclaration>(cu.Members);
        if (classDecl == null)
            throw new InvalidOperationException("Template scaffold did not produce a class declaration");

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
        return methodDecl.Body;
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
        return visitor.VisitNonNull(tree, 0);
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
    /// </summary>
    internal static J AutoFormat(J tree, Cursor cursor)
    {
        var cu = cursor.FirstEnclosing<CompilationUnit>();
        if (cu == null)
            return tree;

        // The cursor's value is the original node being replaced
        if (cursor.Value is not J original)
            return tree;

        // Assign a fresh ID so FindById targets exactly this instance,
        // not a stale node from a prior application of the same template
        tree = J.SetId(tree, Guid.NewGuid());

        return RoslynFormatter.FormatSubtree(cu, original.Id, tree, stopAfter: null);
    }

    private static string BuildCacheKey(string code, IReadOnlyDictionary<string, object> captures,
        IReadOnlyList<string> usings, IReadOnlyList<string> context,
        IReadOnlyDictionary<string, string> dependencies)
    {
        var sb = new System.Text.StringBuilder();
        sb.Append("code:");
        sb.Append(code);

        // Include capture types in cache key so typed and untyped patterns
        // with the same code don't collide
        foreach (var kvp in captures.OrderBy(c => c.Key))
        {
            var captureType = kvp.Value.GetType().GetProperty("Type")?.GetValue(kvp.Value) as string;
            if (!string.IsNullOrEmpty(captureType))
            {
                sb.Append($"|type:{kvp.Key}={captureType}");
            }
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

    public override J VisitMethodInvocation(MethodInvocation mi, int p)
    {
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

        // Substitute variadic placeholder in arguments
        mi = ExpandVariadicArgs(mi);

        return mi;
    }

    public override J VisitFieldAccess(FieldAccess fieldAccess, int p)
    {
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

        return fieldAccess;
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
}
