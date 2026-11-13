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
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;
using OpenRewrite.Test;
using OpenRewrite.Tests.Template;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.CSharp;

public class PrimaryConstructorTemplateTests
{
    [Fact]
    public void InsertionsIntoTwoClassesHaveUniqueIds()
    {
        // given
        var cu = new CSharpParser().Parse("class First { } class Second { }");
        var recipe = new PrimaryConstructorTemplateRecipe();

        // when
        var updated = recipe.GetVisitor().Visit(cu, new ExecutionContext());

        // then
        var compilationUnit = Assert.IsType<CompilationUnit>(updated);
        Assert.Equal(2, compilationUnit.Members.Count);
        Assert.All(compilationUnit.Members, member =>
        {
            var declaration = Assert.IsType<ClassDeclaration>(member.Element);
            var constructor = Assert.IsType<MethodDeclaration>(Assert.Single(declaration.Body.Statements).Element);
            Assert.NotNull(constructor.Markers.FindFirst<PrimaryConstructor>());
            Assert.Single(constructor.Parameters.Elements);
        });
        new UniqueIdVisitor().Visit(updated, new HashSet<Guid>());
    }

    [Theory]
    [InlineData("Existing", "int")]
    [InlineData("Existing<T>", "T")]
    public async Task ConversionCanPreserveExistingConstructorAndParameterBindings(string name, string parameterType)
    {
        // given
        var source = $"public class {name} {{ private readonly {parameterType} field; public Existing({parameterType} value) {{ field = value; }} }}";
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Conversion", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
        var declaration = Assert.IsType<ClassDeclaration>(cu.Members[0].Element);
        var field = Assert.IsType<VariableDeclarations>(declaration.Body.Statements[0].Element);
        var original = Assert.IsType<MethodDeclaration>(declaration.Body.Statements[1].Element);
        var marker = new PreservedMarker(Guid.NewGuid());
        var parameter = Assert.IsType<VariableDeclarations>(original.Parameters.Elements[0].Element);
        parameter = parameter.WithMarkers(parameter.Markers.Add(marker));
        original = original.WithParameters(original.Parameters.WithElements(
            [original.Parameters.Elements[0].WithElement(parameter)]));
        original = original.WithMarkers(original.Markers.Add(marker));
        var statement = Assert.IsType<ExpressionStatement>(Assert.Single(original.Body!.Statements).Element);
        var assignment = Assert.IsType<Assignment>(statement.Expression);
        var reference = Assert.IsType<Identifier>(assignment.AssignmentValue.Element);
        reference = reference.WithMarkers(reference.Markers.Add(marker));
        var method = Assert.IsType<JavaType.Method>(original.MethodType);
        var binding = Assert.IsType<JavaType.Variable>(parameter.Variables[0].Element.Name.FieldType);
        Assert.Same(declaration.Type, method.DeclaringType);
        Assert.Same(method, binding.Owner);
        Assert.Same(binding, reference.FieldType);

        // when
        var template = CSharpTemplate.ClassMember("class C() { }");
        var scaffold = Assert.IsType<ClassDeclaration>(template.Apply(new OpenRewrite.Core.Cursor(null, declaration)));
        var synthetic = Assert.IsType<MethodDeclaration>(scaffold.Body.Statements[0].Element);
        var constructor = synthetic.WithParameters(original.Parameters)
            .WithId(original.Id)
            .WithMarkers(original.Markers.Add(synthetic.Markers.FindFirst<PrimaryConstructor>()!))
            .WithMethodType(method)
            .WithName(synthetic.Name.WithType(method));
        var initializedField = field.WithVariables([field.Variables[0].WithElement(
            field.Variables[0].Element.WithInitializer(assignment.AssignmentValue.WithElement(reference)))]);
        var updated = declaration.WithBody(declaration.Body.WithStatements(
            [scaffold.Body.Statements[0].WithElement(constructor),
                declaration.Body.Statements[0].WithElement(initializedField)]));

        // then
        Assert.Equal($"public class {name}({parameterType} value) {{ private readonly {parameterType} field = value; }}",
            new CSharpPrinter<object>().Print(updated));
        Assert.NotNull(constructor.Markers.FindFirst<PrimaryConstructor>());
        Assert.Same(marker, constructor.Markers.FindFirst<PreservedMarker>());
        Assert.Same(marker, parameter.Markers.FindFirst<PreservedMarker>());
        Assert.Same(marker, reference.Markers.FindFirst<PreservedMarker>());
        Assert.Equal(original.Id, constructor.Id);
        Assert.Same(original.Parameters, constructor.Parameters);
        Assert.Same(parameter, constructor.Parameters.Elements[0].Element);
        Assert.Same(method, constructor.MethodType);
        Assert.Same(method, constructor.Name.Type);
        Assert.Same(declaration.Type, constructor.MethodType!.DeclaringType);
        Assert.Same(declaration.Type, constructor.MethodType.ReturnType);
        Assert.Same(binding.Type, Assert.Single(constructor.MethodType.ParameterTypes!));
        Assert.Same(method, binding.Owner);
        Assert.Same(reference, initializedField.Variables[0].Element.Initializer!.Element);
        Assert.Same(binding, reference.FieldType);
        Assert.Same(field.Variables[0].Element.Name.FieldType, initializedField.Variables[0].Element.Name.FieldType);
        Assert.Equal(field.Id, initializedField.Id);
        Assert.Equal(field.Variables[0].Element.Id, initializedField.Variables[0].Element.Id);
        new UniqueIdVisitor().Visit(updated.Body, new HashSet<Guid>());
        var modifiedSyntax = CSharpSyntaxTree.ParseText(new CSharpPrinter<object>().Print(updated));
        Assert.Empty(compilation.ReplaceSyntaxTree(syntax, modifiedSyntax).GetDiagnostics()
            .Where(d => d.Severity == DiagnosticSeverity.Error));
    }

