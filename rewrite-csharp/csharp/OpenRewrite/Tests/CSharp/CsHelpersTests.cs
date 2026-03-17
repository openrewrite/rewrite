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
using OpenRewrite.CSharp;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.CSharp;

public class CsHelpersTests
{
    private static Identifier MakeId(string name) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty, [], name, null, null);

    private static FieldAccess MakeFieldAccess(string target, string name) =>
        new(Guid.NewGuid(), Space.Empty, Markers.Empty,
            MakeId(target),
            new JLeftPadded<Identifier>(Space.Empty, MakeId(name)),
            null);

    // =============================================================
    // GetSimpleName
    // =============================================================

    [Fact]
    public void GetSimpleName_Identifier()
    {
        var id = MakeId("Obsolete");
        Assert.Equal("Obsolete", Cs.GetSimpleName(id));
    }

    [Fact]
    public void GetSimpleName_FieldAccess()
    {
        var fa = MakeFieldAccess("System", "Obsolete");
        Assert.Equal("Obsolete", Cs.GetSimpleName(fa));
    }

    [Fact]
    public void GetSimpleName_UnsupportedNameTree()
    {
        var parameterized = new ParameterizedType(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            MakeId("List"), null, null);
        Assert.Null(Cs.GetSimpleName(parameterized));
    }

    // =============================================================
    // HasNoArguments
    // =============================================================

    [Fact]
    public void HasNoArguments_EmptyList()
    {
        var mi = new MethodInvocation(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("Foo"), null,
            new JContainer<Expression>(Space.Empty, [], Markers.Empty),
            null);
        Assert.True(Cs.HasNoArguments(mi));
    }

    [Fact]
    public void HasNoArguments_EmptySentinel()
    {
        var empty = new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty);
        var mi = new MethodInvocation(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("Foo"), null,
            new JContainer<Expression>(Space.Empty, [Pad<Expression>(empty)], Markers.Empty),
            null);
        Assert.True(Cs.HasNoArguments(mi));
    }

    [Fact]
    public void HasNoArguments_WithArgument()
    {
        var arg = MakeId("x");
        var mi = new MethodInvocation(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("Foo"), null,
            new JContainer<Expression>(Space.Empty, [Pad<Expression>(arg)], Markers.Empty),
            null);
        Assert.False(Cs.HasNoArguments(mi));
    }

    [Fact]
    public void HasNoArguments_MultipleArguments()
    {
        var mi = new MethodInvocation(
            Guid.NewGuid(), Space.Empty, Markers.Empty,
            null, MakeId("Foo"), null,
            new JContainer<Expression>(Space.Empty,
                [Pad<Expression>(MakeId("x")), Pad<Expression>(MakeId("y"))], Markers.Empty),
            null);
        Assert.False(Cs.HasNoArguments(mi));
    }

    private static JRightPadded<T> Pad<T>(T element) =>
        new(element, Space.Empty, Markers.Empty);
}
