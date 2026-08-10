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

namespace OpenRewrite.Tests.Core;

/// <summary>
/// Every LST node has a prefix and markers, so every concrete node type must expose the withers for
/// them. <c>Markup</c>, <c>SearchResult</c>, <c>J.SetPrefix</c> and <c>J.SetMarkers</c> all reach
/// those withers reflectively and now throw when a type has none — this test is what keeps that
/// throw unreachable for in-tree nodes.
/// <para>
/// The gap this guards against is invisible without it: <c>Cs.ExpressionStatement</c> delegated
/// <c>Prefix</c>/<c>Markers</c> to the expression it wraps but declared neither wither, so every
/// search recipe that marked a bare expression statement (<c>Foo();</c>) reported nothing at all.
/// </para>
/// </summary>
public class JWitherContractTests
{
    public static TheoryData<Type> NodeTypes()
    {
        var data = new TheoryData<Type>();
        foreach (var type in typeof(J).Assembly.GetTypes())
        {
            if (type is { IsClass: true, IsAbstract: false } && typeof(J).IsAssignableFrom(type))
            {
                data.Add(type);
            }
        }
        return data;
    }

    [Theory]
    [MemberData(nameof(NodeTypes))]
    public void ExposesWithMarkers(Type nodeType) =>
        Assert.True(nodeType.GetMethod("WithMarkers", [typeof(Markers)]) != null,
            $"{nodeType.FullName} has no WithMarkers(Markers); markers attached to it would be silently dropped.");

    [Theory]
    [MemberData(nameof(NodeTypes))]
    public void ExposesWithPrefix(Type nodeType) =>
        Assert.True(nodeType.GetMethod("WithPrefix", [typeof(Space)]) != null,
            $"{nodeType.FullName} has no WithPrefix(Space); whitespace assigned to it would be silently dropped.");
}