    [Theory]
    [InlineData("Existing")]
    [InlineData("Existing<T>")]
    public async Task InsertedConstructorRequiresDestinationAttribution(string name)
    {
        // given
        var source = $"class {name} {{ int field = value; }}";
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Destination", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Contains(compilation.GetDiagnostics(), d => d.Id == "CS0103");
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));

        // when
        var updated = Assert.IsType<CompilationUnit>(new PrimaryConstructorTemplateRecipe()
            .GetVisitor().Visit(cu, new ExecutionContext()));

        // then
        var declaration = Assert.IsType<ClassDeclaration>(updated.Members[0].Element);
        var constructor = Assert.IsType<MethodDeclaration>(declaration.Body.Statements[0].Element);
        var method = Assert.IsType<JavaType.Method>(constructor.MethodType);
        Assert.NotSame(declaration.Type, method.DeclaringType);
        Assert.Equal("__T__$C", Assert.IsType<JavaType.Class>(method.DeclaringType).FullyQualifiedName);
        var parameter = Assert.IsType<VariableDeclarations>(constructor.Parameters.Elements[0].Element);
        var binding = Assert.IsType<JavaType.Variable>(parameter.Variables[0].Element.Name.FieldType);
        Assert.Same(method, binding.Owner);
        var field = Assert.IsType<VariableDeclarations>(declaration.Body.Statements[1].Element);
        var reference = Assert.IsType<Identifier>(field.Variables[0].Element.Initializer!.Element);
        Assert.Null(reference.FieldType);

        var modifiedSource = new CSharpPrinter<object>().Print(updated);
        var modifiedSyntax = CSharpSyntaxTree.ParseText(modifiedSource);
        var modifiedCompilation = compilation.ReplaceSyntaxTree(syntax, modifiedSyntax);
        Assert.Empty(modifiedCompilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
        var attributed = new CSharpParser().Parse(modifiedSource,
            semanticModel: modifiedCompilation.GetSemanticModel(modifiedSyntax));
        var destination = Assert.IsType<ClassDeclaration>(attributed.Members[0].Element);
        var destinationConstructor = Assert.IsType<MethodDeclaration>(destination.Body.Statements[0].Element);
        var destinationMethod = Assert.IsType<JavaType.Method>(destinationConstructor.MethodType);
        Assert.Same(destination.Type, destinationMethod.DeclaringType);
        Assert.Same(destination.Type, destinationMethod.ReturnType);
        Assert.Same(destinationMethod, destinationConstructor.Name.Type);
        var destinationParameter = Assert.IsType<VariableDeclarations>(destinationConstructor.Parameters.Elements[0].Element);
        var destinationBinding = Assert.IsType<JavaType.Variable>(destinationParameter.Variables[0].Element.Name.FieldType);
        Assert.Same(destinationMethod, destinationBinding.Owner);
        var destinationField = Assert.IsType<VariableDeclarations>(destination.Body.Statements[1].Element);
        var destinationReference = Assert.IsType<Identifier>(destinationField.Variables[0].Element.Initializer!.Element);
        Assert.Same(destinationBinding, destinationReference.FieldType);
    }

    [Theory]
    [InlineData("class")]
    [InlineData("struct")]
    public void PrimaryConstructorClassMemberTemplate(string kind)
    {
        // given
        var template = CSharpTemplate.ClassMember($"{kind} C(int value) {{ int Value = value; }}");

        // when
        var cls = Assert.IsType<ClassDeclaration>(template.GetTree());

        // then
        var constructor = Assert.IsType<MethodDeclaration>(cls.Body.Statements[0].Element);
        Assert.NotNull(constructor.Markers.FindFirst<PrimaryConstructor>());
        Assert.Equal("<constructor>", constructor.Name.SimpleName);
        Assert.NotNull(constructor.Name.Markers.FindFirst<Implicit>());
        Assert.Null(cls.Markers.FindFirst<PrimaryConstructor>());
        Assert.Null(cls.PrimaryConstructor);
        Assert.Equal("value", Assert.IsType<VariableDeclarations>(constructor.Parameters.Elements[0].Element)
            .Variables[0].Element.Name.SimpleName);
    }

    private class UniqueIdVisitor : CSharpVisitor<HashSet<Guid>>
    {
        public override J PostVisit(J tree, HashSet<Guid> ids)
        {
            Assert.True(ids.Add(tree.Id), $"Duplicate ID on {tree.GetType().Name}: {tree.Id}");
            return tree;
        }
    }

    private sealed class PreservedMarker(Guid id) : OpenRewrite.Core.Marker
    {
        public Guid Id { get; } = id;
    }
}
