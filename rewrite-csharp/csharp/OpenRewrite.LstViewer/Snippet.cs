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
using Microsoft.CodeAnalysis.Testing;
using OpenRewrite.CSharp;
using PackageIdentity = Microsoft.CodeAnalysis.Testing.PackageIdentity;

namespace OpenRewrite.LstViewer;

internal enum WrapMode
{
    Auto,
    None,
    Member,
    Statement,
    Expression
}

internal sealed record ParsedSnippet(
    string Source,
    WrapMode Wrap,
    CompilationUnit Tree,
    IReadOnlyList<Diagnostic> SyntaxErrors);

internal static class Snippet
{
    private const string SourcePath = "snippet.cs";

    public static async Task<ParsedSnippet> ParseAsync(string code, WrapMode wrap, string framework, bool semantic,
        CancellationToken cancellation = default)
    {
        var applied = wrap == WrapMode.Auto ? Detect(code) : wrap;
        var source = Wrap(applied, code);
        var syntaxTree = CSharpSyntaxTree.ParseText(source, path: SourcePath);
        var errors = syntaxTree.GetDiagnostics()
            .Where(d => d.Severity == DiagnosticSeverity.Error)
            .ToList();

        SemanticModel? model = null;
        if (semantic)
        {
            var references = await ReferencesFor(framework)
                .ResolveAsync(LanguageNames.CSharp, cancellation);
            model = CSharpCompilation.Create("Snippet")
                .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
                .AddReferences(references)
                .AddSyntaxTrees(syntaxTree)
                .GetSemanticModel(syntaxTree);
        }

        var tree = new CSharpParser().Parse(source, SourcePath, model);
        return new ParsedSnippet(source, applied, tree, errors);
    }

    public static bool TryParseWrap(string value, out WrapMode wrap)
    {
        foreach (var mode in Enum.GetValues<WrapMode>())
        {
            if (string.Equals(mode.ToString(), value, StringComparison.OrdinalIgnoreCase))
            {
                wrap = mode;
                return true;
            }
        }
        wrap = WrapMode.Auto;
        return false;
    }

    private static WrapMode Detect(string code)
    {
        foreach (var candidate in new[] { WrapMode.None, WrapMode.Member, WrapMode.Statement })
        {
            if (!HasSyntaxErrors(Wrap(candidate, code)))
            {
                return candidate;
            }
        }
        return WrapMode.None;
    }

    private static bool HasSyntaxErrors(string source) =>
        CSharpSyntaxTree.ParseText(source, path: SourcePath)
            .GetDiagnostics()
            .Any(d => d.Severity == DiagnosticSeverity.Error);

    private static string Wrap(WrapMode wrap, string code) => wrap switch
    {
        WrapMode.Member => $"class Snippet\n{{\n{code.TrimEnd()}\n}}\n",
        WrapMode.Statement => $"class Snippet\n{{\n    void Run()\n    {{\n{code.TrimEnd()}\n    }}\n}}\n",
        WrapMode.Expression => $"class Snippet\n{{\n    void Run()\n    {{\n        var expr = {code.Trim()};\n    }}\n}}\n",
        _ => code
    };

    private static ReferenceAssemblies ReferencesFor(string framework)
    {
        var (assemblies, aspNet) = framework.ToLowerInvariant() switch
        {
            "net9" or "net9.0" => (ReferenceAssemblies.Net.Net90, false),
            "net10" or "net10.0" => (ReferenceAssemblies.Net.Net100, false),
            "net48" or "net4.8" => (ReferenceAssemblies.NetFramework.Net48.Default, false),
            "aspnet9" or "aspnet9.0" => (ReferenceAssemblies.Net.Net90, true),
            "aspnet10" or "aspnet10.0" => (ReferenceAssemblies.Net.Net100, true),
            _ => throw new ArgumentException(
                $"Unknown framework '{framework}'. Use net9, net10, net48, aspnet9 or aspnet10.")
        };

        if (!aspNet)
        {
            return assemblies;
        }

        var version = assemblies.ReferenceAssemblyPackage?.Version
                      ?? throw new InvalidOperationException(
                          $"{framework} has no reference assembly package to take a version from.");
        return assemblies.AddPackages([new PackageIdentity("Microsoft.AspNetCore.App.Ref", version)]);
    }
}
