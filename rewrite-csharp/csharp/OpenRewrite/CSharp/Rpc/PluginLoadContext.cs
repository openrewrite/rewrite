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
using System.Reflection;
using System.Runtime.Loader;
using Serilog;

namespace OpenRewrite.CSharp.Rpc;

/// <summary>
/// An isolated AssemblyLoadContext for loading plugin NuGet packages.
/// Uses <see cref="AssemblyDependencyResolver"/> (backed by .deps.json from dotnet publish)
/// to resolve plugin-private dependencies. Assemblies already loaded by the host are
/// returned directly from <see cref="AssemblyLoadContext.Default"/>, preserving type identity
/// for shared types like Recipe and IRecipeActivator.
/// </summary>
public class PluginLoadContext : AssemblyLoadContext
{
    private readonly AssemblyDependencyResolver _resolver;

    public PluginLoadContext(string pluginMainDllPath)
        : base(Path.GetFileNameWithoutExtension(pluginMainDllPath), isCollectible: false)
    {
        _resolver = new AssemblyDependencyResolver(pluginMainDllPath);
    }

    protected override Assembly? Load(AssemblyName assemblyName)
    {
        // If the assembly is already loaded in the Default ALC, return it directly.
        // This preserves type identity for shared host types (Recipe, IRecipeActivator, etc.)
        // and handles version mismatches between the plugin's reference and the host's loaded version.
        var existing = Default.Assemblies
            .FirstOrDefault(a => string.Equals(a.GetName().Name, assemblyName.Name,
                StringComparison.OrdinalIgnoreCase));
        if (existing != null)
            return existing;

        // Resolve from plugin's dependency graph via .deps.json
        var path = _resolver.ResolveAssemblyToPath(assemblyName);
        if (path != null)
        {
            try
            {
                Log.Debug("Plugin ALC {Context} loading {Assembly} from {Path}",
                    Name, assemblyName.Name, path);
                return LoadFromAssemblyPath(path);
            }
            catch (Exception ex)
            {
                Log.Warning("Plugin ALC {Context} failed to load {Assembly} from {Path}: {Error}",
                    Name, assemblyName.Name, path, ex.Message);
                return null;
            }
        }

        return null; // fall back to Default ALC probing
    }
}
