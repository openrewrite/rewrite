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
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;
using static OpenRewrite.Java.J;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.CSharp;

public class CSharpTypeMappingTests
{
    private static async Task<SourceFile> ParseWithAssemblies(string code)
    {
        var parser = new CSharpParser();
        var syntaxTree = CSharpSyntaxTree.ParseText(code, path: "source.cs");
        var refs = await Assemblies.Net90
            .ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var compilation = CSharpCompilation.Create("Test")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(refs)
            .AddSyntaxTrees(syntaxTree);
        var semanticModel = compilation.GetSemanticModel(syntaxTree);
        return parser.Parse(code, semanticModel: semanticModel);
    }

    [Fact]
    public async Task ClassTypeShouldHaveMethods()
    {
        var source = await ParseWithAssemblies(
            "using System; class T { void M() { var ex = new ArgumentNullException(\"param\"); } }");

        // Find the NewClass node and get its type
        var finder = new TypeFinder("System.ArgumentNullException");
        finder.Visit(source, new ExecutionContext());
        var type = finder.Found;

        Assert.NotNull(type);
        Assert.Equal("System.ArgumentNullException", type!.FullyQualifiedName);

        // Methods should be populated
        Assert.NotNull(type.Methods);
        Assert.NotEmpty(type.Methods!);

        // Should contain ThrowIfNull (available since .NET 6)
        Assert.True(TypeUtils.HasMethod(type, "ThrowIfNull"),
            $"Expected System.ArgumentNullException to have ThrowIfNull method. " +
            $"Methods found: [{string.Join(", ", type.Methods?.Select(m => m.Name) ?? [])}]");
    }

    [Fact]
    public async Task ClassTypeShouldHaveMethodsOnException()
    {
        var source = await ParseWithAssemblies(
            "using System; class T { void M() { var ex = new Exception(\"msg\"); } }");

        var finder = new TypeFinder("System.Exception");
        finder.Visit(source, new ExecutionContext());
        var type = finder.Found;

        Assert.NotNull(type);
        Assert.Equal("System.Exception", type!.FullyQualifiedName);

        // Methods should be populated
        Assert.NotNull(type.Methods);
        Assert.True(TypeUtils.HasMethod(type, "GetBaseException"));
    }

    [Fact]
    public async Task UserDefinedClassShouldHaveMembers()
    {
        var source = await ParseWithAssemblies(
            """
            class T {
                public int X;
                public string Name;
                void M() { }
            }
            """);

        var finder = new TypeFinder("T");
        finder.Visit(source, new ExecutionContext());
        var type = finder.Found;

        Assert.NotNull(type);
        Assert.NotNull(type!.Members);
        Assert.True(type.Members!.Count >= 2,
            $"Expected at least 2 members but found: [{string.Join(", ", type.Members.Select(m => m.Name))}]");
    }

    /// <summary>
    /// Visitor that finds the first JavaType.Class matching a given FQN.
    /// </summary>
    private class TypeFinder(string targetFqn) : CSharpVisitor<ExecutionContext>
    {
        public JavaType.Class? Found { get; private set; }

        public override J? VisitNewClass(NewClass newClass, ExecutionContext ctx)
        {
            TryCapture(newClass.Type);
            return base.VisitNewClass(newClass, ctx);
        }

        public override J? VisitClassDeclaration(ClassDeclaration classDeclaration, ExecutionContext ctx)
        {
            TryCapture(classDeclaration.Type);
            return base.VisitClassDeclaration(classDeclaration, ctx);
        }

        private void TryCapture(JavaType? type)
        {
            if (Found != null) return;
            var cls = TypeUtils.AsClass(type);
            if (cls != null && cls.FullyQualifiedName == targetFqn)
                Found = cls;
        }
    }
}
