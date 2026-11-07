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
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Test;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

public class OperatorRpcTest(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    [Fact]
    public async Task MethodTypesCrossToJavaAndBack()
    {
        const string source = """
            class B { }
            class A
            {
                public static A operator +(A a, B b) => a;
                public static implicit operator A(B b) => new A();
            }
            """;
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Conversions", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
        var server = RewriteRpcServer.Current!;
        server.StoreLocalObject(cu.Id.ToString(), cu);
        var ctxId = Guid.NewGuid().ToString();
        server.StoreLocalObject(ctxId, new ExecutionContext());

        var returned = (CompilationUnit)server.VisitOnRemote("org.openrewrite.csharp.rpc.OperatorProbe",
            cu.Id.ToString(), "org.openrewrite.csharp.tree.Cs$CompilationUnit", ctxId,
            new Dictionary<string, object?>());

        var members = ((ClassDeclaration)returned.Members[1].Element).Body.Statements;
        var conversion = (ConversionOperatorDeclaration)members[1].Element;
        Assert.Equal("A{name=op_Implicit,return=A,parameters=[B]}",
            conversion.Markers.MarkerList.OfType<SearchResult>().Single().Description);
        Assert.Equal("probed:op_Implicit", conversion.MethodType?.Name);

        Assert.Equal("probed:op_Addition", ((OperatorDeclaration)members[0].Element).MethodType?.Name);
    }
}
