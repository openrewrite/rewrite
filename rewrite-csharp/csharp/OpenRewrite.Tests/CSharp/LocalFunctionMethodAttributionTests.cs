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
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp;

public class LocalFunctionMethodAttributionTests : RewriteTest
{
    [Theory]
    [InlineData("Local")]
    [InlineData("@Local")]
    [InlineData("\\u004cocal")]
    public async Task StaticDeclarationAndCallShareActualMethod(string name)
    {
        var source = $$"""
            class C
            {
                static string Run(byte[] bytes)
                {
                    static string {{name}}(byte[] value)
                    {
                        var copy = value;
                        return System.Convert.ToBase64String(copy);
                    }
                    return {{name}}(bytes);
                }
            }
            """;
        var (tree, symbols) = await Parse(source);
        var helper = Assert.Single(symbols.Declarations, d => Name(d.Name.SimpleName) == "Local");
        var method = Assert.IsType<JavaType.Method>(helper.MethodType);
        Assert.Equal("Local", method.Name);
        Assert.Equal(8L, method.FlagsBitMap & 8L);
        Assert.Same(method, helper.Name.Type);
        Assert.Same(Assert.Single(symbols.Classes).Type, method.DeclaringType);
        var call = Assert.Single(symbols.Calls, c => Name(c.Call.Name.SimpleName) == "Local").Call;
        Assert.Same(method, call.MethodType);
        Assert.Same(method, call.Name.Type);
        foreach (var variableName in new[] { "value", "copy" })
        {
            var identifiers = symbols.Identifiers.Where(i => i.SimpleName == variableName).ToArray();
            Assert.True(identifiers.Length >= 2);
            var variable = Assert.IsType<JavaType.Variable>(identifiers[0].FieldType);
            Assert.All(identifiers, identifier => Assert.Same(variable, identifier.FieldType));
        }
        Assert.Equal(source, new CSharpPrinter<int>().Print(tree));
    }

    [Fact]
    public async Task CapturingLocalFunctionIsNotAttributedAsStatic()
    {
        var (_, symbols) = await Parse("class C { int Run(int seed) { int Next() { return seed + 1; } return Next(); } }");
        var helper = Assert.Single(symbols.Declarations, d => d.Name.SimpleName == "Next");
        var method = Assert.IsType<JavaType.Method>(helper.MethodType);
        Assert.Equal(0L, method.FlagsBitMap & 8L);
        Assert.Same(method, helper.Name.Type);
        Assert.Same(method, Assert.Single(symbols.Calls).Call.MethodType);
    }

    [Fact]
    public async Task SameNamedLocalsInDifferentMethodsHaveDistinctCanonicalSymbols()
    {
        var (_, symbols) = await Parse("""
            class C
            {
                static int A() { int Local(int value) => value + 1; return Local(1); }
                static int B() { int Local(int value) => value + 2; return Local(2); }
            }
            """);
        var helpers = symbols.Declarations.Where(d => d.Name.SimpleName == "Local").ToArray();
        Assert.Equal(2, helpers.Length);
        Assert.NotNull(helpers[0].MethodType);
        Assert.NotNull(helpers[1].MethodType);
        Assert.NotSame(helpers[0].MethodType, helpers[1].MethodType);
        Assert.Same(helpers[0].MethodType, Assert.Single(symbols.Calls, c => c.Owner == "A").Call.MethodType);
        Assert.Same(helpers[1].MethodType, Assert.Single(symbols.Calls, c => c.Owner == "B").Call.MethodType);
    }

    [Fact]
    public async Task GenericLocalDeclarationRetainsTypeParameterAndConstructedCall()
    {
        var (_, symbols) = await Parse("class C { string Run() { static T Echo<T>(T value) => value; return Echo<string>(\"value\"); } }");
        var helper = Assert.Single(symbols.Declarations, d => d.Name.SimpleName == "Echo");
        var method = Assert.IsType<JavaType.Method>(helper.MethodType);
        var parameter = Assert.IsType<JavaType.GenericTypeVariable>(Assert.Single(method.ParameterTypes!));
        Assert.Equal("T", parameter.Name);
        Assert.Same(parameter, method.ReturnType);
        var call = Assert.Single(symbols.Calls).Call;
        Assert.NotSame(method, call.MethodType);
        Assert.True(TypeUtils.IsString(call.MethodType!.ReturnType));
        Assert.Same(call.MethodType, call.Name.Type);
    }

    [Fact]
    public void UnattributedLocalSyntaxDoesNotInventMethodSymbols()
    {
        var tree = new CSharpParser().Parse("class C { int Run() { int Local() => 1; return Local(); } }");
        var symbols = new Symbols();
        symbols.Visit(tree, 0);
        var helper = Assert.Single(symbols.Declarations, d => d.Name.SimpleName == "Local");
        Assert.Null(helper.MethodType);
        Assert.Null(helper.Name.Type);
    }

    private static string Name(string name) => SyntaxFactory.ParseToken(name).ValueText;

    private static async Task<(CompilationUnit Tree, Symbols Symbols)> Parse(string source)
    {
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("LocalFunctionAttribution", [syntax],
            await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None),
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.DoesNotContain(compilation.GetDiagnostics(), d => d.Severity == DiagnosticSeverity.Error);
        var tree = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
        var symbols = new Symbols();
        symbols.Visit(tree, 0);
        return (tree, symbols);
    }

    private class Symbols : CSharpVisitor<int>
    {
        public List<ClassDeclaration> Classes { get; } = [];
        public List<MethodDeclaration> Declarations { get; } = [];
        public List<(MethodInvocation Call, string? Owner)> Calls { get; } = [];
        public List<Identifier> Identifiers { get; } = [];
        public override J VisitClassDeclaration(ClassDeclaration declaration, int p)
        {
            Classes.Add(declaration);
            return base.VisitClassDeclaration(declaration, p);
        }
        public override J VisitMethodDeclaration(MethodDeclaration declaration, int p)
        {
            Declarations.Add(declaration);
            return base.VisitMethodDeclaration(declaration, p);
        }
        public override J VisitMethodInvocation(MethodInvocation invocation, int p)
        {
            Calls.Add((invocation, Cursor.FirstEnclosing<MethodDeclaration>()?.Name.SimpleName));
            return base.VisitMethodInvocation(invocation, p);
        }
        public override J VisitIdentifier(Identifier identifier, int p)
        {
            Identifiers.Add(identifier);
            return identifier;
        }
    }
}
