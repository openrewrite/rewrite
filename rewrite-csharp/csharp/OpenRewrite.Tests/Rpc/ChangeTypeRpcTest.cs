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

public class ChangeTypeRpcTest(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    [Theory]
    [InlineData("Microsoft")]
    [InlineData("Acme")]
    public async Task PreservesCapitalizedNamespaces(string root)
    {
        // given
        const string source = "class C { public System.Web.Mvc.ActionResult Make() => new System.Web.Mvc.HttpStatusCodeResult(400); }";
        var declarations = CSharpSyntaxTree.ParseText("""
            namespace System.Web.Mvc {
                public class ActionResult { }
                public class HttpStatusCodeResult : ActionResult { public HttpStatusCodeResult(int status) { } }
            }
            """);
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("Before", [syntax, declarations], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
        var server = RewriteRpcServer.Current!;
        var ctxId = Guid.NewGuid().ToString();
        server.StoreLocalObject(ctxId, new ExecutionContext());

        // when
        foreach (var (oldName, newName) in new[] { ("ActionResult", "ActionResult"), ("HttpStatusCodeResult", "StatusCodeResult") })
        {
            var recipe = server.PrepareRecipeOnRemote("org.openrewrite.java.ChangeType", new Dictionary<string, object?>
            {
                ["oldFullyQualifiedTypeName"] = "System.Web.Mvc." + oldName,
                ["newFullyQualifiedTypeName"] = root + ".AspNetCore.Mvc." + newName,
                ["ignoreDefinition"] = true
            });
            server.StoreLocalObject(cu.Id.ToString(), cu);
            cu = (CompilationUnit)server.VisitOnRemote(recipe.EditVisitor, cu.Id.ToString(),
                "org.openrewrite.csharp.tree.Cs$CompilationUnit", ctxId, new Dictionary<string, object?>());
        }

        // then
        var output = new CSharpPrinter<object>().Print(cu);
        Assert.Contains(root + ".AspNetCore.Mvc", output);
        Assert.DoesNotContain("System.Web", output);
        var targetDeclarations = CSharpSyntaxTree.ParseText($$"""
            namespace {{root}}.AspNetCore.Mvc {
                public class ActionResult { }
                public class StatusCodeResult : ActionResult { public StatusCodeResult(int status) { } }
            }
            """);
        var after = CSharpCompilation.Create("After", [CSharpSyntaxTree.ParseText(output), targetDeclarations], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.DoesNotContain(after.GetDiagnostics(), d => d.Severity == DiagnosticSeverity.Error);
        var declaration = (ClassDeclaration)cu.Members.Single().Element;
        var method = (MethodDeclaration)declaration.Body.Statements.Single().Element;
        var returnType = Assert.IsAssignableFrom<JavaType.Class>(method.MethodType!.ReturnType);
        Assert.Equal(root + ".AspNetCore.Mvc.ActionResult", returnType.FullyQualifiedName);
        Assert.Null(returnType.OwningClass);
        var constructors = new List<NewClass>();
        new ConstructorFinder(constructors.Add).Visit(cu, 0);
        var constructor = Assert.Single(constructors);
        var constructedType = Assert.IsAssignableFrom<JavaType.Class>(constructor.Type);
        Assert.Equal(root + ".AspNetCore.Mvc.StatusCodeResult", constructedType.FullyQualifiedName);
        Assert.Null(constructedType.OwningClass);
        Assert.Equal(constructedType.FullyQualifiedName,
            Assert.IsAssignableFrom<JavaType.Class>(constructor.ConstructorType!.DeclaringType).FullyQualifiedName);
    }

    private sealed class ConstructorFinder(Action<NewClass> callback) : CSharpVisitor<int>
    {
        public override J VisitNewClass(NewClass nc, int p)
        {
            callback(nc);
            return base.VisitNewClass(nc, p);
        }
    }
}
