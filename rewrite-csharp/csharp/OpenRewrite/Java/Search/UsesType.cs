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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Java.Search;

/// <summary>
/// Match files using a specific type by walking the tree and checking
/// node-level type attribution against a glob pattern.
///
/// Mirrors <c>org.openrewrite.java.search.HasType</c>. The pattern
/// supports <c>*</c> as a single-segment wildcard
/// (e.g. <c>"java.util.*"</c> matches <c>"java.util.List"</c>) and
/// <c>*..*</c> as the universal "any type" wildcard. Used as the
/// LocalVisitor bundled with the <see cref="RecipeRef"/> returned by
/// <c>UsesType</c> / <c>FindTypes</c> so unit tests without an active
/// RPC connection still see real filtering.
/// </summary>
public class UsesType : JavaVisitor<ExecutionContext>
{
    private readonly string _pattern;
    private bool _found;

    public UsesType(string fullyQualifiedTypeName)
    {
        _pattern = fullyQualifiedTypeName;
    }

    public override J? Visit(Tree? tree, ExecutionContext ctx)
    {
        if (tree is not SourceFile sf) return tree as J;
        _found = false;
        base.Visit(tree, ctx);
        if (_found)
        {
            return SearchResult.Found(sf) as J;
        }
        return sf as J;
    }

    public override J VisitIdentifier(Identifier id, ExecutionContext ctx)
    {
        CheckType(id.Type);
        return base.VisitIdentifier(id, ctx);
    }

    public override J VisitFieldAccess(FieldAccess fa, ExecutionContext ctx)
    {
        CheckType(fa.Type);
        return base.VisitFieldAccess(fa, ctx);
    }

    public override J VisitClassDeclaration(ClassDeclaration cd, ExecutionContext ctx)
    {
        CheckType(cd.Type);
        return base.VisitClassDeclaration(cd, ctx);
    }

    public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
    {
        if (mi.MethodType?.DeclaringType != null)
        {
            CheckType(mi.MethodType.DeclaringType);
        }
        CheckType(mi.Type);
        return base.VisitMethodInvocation(mi, ctx);
    }

    private void CheckType(JavaType? type)
    {
        if (_found || type == null) return;
        var fqn = FullyQualifiedName(type);
        if (fqn != null && MatchTypeGlob(_pattern, fqn))
        {
            _found = true;
        }
    }

    /// <summary>
    /// Best-effort accessor for a JavaType.FullyQualified-shaped object's
    /// FQN. Returns null when the type isn't fully qualified or hasn't
    /// been attributed.
    /// </summary>
    internal static string? FullyQualifiedName(JavaType type)
    {
        return type switch
        {
            JavaType.Class c => c.FullyQualifiedName,
            JavaType.Parameterized p => p.Type != null ? FullyQualifiedName(p.Type) : null,
            JavaType.Annotation a => a.AnnotationType != null ? FullyQualifiedName(a.AnnotationType) : null,
            JavaType.Array arr => arr.ElemType != null ? FullyQualifiedName(arr.ElemType) : null,
            _ => null,
        };
    }

    private static bool MatchTypeGlob(string pattern, string fqn)
    {
        if (pattern == fqn) return true;
        if (pattern == "*..*") return true;

        var patternParts = pattern.Split('.');
        var fqnParts = fqn.Split('.');
        if (patternParts.Length != fqnParts.Length) return false;
        for (var i = 0; i < patternParts.Length; i++)
        {
            if (patternParts[i] == "*") continue;
            if (patternParts[i] != fqnParts[i]) return false;
        }
        return true;
    }
}
