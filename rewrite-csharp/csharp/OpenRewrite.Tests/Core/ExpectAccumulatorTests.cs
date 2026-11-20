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
/// A scanning recipe decides what to change by what its scanner found, which a test asserts on to show why the recipe
/// left a file alone.
/// </summary>
public class ExpectAccumulatorTests : RewriteTest
{
    [Fact]
    public void TheTestSeesWhatTheScannerFound()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameUnlessShadowed())
                .ExpectAccumulator<RenameUnlessShadowed.Blocked>(blocked =>
                    Assert.Equal(["Shadow.cs declares Foo"], blocked.Reasons)),
            CSharp("class C { void M() { Foo(); } void Foo() { } }", sourcePath: "C.cs"),
            CSharp("class Shadow { class Foo { } }", sourcePath: "Shadow.cs")
        );
    }

    [Fact]
    public void ARunWithoutTheAccumulatorFails()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            spec => spec.SetRecipe(new RenameUnlessShadowed()).ExpectAccumulator<List<string>>(_ => { }),
            CSharp("class C { }")
        ));

        Assert.Equal(
            "ExpectAccumulator<List<String>> found no accumulator of that type: the scanning recipes keep Blocked.",
            failure.Message);
    }

    /// <summary>Renames calls to Foo to Bar, unless a source file declares a type named Foo.</summary>
    private class RenameUnlessShadowed : ScanningRecipe<RenameUnlessShadowed.Blocked>
    {
        public override string DisplayName => "Rename unless shadowed";
        public override string Description => "Renames calls to Foo to Bar unless a type named Foo exists.";

        public override Blocked GetInitialValue(ExecutionContext ctx) => new();

        public override ITreeVisitor<ExecutionContext> GetScanner(Blocked blocked) => new Scanner(blocked);

        public override ITreeVisitor<ExecutionContext> GetVisitor(Blocked blocked) => new Renamer(blocked);

        public class Blocked
        {
            public List<string> Reasons { get; } = [];
        }

        private class Scanner(Blocked blocked) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitClassDeclaration(ClassDeclaration classDecl, ExecutionContext ctx)
            {
                if (classDecl.Name.SimpleName == "Foo")
                {
                    blocked.Reasons.Add($"{Cursor.FirstEnclosing<CompilationUnit>()!.SourcePath} declares Foo");
                }
                return base.VisitClassDeclaration(classDecl, ctx);
            }
        }

        private class Renamer(Blocked blocked) : CSharpVisitor<ExecutionContext>
        {
            public override J VisitMethodInvocation(MethodInvocation method, ExecutionContext ctx)
            {
                var call = (MethodInvocation)base.VisitMethodInvocation(method, ctx);
                return blocked.Reasons.Count == 0 && call.Name.SimpleName == "Foo"
                    ? call.WithName(call.Name.WithSimpleName("Bar"))
                    : call;
            }
        }
    }
}
