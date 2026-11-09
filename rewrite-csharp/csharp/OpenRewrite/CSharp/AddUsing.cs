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

namespace OpenRewrite.CSharp;

/// <summary>
/// Adds a plain namespace <c>using</c> directive for the namespace of a type to a
/// <see cref="CompilationUnit"/>, the C# counterpart of Java's <c>AddImport</c>. Designed to run
/// as an after-visitor via <see cref="CSharpVisitor{P}.MaybeAddUsing"/>: the directive is only
/// added when the type is referenced by its simple name (unless <c>onlyIfReferenced</c> is
/// false), never duplicated — a textually matching plain or <c>global</c> using anywhere in the
/// file is a no-op — and declined when the file already binds the simple name to a different
/// type (the CS0104 risk). <c>using static</c> and alias directives are out of scope.
/// <para>
/// Placement follows the file: the compilation unit's using list when it has one, otherwise the
/// first block-scoped namespace that keeps usings, otherwise the compilation unit (file-scoped
/// namespaces keep usings at the compilation unit level). A sorted list stays sorted — plain
/// ordinal or Visual Studio's System-first order, whichever the file already satisfies — and an
/// unsorted list appends after its last using.
/// </para>
/// <para>
/// A file with <c>#if</c> blocks gets the using at the compilation unit level, above its first
/// conditional directive. The text above that directive is the same in every branch, and every
/// configuration compiles it. Extern aliases, global usings and <c>#define</c> or <c>#undef</c>
/// lines must precede a plain using, so a file with a conditional directive before one of them is
/// left alone.
/// </para>
/// </summary>
public class AddUsing<P> : CSharpVisitor<P>, IEquatable<AddUsing<P>>
{
    private readonly string _fullyQualifiedTypeName;
    private readonly string? _namespace;
    private readonly string _typeName;
    private readonly bool _onlyIfReferenced;

    /// <param name="fullyQualifiedTypeName">The type whose namespace should be imported, e.g.
    /// <c>System.Windows.DependencyProperty</c>. A name without a namespace is a no-op.</param>
    /// <param name="onlyIfReferenced">When true (the default), the using is only added when the
    /// file references the type by its simple name.</param>
    public AddUsing(string fullyQualifiedTypeName, bool onlyIfReferenced = true)
    {
        _fullyQualifiedTypeName = fullyQualifiedTypeName;
        var lastDot = fullyQualifiedTypeName.LastIndexOf('.');
        _namespace = lastDot > 0 ? fullyQualifiedTypeName[..lastDot] : null;
        _typeName = lastDot > 0 ? fullyQualifiedTypeName[(lastDot + 1)..] : fullyQualifiedTypeName;
        _onlyIfReferenced = onlyIfReferenced;
    }

    public override J? PreVisit(J tree, P p)
    {
        StopAfterPreVisit();
        if (tree is not CompilationUnit cu || _namespace == null)
        {
            return tree;
        }

        if (AddUsing.HasNamespaceUsing(cu, _namespace)
            || (_onlyIfReferenced && !IsReferenced(cu))
            || AddUsing.IsShadowedInFile(cu, _typeName, _fullyQualifiedTypeName))
        {
            return cu;
        }

        // Each branch is the whole file as one set of symbols compiles it.
        if (cu.Members is [{ Element: ConditionalDirective directive } member])
        {
            if (directive.Branches.Any(branch => HasDirectiveBeforeWhatMustPrecedeUsings(branch.Element)))
            {
                return cu;
            }
            return cu.WithMembers([member.WithElement(directive.WithBranches(directive.Branches
                .Select(branch => branch.WithElement(Insert(branch.Element, inBranch: true)))
                .ToList()))]);
        }
        return Insert(cu, inBranch: false);
    }

    private bool IsReferenced(CompilationUnit cu)
    {
        var finder = new FindSimpleNameReference(_typeName, _fullyQualifiedTypeName);
        finder.Visit(cu, 0);
        return finder.Found;
    }

