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

namespace OpenRewrite.Tests.Core;

/// <summary>
/// Recipes that run later match the tree the parser builds, so the harness fails a recipe whose output holds a node
/// that the parser builds differently from the text it prints.
/// </summary>
public class ParserShapedTreesTests : RewriteTest
{
    private static string Returns(string value) =>
        $$"""
          class A
          {
              object F()
              {
                  return {{value}};
              }
          }
          """;

    [Fact]
    public void ANodeOfAnotherKindThanTheParserBuildsFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ReturnNull(literal =>
                new Identifier(literal.Id, literal.Prefix, literal.Markers, [], "null", null, null))),
            CSharp(Returns("0"), Returns("null"))
        ));

        Assert.Equal(
            "Write code with a CSharpTemplate or the C# parser, or build the nodes the way the parser builds the " +
            "text they print.\n" +
            "The output of OpenRewrite.Tests.Core.ParserShapedTreesTests+ReturnNull holds nodes that the parser " +
            "makes differently of the same text:\n" +
            "  source.cs:\n" +
            "    `null` is an Identifier where the parser makes a Literal\n" +
            "A recipe that runs after it matches the tree, not the text, so it misses or mistakes a node that parsed " +
            "code never holds. If the output is meant to differ from parsed code, opt out with " +
            "spec.SetValidations(new Validations { ParserShapedTrees = false }).",
            failure.Message);
    }

    [Fact]
    public void ANodeWithAnotherValueThanTheParserGivesItFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ReturnNull(literal => literal
                .WithValue("null").WithValueSource("null")
                .WithType(JavaType.Primitive.Of(JavaType.PrimitiveKind.String)))),
            CSharp(Returns("0"), Returns("null"))
        ));

        Assert.Contains(
            "\n    `null` has the String value \"null\" of type String where the parser makes the value null of " +
            "type Null\n",
            failure.Message);
    }

    [Fact]
    public void OutputThatDoesNotParseFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ReturnNull(literal => literal.WithValueSource("null null"))),
            CSharp(Returns("0"), Returns("null null"))
        ));

        Assert.Contains("\n    the text it prints doesn't parse: CS1002 ; expected at `return null null;`\n",
            failure.Message);
    }

    /// <summary>A recipe that keeps code the parser couldn't read, such as a test's broken code, didn't break it.</summary>
    [Fact]
    public void ASourceThatAlreadyDidNotParsePasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ReturnNull(literal => literal
                .WithValue(null).WithValueSource("null")
                .WithType(JavaType.Primitive.Of(JavaType.PrimitiveKind.Null)))),
            CSharp(Returns("0;\n        owner."), Returns("null;\n        owner."))
        );
    }

    [Fact]
    public void ANodeTheParserBuildsTheSamePasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ReturnNull(literal => literal
                .WithValue(null).WithValueSource("null")
                .WithType(JavaType.Primitive.Of(JavaType.PrimitiveKind.Null)))),
            CSharp(Returns("0"), Returns("null"))
        );
    }

    [Fact]
    public void OptOutDisablesTheCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new ReturnNull(literal =>
                    new Identifier(literal.Id, literal.Prefix, literal.Markers, [], "null", null, null)))
                .SetValidations(new Validations { ParserShapedTrees = false }),
            CSharp(Returns("0"), Returns("null"))
        );
    }

    /// <summary>Replaces a returned <c>0</c> with what the test builds for <c>null</c>.</summary>
    private class ReturnNull(Func<Literal, Expression> build) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Return null";
        public override string Description => "Returns null where a method returns 0.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(build);

        private class Visitor(Func<Literal, Expression> build) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitReturn(Return @return, ExecutionContext ctx)
            {
                var visited = base.VisitReturn(@return, ctx);
                return visited is Return { Expression: Literal { Value: 0 } literal } ret
                    ? ret.WithExpression(build(literal))
                    : visited;
            }
        }
    }
}
