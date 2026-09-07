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
using System.Text.Json;

namespace OpenRewrite.CSharp.Rpc;

/// <summary>
/// What a recipe bundle's publish output says about itself, read from the staging project's
/// <c>Recipes.deps.json</c>: the NuGet package the staging project referenced, the version that
/// resolved to, and the runtime assemblies that package contributed itself (as opposed to its
/// transitive dependencies). Because all of this lives in the publish directory, a host can hand
/// the directory back to a later server instance and have it loaded without a registry.
/// </summary>
internal sealed record PublishedBundle(string PackageName, string Version, HashSet<string> OwnAssemblyNames)
{
    public static PublishedBundle Read(string publishDir)
    {
        var depsJsonPath = Path.Combine(publishDir, "Recipes.deps.json");
        if (!File.Exists(depsJsonPath))
        {
            throw new InvalidOperationException(
                $"{publishDir} is not a recipe bundle publish directory: no Recipes.deps.json");
        }

        using var doc = JsonDocument.Parse(File.ReadAllBytes(depsJsonPath));
        var depsJson = doc.RootElement;
        var runtimeTarget = depsJson.GetProperty("runtimeTarget").GetProperty("name").GetString()!;
        var target = depsJson.GetProperty("targets").GetProperty(runtimeTarget);

        // The staging project is the only project-typed library; its one direct dependency is the bundle.
        var stagingProject = depsJson.GetProperty("libraries").EnumerateObject()
            .Where(library => library.Value.GetProperty("type").GetString() == "project")
            .Select(library => library.Name)
            .SingleOrDefault()
            ?? throw new InvalidOperationException($"No staging project in {depsJsonPath}");
        var dependencies = target.GetProperty(stagingProject).TryGetProperty("dependencies", out var deps)
            ? deps.EnumerateObject().ToList()
            : [];
        if (dependencies.Count != 1)
        {
            throw new InvalidOperationException(
                $"Expected {stagingProject} in {depsJsonPath} to reference exactly one package, found {dependencies.Count}");
        }
        var packageName = dependencies[0].Name;
        var version = dependencies[0].Value.GetString()!;

        var ownAssemblyNames = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        if (target.GetProperty($"{packageName}/{version}").TryGetProperty("runtime", out var runtime))
        {
            foreach (var assembly in runtime.EnumerateObject())
            {
                ownAssemblyNames.Add(Path.GetFileNameWithoutExtension(assembly.Name));
            }
        }
        return new PublishedBundle(packageName, version, ownAssemblyNames);
    }
}
