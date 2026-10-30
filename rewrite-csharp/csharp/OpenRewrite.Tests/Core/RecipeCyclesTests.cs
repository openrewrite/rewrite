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
/// The harness runs a recipe once more than the cycles it is expected to need, and once more on a fresh
/// parse of its output, and fails a recipe that changes the sources again.
/// </summary>
public class RecipeCyclesTests : RewriteTest
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
    public void ARecipeThatChangesItsExpectedResultAgainFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameCalls(new() { ["Foo"] = "Bar", ["Bar"] = "Foo" })),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.StartsWith(
            "Expected recipe to complete in 1 cycle, but cycle 2 changed the sources again:\n  source.cs\n",
            failure.Message);
        Assert.Contains("-        Bar();\n+        Foo();", failure.Message);
    }

    [Fact]
    public void ExpectedCyclesThatMakeChangesComparesTheResultOfTheLastExpectedCycle()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls(new() { ["Foo"] = "Bar", ["Bar"] = "Baz" }))
                .SetExpectedCyclesThatMakeChanges(2),
            CSharp(Calls("Foo"), Calls("Baz"))
        );
    }

    [Fact]
    public void ExpectedCyclesThatMakeChangesFailsARecipeThatSettlesSooner()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls(new() { ["Foo"] = "Bar" }))
                .SetExpectedCyclesThatMakeChanges(2),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Equal(
            "Expected recipe to complete in 2 cycles, but it settled after 1 cycle. " +
            "Lower ExpectedCyclesThatMakeChanges.",
            failure.Message);
    }

    [Fact]
    public void ExpectedCyclesThatMakeChangesOfZeroFailsARecipeThatChanges()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls(new() { ["Foo"] = "Bar" }))
                .SetExpectedCyclesThatMakeChanges(0),
            CSharp(Calls("Foo"))
        ));

        Assert.StartsWith("Expected recipe to complete in 0 cycles, but cycle 1 changed the sources again:",
            failure.Message);
    }

    [Fact]
    public void FewerCyclesThanExpectedToMakeChangesIsRejected()
    {
        Assert.Throws<ArgumentException>(() => RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls(new() { ["Foo"] = "Bar" }))
                .SetCycles(1)
                .SetExpectedCyclesThatMakeChanges(2),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));
    }

    [Fact]
    public void OneCycleTurnsTheCheckOff()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameCalls(new() { ["Foo"] = "Bar", ["Bar"] = "Foo" }))
                .SetCycles(1),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void ARecipeThatChangesTheLstAgainWithoutChangingTheTextFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ChangeCalls(call => call.Name.SimpleName switch
            {
                "Foo" => call.WithName(call.Name.WithSimpleName("Bar")),
                "Bar" => call.WithName(call.Name.WithId(Guid.NewGuid())),
                _ => call
            })),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.StartsWith(
            "Expected recipe to complete in 1 cycle, but cycle 2 changed the sources again:\n" +
            "  source.cs, whose LST it changed without changing the printed source\n",
            failure.Message);
    }

    [Fact]
    public void ARecipeThatGeneratesAFileAgainFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new GenerateClass()),
            CSharp(Calls("Foo"))
        ));

        Assert.StartsWith(
            "Expected recipe to complete in 1 cycle, but cycle 2 changed the sources again:\n" +
            "  Generated.cs, which it generated\n",
            failure.Message);
    }

    [Fact]
    public void DataTablesAreWrittenByTheFirstCycleOnly()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameAndReportCalls())
                .ExpectDataTable<CalledMethod>(CalledMethods.Name, CalledMethods.InstanceName,
                    rows => Assert.Equal([new CalledMethod { Name = "Foo" }], rows)),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    /// <summary>
    /// The recipe writes <c>A.Foo</c> as a single identifier, which it does not match again, but a fresh
    /// parse reads it as a call to <c>Foo</c> on <c>A</c>.
    /// </summary>
    [Fact]
    public void ARecipeThatRecognizesItsOutputOnlyByTheShapeOfItsLstFailsOnAFreshParse()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new ChangeCalls(call => call.Name.SimpleName == "Foo"
                ? call.WithName(call.Name.WithSimpleName("A.Foo"))
                : call)),
            CSharp(Calls("Foo"), Calls("A.Foo"))
        ));

        Assert.StartsWith(
            "Running the recipe on a fresh parse of its output changed the sources again:\n  source.cs\n",
            failure.Message);
        Assert.Contains("-        A.Foo();\n+        A.A.Foo();", failure.Message);
    }

    [Fact]
    public void AFreshParseIsTypeAttributed()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeCalls(call => call switch
            {
                { MethodType: null } => call.WithName(call.Name.WithSimpleName("Untyped")),
                { Name.SimpleName: "Foo" } => call.WithName(call.Name.WithSimpleName("Bar")),
                _ => call
            })),
            CSharp(
                """
                class C
                {
                    void M()
                    {
                        Foo();
                    }

                    void Foo() { }
                    void Bar() { }
                }
                """,
                """
                class C
                {
                    void M()
                    {
                        Bar();
                    }

                    void Foo() { }
                    void Bar() { }
                }
                """
            )
        );
    }

    private class RenameCalls(Dictionary<string, string> renames) : ChangeCalls(call =>
        renames.TryGetValue(call.Name.SimpleName, out var to) ? call.WithName(call.Name.WithSimpleName(to)) : call);

    private class ChangeCalls(Func<MethodInvocation, ExecutionContext, MethodInvocation> change)
        : global::OpenRewrite.Core.Recipe
    {
        public ChangeCalls(Func<MethodInvocation, MethodInvocation> change) : this((call, _) => change(call))
        {
        }

        public override string DisplayName => "Change calls";
        public override string Description => "Applies a change to every method call.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() => new Visitor(change);

        private class Visitor(Func<MethodInvocation, ExecutionContext, MethodInvocation> change)
            : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var visited = base.VisitMethodInvocation(method, ctx);
                return visited is MethodInvocation call ? change(call, ctx) : visited;
            }
        }
    }

    private class GenerateClass : ScanningRecipe<object>
    {
        public override string DisplayName => "Generate a class";
        public override string Description => "Generates a class without checking whether it exists.";

        public override object GetInitialValue(ExecutionContext ctx) => new();

        public override ITreeVisitor<ExecutionContext> GetScanner(object acc) => ITreeVisitor<ExecutionContext>.Noop();

        public override IEnumerable<SourceFile> Generate(object acc, ExecutionContext ctx) =>
            [new CSharpParser().Parse("class G { }", sourcePath: "Generated.cs")];
    }

    private record CalledMethod
    {
        [Column(DisplayName = "Name", Description = "The name of the called method.")]
        public string Name { get; init; } = "";
    }

    private static readonly DataTable<CalledMethod> CalledMethods =
        new("org.openrewrite.test.CalledMethods", "Called methods", "Methods called.");

    private class RenameAndReportCalls() : ChangeCalls((call, ctx) =>
    {
        CalledMethods.InsertRow(ctx, new CalledMethod { Name = call.Name.SimpleName });
        return call.Name.SimpleName == "Foo" ? call.WithName(call.Name.WithSimpleName("Bar")) : call;
    });
}
