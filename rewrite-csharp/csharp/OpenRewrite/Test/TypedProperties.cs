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
using OpenRewrite.Java.Rpc;

namespace OpenRewrite.Test;

/// <summary>The properties of a node that hold its types, such as an invocation's <c>MethodType</c>.</summary>
internal static class TypedProperties
{
    private static readonly ConcurrentDictionary<Type, PropertyInfo[]> Properties = new();

    /// <summary>The types a node holds, by the name of the property that holds each, in name order.</summary>
    public static List<(string Property, JavaType? Type)> Of(Tree tree) =>
        Properties.GetOrAdd(tree.GetType(), type => type
                .GetProperties(BindingFlags.Public | BindingFlags.Instance)
                .Where(p => p.GetIndexParameters().Length == 0 && typeof(JavaType).IsAssignableFrom(p.PropertyType))
                .OrderBy(p => p.Name, StringComparer.Ordinal)
                .ToArray())
            .Select(p => (p.Name, p.GetValue(tree) as JavaType))
            .ToList();

    public static string Signature(JavaType type) => JavaSender.TypeSignature(type).ToString() ?? "";
}
