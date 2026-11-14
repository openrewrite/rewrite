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
using Microsoft.CodeAnalysis.CSharp.Syntax;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp;

public class ConstructorAttributionTests
{
    [Fact]
    public async Task DeclarationsShareActualSymbolTypesAndParameterOwners()
    {
        const string source = """
            partial class Outer<T> {
                public class Inner {
                    static Inner() { }
                    public Inner(ref T value, int count = 0) { }
                    private Inner(T value) : this(ref value, 1) { }
                    public void Ordinary(T value) { }
                }
            }
            partial class Outer<T> { }
            record R(int X) { public R() : this(0) { } }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Constructors", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
        var model = compilation.GetSemanticModel(syntax);
        var mapping = new CSharpTypeMapping(model);
        foreach (var declaration in syntax.GetRoot().DescendantNodes().OfType<BaseMethodDeclarationSyntax>()
                     .Where(n => n is ConstructorDeclarationSyntax or MethodDeclarationSyntax))
        {
            var symbol = Assert.IsAssignableFrom<IMethodSymbol>(model.GetDeclaredSymbol(declaration));
            var method = Assert.IsType<JavaType.Method>(mapping.MethodType(declaration));
            Assert.Equal(symbol.Name, method.Name);
            Assert.Same(mapping.MapType(symbol.ContainingType), method.DeclaringType);
            Assert.Same(mapping.MapType(symbol.MethodKind == MethodKind.Constructor
                ? symbol.ContainingType : symbol.ReturnType), method.ReturnType);
            foreach (var parameter in declaration.ParameterList.Parameters)
            {
                var variable = Assert.IsType<JavaType.Variable>(mapping.VariableType(parameter));
                Assert.Same(method, variable.Owner);
                Assert.Same(mapping.MapType(model.GetDeclaredSymbol(parameter)!.Type), variable.Type);
            }
        }
        var cu = new CSharpParser().Parse(source, semanticModel: model);
        Assert.Equal(source, new CSharpPrinter<object>().Print(cu));
        var visitor = new ConstructorVisitor();
        visitor.Visit(cu, 0);
        Assert.Equal(4, visitor.Constructors);
    }

    [Theory]
    [InlineData("class C() { }")]
    [InlineData("record C();")]
    [InlineData("class C(int x) { }")]
    [InlineData("struct C(int x) { }")]
    [InlineData("record C(int X);")]
    [InlineData("record struct C(int X);")]
    public async Task PrimaryConstructorUsesItsDeclaringSyntax(string source)
    {
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Primary", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
        var model = compilation.GetSemanticModel(syntax);
        var declaration = syntax.GetRoot().DescendantNodes().OfType<TypeDeclarationSyntax>().Single();
        var parameter = declaration.ParameterList!.Parameters.FirstOrDefault();
        var mapping = new CSharpTypeMapping(model);
        var method = Assert.IsType<JavaType.Method>(mapping.MethodType(declaration.ParameterList));
        if (parameter != null) Assert.Same(method, mapping.VariableType(parameter)!.Owner);
        Assert.Equal(".ctor", method.Name);
        var cu = new CSharpParser().Parse(source, semanticModel: model);
        var type = Assert.IsType<ClassDeclaration>(cu.Members[0].Element);
        var ctor = Assert.IsType<MethodDeclaration>(type.Body.Statements[0].Element);
        Assert.NotNull(ctor.MethodType);
        Assert.Same(ctor.MethodType, ctor.Name.Type);
        Assert.Equal(source, new CSharpPrinter<object>().Print(cu));
    }

    [Fact]
    public void WithoutSemanticModelDoesNotInventBindings()
    {
        const string source = "class C { C(ref Missing value) { } }";
        var cu = new CSharpParser().Parse(source);
        var type = Assert.IsType<ClassDeclaration>(cu.Members[0].Element);
        var ctor = Assert.IsType<MethodDeclaration>(type.Body.Statements[0].Element);
        Assert.Null(ctor.MethodType);
        Assert.Null(ctor.Name.Type);
        Assert.Equal(source, new CSharpPrinter<object>().Print(cu));
    }

    [Fact]
    public async Task PartialConstructorDeclarationsUseActualSymbols()
    {
        const string source = "partial class C { public partial C(int x); public partial C(int x) { } }";
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source, new CSharpParseOptions(LanguageVersion.Preview));
        var compilation = CSharpCompilation.Create("Partial", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
        var model = compilation.GetSemanticModel(syntax);
        var mapping = new CSharpTypeMapping(model);
        foreach (var declaration in syntax.GetRoot().DescendantNodes().OfType<ConstructorDeclarationSyntax>())
        {
            var method = Assert.IsType<JavaType.Method>(mapping.MethodType(declaration));
            Assert.Equal(model.GetDeclaredSymbol(declaration)!.Name, method.Name);
            Assert.Same(method, mapping.VariableType(declaration.ParameterList.Parameters.Single())!.Owner);
        }
    }

    [Fact]
    public async Task UnresolvedParameterRemainsUnknown()
    {
        const string source = "class C { C(Missing value) { } }";
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Unresolved", [syntax], refs);
        Assert.Contains(compilation.GetDiagnostics(), d => d.Id == "CS0246");
        var model = compilation.GetSemanticModel(syntax);
        var mapping = new CSharpTypeMapping(model);
        var declaration = syntax.GetRoot().DescendantNodes().OfType<ConstructorDeclarationSyntax>().Single();
        var method = Assert.IsType<JavaType.Method>(mapping.MethodType(declaration));
        Assert.IsType<JavaType.Unknown>(method.ParameterTypes!.Single());
        Assert.Same(method, mapping.VariableType(declaration.ParameterList.Parameters.Single())!.Owner);
    }

    private class ConstructorVisitor : CSharpVisitor<int>
    {
        public int Constructors;
        public override J VisitMethodDeclaration(MethodDeclaration method, int p)
        {
            if (method.ReturnTypeExpression == null && method.Name.SimpleName != "<constructor>")
            {
                Constructors++;
                var type = Assert.IsType<JavaType.Method>(method.MethodType);
                Assert.Same(type, method.Name.Type);
                if (method.DefaultValue?.Element is MethodInvocation initializer)
                {
                    Assert.NotNull(initializer.MethodType);
                    Assert.Equal(".ctor", initializer.MethodType.Name);
                    Assert.Same(initializer.MethodType, initializer.Name.Type);
                }
                foreach (var parameter in method.Parameters.Elements.Select(x => x.Element).OfType<VariableDeclarations>())
                    foreach (var variable in parameter.Variables)
                        Assert.Same(type, ((JavaType.Variable)variable.Element.Name.FieldType!).Owner);
            }
            return base.VisitMethodDeclaration(method, p);
        }
    }
}