    private CompilationUnit Insert(CompilationUnit cu, bool inBranch)
    {
        var newline = DetectNewline(cu);

        if (inBranch && InsertAboveOpeningDirective(cu, newline) is { } opened)
        {
            return opened;
        }

        var shared = inBranch
            ? cu.Usings.TakeWhile(u => IndexOfDirectiveLine(u.Element.Prefix) < 0).Count()
            : cu.Usings.Count;
        if (cu.Usings.Take(shared).Any(u => u.Element is UsingDirective))
        {
            return cu.WithUsings(InsertInto(cu.Usings, shared, newline));
        }
        if (shared < cu.Usings.Count)
        {
            return cu.WithUsings(InsertAboveDirectiveLine(cu.Usings, shared, newline));
        }

        // A branch keeps the using at the compilation unit level, as a namespace's usings can sit
        // below a conditional directive.
        if (!inBranch)
        {
            foreach (var ns in AddUsing.Namespaces(cu.Members))
            {
                if (ns.Name.Markers.FindFirst<Semicolon>() == null &&
                    ns.Usings.Any(u => u.Element is UsingDirective))
                {
                    return (CompilationUnit)new ReplaceNamespaceUsings(ns.Id,
                            InsertInto(ns.Usings, ns.Usings.Count, newline))
                        .Visit(cu, 0)!;
                }
            }
        }

        return InsertFirstUsing(cu, newline);
    }

    private static bool HasDirectiveBeforeWhatMustPrecedeUsings(CompilationUnit branch)
    {
        static bool MustPrecede(JRightPadded<Statement> u) =>
            u.Element is UsingDirective { Global.Element: true } or DefineDirective or UndefDirective;
        return IndexOfDirectiveLine(branch.Prefix) >= 0
            ? branch.Externs.Count > 0 || branch.Usings.Any(MustPrecede)
            : branch.Externs.Any(e => IndexOfDirectiveLine(e.Element.Prefix) >= 0)
              || branch.Usings.SkipWhile(u => IndexOfDirectiveLine(u.Element.Prefix) < 0).Any(MustPrecede);
    }

    /// <summary>A branch whose prefix holds a conditional directive gets the using right above it,
    /// after any file header.</summary>
    private CompilationUnit? InsertAboveOpeningDirective(CompilationUnit cu, string newline)
    {
        if (IndexOfDirectiveLine(cu.Prefix) < 0)
        {
            return null;
        }

        var (above, from) = SplitAtDirectiveLine(cu.Prefix, newline);
        var added = JRightPadded<Statement>.Build(CreateUsing(Space.Empty));
        cu = cu.WithPrefix(above);
        // The prefix holds the first token's leading trivia, so whatever follows it has an empty prefix.
        if (cu.Usings.Count > 0)
        {
            var first = cu.Usings[0];
            return cu.WithUsings([added, first.WithElement(J.SetPrefix(first.Element, from)), .. cu.Usings.Skip(1)]);
        }
        return WithDeclarationPrefix(cu.WithUsings([added]), _ => from);
    }

    private List<JRightPadded<Statement>> InsertAboveDirectiveLine(IList<JRightPadded<Statement>> list, int index,
        string newline)
    {
        var below = list[index];
        var result = new List<JRightPadded<Statement>>(list);
        if (index > 0 && below.Element.Prefix.Whitespace.Contains('\n'))
        {
            // The blank lines and comments above the directive stay with it.
            result.Insert(index, JRightPadded<Statement>.Build(CreateUsing(Space.Format(newline))));
            return result;
        }

        var (above, from) = SplitAtDirectiveLine(below.Element.Prefix, newline);
        result[index] = below.WithElement(J.SetPrefix(below.Element, from));
        result.Insert(index, JRightPadded<Statement>.Build(CreateUsing(above)));
        return result;
    }

    /// <summary>Splits a space before its first conditional directive line, which starts the second
    /// part on a new line. Doc comments right above the directive document the declaration below it,
    /// so they go with the directive.</summary>
    private static (Space Above, Space From) SplitAtDirectiveLine(Space space, string newline)
    {
        var at = IndexOfDirectiveLine(space);
        while (at > 0 && space.Comments[at - 1] is CsDocComment)
        {
            at--;
        }
        return (new Space(space.Whitespace, space.Comments.Take(at).ToList()),
            new Space(newline, space.Comments.Skip(at).ToList()));
    }

    private static int IndexOfDirectiveLine(Space space)
    {
        for (var i = 0; i < space.Comments.Count; i++)
        {
            if (DirectiveBoundaryInjector.DirectiveIndex(space.Comments[i]) != null)
            {
                return i;
            }
        }
        return -1;
    }

    /// <summary>Inserts among the first <paramref name="count"/> elements of the list. The element
    /// after them, if any, starts with a conditional directive line.</summary>
    private List<JRightPadded<Statement>> InsertInto(IList<JRightPadded<Statement>> list, int count, string newline)
    {
        var index = InsertionIndex(list, count);
        if (index == count && count < list.Count)
        {
            return InsertAboveDirectiveLine(list, index, newline);
        }

        var linePrefix = Space.Format(newline + IndentOf(list));

        var result = new List<JRightPadded<Statement>>(list);
        if (index == 0)
        {
            var displaced = result[0];
            result[0] = displaced.WithElement(J.SetPrefix(displaced.Element, linePrefix));
            result.Insert(0, JRightPadded<Statement>.Build(CreateUsing(displaced.Element.Prefix)));
        }
        else
        {
            result.Insert(index, JRightPadded<Statement>.Build(CreateUsing(linePrefix)));
        }
        return result;
    }

