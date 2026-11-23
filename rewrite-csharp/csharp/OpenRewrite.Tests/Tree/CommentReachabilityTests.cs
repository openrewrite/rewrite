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

namespace OpenRewrite.Tests.Tree;

public class CommentReachabilityTests
{
    private static bool ProbeReachable(string code)
    {
        var cu = new CSharpParser().Parse(code);
        var probe = new SpaceProbe();
        probe.Visit(cu, 0);
        return probe.Found;
    }

    private static void AssertRoundTrips(string code)
    {
        Assert.Equal(code, new CSharpPrinter<int>().Print(new CSharpParser().Parse(code, "Test.cs")));
    }

    private static void AssertReachable(string code)
    {
        Assert.True(ProbeReachable(code),
            "Expected the PROBE comment to be reachable through VisitSpace, but it never was.");
        AssertRoundTrips(code);
    }

    [Fact]
    public void Control()
    {
        AssertReachable(
            """
            class Test
            {
                // PROBE
                void M() { }
            }
            """);
    }

    [Fact]
    public void NullableDirectiveTrailingComment() =>
        AssertReachable("#nullable enable // PROBE\n\nclass Test { }");

    [Fact]
    public void DefineDirectiveTrailingComment() =>
        AssertReachable("#define FOO // PROBE\n\nclass Test { }");

    [Fact]
    public void UndefDirectiveTrailingComment() =>
        AssertReachable("#undef FOO // PROBE\n\nclass Test { }");

    [Fact]
    public void PragmaWarningDirectiveTrailingComment() =>
        AssertReachable("#pragma warning disable CS0618 // PROBE\n\nclass Test { }");

    [Fact(Skip = "#if/#elif/#else/#endif trivia needs the ConditionalDirective text model reworked")]
    public void ConditionalDirectiveTrailingComment() =>
        AssertReachable("#if DEBUG // PROBE\nclass Test { }\n#endif\n");

    [Fact]
    public void EndRegionDirectiveTrailingComment() =>
        AssertReachable("#region Helpers\nclass Test { }\n#endregion // PROBE\n");

    [Fact]
    public void LineDirectiveTrailingComment() =>
        AssertReachable("#line 5 \"f.cs\" // PROBE\n\nclass Test { }");

    [Fact]
    public void LineHiddenDirectiveTrailingComment() =>
        AssertReachable("#line hidden // PROBE\n\nclass Test { }");

    [Fact]
    public void PragmaChecksumDirectiveTrailingComment() =>
        AssertReachable(
            "#pragma checksum \"f.cs\" \"{ff1816ec-aa5e-4d10-87f7-6f4963833460}\" \"ab\" // PROBE\n\nclass Test { }");

    [Fact]
    public void DirectiveBlockComment() =>
        AssertReachable("#nullable enable /* PROBE */\n\nclass Test { }");

    [Fact]
    public void InteriorBlockCommentStaysWithDirective() =>
        AssertRoundTrips("#pragma warning disable /* PROBE */ CS0618\n\nclass Test { }");

    [Theory]
    [InlineData("#region Helpers // PROBE\nclass Test { }\n#endregion\n")]
    [InlineData("#error boom // PROBE\n\nclass Test { }")]
    [InlineData("#warning boom // PROBE\n\nclass Test { }")]
    public void PreprocessingMessageIsNotAComment(string code)
    {
        Assert.False(ProbeReachable(code),
            "The rest of the line is a preprocessing message, so it must not become trivia.");
        AssertRoundTrips(code);
    }

    [Fact]
    public void AttributeOnModifiedMethod() =>
        AssertReachable(
            """
            class Test
            {
                [System.Obsolete] // PROBE
                public void M() { }
            }
            """);

    [Fact]
    public void AttributeOnUnmodifiedType() =>
        AssertReachable("[System.Obsolete] // PROBE\nclass Test { }");

    [Fact]
    public void AttributeOnAccessor() =>
        AssertReachable(
            """
            class Test
            {
                int P
                {
                    [System.Obsolete] // PROBE
                    get { return 1; }
                }
            }
            """);

    [Fact]
    public void TrailingCommaInEnum() =>
        AssertReachable(
            """
            enum E
            {
                A = 1,
                // PROBE
            }
            """);

    [Fact]
    public void TrailingCommaInCollectionInitializer() =>
        AssertReachable(
            """
            using System.Collections.Generic;

            class Test
            {
                void M()
                {
                    var d = new List<int>
                    {
                        1,
                        // PROBE
                    };
                }
            }
            """);

    [Fact]
    public void ArrayRankSpecifier() =>
        AssertReachable(
            """
            class Test
            {
                int[ /* PROBE */ ] A;
            }
            """);

    [Fact]
    public void AttributeOnTypeParameter() =>
        AssertReachable("class C<[System.Obsolete] // PROBE\n T> { }");

    [Fact]
    public void GenericTypeArgumentLeadingComment() =>
        AssertReachable(
            """
            using System.Collections.Generic;

            class Test
            {
                List</* PROBE */ int> A;
            }
            """);

    [Fact]
    public void GenericTypeArgumentTrailingComment() =>
        AssertReachable(
            """
            using System.Collections.Generic;

            class Test
            {
                List<int // PROBE
                    > A;
            }
            """);

    private sealed class SpaceProbe : CSharpVisitor<int>
    {
        public bool Found { get; private set; }

        public override Space VisitSpace(Space space, int p)
        {
            foreach (var comment in space.Comments)
            {
                if (comment.Text.Contains("PROBE"))
                {
                    Found = true;
                }
            }

            return space;
        }
    }
}
