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
using System.Collections;
using System.Reflection;
using System.Text;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>The padding wrapper an LST node sits inside, if any.</summary>
public enum PaddingKind
{
    /// <summary>The parent property holds the node directly.</summary>
    None,

    /// <summary>The node is the element of a <see cref="JRightPadded{T}"/>.</summary>
    RightPadded,

    /// <summary>The node is the element of a <see cref="JLeftPadded{T}"/>.</summary>
    LeftPadded,

    /// <summary>The node is an element of a <see cref="JContainer{T}"/>.</summary>
    Container
}

/// <summary>
/// One row of an <see cref="LstOutline"/>: an LST node, or a marker attached to one.
/// </summary>
public sealed class LstOutlineNode
{
    /// <summary>The node's LST type, qualified by language (<c>Cs.UsingDirective</c>, <c>J.Identifier</c>).</summary>
    public required string Type { get; init; }

    /// <summary>
    /// The property of the nearest LST ancestor that holds this node, indexed when the property
    /// is a list (<c>Statements[2]</c>). Empty for the node the outline starts at.
    /// </summary>
    public string Property { get; init; } = "";

    /// <summary>The padding wrapper between this node and <see cref="Property"/>.</summary>
    public PaddingKind Padding { get; init; }

    /// <summary>The <see cref="JavaType"/> attested on this node, or null when it carries none.</summary>
    public string? AttestedType { get; init; }

    /// <summary>True when this row is a marker rather than an LST node.</summary>
    public bool IsMarker { get; init; }

    /// <summary>A marker's non-identity state, for example <c>DotPrefix=·</c>. Empty when it has none.</summary>
    public string MarkerState { get; init; } = "";

    /// <summary>
    /// Offset into <see cref="LstOutline.Source"/> where the node's printed text starts. The offset
    /// is taken before the node's prefix is printed, so the span covers the whitespace the node owns.
    /// -1 only when the span could be neither measured nor derived.
    /// </summary>
    public int Start { get; internal set; } = -1;

    /// <summary>Length of the node's printed text, in characters.</summary>
    public int Length { get; internal set; }

    /// <summary>
    /// True when the span was derived rather than measured, because <c>CSharpPrinter</c> writes this
    /// node inline instead of reaching it through <c>Visit</c> or <c>BeforeSyntax</c>
    /// (<c>NamedVariable</c> and <c>Modifier</c>, for example). The span is then the hull of the
    /// node's measured descendants, or the gap its measured siblings leave inside the parent, so it
    /// locates the node but may be wider than the text it really owns. Such a node is also the one
    /// case where a marker placed on it never prints.
    /// </summary>
    public bool IsApproximate { get; internal set; }

    private readonly List<LstOutlineNode> _children = [];

    /// <summary>Child nodes and markers, in document order.</summary>
    public IReadOnlyList<LstOutlineNode> Children => _children;

    internal void Add(LstOutlineNode child) => _children.Add(child);
}

/// <summary>
/// A flattened view of an LST built for answering "which node owns which piece of syntax".
/// <para>
/// Each node is paired with the property of its parent that holds it, the padding wrapper it sits
/// in, and the span of printed source it covers — spans start before the node's
/// <see cref="J.Prefix"/>, so they show the whitespace a node owns, which is the part of the model
/// hardest to read off the type declarations. Markers appear as rows under the node they decorate.
/// </para>
/// </summary>
public sealed class LstOutline
{
    private LstOutline(string source, LstOutlineNode root)
    {
        Source = source;
        Root = root;
    }

    /// <summary>The whole of <c>root</c> as printed back to source. Node spans index into this.</summary>
    public string Source { get; }

    /// <summary>The outline's starting node.</summary>
    public LstOutlineNode Root { get; }

