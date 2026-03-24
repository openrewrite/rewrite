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

/// <summary>
/// Tests that the C# parser's type mapping correctly handles generic types,
/// especially how parameterized interfaces store type arguments.
/// </summary>
public class CSharpTypeMappingTests : RewriteTest
{
    /// <summary>
    /// Parse source code with reference assemblies and return the CompilationUnit.
    /// </summary>
    private static readonly SyntaxTree ImplicitUsingsSyntaxTree = CSharpSyntaxTree.ParseText(
        """
        global using System;
        global using System.Collections.Generic;
        global using System.IO;
        global using System.Linq;
        global using System.Net.Http;
        global using System.Threading;
        global using System.Threading.Tasks;
        """,
        path: "__GlobalUsings__.g.cs");

    private static CompilationUnit ParseWithSemanticModel(string code)
    {
        var refs = Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None)
            .GetAwaiter().GetResult();
        var syntaxTree = CSharpSyntaxTree.ParseText(code, path: "source.cs");
        var compilation = CSharpCompilation.Create("TestCompilation")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(refs)
            .AddSyntaxTrees(ImplicitUsingsSyntaxTree, syntaxTree);
        var semanticModel = compilation.GetSemanticModel(syntaxTree);

        var parser = new CSharpParser();
        return parser.Parse(code, semanticModel: semanticModel);
    }

    /// <summary>
    /// Find the first variable declaration in a compilation unit by variable name.
    /// </summary>
    private static VariableDeclarations? FindVariableDeclaration(CompilationUnit cu, string varName)
    {
        var finder = new VarFinder(varName);
        finder.Cursor = new Core.Cursor(null, Core.Cursor.ROOT_VALUE);
        finder.Visit(cu, 0);
        return finder.Found;
    }

    private class VarFinder(string name) : CSharpVisitor<int>
    {
        public VariableDeclarations? Found { get; private set; }

        public override J? VisitVariableDeclarations(VariableDeclarations multiVariable, int p)
        {
            if (multiVariable.Variables.Any(v => v.Element.Name.SimpleName == name))
                Found = multiVariable;
            return base.VisitVariableDeclarations(multiVariable, p);
        }
    }

    [Fact]
    public void GenericClassField_HasParameterizedTypeWithGenericTypeVariables()
    {
        // A class with type parameters and a field whose type references them.
        // The field's type should be Parameterized with GenericTypeVariable entries.
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Foo<TKey, TValue>
            {
                IDictionary<TKey, TValue> dict;
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);

        // The declared type should be Parameterized(IDictionary, [GTV(TKey), GTV(TValue)])
        var declType = varDecl!.TypeExpression?.Type;
        Assert.NotNull(declType);
        var paramType = Assert.IsType<JavaType.Parameterized>(declType);

        Assert.NotNull(paramType.Type);
        Assert.Contains("IDictionary", TypeUtils.GetFullyQualifiedName(paramType.Type));

        Assert.NotNull(paramType.TypeParameters);
        Assert.Equal(2, paramType.TypeParameters!.Count);

        var tp0 = Assert.IsType<JavaType.GenericTypeVariable>(paramType.TypeParameters[0]);
        Assert.Equal("TKey", tp0.Name);

        var tp1 = Assert.IsType<JavaType.GenericTypeVariable>(paramType.TypeParameters[1]);
        Assert.Equal("TValue", tp1.Name);
    }

    [Fact]
    public void ConcreteGenericVariable_HasParameterizedTypeWithConcreteArgs()
    {
        // A local variable with a concrete generic type.
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Test
            {
                void M()
                {
                    Dictionary<string, int> dict = new Dictionary<string, int>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);

        var declType = varDecl!.TypeExpression?.Type;
        Assert.NotNull(declType);
        var paramType = Assert.IsType<JavaType.Parameterized>(declType);

        Assert.Contains("Dictionary", TypeUtils.GetFullyQualifiedName(paramType.Type));

        // Type args should be concrete: String, Int32
        Assert.NotNull(paramType.TypeParameters);
        Assert.Equal(2, paramType.TypeParameters!.Count);

        // string is mapped as Primitive(String) or Class(System.String)
        var tp0Fqn = TypeUtils.GetFullyQualifiedName(paramType.TypeParameters[0]);
        Assert.NotNull(tp0Fqn);
        Assert.Contains("String", tp0Fqn);

        // int is mapped as Primitive(Int) or Class(System.Int32)
        var tp1 = paramType.TypeParameters[1];
        Assert.True(
            tp1 is JavaType.Primitive { Kind: JavaType.Primitive.PrimitiveKind.Int } ||
            TypeUtils.GetFullyQualifiedName(tp1)?.Contains("Int32") == true,
            $"Expected int type but got: {tp1.GetType().Name}");
    }

    [Fact]
    public void ConcreteGenericVariable_InterfacesHaveUnsubstitutedTypeParams()
    {
        // Verify that the underlying Class for Dictionary stores interfaces
        // with unsubstituted type parameters (GenericTypeVariable), not
        // concrete types. This is the expected behavior — TypeUtils must
        // resolve these at comparison time.
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Test
            {
                void M()
                {
                    Dictionary<string, int> dict = new Dictionary<string, int>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);

        var paramType = Assert.IsType<JavaType.Parameterized>(varDecl!.TypeExpression?.Type);
        var dictClass = TypeUtils.AsClass(paramType);
        Assert.NotNull(dictClass);

        // Dictionary should implement IDictionary (among other interfaces)
        Assert.NotNull(dictClass!.Interfaces);
        var idict = dictClass.Interfaces!
            .Select(TypeUtils.AsClass)
            .FirstOrDefault(c => c?.FullyQualifiedName.Contains("IDictionary") == true);
        Assert.NotNull(idict);

        // Find the Parameterized form of the IDictionary interface
        var idictParam = dictClass.Interfaces!
            .OfType<JavaType.Parameterized>()
            .FirstOrDefault(p => TypeUtils.GetFullyQualifiedName(p.Type)?.Contains("IDictionary") == true);

        // The interface should be parameterized with GenericTypeVariables
        // (TKey, TValue from the Dictionary definition), NOT concrete types
        if (idictParam != null && idictParam.TypeParameters != null)
        {
            // At least one type param should be a GenericTypeVariable
            // (confirming these are unsubstituted from the original definition)
            var hasGtv = idictParam.TypeParameters.Any(tp => tp is JavaType.GenericTypeVariable);
            Assert.True(hasGtv,
                "Expected IDictionary interface to have GenericTypeVariable type params " +
                "(unsubstituted from Dictionary's original definition), but found: " +
                string.Join(", ", idictParam.TypeParameters.Select(tp => tp.GetType().Name)));
        }
    }

    [Fact]
    public void ImplicitUsings_ShortNameResolvesWithoutExplicitUsing()
    {
        // In .NET 6+, System.Collections.Generic is an implicit using.
        // Test whether Dictionary (short name, no using directive) gets
        // type attribution. This will fail if implicit usings are not
        // configured in the compilation.
        var cu = ParseWithSemanticModel("""
            class Test
            {
                void M()
                {
                    Dictionary<string, int> dict = new Dictionary<string, int>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);
        var declType = varDecl!.TypeExpression?.Type;
        // If implicit usings work, this should be Parameterized.
        // If not, it will be null or Unknown (unresolved).
        Assert.NotNull(declType);
        Assert.IsType<JavaType.Parameterized>(declType);
    }

    [Fact]
    public void DictionaryClass_HasFormalTypeParameters()
    {
        // Verify that the underlying Class for Dictionary has formal type parameters
        // (needed for building substitution maps during type comparison).
        var cu = ParseWithSemanticModel("""
            using System.Collections.Generic;
            class Test
            {
                void M()
                {
                    Dictionary<string, int> dict = new Dictionary<string, int>();
                }
            }
            """);

        var varDecl = FindVariableDeclaration(cu, "dict");
        Assert.NotNull(varDecl);

        var paramType = Assert.IsType<JavaType.Parameterized>(varDecl!.TypeExpression?.Type);
        var dictClass = TypeUtils.AsClass(paramType);
        Assert.NotNull(dictClass);

        // The Class should have TypeParameters listing formal type params
        Assert.NotNull(dictClass!.TypeParameters);
        Assert.Equal(2, dictClass.TypeParameters!.Count);

        var formal0 = Assert.IsType<JavaType.GenericTypeVariable>(dictClass.TypeParameters[0]);
        Assert.Equal("TKey", formal0.Name);

        var formal1 = Assert.IsType<JavaType.GenericTypeVariable>(dictClass.TypeParameters[1]);
        Assert.Equal("TValue", formal1.Name);
    }
}
