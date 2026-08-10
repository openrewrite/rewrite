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
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// <c>RpcSendQueue</c>'s Java-type-name overrides are process-wide mutable state: bundles register
/// into it on whichever thread loads them, while every send queue in the process reads it in
/// parallel. Held in a plain <c>Dictionary</c>, a write that resizes the buckets while another
/// thread is walking them corrupts the map — observed as a burst of <c>IndexOutOfRangeException</c>s
/// thrown from lookups that have nothing to do with the type being registered.
/// </summary>
public class RpcSendQueueJavaTypeNameConcurrencyTest
{
    /// <summary>Nesting this gives an endless supply of distinct <see cref="Type"/> keys.</summary>
    private sealed class Box<T>;

    private static List<Type> DistinctTypes(int count)
    {
        var types = new List<Type>(count);
        var t = typeof(RpcSendQueueJavaTypeNameConcurrencyTest);
        for (var i = 0; i < count; i++)
        {
            t = typeof(Box<>).MakeGenericType(t);
            types.Add(t);
        }
        return types;
    }

    [Fact]
    public void OverrideMapIsConcurrent()
    {
        var field = typeof(RpcSendQueue).GetField("JavaTypeNameOverrides",
            BindingFlags.NonPublic | BindingFlags.Static);

        Assert.NotNull(field);
        Assert.True(field!.FieldType.IsGenericType &&
                    field.FieldType.GetGenericTypeDefinition() == typeof(ConcurrentDictionary<,>),
            $"JavaTypeNameOverrides is a {field.FieldType.Name}. It is written from bundle-loading " +
            "threads while send queues read it in parallel, so it has to be a concurrent map.");
    }

    [Fact]
    public void RegistrationAndLookupRunInParallelWithoutCorruption()
    {
        var types = DistinctTypes(400);
        var registered = new ConcurrentBag<Type>();

        // The registrations resize the map repeatedly while the lookups walk it.
        Parallel.For(0, types.Count, new ParallelOptions { MaxDegreeOfParallelism = 8 }, i =>
        {
            RpcSendQueue.RegisterJavaTypeName(types[i], "org.openrewrite.test.Box" + i);
            registered.Add(types[i]);

            foreach (var seen in registered)
            {
                RpcSendQueue.ToJavaTypeName(seen);
            }
        });

        for (var i = 0; i < types.Count; i++)
        {
            Assert.Equal("org.openrewrite.test.Box" + i, RpcSendQueue.ToJavaTypeName(types[i]));
        }
    }
}
