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
using System.Collections.Concurrent;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Text;
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Xml;

namespace OpenRewrite.Test;

/// <summary>
/// Fails a recipe run that leaves markers on the LST which neither the sources it was given nor a
/// fresh parse of its output carry. Markers travel on to every later recipe and tool, and over RPC
/// to languages that cannot deserialize a marker type of the recipe's own.
/// </summary>
internal static class AddedMarkersCheck
{
    /// <summary>A marker type, placed either on the source file itself or on an element inside it.</summary>
    public readonly record struct Placement(string MarkerType, bool OnSourceFile);

    /// <summary>The stored properties of a node type that hold its markers and its children.</summary>
    private sealed record Shape(PropertyInfo? Markers, (PropertyInfo Property, bool Many)[] Children);

    private static readonly Type[] PaddingTypes = [typeof(JRightPadded<>), typeof(JLeftPadded<>), typeof(JContainer<>)];

    private static readonly ConcurrentDictionary<Type, Shape> Shapes = new();

    /// <summary>Markers that rewrite's own Java recipes still leave for themselves, as the Java harness allows.</summary>
    private static readonly HashSet<string> RewriteRecipeMarkers =
    [
        "org.openrewrite.marker.AlreadyReplaced",
        "org.openrewrite.maven.marker.AlreadyIncremented",
        "org.openrewrite.yaml.MultilineScalarChanged",
        "org.openrewrite.java.search.UsesMethod$MethodMatch"
    ];

    public static HashSet<Placement> Placements(IEnumerable<SourceFile> sources) =>
        sources.SelectMany(source => PlacementsIn(source).Select(found => found.Placement)).ToHashSet();

    public static void Verify(HashSet<Placement> explained, IEnumerable<Result> results)
    {
        var report = new StringBuilder();
        foreach (var result in results)
        {
            if (result.After is not { } after)
            {
                continue;
            }

            var unexplained = PlacementsIn(after).Where(found => !explained.Contains(found.Placement)).ToList();
            if (unexplained.Count > 0 && after is CompilationUnit)
            {
                var reparsed = PlacementsInFreshParse(after);
                unexplained.RemoveAll(found => reparsed.Contains(found.Placement));
            }
            if (unexplained.Count == 0)
            {
                continue;
            }

            report.Append($"  {after.SourcePath}{Note(result.Before, after)}\n");
            foreach (var (placement, owner) in unexplained)
            {
                report.Append($"    {placement.MarkerType} {Where(placement, owner)}\n");
            }
        }

        if (report.Length > 0)
        {
            Assert.Fail(
                "The recipe added markers to the LST that neither the sources it was given nor a fresh parse " +
                "of its output contain:\n" +
                report +
                "Markers travel with the LST to every recipe that runs after this one and to every tool that " +
                "reads the result, so they are not a place for a recipe to keep information for itself. Instead:\n" +
                "  - share information between the visit methods of one visitor through fields of a visitor that " +
                "GetVisitor() creates for each source file, or look up enclosing elements with " +
                "Cursor.FirstEnclosing<T>()\n" +
                "  - share information between source files, or from scanning to editing, through the accumulator " +
                "of a ScanningRecipe<TAccumulator>\n" +
                "  - keep the recipe idempotent by recognizing code it has already changed, rather than tagging " +
                "that code\n" +
                "  - report a finding with SearchResult, and a problem with Markup");
        }
    }

    /// <summary>
    /// The placements in a tree, each with the node it is first found on. Padding carries markers
    /// too, so the walk covers it; type attribution carries none and is left out.
    /// </summary>
    private static List<(Placement Placement, object Owner)> PlacementsIn(SourceFile root)
    {
        var found = new List<(Placement, object)>();
        var seen = new HashSet<Placement>();
        var visited = new HashSet<object>(ReferenceEqualityComparer.Instance);
        var pending = new Stack<object>();
        pending.Push(root);
        while (pending.TryPop(out var node))
        {
            if (!visited.Add(node))
            {
                continue;
            }

            foreach (var marker in MarkersOf(node)?.MarkerList ?? [])
            {
                if (marker is SearchResult or Markup or RecipesThatMadeChanges ||
                    RewriteRecipeMarkers.Contains(MarkerType(marker)))
                {
                    continue;
                }
                var placement = new Placement(MarkerType(marker), ReferenceEquals(node, root));
                if (seen.Add(placement))
                {
                    found.Add((placement, node));
                }
            }

            var children = ChildrenOf(node);
            for (var i = children.Count - 1; i >= 0; i--)
            {
                pending.Push(children[i]);
            }
        }
        return found;
    }

