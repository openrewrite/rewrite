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

public class ConstructorAttributionRpcTest(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    [Fact]
    public async Task DescriptorsAndSourceIdentitySurviveJavaRoundTrip()
    {
        const string source = "class C { public C(ref int value, int count = 0) { } }";
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Constructors", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
        var before = Constructor(cu);
        var server = RewriteRpcServer.Current!;
        server.StoreLocalObject(cu.Id.ToString(), cu);
        var ctxId = Guid.NewGuid().ToString();
        server.StoreLocalObject(ctxId, new ExecutionContext());
        var returned = (CompilationUnit)server.VisitOnRemote("org.openrewrite.csharp.rpc.ConstructorProbe",
            cu.Id.ToString(), "org.openrewrite.csharp.tree.Cs$CompilationUnit", ctxId,
            new Dictionary<string, object?>());
        Assert.Equal("Attributed constructors: 1", returned.Markers.MarkerList.OfType<SearchResult>().Single().Description);
        var after = Constructor(returned);
        Assert.Equal(before.Id, after.Id);
        Assert.Equal(before.Name.Id, after.Name.Id);
        Assert.NotNull(after.MethodType);
        Assert.Same(after.MethodType, after.Name.Type);
        Assert.Equal(".ctor", after.MethodType.Name);
        Assert.Equal(source, new CSharpPrinter<object>().Print(returned.WithMarkers(cu.Markers)));
        var fresh = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
        Assert.Equal(after.MethodType.Name, Constructor(fresh).MethodType!.Name);
    }

    private static MethodDeclaration Constructor(CompilationUnit cu) =>
        (MethodDeclaration)((ClassDeclaration)cu.Members.Single().Element).Body.Statements.Single().Element;
}
