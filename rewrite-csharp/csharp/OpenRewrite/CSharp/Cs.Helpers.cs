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
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

public partial interface Cs
{
    /// <summary>
    /// Extract the simple name from a <see cref="NameTree"/> node.
    /// Works with <see cref="Identifier"/> and <see cref="FieldAccess"/>.
    /// </summary>
    public static string? GetSimpleName(NameTree node) => node switch
    {
        Identifier id => id.SimpleName,
        FieldAccess fa => fa.Name.Element.SimpleName,
        _ => null
    };

    /// <summary>
    /// Check if a <see cref="MethodInvocation"/> has zero real arguments.
    /// Handles the <see cref="Empty"/> sentinel that represents an empty argument list.
    /// </summary>
    public static bool HasNoArguments(MethodInvocation mi)
    {
        var elems = mi.Arguments.Elements;
        return elems.Count == 0 || (elems.Count == 1 && elems[0].Element is Empty);
    }

    /// <summary>
    /// The name an <see cref="Identifier"/> declares or references, the way Roslyn's
    /// <c>SyntaxToken.ValueText</c> reports it: without a leading <c>@</c> and with Unicode
    /// escapes resolved, so <c>@class</c> and <c>class</c>, or <c>\u0061</c> and <c>a</c>, compare equal.
    /// </summary>
    public static string GetValueText(Identifier identifier) => GetValueText(identifier.SimpleName);

    /// <inheritdoc cref="GetValueText(Identifier)"/>
    public static string GetValueText(string simpleName)
    {
        if (IsPlainAsciiName(simpleName))
            return simpleName;
        var token = SyntaxFactory.ParseToken(simpleName);
        return token.Kind() == SyntaxKind.IdentifierToken && token.FullSpan.Length == simpleName.Length
            ? token.ValueText
            : simpleName;
    }

    private static bool IsPlainAsciiName(string name)
    {
        foreach (var c in name)
        {
            if (c is '@' or '\\' or > '\x7F')
                return false;
        }
        return true;
    }

    /// <summary>
    /// Whether any <see cref="Space"/> in <paramref name="tree"/> carries a comment, including
    /// structured documentation comments and the spaces held by <see cref="JRightPadded{T}.After"/>,
    /// <see cref="JLeftPadded{T}.Before"/> and <see cref="JContainer{T}.Before"/>.
    /// </summary>
    /// <param name="tree">The subtree to search.</param>
    /// <param name="includePrefix">Whether comments in the prefix of <paramref name="tree"/> itself count.</param>
    /// <param name="includeDirectives">Whether preprocessor directives (<c>#region</c>, <c>#pragma</c>,
    /// <c>#if</c>, ...) count as well, for callers that must not drop them either.</param>
    public static bool HasComments(J tree, bool includePrefix = true, bool includeDirectives = false)
    {
        var finder = new CommentFinder(includeDirectives);
        finder.Visit(includePrefix || tree.Prefix.Comments.Count == 0 ? tree : J.SetPrefix(tree, Space.Empty), 0);
        return finder.Found;
    }

    /// <summary>
    /// Whether any element of <paramref name="container"/>, its padding or the space before its
    /// opening delimiter carries a comment. See <see cref="HasComments(J, bool, bool)"/>.
    /// </summary>
    public static bool HasComments<T>(JContainer<T> container, bool includeDirectives = false) where T : J
    {
        if (container.Before.Comments.Count > 0)
            return true;
        return HasComments(container.Elements, includeDirectives);
    }

    /// <summary>
    /// Whether any of <paramref name="elements"/> or the space after it carries a comment, as for
    /// the statements of a block or switch section. See <see cref="HasComments(J, bool, bool)"/>.
    /// </summary>
    public static bool HasComments<T>(IEnumerable<JRightPadded<T>> elements, bool includeDirectives = false) where T : J
    {
        foreach (var element in elements)
        {
            if (element.After.Comments.Count > 0 || HasComments(element.Element, true, includeDirectives))
                return true;
        }
        return false;
    }

    private sealed class CommentFinder(bool includeDirectives) : CSharpVisitor<int>
    {
        public bool Found { get; private set; }

        public override J? Visit(Tree? tree, int p)
        {
            if (Found)
                return tree as J;
            if (includeDirectives && IsPreprocessorDirective(tree))
            {
                Found = true;
                return tree as J;
            }
            return base.Visit(tree, p);
        }

        public override Space VisitSpace(Space space, int p)
        {
            Found |= space.Comments.Count > 0;
            return space;
        }

        private static bool IsPreprocessorDirective(Tree? tree) => tree is ConditionalDirective
            or PragmaWarningDirective or PragmaChecksumDirective or NullableDirective
            or RegionDirective or EndRegionDirective or DefineDirective or UndefDirective
            or ErrorDirective or WarningDirective or LineDirective;
    }
}