    private int InsertionIndex(IList<JRightPadded<Statement>> list, int count)
    {
        var plain = new List<(int Index, string Name)>();
        var lastUsing = -1;
        for (var i = 0; i < count; i++)
        {
            if (list[i].Element is UsingDirective directive)
            {
                lastUsing = i;
                if (directive is { IsStatic: false, Alias: null }
                    && AddUsing.QualifiedNameOf(directive.NamespaceOrType) is { } name)
                {
                    plain.Add((i, name));
                }
            }
        }

        if (plain.Count > 0)
        {
            foreach (var compare in (Comparison<string>[])[CompareOrdinal, CompareSystemFirst])
            {
                if (!IsSorted(plain, compare))
                {
                    continue;
                }
                foreach (var (index, name) in plain)
                {
                    if (compare(name, _namespace!) > 0)
                    {
                        return index;
                    }
                }
                return plain[^1].Index + 1;
            }
        }

        return lastUsing + 1;
    }

    private static bool IsSorted(List<(int Index, string Name)> plain, Comparison<string> compare)
    {
        for (var i = 1; i < plain.Count; i++)
        {
            if (compare(plain[i - 1].Name, plain[i].Name) > 0)
            {
                return false;
            }
        }
        return true;
    }

    private static int CompareOrdinal(string left, string right) => string.CompareOrdinal(left, right);

    /// <summary>Visual Studio's "place System directives first" ordering.</summary>
    private static int CompareSystemFirst(string left, string right)
    {
        var systemLeft = IsSystemNamespace(left);
        var systemRight = IsSystemNamespace(right);
        return systemLeft == systemRight ? string.CompareOrdinal(left, right) : systemLeft ? -1 : 1;
    }

    private static bool IsSystemNamespace(string ns) =>
        ns == "System" || ns.StartsWith("System.", StringComparison.Ordinal);

    private static string IndentOf(IList<JRightPadded<Statement>> list)
    {
        foreach (var padded in list)
        {
            var whitespace = padded.Element.Prefix.Whitespace;
            var newlineIndex = whitespace.LastIndexOf('\n');
            if (newlineIndex >= 0)
            {
                return whitespace[(newlineIndex + 1)..];
            }
        }
        return "";
    }

    private static string DetectNewline(CompilationUnit cu)
    {
        foreach (var whitespace in CandidateWhitespace(cu))
        {
            if (whitespace.Contains("\r\n"))
            {
                return "\r\n";
            }
            if (whitespace.Contains('\n'))
            {
                return "\n";
            }
        }
        return "\n";
    }

    private static IEnumerable<string> CandidateWhitespace(CompilationUnit cu)
    {
        yield return cu.Prefix.Whitespace;
        foreach (var comment in cu.Prefix.Comments)
        {
            yield return comment.Suffix;
        }
        foreach (var list in AddUsing.UsingLists(cu))
        {
            foreach (var padded in list)
            {
                yield return padded.Element.Prefix.Whitespace;
            }
        }
        foreach (var member in cu.Members)
        {
            yield return member.Element.Prefix.Whitespace;
        }
        yield return cu.Eof.Whitespace;
    }

    /// <summary>A file with no usings gets its first one at the compilation unit level: after
    /// the file header (which lives in the compilation unit prefix), before the first
    /// declaration, separated from it by a blank line.</summary>
    private CompilationUnit InsertFirstUsing(CompilationUnit cu, string newline)
    {
        var usings = new List<JRightPadded<Statement>>(cu.Usings);
        var ownLine = usings.Count > 0 || cu.Externs.Count > 0;
        usings.Add(JRightPadded<Statement>.Build(CreateUsing(ownLine ? Space.Format(newline) : Space.Empty)));
        cu = cu.WithUsings(usings);

        IList<Comment> moved = [];
        var headerComments = cu.Prefix.Comments;
        var split = headerComments.Count;
        while (split > 0 && headerComments[split - 1] is CsDocComment)
        {
            split--;
        }
        if (split < headerComments.Count && cu.AttributeLists.Count == 0 && cu.Members.Count > 0)
        {
            moved = headerComments.Skip(split).ToList();
            cu = cu.WithPrefix(cu.Prefix.WithComments(headerComments.Take(split).ToList()));
        }

        if (cu.AttributeLists.Count == 0 && cu.Members.Count == 0)
        {
            return cu;
        }
        var blank = newline + newline;
        return WithDeclarationPrefix(cu, prefix =>
            moved.Count > 0 ? new Space(blank, moved.Concat(prefix.Comments).ToList())
            : prefix.Whitespace.Contains('\n') ? prefix
            : prefix.WithWhitespace(blank));
    }

