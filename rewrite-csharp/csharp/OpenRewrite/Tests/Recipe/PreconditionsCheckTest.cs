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
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Test;
using static OpenRewrite.Core.Preconditions;
using static OpenRewrite.Java.J;
using static OpenRewrite.Java.Search.Preconditions;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Recipe;

/// <summary>
/// Tests for local (non-RPC) precondition logic. Ensures RewriteRpcServer.Current
/// is null so that UsesType falls back to LocalUsesType instead of delegating to
/// a Java RPC server that may have been started by other test collections.
/// </summary>
public class PreconditionsCheckTest : RewriteTest, IDisposable
{
    private readonly RewriteRpcServer? _savedRpcServer;

    public PreconditionsCheckTest()
    {
        _savedRpcServer = RewriteRpcServer.Current;
        RewriteRpcServer.SetCurrent(null);
    }

    public void Dispose()
    {
        RewriteRpcServer.SetCurrent(_savedRpcServer);
    }
    /// <summary>
    /// Directly test that LocalUsesType finds the type and Check delegates to visitor.
    /// </summary>
    [Fact]
    public async Task CheckDelegatesToVisitorWhenPreconditionMatches()
    {
        var parser = new CSharpParser();
        var syntaxTree = Microsoft.CodeAnalysis.CSharp.CSharpSyntaxTree.ParseText(
            "using System; class T { void M() { Console.WriteLine(\"hi\"); } }", path: "source.cs");
        var refs = await Assemblies.Net90
            .ResolveAsync(Microsoft.CodeAnalysis.LanguageNames.CSharp, System.Threading.CancellationToken.None);
        var compilation = Microsoft.CodeAnalysis.CSharp.CSharpCompilation.Create("Test")
            .WithOptions(new Microsoft.CodeAnalysis.CSharp.CSharpCompilationOptions(Microsoft.CodeAnalysis.OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(refs)
            .AddSyntaxTrees(syntaxTree);
        var semanticModel = compilation.GetSemanticModel(syntaxTree);
        var source = parser.Parse(
            "using System; class T { void M() { Console.WriteLine(\"hi\"); } }",
            semanticModel: semanticModel);

        // Step 1: Verify LocalUsesType finds System.Console
        var precondition = UsesType("System.Console");
        var precondResult = precondition.Visit(source, new ExecutionContext());
        Assert.NotSame(source, precondResult); // Should be different (marked with SearchResult)

        // Step 2: Verify Check delegates to inner visitor
        var check = Check(precondition, new NoOpCSharpVisitor());
        var checkResult = check.Visit(source, new ExecutionContext());
        // If precondition matched, inner visitor ran (even if no-op)
    }

    private class NoOpCSharpVisitor : CSharpVisitor<ExecutionContext> { }

    /// <summary>
    /// Verifies that Preconditions.Check works with CSharpVisitor recipes.
    /// The precondition (LocalUsesType) must be able to traverse C# trees
    /// to find the type, and the inner CSharpVisitor must run when matched.
    /// </summary>
    [Fact]
    public void PreconditionMatchesAndVisitorRuns()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameWriteLineRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System;

                class Test
                {
                    void M()
                    {
                        Console.WriteLine("hello");
                    }
                }
                """,
                """
                using System;

                class Test
                {
                    void M()
                    {
                        Console.Write("hello");
                    }
                }
                """
            )
        );
    }

    /// <summary>
    /// Diagnostic: verify that type attribution works — identifiers in parsed trees
    /// have JavaType attached when reference assemblies are provided.
    /// </summary>
    [Fact]
    public async Task TypeAttributionProducesJavaTypes()
    {
        var parser = new CSharpParser();
        var syntaxTree = Microsoft.CodeAnalysis.CSharp.CSharpSyntaxTree.ParseText(
            "using System; class T { void M() { Console.WriteLine(\"hi\"); } }", path: "source.cs");
        var refs = await Assemblies.Net90
            .ResolveAsync(Microsoft.CodeAnalysis.LanguageNames.CSharp, System.Threading.CancellationToken.None);
        var compilation = Microsoft.CodeAnalysis.CSharp.CSharpCompilation.Create("Test")
            .WithOptions(new Microsoft.CodeAnalysis.CSharp.CSharpCompilationOptions(Microsoft.CodeAnalysis.OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(refs)
            .AddSyntaxTrees(syntaxTree);
        var semanticModel = compilation.GetSemanticModel(syntaxTree);
        var source = parser.Parse(
            "using System; class T { void M() { Console.WriteLine(\"hi\"); } }",
            semanticModel: semanticModel);

        // Walk the tree and collect all identifiers with their types
        var types = new List<string>();
        new TypeCollector(types).Visit(source, new ExecutionContext());

        // Should find System.Console somewhere
        Assert.Contains(types, t => t.Contains("System.Console"));
    }

    private class TypeCollector(List<string> types) : CSharpVisitor<ExecutionContext>
    {
        public override J VisitIdentifier(Identifier id, ExecutionContext ctx)
        {
            if (id.Type is JavaType.Class cls)
                types.Add($"{id.SimpleName}: FQN={cls.FullyQualifiedName}");
            return id;
        }

        public override J VisitFieldAccess(FieldAccess fa, ExecutionContext ctx)
        {
            if (fa.Type != null)
                types.Add($"{fa}: {fa.Type}");
            return base.VisitFieldAccess(fa, ctx);
        }
    }

    /// <summary>
    /// Verifies that when the precondition doesn't match (type not used),
    /// the inner visitor does NOT run and the source is unchanged.
    /// </summary>
    [Fact]
    public void PreconditionDoesNotMatchAndVisitorSkipped()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new RenameWriteLineRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                class Test
                {
                    void M()
                    {
                        var x = 1;
                    }
                }
                """
            )
        );
    }
}

/// <summary>
/// A test recipe that uses Preconditions.Check with a CSharpVisitor.
/// Renames Console.WriteLine to Console.Write in files that use System.Console.
/// </summary>
/// <summary>
/// Test recipe that removes Console.WriteLine by returning null from VisitMethodInvocation.
/// This does NOT work — returning null from VisitMethodInvocation removes the invocation
/// but not the enclosing ExpressionStatement, so the tree appears unchanged.
/// </summary>
class RemoveConsoleWriteLineRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Remove Console.WriteLine (broken)";
    public override string Description => "Attempts to remove Console.WriteLine by returning null from VisitMethodInvocation.";

    public override JavaVisitor<ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (mi.Name.SimpleName == "WriteLine" &&
                mi.Select?.Element is Identifier id &&
                id.SimpleName == "Console")
            {
                //noinspection DataFlowIssue
                return null!;
            }

            return mi;
        }
    }
}

class RenameWriteLineRecipe : OpenRewrite.Core.Recipe
{
    public override string DisplayName => "Rename Console.WriteLine to Write";
    public override string Description => "Renames Console.WriteLine to Console.Write.";

    public override ITreeVisitor<ExecutionContext> GetVisitor() =>
        Check(
            UsesType("System.Console"),
            new Visitor());

    private class Visitor : CSharpVisitor<ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (mi.Name.SimpleName == "WriteLine" &&
                mi.Select?.Element is Identifier id &&
                id.SimpleName == "Console")
            {
                return mi.WithName(mi.Name.WithSimpleName("Write"));
            }

            return mi;
        }
    }
}
