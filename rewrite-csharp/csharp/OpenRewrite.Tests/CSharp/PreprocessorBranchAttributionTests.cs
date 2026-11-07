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
using OpenRewrite.CSharp.Search;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp;

public class PreprocessorBranchAttributionTests
{
    [Fact]
    public async Task TheBranchWithTypesIsTheOneTheCompilationDefines()
    {
        await AssertBranchWithTypesIsAttributed(
            """
            #if NET20
            using Bridge;
            #else
            using System.Linq;
            #endif
            class C
            {
                int M(int[] a)
                {
            #if NETCOREAPP
                    return a.Count();
            #else
                    return a.Length;
            #endif
                }
            }
            """);
    }

    [Fact]
    public async Task TheBranchWithTypesHonorsTheFilesOwnDefines()
    {
        await AssertBranchWithTypesIsAttributed(
            """
            #define MODERN
            #if MODERN
            using System.Linq;
            #else
            using Bridge;
            #endif
            class C
            {
                int M(int[] a) => a.Count();
            }
            """);
    }

    private static async Task AssertBranchWithTypesIsAttributed(string source)
    {
        var refs = await Assemblies.Net90.ResolveAsync(LanguageNames.CSharp, CancellationToken.None);
        var syntax = CSharpSyntaxTree.ParseText(source, new CSharpParseOptions(preprocessorSymbols: ["NETCOREAPP"]));
        var compilation = CSharpCompilation.Create("Branches", [syntax], refs,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary));
        Assert.Empty(compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error));

        var cu = new CSharpParser().Parse(source, semanticModel: compilation.GetSemanticModel(syntax));

        Assert.Empty(FindMissingTypes.Find(cu));
        Assert.Equal(source, new CSharpPrinter<object>().Print(cu));
    }
}
