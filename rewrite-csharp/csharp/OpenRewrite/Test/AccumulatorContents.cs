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
using System.Text.RegularExpressions;
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Test;

/// <summary>
/// Finds the <see cref="IHeavyweight"/> objects, such as trees, that ScanningRecipe accumulators keep. The walk reads
/// fields and the collections of .NET itself, and runs no code of the recipe's own.
/// </summary>
internal sealed class AccumulatorContents
{
    /// <summary>Beyond this many objects in one accumulator the walk stops descending.</summary>
    private const int MaxObjects = 100_000;

    private const int MaxPaths = 5;

    private static readonly Regex KeyOrIndex = new(@"\[[^\]]*\]");

    private static readonly Regex GeneratedFieldName = new(@"^<(.+)>(k__BackingField|P)$");

    private static readonly Assembly Sdk = typeof(Tree).Assembly;

    private enum Kind { Retained, Leaf, Collection, Dictionary, Delegate, Object }

    private static readonly ConcurrentDictionary<Type, Kind> Kinds = new();
    private static readonly ConcurrentDictionary<Type, FieldInfo[]> Fields = new();
    private static readonly ConcurrentDictionary<Assembly, bool> BuiltOnTheSdk = new();

    /// <summary>What each recipe's accumulator keeps, by recipe name and then by the shape of the path to it.</summary>
    private readonly Dictionary<string, Dictionary<string, Finding>> _findings = new();

    /// <summary>Walks the accumulator of each scanning recipe that ran in a cycle so far.</summary>
    public void Check(IReadOnlyDictionary<IScanningRecipe, object> accumulators, string when)
    {
        var walked = new HashSet<object>(ReferenceEqualityComparer.Instance);
        foreach (var (recipe, acc) in accumulators)
        {
            if (!walked.Add(acc))
            {
                continue;
            }
            try
            {
                new Walk(this, ((Recipe)recipe).Name, when).Run(acc);
            }
            catch (Exception)
            {
                // an accumulator the walk can't read is left alone
            }
        }
    }

    public void AssertNoneFound()
    {
        if (_findings.Count > 0)
        {
            Assert.Fail(Message());
        }
    }

    private void Found(string recipe, Node node, string when)
    {
        var path = node.Path();
        var what = Describe(node.Value);
        if (!_findings.TryGetValue(recipe, out var byShape))
        {
            _findings[recipe] = byShape = new Dictionary<string, Finding>();
        }
        byShape.TryAdd(KeyOrIndex.Replace(path, "[]") + " -> " + what, new Finding(path, what, when));
    }

    private sealed class Walk(AccumulatorContents contents, string recipe, string when)
    {
        private readonly HashSet<object> _visited = new(ReferenceEqualityComparer.Instance);
        private readonly Queue<Node> _queue = new();
        private int _budget = MaxObjects;

        /// <summary>Breadth first, so that the path to what the accumulator keeps is a short one.</summary>
        public void Run(object acc)
        {
            Visit(acc, null, TypeName(acc.GetType()));
            while (_queue.TryDequeue(out var node))
            {
                var value = node.Value;
                switch (KindOf(value.GetType()))
                {
                    case Kind.Collection:
                        var i = 0;
                        foreach (var element in (IEnumerable)value)
                        {
                            Visit(element, node, i++);
                        }
                        break;
                    case Kind.Dictionary:
                        var entries = ((IDictionary)value).GetEnumerator();
                        while (entries.MoveNext())
                        {
                            var key = new Key(entries.Key);
                            Visit(entries.Key, node, key);
                            Visit(entries.Value, node, key);
                        }
                        break;
                    case Kind.Delegate:
                        foreach (var invocation in ((Delegate)value).GetInvocationList())
                        {
                            Visit(invocation.Target, node, ".Target");
                        }
                        break;
                    default:
                        foreach (var field in FieldsOf(value.GetType()))
                        {
                            Visit(field.GetValue(value), node, field);
                        }
                        break;
                }
            }
        }

