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

public class ByteOrderMarkTests : RewriteTest
{
    [Fact]
    public void LeadingByteOrderMarkSetsCharsetBomMarked()
    {
        // given
        var source = "\uFEFFclass Foo { }";

        // when
        var cu = new CSharpParser().Parse(source);

        // then
        Assert.True(cu.CharsetBomMarked);
        Assert.Equal("", cu.Prefix.Whitespace);
        Assert.Equal("class Foo { }", new CSharpPrinter<int>().Print(cu));
    }

    [Fact]
    public void LeadingByteOrderMarkWithSemanticModelOfTheSameText()
    {
        // given
        var source = "\uFEFFclass Foo { int Bar() => 1; }";

        // when
        var cu = Parse(source);

        // then
        Assert.True(cu.CharsetBomMarked);
        Assert.Equal("class Foo { int Bar() => 1; }", new CSharpPrinter<int>().Print(cu));
    }

    [Fact]
    public void RoundTripsByteOrderMark()
    {
        RewriteRun(
            CSharp("\uFEFFclass Foo { }")
        );
    }

    [Fact]
    public void RoundTripsByteOrderMarkBeforeConditionalDirective()
    {
        RewriteRun(
            CSharp(
                "\uFEFF" +
                """
                #if DEBUG
                class Foo { }
                #endif
                """
            )
        );
    }

    [Fact]
    public void RoundTripsByteOrderMarkWithReferenceAssemblies()
    {
        RewriteRun(
            spec => spec.SetReferenceAssemblies(Assemblies.Net90),
            CSharp("\uFEFFclass Foo { int Bar() => 1; }")
        );
    }
}
