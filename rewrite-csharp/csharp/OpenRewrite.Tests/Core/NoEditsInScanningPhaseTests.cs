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
/// The scheduler discards what a ScanningRecipe's scanner returns, so the harness fails a scanner that edits.
/// </summary>
public class NoEditsInScanningPhaseTests : RewriteTest
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
    public void AScannerThatEditsFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(RecordingAndThen(new RenameFoo())),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Equal(
            "Record what the scanner finds in the accumulator, and make the edits in GetVisitor(acc).\n" +
            "OpenRewrite.Tests.Core.NoEditsInScanningPhaseTests+RenameFooWhenCalled's scanner changed source.cs, " +
            "and the scanning phase discards edits. A scanner that reuses an editing visitor opts out with " +
            "spec.SetValidations(new Validations { NoEditsInScanningPhase = false }).",
            failure.Message);
    }

    [Fact]
    public void AScannerThatDeletesFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(RecordingAndThen(new DeleteFile())),
            CSharp(Calls("Foo"), Calls("Bar"))
        ));

        Assert.Contains("RenameFooWhenCalled's scanner deleted source.cs, and the scanning phase discards edits.",
            failure.Message);
    }

    [Fact]
    public void AScannerThatRecordsWhatItFindsPasses()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RenameFooWhenCalled(calls => new CollectCalls(calls))),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    [Fact]
    public void OptOutDisablesTheCheck()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(RecordingAndThen(new RenameFoo()))
                .SetValidations(new Validations { NoEditsInScanningPhase = false }),
            CSharp(Calls("Foo"), Calls("Bar"))
        );
    }

    /// <summary>A scanner that records the calls and then runs <paramref name="visitor"/> on the file.</summary>
    private static RenameFooWhenCalled RecordingAndThen(ITreeVisitor<ExecutionContext> visitor) =>
        new(calls => new CollectCalls(calls).Combine(visitor));

    /// <summary>Renames calls to Foo once a scan saw one, with the scanner the test gives it.</summary>
    private class RenameFooWhenCalled(Func<HashSet<string>, ITreeVisitor<ExecutionContext>> scanner)
        : ScanningRecipe<HashSet<string>>
    {
        public override string DisplayName => "Rename Foo to Bar when called";
        public override string Description => "Renames calls to Foo when a source file calls it.";

        public override HashSet<string> GetInitialValue(ExecutionContext ctx) => [];

        public override ITreeVisitor<ExecutionContext> GetScanner(HashSet<string> calls) => scanner(calls);

        public override ITreeVisitor<ExecutionContext> GetVisitor(HashSet<string> calls) =>
            calls.Contains("Foo") ? new RenameFoo() : ITreeVisitor<ExecutionContext>.Noop();
    }

    private class CollectCalls(HashSet<string> calls) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
        {
            calls.Add(method.Name.SimpleName);
            return base.VisitMethodInvocation(method, ctx);
        }
    }

    private class RenameFoo : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
        {
            var visited = base.VisitMethodInvocation(method, ctx);
            return visited is MethodInvocation { Name.SimpleName: "Foo" } call
                ? call.WithName(call.Name.WithSimpleName("Bar"))
                : visited;
        }
    }

    private class DeleteFile : CSharpVisitor<ExecutionContext>
    {
        public override J? Visit(global::OpenRewrite.Core.Tree? tree, ExecutionContext ctx) => null;
    }
}