        /// <param name="step">How the parent reaches the value: a field, an index, a <see cref="Key"/> or the text
        /// of the step.</param>
        private void Visit(object? value, Node? parent, object step)
        {
            if (value == null)
            {
                return;
            }
            var kind = KindOf(value.GetType());
            if (kind == Kind.Retained)
            {
                contents.Found(recipe, new Node(value, parent, step), when);
            }
            else if (kind != Kind.Leaf && _budget > 0 && _visited.Add(value))
            {
                _budget--;
                _queue.Enqueue(new Node(value, parent, step));
            }
        }
    }

    private static Kind KindOf(Type type) => Kinds.GetOrAdd(type, t =>
    {
        if (typeof(IHeavyweight).IsAssignableFrom(t))
        {
            return Kind.Retained;
        }
        if (t.IsPrimitive || t.IsEnum || t.IsPointer || t == typeof(string) ||
            // a recipe and the execution context's build context live for the whole run anyway, type attribution
            // refers to no tree, and a template or pattern keeps only the tree of its own code
            typeof(Recipe).IsAssignableFrom(t) || t == typeof(DotNetBuildContext) ||
            typeof(JavaType).IsAssignableFrom(t) || t == typeof(CSharpTemplate) || t == typeof(CSharpPattern))
        {
            return Kind.Leaf;
        }
        if (t.IsArray)
        {
            return t.GetElementType()!.IsPrimitive ? Kind.Leaf : Kind.Collection;
        }
        if (typeof(Delegate).IsAssignableFrom(t))
        {
            return Kind.Delegate;
        }
        if (typeof(IEnumerable).IsAssignableFrom(t) && KeepsElementsInDotNet(t))
        {
            return typeof(IDictionary).IsAssignableFrom(t) ? Kind.Dictionary : Kind.Collection;
        }
        // read by its fields, since enumerating a collection class of the recipe's own runs its code
        return IsHolder(t) || IsBuiltOnTheSdk(t.Assembly) ? Kind.Object : Kind.Leaf;
    });

    /// <summary>
    /// Whether a collection is one of .NET's own or extends one, so that its elements are read by enumerating it.
    /// </summary>
    private static bool KeepsElementsInDotNet(Type type)
    {
        for (var t = type; t != null; t = t.BaseType)
        {
            if (t.Namespace?.StartsWith("System.Collections", StringComparison.Ordinal) == true)
            {
                return true;
            }
        }
        return false;
    }

    /// <summary>The .NET types that hold values for their user, whose fields the walk reads.</summary>
    private static bool IsHolder(Type type)
    {
        if (!type.IsGenericType)
        {
            return false;
        }
        var definition = type.GetGenericTypeDefinition();
        return definition == typeof(KeyValuePair<,>) || definition == typeof(StrongBox<>) ||
               definition.FullName!.StartsWith("System.Tuple`", StringComparison.Ordinal) ||
               definition.FullName.StartsWith("System.ValueTuple`", StringComparison.Ordinal);
    }

    /// <summary>The SDK and the assemblies built on it, such as recipes and their tests.</summary>
    private static bool IsBuiltOnTheSdk(Assembly assembly) => BuiltOnTheSdk.GetOrAdd(assembly, a =>
    {
        try
        {
            return a == Sdk || a.GetReferencedAssemblies().Any(r => r.Name == Sdk.GetName().Name);
        }
        catch (Exception)
        {
            return false;
        }
    });

    /// <summary>The instance fields of a type and of its base types built on the SDK that can hold a reference.</summary>
    private static FieldInfo[] FieldsOf(Type type) => Fields.GetOrAdd(type, t =>
    {
        var fields = new List<FieldInfo>();
        for (var c = t; c != null && (IsHolder(c) || IsBuiltOnTheSdk(c.Assembly)); c = c.BaseType)
        {
            foreach (var field in c.GetFields(BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic |
                                              BindingFlags.DeclaredOnly))
            {
                var fieldType = field.FieldType;
                if (!fieldType.IsPrimitive && !fieldType.IsPointer && !fieldType.IsByRefLike)
                {
                    fields.Add(field);
                }
            }
        }
        return fields.ToArray();
    });

