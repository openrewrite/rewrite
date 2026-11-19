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
using Xunit.Sdk;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// The Java recipes and the recipes of other languages that run after a C# recipe read its output as Java received
/// it, so a test that has the Java peer fails a recipe whose output doesn't survive the trip there and back.
/// </summary>
public class RpcRoundTripTest(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    private const string Hashes =
        """
        using System.Security.Cryptography;
        class Test
        {
            byte[] M(byte[] data) => SHA256.Create().ComputeHash(data, 1, data.Length);
        }
        """;

    [Fact]
    public void OutputThatSurvivesTheTripPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeLiteral("1", "0")),
            CSharp(Hashes, Hashes.Replace("data, 1,", "data, 0,"))
        );
    }

    [Fact]
    public void ATypeJavaDoesNotKeepFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ChangeLiteral("1", "0", JavaType.ShallowClass.Build("System.Decimal"))),
            CSharp(Hashes, Hashes.Replace("data, 1,", "data, 0,"))
        ));

        Assert.StartsWith(
            "The recipe's output came back from the Java peer different from what this side sent, in source.cs:\n" +
            "  sent     Literal ", failure.Message);
        Assert.Contains(" Type=System.Decimal\n  received Literal ", failure.Message);
    }

    [Fact]
    public void OptOutDisablesTheCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ChangeLiteral("1", "0", JavaType.ShallowClass.Build("System.Decimal")))
                .SetValidations(new Validations { RpcRoundTrip = false, ParserShapedTrees = false }),
            CSharp(Hashes, Hashes.Replace("data, 1,", "data, 0,"))
        );
    }

    private class ChangeLiteral(string from, string to, JavaType? type = null) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Change a literal";
        public override string Description => "Changes the integer literals of one value to another.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(from, to, type);

        private class Visitor(string from, string to, JavaType? type) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitLiteral(Literal literal, ExecutionContext ctx)
            {
                if (literal.ValueSource != from)
                {
                    return base.VisitLiteral(literal, ctx);
                }
                var changed = literal.WithValue(int.Parse(to)).WithValueSource(to);
                return type == null ? changed : changed.WithType(type);
            }
        }
    }
}
