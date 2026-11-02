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
/// Every recipe and source file in a run shares the execution context, so the harness fails a recipe that keeps
/// its own state there.
/// </summary>
public class ExecutionContextUnchangedTests : RewriteTest
{
    private static string Calls(string name) =>
        $$"""
          class C
          {
              void M()
              {
                  {{name}}();
              }
          }
          """;

    [Fact]
    public void PuttingAMessageFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFoo(ctx => ctx.PutMessage("renamed", true))),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Equal(
            "Keep state that spans source files in a ScanningRecipe's accumulator, and state within one source " +
            "file in fields of the visitor that GetVisitor() creates for that file, rather than on the " +
            "ExecutionContext.\n" +
            "OpenRewrite.Tests.Core.ExecutionContextUnchangedTests+RenameFoo put \"renamed\" on the " +
            "ExecutionContext, which every recipe and source file in a run shares, so what it keeps there leaks " +
            "into the others. A recipe that must change the ExecutionContext opts out with " +
            "spec.SetValidations(new Validations { ExecutionContextUnchanged = false }).",
            failure.Message);
    }

    [Fact]
    public void ComputingAMessageFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFoo(ctx => ctx.ComputeMessageIfAbsent("renamed", _ => new object()))),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("RenameFoo put \"renamed\" on the ExecutionContext", failure.Message);
    }

    [Fact]
    public void TheFailureNamesTheRecipeOfACompositeThatPutTheMessage()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new Composite(new RenameFoo(_ => { }), new CountCalls())),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("\nOpenRewrite.Tests.Core.ExecutionContextUnchangedTests+CountCalls put \"calls\"",
            failure.Message);
    }

    [Fact]
    public void AFailureTheRecipeSwallowsStillFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameFoo(ctx =>
            {
                try
                {
                    ctx.PutMessage("renamed", true);
                }
                catch (Exception)
                {
                    // a recipe that guards its visit against every exception
                }
            })),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("RenameFoo put \"renamed\" on the ExecutionContext", failure.Message);
    }

    [Fact]
    public void ARecipeThatWritesADataTablePasses()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameFoo(ctx => RenamedCalls.InsertRow(ctx, new RenamedCall { Name = "Foo" })))
                .ExpectDataTable<RenamedCall>(RenamedCalls.Name, RenamedCalls.InstanceName,
                    rows => Assert.Equal([new RenamedCall { Name = "Foo" }], rows)),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void OptOutDisablesTheCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameFoo(ctx => ctx.PutMessage("renamed", true)))
                .SetValidations(new Validations { ExecutionContextUnchanged = false }),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    private class RenameFoo(Action<ExecutionContext> onRename) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Rename Foo to Bar";
        public override string Description => "Renames every call to Foo.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(onRename);

        private class Visitor(Action<ExecutionContext> onRename) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var visited = base.VisitMethodInvocation(method, ctx);
                if (visited is not MethodInvocation { Name.SimpleName: "Foo" } call)
                {
                    return visited;
                }
                onRename(ctx);
                return call.WithName(call.Name.WithSimpleName("Bar"));
            }
        }
    }

    /// <summary>Counts the calls in the execution context.</summary>
    private class CountCalls : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Count calls";
        public override string Description => "Counts method calls.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor();

        private class Visitor : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                ctx.PutMessage("calls", (ctx.GetMessage<object>("calls") as int? ?? 0) + 1);
                return base.VisitMethodInvocation(method, ctx);
            }
        }
    }

    private class Composite(params global::OpenRewrite.Core.Recipe[] recipes) : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Composite";
        public override string Description => "Runs its recipe list.";

        public override List<global::OpenRewrite.Core.Recipe> GetRecipeList() => [.. recipes];
    }

    private record RenamedCall
    {
        [Column(DisplayName = "Name", Description = "The name of the renamed method.")]
        public string Name { get; init; } = "";
    }

    private static readonly DataTable<RenamedCall> RenamedCalls =
        new("org.openrewrite.test.RenamedCalls", "Renamed calls", "Calls the recipe renamed.");
}
