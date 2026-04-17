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

namespace OpenRewrite.Java;

public partial interface J
{
    private static readonly ConcurrentDictionary<Type, MethodInfo?> WithPrefixCache = new();
    private static readonly ConcurrentDictionary<Type, MethodInfo?> WithIdCache = new();

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
}
