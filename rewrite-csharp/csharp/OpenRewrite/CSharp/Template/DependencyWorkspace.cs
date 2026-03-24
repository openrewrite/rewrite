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
using System.Collections.Concurrent;
using System.Collections.Immutable;
using System.Security.Cryptography;
using System.Text;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.Testing;

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Resolves NuGet package dependencies into Roslyn <see cref="MetadataReference"/>s
/// for template scaffold compilation. Uses <see cref="ReferenceAssemblies"/> from
/// Microsoft.CodeAnalysis.Testing to handle NuGet package resolution and caching.
/// </summary>
internal static class DependencyWorkspace
{
    private static readonly ConcurrentDictionary<string, ImmutableArray<MetadataReference>> ReferencesCache = new();

    /// <summary>
    /// Resolve NuGet dependencies into metadata references.
    /// Results are cached by the sorted dependency set.
    /// </summary>
    internal static ImmutableArray<MetadataReference> ResolveReferences(
        IReadOnlyDictionary<string, string> dependencies)
    {
        var cacheKey = BuildCacheKey(dependencies);
        return ReferencesCache.GetOrAdd(cacheKey, _ => ResolveInternal(dependencies));
    }

    /// <summary>
    /// Create a Roslyn <see cref="SemanticModel"/> for the given source code,
    /// with NuGet package references resolved for type attribution.
    /// </summary>
    internal static SemanticModel CreateSemanticModel(string source,
        IReadOnlyDictionary<string, string> dependencies)
    {
        var references = ResolveReferences(dependencies);
        var syntaxTree = CSharpSyntaxTree.ParseText(source, path: "__template__.cs");
        var compilation = CSharpCompilation.Create("TemplateCompilation")
            .WithOptions(new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary))
            .AddReferences(references)
            .AddSyntaxTrees(syntaxTree);
        return compilation.GetSemanticModel(syntaxTree);
    }

    private static ImmutableArray<MetadataReference> ResolveInternal(
        IReadOnlyDictionary<string, string> dependencies)
    {
        var refAssemblies = ReferenceAssemblies.Net.Net90;
        var packages = dependencies
            .Select(d => new PackageIdentity(d.Key, d.Value))
            .ToImmutableArray();

        if (packages.Length > 0)
        {
            refAssemblies = refAssemblies.AddPackages(packages);
        }

        return refAssemblies
            .ResolveAsync(LanguageNames.CSharp, CancellationToken.None)
            .GetAwaiter()
            .GetResult();
    }

    private static string BuildCacheKey(IReadOnlyDictionary<string, string> dependencies)
    {
        var sb = new StringBuilder();
        foreach (var kvp in dependencies.OrderBy(d => d.Key, StringComparer.Ordinal))
        {
            sb.Append(kvp.Key);
            sb.Append('=');
            sb.Append(kvp.Value);
            sb.Append(';');
        }

        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(sb.ToString()));
        return Convert.ToHexString(bytes)[..16];
    }
}
