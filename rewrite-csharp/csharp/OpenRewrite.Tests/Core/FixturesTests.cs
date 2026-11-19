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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// A test of a recipe on code taken from a project keeps that code in a fixture, on disk or embedded in the test
/// assembly, and checks it is the code it cites.
/// </summary>
public class FixturesTests : RewriteTest
{
    private const string Sha256 = "e99a0fe9c71ba0cd742959d3310ba260bcf8d596c67a65f0b41cf97128ef0bff";

    private const string Renamed =
        """
        class Fixture
        {
            void Foo() { }
            void Bar() { }
            void M()
            {
                Bar();
            }
        }

        """;

    [Fact]
    public void ASourceComesFromAFileInTheOutputDirectory()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls()),
            CSharpFromFile("Core/Fixtures/Fixture.cs.txt", Renamed, Sha256)
                .AfterRecipe(cu => Assert.Equal("Fixture.cs", cu.SourcePath))
        );
    }

    [Fact]
    public void ASourceComesFromAnEmbeddedResource()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameCalls()),
            CSharpFromResource("Fixture.cs.txt", Renamed, Sha256)
        );
    }

    [Fact]
    public void AFixtureThatIsNotTheCitedCodeIsRejected()
    {
        var failure = Assert.Throws<ArgumentException>(() => CSharpFromResource("Fixture.cs.txt", sha256: "00"));

        Assert.Equal($"Fixture.cs.txt has the SHA-256 {Sha256}, not 00, so it isn't the code the test cites.",
            failure.Message);
    }

    [Fact]
    public void AMissingResourceNamesTheOnesThereAre()
    {
        var failure = Assert.Throws<ArgumentException>(() => CSharpFromResource("Missing.cs.txt"));

        Assert.StartsWith("OpenRewrite.Tests embeds no resources named Missing.cs.txt, and has: ", failure.Message);
        Assert.Contains("OpenRewrite.Tests.Core.Fixtures.Fixture.cs.txt", failure.Message);
    }

    private class RenameCalls : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename calls";
        public override string Description => "Renames the calls to Foo to Bar.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var call = (MethodInvocation)base.VisitMethodInvocation(method, ctx);
                return call.Name.SimpleName == "Foo" ? call.WithName(call.Name.WithSimpleName("Bar")) : call;
            }
        }
    }
}
