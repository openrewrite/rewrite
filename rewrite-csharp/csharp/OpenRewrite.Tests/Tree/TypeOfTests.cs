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
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class TypeOfTests : RewriteTest
{
    [Theory]
    [InlineData("typeof(int)")]
    [InlineData("typeof (int)")]
    [InlineData("typeof( int )")]
    [InlineData("typeof(  System.Collections.Generic.List<int>  )")]
    [InlineData("typeof(System.Collections.Generic.Dictionary<,> )")]
    [InlineData("typeof(int[] )")]
    [InlineData("typeof(/*a*/ int /*b*/)")]
    [InlineData("typeof(int ).Name")]
    public void RoundTrips(string expression)
    {
        RewriteRun(
            CSharp(
                $$"""
                class Foo {
                    object Bar() {
                        return {{expression}};
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void ParenthesesCarryTheirWhitespace()
    {
        var typeOf = Single<TypeOf>("class Foo { object Bar() => typeof (  int   ); }");

        Assert.Equal(" ", typeOf.Clazz.Prefix.Whitespace);
        Assert.Equal("  ", typeOf.Clazz.Tree.Element.Prefix.Whitespace);
        Assert.Equal("   ", typeOf.Clazz.Tree.After.Whitespace);
        Assert.Equal(JavaType.PrimitiveKind.Int, Assert.IsType<Primitive>(typeOf.Clazz.Tree.Element).Kind);
    }

    [Fact]
    public void IsNotModelledAsInstanceOf()
    {
        var cu = Parse("class Foo { bool Bar(object o) => typeof(int) == o.GetType() && o is string; }");

        Assert.Single(Collect<TypeOf>(cu));
        var instanceOf = Assert.Single(Collect<InstanceOf>(cu));
        Assert.IsType<Identifier>(instanceOf.Expression.Element);
    }

    internal static CompilationUnit Parse(string source) =>
        (CompilationUnit)new CSharpParser().Parse(source);

    internal static T Single<T>(string source) where T : class, J =>
        Assert.Single(Collect<T>(Parse(source)));

    internal static List<T> Collect<T>(J tree) where T : class, J
    {
        var collector = new Collector<T>();
        collector.Visit(tree, 0);
        return collector.Found;
    }

    private class Collector<T> : CSharpVisitor<int> where T : class, J
    {
        public List<T> Found { get; } = [];

        public override J? PreVisit(J tree, int p)
        {
            if (tree is T t) Found.Add(t);
            return tree;
        }
    }
}
