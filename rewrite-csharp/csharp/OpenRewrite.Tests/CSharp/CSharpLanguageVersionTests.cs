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
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests.CSharp;

public class CSharpLanguageVersionTests
{
    [Theory]
    [InlineData(LanguageVersion.CSharp10, "10.0")]
    [InlineData(LanguageVersion.CSharp12, "12.0")]
    [InlineData(LanguageVersion.Latest, "14.0")]
    [InlineData(LanguageVersion.Preview, "preview")]
    public void EffectiveLanguageVersion(LanguageVersion version, string expected)
    {
        // given
        const string source = "class C {}";
        var tree = CSharpSyntaxTree.ParseText(source, new CSharpParseOptions(version));
        var model = CSharpCompilation.Create("test", [tree]).GetSemanticModel(tree);

        // when
        var cu = new CSharpParser().Parse(source, semanticModel: model);

        // then
        var marker = cu.Markers.FindFirst<CSharpLanguageVersion>();
        Assert.NotNull(marker);
        Assert.Equal(expected, marker.Version);
    }

    [Theory]
    [InlineData("#if DEBUG\nclass C {}\n#endif")]
    [InlineData("class C {}")]
    public void StandaloneLanguageVersion(string source)
    {
        // given
        var parser = new CSharpParser();

        // when
        var cu = parser.Parse(source);

        // then
        var marker = cu.Markers.FindFirst<CSharpLanguageVersion>();
        Assert.NotNull(marker);
        Assert.Equal(CSharpParseOptions.Default.LanguageVersion.ToDisplayString(), marker.Version);
        Assert.False(marker.IsTargetFrameworkCoverageComplete);
    }

    [Theory]
    [InlineData(LanguageVersion.CSharp7_3, "7.3", false)]
    [InlineData(LanguageVersion.CSharp7_3, "7.3", true)]
    [InlineData(LanguageVersion.Preview, "preview", false)]
    public void ConditionalBranchesPreserveLanguageVersion(LanguageVersion version, string expected,
        bool withConfigurations)
    {
        // given
        const string source = """
            #if OUTER
            #if INNER
            class A { }
            #else
            class B { }
            #endif
            #else
            class C { }
            #endif
            """;
        var tree = CSharpSyntaxTree.ParseText(source, new CSharpParseOptions(version));
        var model = CSharpCompilation.Create("test", [tree]).GetSemanticModel(tree);
        var parser = new CSharpParser();

        // when
        var cu = withConfigurations
            ? parser.ParseWithConfigurations(source, "source.cs", model, [new(), new() { "OUTER" }])
            : parser.Parse(source, semanticModel: model);

        // then
        var directive = Assert.IsType<ConditionalDirective>(Assert.Single(cu.Members).Element);
        Assert.True(directive.Branches.Count >= 3);
        Assert.All(new[] { cu }.Concat(directive.Branches.Select(b => b.Element)), unit =>
        {
            var marker = unit.Markers.FindFirst<CSharpLanguageVersion>();
            Assert.NotNull(marker);
            Assert.Equal(expected, marker.Version);
            Assert.False(marker.IsTargetFrameworkCoverageComplete);
        });
        Assert.Equal(source, new CSharpPrinter<int>().Print(cu));
    }

}
