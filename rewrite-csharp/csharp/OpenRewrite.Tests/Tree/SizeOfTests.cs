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
using OpenRewrite.CSharp;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class SizeOfTests : RewriteTest
{
    [Theory]
    [InlineData("sizeof(int)")]
    [InlineData("sizeof (int)")]
    [InlineData("sizeof( int )")]
    [InlineData("sizeof(/*a*/ long /*b*/)")]
    public void RoundTrips(string expression)
    {
        RewriteRun(
            CSharp(
                $$"""
                class Foo {
                    int Bar() {
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
        var sizeOf = TypeOfTests.Single<SizeOf>("class Foo { int Bar() => sizeof (  int   ); }");

        Assert.Equal(" ", sizeOf.Clazz.Prefix.Whitespace);
        Assert.Equal("  ", sizeOf.Clazz.Tree.Element.Prefix.Whitespace);
        Assert.Equal("   ", sizeOf.Clazz.Tree.After.Whitespace);
    }
}