    /// <summary>
    /// Builds an outline of <paramref name="focus"/>, or of <paramref name="root"/> when no focus is
    /// given. <see cref="Source"/> is always all of <paramref name="root"/>, so a focused outline
    /// still places each node in the context of the whole tree.
    /// </summary>
    public static LstOutline Of(Tree root, Tree? focus = null)
    {
        var spans = new SpanPrinter();
        var source = spans.Print(root);

        var walker = new Walker(spans.Spans);
        walker.Visit(focus ?? root, null);

        var outlineRoot = walker.Root ?? new LstOutlineNode { Type = DisplayName(root.GetType()) };
        DeriveMissingSpans(outlineRoot, source);
        return new LstOutline(source, outlineRoot);
    }

    private static void DeriveMissingSpans(LstOutlineNode node, string source)
    {
        if (node.Start < 0 && Hull(node) is { } hull && hull.End > hull.Start)
        {
            Locate(node, hull.Start, hull.End - hull.Start);
        }

        if (node.Start >= 0)
        {
            DeriveChildSpans(node, source);
        }

        foreach (var child in node.Children)
        {
            if (!child.IsMarker)
            {
                DeriveMissingSpans(child, source);
            }
        }
    }

    private static void DeriveChildSpans(LstOutlineNode parent, string source)
    {
        var children = parent.Children.Where(c => !c.IsMarker).ToList();
        var anchors = children.Select(Anchor).ToList();
        var parentEnd = parent.Start + parent.Length;

        var i = 0;
        while (i < children.Count)
        {
            if (anchors[i] is { } anchor)
            {
                if (children[i].Start < 0)
                {
                    Locate(children[i], anchor.Start, anchor.End - anchor.Start);
                }
                i++;
                continue;
            }

            var runEnd = i;
            while (runEnd < children.Count && anchors[runEnd] == null)
            {
                runEnd++;
            }

            var lower = parent.Start;
            for (var before = i - 1; before >= 0; before--)
            {
                if (anchors[before] is { } previous)
                {
                    lower = previous.End;
                    break;
                }
            }

            var upper = parentEnd;
            for (var after = runEnd; after < children.Count; after++)
            {
                if (anchors[after] is { } following)
                {
                    upper = following.Start;
                    break;
                }
            }

            upper = Math.Max(lower, upper);
            var parts = SplitGap(source, lower, upper, runEnd - i);
            for (var child = i; child < runEnd; child++)
            {
                var (partStart, partEnd) = parts?[child - i] ?? (lower, upper);
                var (tightStart, tightLength) = Tighten(source, partStart, partEnd);
                Locate(children[child], tightStart, tightLength);
            }

            i = runEnd;
        }
    }

    private static List<(int Start, int End)>? SplitGap(string source, int start, int end, int count)
    {
        if (count <= 1)
        {
            return count == 1 ? [(start, end)] : null;
        }

        var commas = new List<int>();
        for (var i = start; i < end; i++)
        {
            if (source[i] == ',')
            {
                commas.Add(i);
            }
        }

        if (commas.Count == count - 1)
        {
            var parts = new List<(int, int)>(count);
            var cursor = start;
            foreach (var comma in commas)
            {
                parts.Add((cursor, comma));
                cursor = comma + 1;
            }
            parts.Add((cursor, end));
            return parts;
        }

        var tokenEnds = new List<int>();
        var scan = start;
        while (scan < end)
        {
            while (scan < end && char.IsWhiteSpace(source[scan]))
            {
                scan++;
            }
            if (scan >= end)
            {
                break;
            }
            while (scan < end && !char.IsWhiteSpace(source[scan]))
            {
                scan++;
            }
            tokenEnds.Add(scan);
        }

        if (tokenEnds.Count != count)
        {
            return null;
        }

        var tokens = new List<(int, int)>(count);
        var from = start;
        foreach (var tokenEnd in tokenEnds)
        {
            tokens.Add((from, tokenEnd));
            from = tokenEnd;
        }
        return tokens;
    }