    /// <summary>Maps the prefix of the first assembly attribute list, else of the first member, else
    /// the end of the file.</summary>
    private static CompilationUnit WithDeclarationPrefix(CompilationUnit cu, Func<Space, Space> map)
    {
        if (cu.AttributeLists.Count > 0)
        {
            var list = cu.AttributeLists[0];
            return cu.WithAttributeLists([list.WithPrefix(map(list.Prefix)), .. cu.AttributeLists.Skip(1)]);
        }
        if (cu.Members.Count > 0)
        {
            var first = cu.Members[0];
            return cu.WithMembers([first.WithElement(J.SetPrefix(first.Element, map(first.Element.Prefix))),
                .. cu.Members.Skip(1)]);
        }
        return cu.WithEof(map(cu.Eof));
    }

    private UsingDirective CreateUsing(Space prefix) =>
        new(Tree.RandomId(),
            prefix,
            Markers.Empty,
            new JRightPadded<bool>(false, Space.Empty, Markers.Empty),
            new JLeftPadded<bool>(Space.Empty, false),
            new JLeftPadded<bool>(Space.Empty, false),
            null,
            BuildNamespaceName(_namespace!));

    /// <summary>Builds the name tree for a dotted namespace; the outermost node carries the
    /// single space that separates it from the <c>using</c> keyword.</summary>
    private static TypeTree BuildNamespaceName(string ns)
    {
        var parts = ns.Split('.');
        Expression name = new Identifier(Tree.RandomId(), Space.Empty, Markers.Empty, [], parts[0], null, null);
        for (var i = 1; i < parts.Length; i++)
        {
            name = new FieldAccess(Tree.RandomId(), Space.Empty, Markers.Empty, name,
                new JLeftPadded<Identifier>(Space.Empty,
                    new Identifier(Tree.RandomId(), Space.Empty, Markers.Empty, [], parts[i], null, null)),
                null);
        }
        return (TypeTree)J.SetPrefix((J)name, Space.SingleSpace);
    }

    public bool Equals(AddUsing<P>? other) =>
        other is not null
        && string.Equals(_fullyQualifiedTypeName, other._fullyQualifiedTypeName, StringComparison.Ordinal)
        && _onlyIfReferenced == other._onlyIfReferenced;

    public override bool Equals(object? obj) => Equals(obj as AddUsing<P>);
    public override int GetHashCode() => HashCode.Combine(_fullyQualifiedTypeName, _onlyIfReferenced);

    private sealed class ReplaceNamespaceUsings(Guid namespaceId, IList<JRightPadded<Statement>> usings)
        : CSharpVisitor<int>
    {
        public override J VisitNamespaceDeclaration(NamespaceDeclaration ns, int p) =>
            ns.Id == namespaceId ? ns.WithUsings(usings) : base.VisitNamespaceDeclaration(ns, p);
    }

    /// <summary>
    /// Looks for the type written by its simple name. An identifier counts when its attributed
    /// type is the requested one, or when it carries no attribution at all — recipes synthesize
    /// short-name identifiers whose types only materialize on the next parse. Qualified
    /// spellings (the name side of a member access) do not count: they resolve without the
    /// using.
    /// </summary>
    private sealed class FindSimpleNameReference(string simpleName, string fullyQualifiedName)
        : CSharpVisitor<int>
    {
        internal bool Found { get; private set; }

        public override J? Visit(Tree? tree, int p) => Found ? tree as J : base.Visit(tree, p);

        public override J VisitUsingDirective(UsingDirective usingDirective, int p) => usingDirective;

        public override J VisitIdentifier(Identifier identifier, int p)
        {
            if (string.Equals(identifier.SimpleName, simpleName, StringComparison.Ordinal)
                && !(Cursor.ParentTree.Value is FieldAccess parent && ReferenceEquals(parent.Name.Element, identifier))
                && (TypeUtils.GetFullyQualifiedName(identifier.Type) is not { Length: > 0 } resolved
                    || string.Equals(resolved, fullyQualifiedName, StringComparison.Ordinal)))
            {
                Found = true;
            }
            return identifier;
        }
    }
}

