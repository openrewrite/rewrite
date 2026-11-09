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

public class ArrayAccessTypeRpcTest(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    [Fact]
    public async Task ElementAccessTypeSurvivesJavaRoundTrip()
    {
        const string source = "class C { int M(int[] a) => a[0]; }";
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("ElementAccess", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
        var server = RewriteRpcServer.Current!;
        server.StoreLocalObject(cu.Id.ToString(), cu);
        var ctxId = Guid.NewGuid().ToString();
        server.StoreLocalObject(ctxId, new ExecutionContext());

        var returned = (CompilationUnit)server.VisitOnRemote("org.openrewrite.csharp.rpc.ArrayAccessTypeProbe",
            cu.Id.ToString(), "org.openrewrite.csharp.tree.Cs$CompilationUnit", ctxId,
            new Dictionary<string, object?>());

        var access = ElementAccesses.In(returned).Single();
        Assert.Equal("int", access.Markers.MarkerList.OfType<SearchResult>().Single().Description);
        Assert.Equal(JavaType.Primitive.PrimitiveKind.Long, Assert.IsType<JavaType.Primitive>(access.Type).Kind);
    }

    private sealed class ElementAccesses : CSharpVisitor<List<ArrayAccess>>
    {
        public static List<ArrayAccess> In(J tree)
        {
            var found = new List<ArrayAccess>();
            new ElementAccesses().Visit(tree, found);
            return found;
        }

        public override J VisitArrayAccess(ArrayAccess arrayAccess, List<ArrayAccess> found)
        {
            found.Add(arrayAccess);
            return base.VisitArrayAccess(arrayAccess, found);
        }
    }
}
