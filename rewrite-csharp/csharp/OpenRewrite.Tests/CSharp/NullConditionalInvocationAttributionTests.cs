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

public class NullConditionalInvocationAttributionTests
{
    [Fact]
    public async Task EachInvocationInANullConditionalChainAndItsNameCarryItsMethod()
    {
        const string source = """
            using System.Linq;
            class C
            {
                void M(string s, string[] arr, System.Collections.Generic.Dictionary<string, int[]> dict)
                {
                    s?.Trim();
                    arr?[0].ToUpper();
                    s?.ToUpper()?.ToLower();
                    s?.Trim()?.ToUpper()?.ToLower();
                    s?.Length.ToString();
                    dict["key"]?.Where(x => x > 0).First();
                    s?.Select<char, int>(c => c);
                }
            }
            """;
        var cu = await ParseAsync(source);

        var invocations = new List<MethodInvocation>();
        new InvocationCollector().Visit(cu, invocations);
        Assert.Equal(
            ["Trim", "ToUpper", "ToUpper", "ToLower", "Trim", "ToUpper", "ToLower", "ToString", "Where", "First", "Select"],
            invocations.Select(mi => mi.MethodType?.Name ?? $"<none for {mi.Name.SimpleName}>"));
        Assert.All(invocations, mi => Assert.Same(mi.MethodType, mi.Name.Type));
    }

    [Fact]
    public async Task EachMemberAccessInANullConditionalChainCarriesItsMember()
    {
        var cu = await ParseAsync("""
            class D
            {
                public event System.EventHandler? Changed;
            }
            class C
            {
                void M(string s, string[] arr, D d, System.EventHandler h)
                {
                    var a = s?.Length;
                    var b = s?.Trim().Length;
                    var c = arr?[0].Length;
                    var e = s?.Trim()?.Length;
                    d?.Changed -= h;
                }
            }
            """);

        var accesses = new List<FieldAccess>();
        new MemberAccessCollector().Visit(cu, accesses);
        Assert.Equal(["Length", "Length", "Length", "Length", "Changed"],
            accesses.Select(fa => (fa.Name.Element.FieldType as JavaType.Variable)?.Name ?? $"<none for {fa.Name.Element.SimpleName}>"));
        Assert.Equal(["int", "int", "int", "int", "System.EventHandler"],
            accesses.Select(fa => TypeUtils.GetFullyQualifiedName(fa.Type) ?? $"<none for {fa.Name.Element.SimpleName}>"));
    }

    private static async Task<CompilationUnit> ParseAsync(string source)
    {
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("NullConditional", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
        return new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));
    }

    private sealed class MemberAccessCollector : CSharpVisitor<List<FieldAccess>>
    {
        public override J VisitFieldAccess(FieldAccess fieldAccess, List<FieldAccess> found)
        {
            if (fieldAccess.Name.Element.SimpleName is "Length" or "Changed")
                found.Add(fieldAccess);
            return base.VisitFieldAccess(fieldAccess, found);
        }
    }

    private sealed class InvocationCollector : CSharpVisitor<List<MethodInvocation>>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, List<MethodInvocation> found)
        {
            var visited = base.VisitMethodInvocation(mi, found);
            found.Add(mi);
            return visited;
        }
    }
}