/// <summary>
/// Companion checks for <see cref="AddUsing{P}"/>, shared with recipes that need the same
/// answers to decide whether a simple name can be written at all.
/// </summary>
public static class AddUsing
{
    /// <summary>
    /// True when the file already writes <paramref name="simpleName"/> as a name that resolves
    /// to something other than <paramref name="fullyQualifiedName"/>.
    /// <para>
    /// Roslyn's <c>ToMinimalDisplayString</c> falls back to the qualified name when the short
    /// one would be ambiguous (CS0104); nothing in the LST answers "could this name be
    /// ambiguous", but the case that actually bites — the file uses that short name for a
    /// different type — is visible, and is checked here. A short name is only ever written when
    /// this returns false, so the failure direction is a needlessly qualified name rather than
    /// a compile error.
    /// </para>
    /// </summary>
    public static bool IsShadowedInFile(CompilationUnit cu, string simpleName, string fullyQualifiedName)
    {
        var collector = new NameCollector(simpleName, fullyQualifiedName);
        collector.Visit(cu, 0);
        return collector.Shadowed;
    }

    /// <summary>Cursor form of <see cref="IsShadowedInFile(CompilationUnit,string,string)"/>;
    /// false when no compilation unit encloses the cursor.</summary>
    public static bool IsShadowedInFile(Cursor cursor, string simpleName, string fullyQualifiedName) =>
        cursor.FirstEnclosing<CompilationUnit>() is { } cu
        && IsShadowedInFile(cu, simpleName, fullyQualifiedName);

    /// <summary>True when the file already has a plain (non-static, non-alias) using for
    /// <paramref name="ns"/> — at the compilation unit level or on any namespace declaration,
    /// including <c>global using</c> directives. In a file with <c>#if</c> blocks, every
    /// configuration that declares something has to have it.</summary>
    public static bool HasNamespaceUsing(CompilationUnit cu, string ns) =>
        cu.Members is [{ Element: ConditionalDirective directive }]
            ? directive.Branches.All(branch => branch.Element is { Members.Count: 0, AttributeLists.Count: 0 }
                || HasNamespaceUsing(branch.Element, ns))
            : UsingLists(cu).SelectMany(list => list).Any(padded =>
                padded.Element is UsingDirective { IsStatic: false, Alias: null } directive
                && string.Equals(QualifiedNameOf(directive.NamespaceOrType), ns, StringComparison.Ordinal));

    internal static IEnumerable<IList<JRightPadded<Statement>>> UsingLists(CompilationUnit cu)
    {
        yield return cu.Usings;
        foreach (var ns in Namespaces(cu.Members))
        {
            yield return ns.Usings;
        }
    }

    internal static IEnumerable<NamespaceDeclaration> Namespaces(IList<JRightPadded<Statement>> members)
    {
        foreach (var member in members)
        {
            if (member.Element is NamespaceDeclaration ns)
            {
                yield return ns;
                foreach (var nested in Namespaces(ns.Members))
                {
                    yield return nested;
                }
            }
        }
    }

    /// <summary>The dotted text of a possibly-qualified name, as written.</summary>
    public static string? QualifiedNameOf(J? expression) => expression switch
    {
        Identifier id => id.SimpleName,
        FieldAccess fa => QualifiedNameOf(fa.Target) is { } target
            ? target + "." + fa.Name.Element.SimpleName
            : null,
        _ => null,
    };

    /// <summary>
    /// Looks for a name written as <c>simpleName</c> that resolves to a type other than
    /// <c>fullyQualifiedName</c> — the in-file evidence that the short name is taken.
    /// </summary>
    private sealed class NameCollector(string simpleName, string fullyQualifiedName)
        : CSharpVisitor<int>
    {
        internal bool Shadowed { get; private set; }

        public override J VisitClassDeclaration(ClassDeclaration cd, int p)
        {
            Check(cd.Name.SimpleName, cd.Type);
            return base.VisitClassDeclaration(cd, p);
        }

        public override J VisitIdentifier(Identifier identifier, int p)
        {
            Check(identifier.SimpleName, identifier.Type);
            return identifier;
        }

        private void Check(string name, JavaType? type)
        {
            if (!Shadowed
                && string.Equals(name, simpleName, StringComparison.Ordinal)
                && TypeUtils.GetFullyQualifiedName(type) is { Length: > 0 } resolved
                && !string.Equals(resolved, fullyQualifiedName, StringComparison.Ordinal))
            {
                Shadowed = true;
            }
        }
    }
}
