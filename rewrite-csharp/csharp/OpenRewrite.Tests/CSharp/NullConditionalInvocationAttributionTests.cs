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
    public async Task EachInvocationInANullConditionalChainCarriesItsMethod()
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
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source);
        var compilation = CSharpCompilation.Create("NullConditional", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));
        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));

        var invocations = new List<MethodInvocation>();
        new InvocationCollector().Visit(cu, invocations);
        Assert.Equal(
            ["Trim", "ToUpper", "ToUpper", "ToLower", "Trim", "ToUpper", "ToLower", "ToString", "Where", "First", "Select"],
            invocations.Select(mi => mi.MethodType?.Name ?? $"<none for {mi.Name.SimpleName}>"));
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