    /// <summary>
    /// The placements of a fresh parse of the printed source, which explain the syntax markers that
    /// code written by a recipe brings along. A source that doesn't re-parse explains nothing.
    /// </summary>
    private static HashSet<Placement> PlacementsInFreshParse(SourceFile after)
    {
        try
        {
            var fresh = new CSharpParser().Parse(Print(after, MarkerPrinter.Sanitized), sourcePath: after.SourcePath);
            return PlacementsIn(fresh).Select(found => found.Placement).ToHashSet();
        }
        catch (Exception)
        {
            return [];
        }
    }

    private static Markers? MarkersOf(object node) => node switch
    {
        // An expression statement passes off its expression's markers as its own; they are found there
        ExpressionStatement => null,
        Tree tree => tree.Markers,
        _ => ShapeOf(node.GetType()).Markers?.GetValue(node) as Markers
    };

    private static List<object> ChildrenOf(object node)
    {
        var children = new List<object>();
        foreach (var (property, many) in ShapeOf(node.GetType()).Children)
        {
            var value = property.GetValue(node);
            if (!many)
            {
                if (value != null)
                {
                    children.Add(value);
                }
            }
            else if (value is IEnumerable items)
            {
                foreach (var item in items)
                {
                    if (item != null)
                    {
                        children.Add(item);
                    }
                }
            }
        }
        return children;
    }

    private static Shape ShapeOf(Type type) => Shapes.GetOrAdd(type, t =>
    {
        // Stored properties only: computed ones re-expose a child already reached through its padding
        var stored = t.GetProperties(BindingFlags.Public | BindingFlags.Instance)
            .Where(p => p.GetIndexParameters().Length == 0 &&
                        p.GetMethod?.IsDefined(typeof(CompilerGeneratedAttribute), false) == true)
            .ToList();
        var children = new List<(PropertyInfo, bool)>();
        foreach (var property in stored)
        {
            if (IsNode(property.PropertyType))
            {
                children.Add((property, false));
            }
            else if (ElementTypeOf(property.PropertyType) is { } element && IsNode(element))
            {
                children.Add((property, true));
            }
        }
        return new Shape(stored.FirstOrDefault(p => p.PropertyType == typeof(Markers)), children.ToArray());
    });

    private static bool IsNode(Type type) =>
        typeof(Tree).IsAssignableFrom(type) ||
        type.IsGenericType && PaddingTypes.Contains(type.GetGenericTypeDefinition());

    private static Type? ElementTypeOf(Type type) =>
        (type.IsGenericType && type.GetGenericTypeDefinition() == typeof(IEnumerable<>)
            ? type
            : type.GetInterfaces().FirstOrDefault(i =>
                i.IsGenericType && i.GetGenericTypeDefinition() == typeof(IEnumerable<>)))
        ?.GetGenericArguments()[0];

    private static string MarkerType(Marker marker) =>
        marker is UnknownMarker { JavaType: { } javaType } ? javaType : marker.GetType().FullName!;

    private static string Note(SourceFile? before, SourceFile after)
    {
        if (before == null)
        {
            return " (generated)";
        }
        return Print(before, MarkerPrinter.Default) == Print(after, MarkerPrinter.Default)
            ? " (the printed source is unchanged)"
            : "";
    }

    private static string Where(Placement placement, object owner)
    {
        if (placement.OnSourceFile)
        {
            return "on the source file";
        }
        var snippet = Snippet(owner);
        return snippet == null ? $"on {TypeName(owner.GetType())}" : $"on {TypeName(owner.GetType())} `{snippet}`";
    }

    /// <summary>The first line of a node's printed text, or null when it prints nothing of its own.</summary>
    private static string? Snippet(object owner)
    {
        // Padding prints as the element it pads; a container has no single element to print
        var tree = owner as Tree ??
                   owner.GetType().GetProperty(nameof(JRightPadded<J>.Element))?.GetValue(owner) as Tree;
        if (tree == null)
        {
            return null;
        }

        try
        {
            var printed = Print(tree is J j ? J.SetPrefix(j, Space.Empty) : tree, MarkerPrinter.Sanitized);
            var line = printed.TrimStart().Split('\n')[0].Trim();
            return line.Length == 0 ? null : line.Length > 60 ? line[..60] + "..." : line;
        }
        catch (Exception)
        {
            // Some nodes only print in the context of their parent; the type alone still locates them
            return null;
        }
    }

    private static string TypeName(Type type)
    {
        if (!type.IsGenericType)
        {
            return type.FullName ?? type.Name;
        }
        var name = type.GetGenericTypeDefinition().FullName!;
        return $"{name[..name.IndexOf('`')]}<{string.Join(", ", type.GetGenericArguments().Select(TypeName))}>";
    }

    private static string Print(Tree tree, IMarkerPrinter markerPrinter)
    {
        var capture = new PrintOutputCapture<object>(null!, markerPrinter);
        if (tree is Xml.Xml)
        {
            new XmlPrinter<object>().Visit(tree, capture);
        }
        else
        {
            new CSharpPrinter<object>().Visit(tree, capture);
        }
        return capture.ToString();
    }
}