    private static (int Start, int End)? Anchor(LstOutlineNode node) =>
        node.Start >= 0 ? (node.Start, node.Start + node.Length) : Hull(node);

    private static (int Start, int End)? Hull(LstOutlineNode node)
    {
        (int Start, int End)? hull = node is { Start: >= 0, IsApproximate: false }
            ? (node.Start, node.Start + node.Length)
            : null;

        foreach (var child in node.Children)
        {
            if (child.IsMarker || Hull(child) is not { } childHull)
            {
                continue;
            }
            hull = hull == null
                ? childHull
                : (Math.Min(hull.Value.Start, childHull.Start), Math.Max(hull.Value.End, childHull.End));
        }
        return hull;
    }

    private static (int Start, int Length) Tighten(string source, int start, int end)
    {
        while (start < end && source[start] is ',' or ';')
        {
            start++;
        }
        while (end > start && (char.IsWhiteSpace(source[end - 1]) || source[end - 1] is ',' or ';'))
        {
            end--;
        }
        return (start, end - start);
    }

    private static void Locate(LstOutlineNode node, int start, int length)
    {
        node.Start = start;
        node.Length = length;
        node.IsApproximate = true;

        foreach (var marker in node.Children.Where(c => c.IsMarker))
        {
            marker.Start = start;
            marker.Length = length;
            marker.IsApproximate = true;
        }
    }

    /// <summary>
    /// Finds the first node in document order whose LST type matches <paramref name="typeName"/>,
    /// given either as a bare name (<c>InstanceOf</c>) or a qualified one (<c>Cs.UsingDirective</c>).
    /// </summary>
    public static J? Find(Tree root, string typeName)
    {
        var finder = new Finder(typeName);
        finder.Visit(root, null);
        return finder.Match;
    }

    private sealed class SpanPrinter : CSharpPrinter<int>
    {
        private readonly Dictionary<Guid, int> _starts = new();

        public Dictionary<Guid, (int Start, int Length)> Spans { get; } = new();

        public override J? PreVisit(J tree, PrintOutputCapture<int> p)
        {
            Open(tree, p);
            return tree;
        }

        public override J? PostVisit(J tree, PrintOutputCapture<int> p)
        {
            Close(tree, p);
            return tree;
        }

        protected override void BeforeSyntax(J j, PrintOutputCapture<int> p)
        {
            Open(j, p);
            base.BeforeSyntax(j, p);
        }

        protected override void AfterSyntax(J j, PrintOutputCapture<int> p)
        {
            base.AfterSyntax(j, p);
            Close(j, p);
        }

        private void Open(J tree, PrintOutputCapture<int> p) => _starts.TryAdd(tree.Id, p.Length);

        private void Close(J tree, PrintOutputCapture<int> p)
        {
            if (_starts.TryGetValue(tree.Id, out var start))
            {
                Spans[tree.Id] = (start, p.Length - start);
            }
        }
    }

    private sealed class Finder(string typeName) : CSharpVisitor<object?>
    {
        public J? Match { get; private set; }

        public override J? PreVisit(J tree, object? p)
        {
            if (Match == null && Matches(tree.GetType(), typeName))
            {
                Match = tree;
            }
            return base.PreVisit(tree, p);
        }

        private static bool Matches(Type type, string name)
        {
            var display = DisplayName(type);
            return string.Equals(type.Name, name, StringComparison.OrdinalIgnoreCase)
                   || string.Equals(display, name, StringComparison.OrdinalIgnoreCase)
                   || display.EndsWith("." + name, StringComparison.OrdinalIgnoreCase);
        }
    }

    private sealed class Walker(Dictionary<Guid, (int Start, int Length)> spans) : CSharpVisitor<object?>
    {
        private readonly Dictionary<Guid, LstOutlineNode> _nodes = new();

        public LstOutlineNode? Root { get; private set; }