    private static string Describe(object value) => value switch
    {
        SourceFile sourceFile => $"{TypeName(value.GetType())} {sourceFile.SourcePath}",
        Cursor => "Cursor",
        ExecutionContext => "ExecutionContext",
        _ when value.GetType().GetInterfaces().Any(i =>
            i.IsGenericType && i.GetGenericTypeDefinition() == typeof(ITreeVisitor<>)) => "TreeVisitor",
        _ => TypeName(value.GetType())
    };

    private static string TypeName(Type type)
    {
        var name = type.Name;
        var arity = name.IndexOf('`');
        if (arity >= 0)
        {
            name = name[..arity];
        }
        return type.DeclaringType == null || type.IsGenericParameter ? name : TypeName(type.DeclaringType) + "." + name;
    }

    /// <summary>The name a field has in the source, for an auto-property's and a primary constructor's own.</summary>
    private static string FieldName(FieldInfo field)
    {
        var generated = GeneratedFieldName.Match(field.Name);
        if (generated.Success)
        {
            return generated.Groups[1].Value;
        }
        var name = field.Name.StartsWith("m_Item", StringComparison.Ordinal) ? field.Name[2..] : field.Name;
        return field.DeclaringType is { IsGenericType: true } declaring &&
               declaring.GetGenericTypeDefinition() == typeof(KeyValuePair<,>)
            ? char.ToUpperInvariant(name[0]) + name[1..]
            : name;
    }

    /// <summary>A key as the path shows it, which calls no <c>ToString()</c> of the recipe's own.</summary>
    private static string KeyText(object? key)
    {
        var text = key switch
        {
            null => "null",
            Recipe recipe => recipe.Name,
            SourceFile sourceFile => sourceFile.SourcePath,
            _ when !key.GetType().IsEnum && IsBuiltOnTheSdk(key.GetType().Assembly) => TypeName(key.GetType()),
            _ => key.ToString() ?? TypeName(key.GetType())
        };
        return text.Length > 60 ? text[..60] + "..." : text;
    }

    private string Message()
    {
        var message = new StringBuilder(
            "Keep the values a recipe needs in its ScanningRecipe accumulator, such as source paths, names, ids and " +
            "positions, and no IHeavyweight objects, such as trees, cursors, visitors, execution contexts and " +
            "MSBuildProject markers:\n");
        foreach (var (recipe, findings) in _findings)
        {
            message.Append("  ").Append(recipe).Append(" keeps\n");
            var shown = 0;
            foreach (var finding in findings.Values)
            {
                if (shown++ == MaxPaths)
                {
                    message.Append("    and ").Append(findings.Count - MaxPaths).Append(" more\n");
                    break;
                }
                message.Append("    ").Append(finding.Path).Append(" -> ").Append(finding.What)
                    .Append(", ").Append(finding.When).Append('\n');
            }
        }
        return message.Append(
                "An accumulator stays in memory from the first source file scanned until the cycle ends, so what " +
                "it keeps for each source file adds up over a large repository. An MSBuildProject marker holds the " +
                "evaluated model of a whole project, while a small marker such as DotNetProject is fine to keep. A " +
                "recipe that must keep such an object opts out with " +
                "spec.SetValidations(new Validations { LightweightAccumulators = false }).")
            .ToString();
    }

    /// <summary>
    /// An object the walk reached, with the way there, which is only spelled out for a finding so that the walk
    /// calls no <c>ToString()</c> of the recipe's own.
    /// </summary>
    private sealed class Node(object value, Node? parent, object step)
    {
        public object Value { get; } = value;

        private Node? Parent { get; } = parent;

        private object Step { get; } = step;

        public string Path()
        {
            var steps = new Stack<string>();
            for (var node = this; node != null; node = node.Parent)
            {
                steps.Push(node.Step switch
                {
                    FieldInfo field => "." + FieldName(field),
                    int index => $"[{index}]",
                    Key key => $"[{KeyText(key.Value)}]",
                    _ => (string)node.Step
                });
            }
            return string.Concat(steps);
        }
    }

    private sealed record Key(object? Value);

    private sealed record Finding(string Path, string What, string When);
}
