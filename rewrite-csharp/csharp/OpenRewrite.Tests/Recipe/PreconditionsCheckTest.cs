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
using static OpenRewrite.Core.Preconditions;
using static OpenRewrite.Java.J;
using static OpenRewrite.Java.Search.Preconditions;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Recipe;

/// <summary>
/// Tests for Preconditions.Check with RPC-backed precondition visitors.
/// These tests require a Java RPC connection for UsesType/UsesMethod.
/// </summary>
public class PreconditionsCheckTest(RpcFixture fixture) : RpcRewriteTest(fixture)
{
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
/// Test recipe that renames Console.WriteLine to Console.Write in files that use System.Console.
/// Uses Preconditions.Check with an RPC-backed UsesType precondition.
/// </summary>
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