        public override J? PreVisit(J tree, object? p)
        {
            var (parent, property) = FindNearestLstParent();
            var span = spans.TryGetValue(tree.Id, out var found) ? found : (Start: -1, Length: 0);

            var node = new LstOutlineNode
            {
                Type = DisplayName(tree.GetType()),
                Property = property,
                Padding = PaddingOf(),
                AttestedType = DescribeType(AttestedType(tree)),
                Start = span.Start,
                Length = span.Length
            };

            _nodes[tree.Id] = node;
            Root ??= node;
            if (parent != null && _nodes.TryGetValue(parent.Id, out var parentNode))
            {
                parentNode.Add(node);
            }

            AddMarkers(tree, node, span);
            return base.PreVisit(tree, p);
        }

        private (J? Parent, string Property) FindNearestLstParent()
        {
            var current = Cursor;
            while (current.Parent is { IsRoot: false } parent)
            {
                if (!IsPadding(parent.Value))
                {
                    return (parent.Value as J, PropertyOf(parent.Value, current.Value) ?? "");
                }
                current = parent;
            }
            return (null, "");
        }

        private PaddingKind PaddingOf()
        {
            if (IsWrapper(Cursor.Parent?.Parent?.Value, typeof(JContainer<>)))
            {
                return PaddingKind.Container;
            }
            var parentValue = Cursor.Parent?.Value;
            if (IsWrapper(parentValue, typeof(JRightPadded<>)))
            {
                return PaddingKind.RightPadded;
            }
            return IsWrapper(parentValue, typeof(JLeftPadded<>)) ? PaddingKind.LeftPadded : PaddingKind.None;
        }

        private void AddMarkers(J tree, LstOutlineNode node, (int Start, int Length) span)
        {
            AddMarkers(node, tree.Markers, PaddingKind.None, span);

            var parentValue = Cursor.Parent?.Value;
            if (IsWrapper(parentValue, typeof(JRightPadded<>))
                && ReadProperty(parentValue!, nameof(JRightPadded<J>.Markers)) is Markers padded)
            {
                AddMarkers(node, padded, PaddingKind.RightPadded, span);
            }
        }

        private static void AddMarkers(LstOutlineNode node, Markers markers, PaddingKind padding,
            (int Start, int Length) span)
        {
            foreach (var marker in markers.MarkerList)
            {
                node.Add(new LstOutlineNode
                {
                    Type = DisplayName(marker.GetType()),
                    Property = nameof(Tree.Markers),
                    Padding = padding,
                    IsMarker = true,
                    MarkerState = DescribeMarker(marker),
                    Start = span.Start,
                    Length = span.Length
                });
            }
        }
    }

    private static bool IsPadding(object? value) =>
        IsWrapper(value, typeof(JRightPadded<>))
        || IsWrapper(value, typeof(JLeftPadded<>))
        || IsWrapper(value, typeof(JContainer<>));

    private static bool IsWrapper(object? value, Type openGeneric) =>
        value != null
        && value.GetType() is { IsGenericType: true } type
        && type.GetGenericTypeDefinition() == openGeneric;

    private static string? PropertyOf(object? parent, object? child)
    {
        if (parent == null || child == null)
        {
            return null;
        }

        var properties = ReadableProperties(parent.GetType());
        foreach (var property in properties)
        {
            if (ReferenceEquals(ReadProperty(parent, property), child))
            {
                return property.Name;
            }
        }

        foreach (var property in properties)
        {
            if (ReadProperty(parent, property) is not IEnumerable items || items is string)
            {
                continue;
            }
            var index = 0;
            foreach (var item in items)
            {
                if (ReferenceEquals(item, child))
                {
                    return $"{property.Name}[{index}]";
                }
                index++;
            }
        }

        return null;
    }

    private static PropertyInfo[] ReadableProperties(Type type) =>
        type.GetProperties(BindingFlags.Public | BindingFlags.Instance)
            .Where(p => p.CanRead && p.GetIndexParameters().Length == 0)
            .ToArray();

