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

public class DestructorAndOperatorAttributionTests
{
    [Fact]
    public async Task DeclarationsCarryTheMethodsTheyDeclare()
    {
        const string source = """
            class B { }
            class A
            {
                ~A() { }
                public static A operator +(A a, B b) => a;
                public static implicit operator A(B b) => new A();
            }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Declarations", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
        var b = Assert.IsType<ClassDeclaration>(cu.Members[0].Element).Type;
        var a = Assert.IsType<ClassDeclaration>(cu.Members[1].Element);
        var members = a.Body.Statements.Select(s => s.Element).ToList();

        AssertDeclares(Assert.IsType<MethodDeclaration>(members[0]).MethodType,
            "Finalize", a.Type, JavaType.Primitive.Of(JavaType.PrimitiveKind.Void));
        AssertDeclares(Assert.IsType<OperatorDeclaration>(members[1]).MethodType,
            "op_Addition", a.Type, a.Type, a.Type, b);
        AssertDeclares(Assert.IsType<ConversionOperatorDeclaration>(members[2]).MethodType,
            "op_Implicit", a.Type, a.Type, b);
    }

    private static void AssertDeclares(JavaType.Method? method, string name, JavaType? declaringType,
        JavaType? returnType, params JavaType?[] parameterTypes)
    {
        Assert.NotNull(method);
        Assert.Equal(name, method.Name);
        Assert.NotNull(declaringType);
        Assert.Same(declaringType, method.DeclaringType);
        Assert.Same(returnType, method.ReturnType);
        var actual = method.ParameterTypes ?? [];
        Assert.Equal(parameterTypes.Length, actual.Count);
        for (var i = 0; i < parameterTypes.Length; i++)
            Assert.Same(parameterTypes[i], actual[i]);
    }
}
