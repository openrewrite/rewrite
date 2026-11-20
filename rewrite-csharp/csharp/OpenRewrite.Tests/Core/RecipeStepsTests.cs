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
/// A test runs several recipes one after the other, such as one that prepares the sources before the recipe under
/// test and one that checks what it left, and expects the sources the last of them leaves.
/// </summary>
public class RecipeStepsTests : RewriteTest
{
    private static string Calls(string name) =>
        $$"""
          class C
          {
              void Foo() { }
              void Bar() { }
              void Baz() { }
              void M()
              {
                  {{name}}();
              }
          }
          """;

    [Fact]
    public void TheStepsRunInOrderAndTheTestExpectsWhatTheLastLeaves()
    {
        RewriteRun(
            spec => spec.SetRecipes(new RenameCalls("Foo", "Bar"), new RenameCalls("Bar", "Baz")),
            CSharp(Calls("Foo"), Calls("Baz"))
        );
    }

    [Fact]
    public void SetRecipesNeedsARecipe()
    {
        Assert.Throws<ArgumentException>(() => RewriteRun(spec => spec.SetRecipes(), CSharp(Calls("Foo"))));
    }

    private class RenameCalls(string from, string to) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => $"Rename {from} to {to}";
        public override string Description => "Renames the calls to one method.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(from, to);

        private class Visitor(string from, string to) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var visited = base.VisitMethodInvocation(method, ctx);
                return visited is MethodInvocation call && call.Name.SimpleName == from
                    ? call.WithName(call.Name.WithSimpleName(to))
                    : visited;
            }
        }
    }
}