    private static object? ReadProperty(object target, string name)
    {
        var property = target.GetType().GetProperty(name, BindingFlags.Public | BindingFlags.Instance);
        return property == null ? null : ReadProperty(target, property);
    }

    private static object? ReadProperty(object target, PropertyInfo property)
    {
        try
        {
            return property.GetValue(target);
        }
        catch (Exception)
        {
            return null;
        }
    }

    private static JavaType? AttestedType(J node) => node switch
    {
        MethodInvocation mi => mi.MethodType,
        MethodDeclaration md => md.MethodType,
        NewClass nc => nc.ConstructorType,
        Expression e => e.Type,
        ClassDeclaration cd => cd.Type,
        NamedVariable nv => nv.Type,
        _ => null
    };

    private static string? DescribeType(JavaType? type) => type switch
    {
        null => null,
        JavaType.Primitive primitive => primitive.Keyword,
        JavaType.Unknown => "<unknown>",
        JavaType.Parameterized parameterized =>
            $"{DescribeType(parameterized.Type)}<{string.Join(", ", (parameterized.TypeParameters ?? []).Select(DescribeType))}>",
        JavaType.Class cls => cls.FullyQualifiedName,
        JavaType.Array array => DescribeType(array.ElemType) + "[]",
        JavaType.GenericTypeVariable generic => generic.Name,
        JavaType.Method method =>
            $"{DescribeType(method.DeclaringType)}.{method.Name}"
            + $"({string.Join(", ", (method.ParameterTypes ?? []).Select(DescribeType))})"
            + $" : {DescribeType(method.ReturnType)}",
        JavaType.Variable variable => $"{variable.Name} : {DescribeType(variable.Type)}",
        JavaType.MultiCatch multiCatch => string.Join(" | ", (multiCatch.ThrowableTypes ?? []).Select(DescribeType)),
        JavaType.Intersection intersection => string.Join(" & ", (intersection.Bounds ?? []).Select(DescribeType)),
        _ => type.GetType().Name
    };

    private static string DescribeMarker(Marker marker)
    {
        var parts = new List<string>();
        foreach (var property in ReadableProperties(marker.GetType()))
        {
            if (property.Name == nameof(Marker.Id))
            {
                continue;
            }
            switch (ReadProperty(marker, property))
            {
                case null:
                case Space { IsEmpty: true }:
                    continue;
                case Space space:
                    parts.Add($"{property.Name}={EscapeWhitespace(space.Whitespace)}");
                    continue;
                case string text:
                    parts.Add($"{property.Name}={text}");
                    continue;
                case IEnumerable items:
                    var elements = string.Join(", ", items.Cast<object?>());
                    if (elements.Length > 0)
                    {
                        parts.Add($"{property.Name}=[{elements}]");
                    }
                    continue;
                case var value:
                    parts.Add($"{property.Name}={value}");
                    continue;
            }
        }
        return string.Join(", ", parts);
    }

    private static string EscapeWhitespace(string whitespace)
    {
        var builder = new StringBuilder(whitespace.Length);
        foreach (var c in whitespace)
        {
            builder.Append(c switch
            {
                ' ' => "·",
                '\t' => "⇥",
                '\n' => "⏎",
                '\r' => "␍",
                _ => c.ToString()
            });
        }
        return builder.ToString();
    }

    private static string DisplayName(Type type)
    {
        var segments = new List<string>();
        for (var current = type; current != null; current = current.DeclaringType)
        {
            var name = current.Name;
            var tick = name.IndexOf('`');
            segments.Add(tick < 0 ? name : name[..tick]);
        }
        segments.Reverse();

        var prefix = type.Namespace switch
        {
            "OpenRewrite.CSharp" => "Cs.",
            "OpenRewrite.Java" => "J.",
            null or "OpenRewrite.Core" => "",
            var ns => ns + "."
        };
        return prefix + string.Join(".", segments);
    }
}
