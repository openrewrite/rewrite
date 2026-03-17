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
using System.Reflection;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Generic helpers for operating on LST nodes without type-specific switches.
/// Uses reflection with caching to call WithPrefix and enumerate structural properties.
/// </summary>
public static class TreeHelper
{
    private static readonly ConcurrentDictionary<Type, MethodInfo?> WithPrefixCache = new();
    private static readonly ConcurrentDictionary<Type, MethodInfo?> WithIdCache = new();

    /// <summary>
    /// Property names to always skip when comparing tree nodes structurally.
    /// </summary>
    private static readonly HashSet<string> SkipPropertyNames =
        ["Id", "Prefix", "Markers", "SourcePath"];

    /// <summary>
    /// Property types to skip when comparing tree nodes structurally.
    /// These represent formatting, identity, or type metadata — not structural content.
    /// </summary>
    private static readonly HashSet<Type> SkipPropertyTypes =
    [
        typeof(Space), typeof(Guid), typeof(Markers),
    ];

    private static readonly ConcurrentDictionary<Type, PropertyInfo[]> StructuralPropertiesCache = new();

    /// <summary>
    /// Call WithPrefix on any J node via cached reflection, preserving the concrete type.
    /// Returns the original node unchanged if the type doesn't have WithPrefix.
    /// </summary>
    public static T SetPrefix<T>(T node, Space prefix) where T : J
    {
        var method = WithPrefixCache.GetOrAdd(node.GetType(), type =>
            type.GetMethod("WithPrefix", [typeof(Space)]));

        if (method != null)
            return (T)method.Invoke(node, [prefix])!;

        return node;
    }

    /// <summary>
    /// Call WithId on any J node via cached reflection, preserving the concrete type.
    /// Returns the original node unchanged if the type doesn't have WithId.
    /// </summary>
    public static T SetId<T>(T node, Guid id) where T : J
    {
        var method = WithIdCache.GetOrAdd(node.GetType(), type =>
            type.GetMethod("WithId", [typeof(Guid)]));

        if (method != null)
            return (T)method.Invoke(node, [id])!;

        return node;
    }

    /// <summary>
    /// Get the structural (non-formatting) properties of a J node for comparison.
    /// Returns properties that represent tree structure: child nodes, names, values, etc.
    /// Skips properties by name (Id, Prefix, Markers, SourcePath) and by type
    /// (Space, Guid, Markers, JavaType and subtypes).
    /// </summary>
    internal static IReadOnlyList<PropertyInfo> GetStructuralProperties(Type type)
    {
        return StructuralPropertiesCache.GetOrAdd(type, t =>
            t.GetProperties(BindingFlags.Public | BindingFlags.Instance)
                .Where(p => !SkipPropertyNames.Contains(p.Name) && !IsSkippedType(p.PropertyType))
                .ToArray());
    }

    private static bool IsSkippedType(Type type)
    {
        // Skip exact type matches
        if (SkipPropertyTypes.Contains(type)) return true;
        // Skip nullable versions of skipped types
        var underlying = Nullable.GetUnderlyingType(type);
        if (underlying != null && SkipPropertyTypes.Contains(underlying)) return true;
        // Skip JavaType and all its subtypes (JavaType.Method, JavaType.FullyQualified, etc.)
        if (typeof(JavaType).IsAssignableFrom(type)) return true;
        return false;
    }

    /// <summary>
    /// Check if a value is a J node (tree element).
    /// </summary>
    internal static bool IsTreeNode(object? value) => value is J;

    /// <summary>
    /// Check if a value is a padded wrapper (JRightPadded, JLeftPadded, JContainer).
    /// </summary>
    internal static bool IsPaddedWrapper(object? value)
    {
        if (value == null) return false;
        var type = value.GetType();
        if (!type.IsGenericType) return false;
        var generic = type.GetGenericTypeDefinition();
        return generic == typeof(JRightPadded<>) ||
               generic == typeof(JLeftPadded<>) ||
               generic == typeof(JContainer<>);
    }

    /// <summary>
    /// Extract the inner element from a padded wrapper.
    /// </summary>
    internal static object? UnwrapPadded(object value)
    {
        var type = value.GetType();
        var elementProp = type.GetProperty("Element");
        return elementProp?.GetValue(value);
    }

    /// <summary>
    /// Extract elements from a JContainer.
    /// </summary>
    internal static IList<object>? GetContainerElements(object value)
    {
        var type = value.GetType();
        if (!type.IsGenericType || type.GetGenericTypeDefinition() != typeof(JContainer<>))
            return null;

        var elementsProp = type.GetProperty("Elements");
        if (elementsProp?.GetValue(value) is not System.Collections.IList list)
            return null;

        var result = new List<object>(list.Count);
        foreach (var item in list)
        {
            if (item != null) result.Add(item);
        }
        return result;
    }
}
