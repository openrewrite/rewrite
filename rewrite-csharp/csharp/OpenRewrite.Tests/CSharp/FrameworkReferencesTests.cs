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
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.CSharp;

public class FrameworkReferencesTests
{
    private static async Task<HashSet<string>> DefinedTypes(string targetFramework, string module)
    {
        var framework = await FrameworkReferences.ResolveAsync(targetFramework);
        Assert.NotNull(framework);
        var dll = framework!.Modules.Single(m => Path.GetFileNameWithoutExtension(m) == module);
        return AssemblyTypeEnumerator.Enumerate([dll], framework.All)
            .OfType<JavaType.Class>()
            .Select(c => c.FullyQualifiedName)
            .ToHashSet();
    }

    [Fact]
    public async Task NetFrameworkUsesItsReferenceAssemblies()
    {
        var names = await new RewriteRpcServer(new RecipeMarketplace())
            .FrameworkAssemblies(new FrameworkAssembliesRequest { TargetFramework = "net48" });

        Assert.Contains("mscorlib", names);
        Assert.Contains("System.Web", names);
        Assert.DoesNotContain("System.Private.CoreLib", names);

        var mscorlib = await DefinedTypes("net48", "mscorlib");
        Assert.Contains("System.String", mscorlib);
        Assert.DoesNotContain("System.Range", mscorlib);
        Assert.Contains("System.Web.HttpContext", await DefinedTypes("net48", "System.Web"));
    }

    [Fact]
    public async Task NetStandardUsesItsReferenceAssemblies()
    {
        var netstandard = await DefinedTypes("netstandard2.0", "netstandard");
        Assert.Contains("System.String", netstandard);
        Assert.DoesNotContain("System.Range", netstandard);

        Assert.Contains("System.Range", await DefinedTypes("netstandard2.1", "netstandard"));
    }

    [Fact]
    public async Task NetCoreAppUsesItsTargetingPack()
    {
        var framework = await FrameworkReferences.ResolveAsync("net8.0-windows");
        Assert.NotNull(framework);
        var names = framework!.Modules.Select(Path.GetFileNameWithoutExtension).ToList();

        Assert.Contains("System.Runtime", names);
        Assert.DoesNotContain("System.Private.CoreLib", names);
        Assert.Contains("System.Range", await DefinedTypes("net8.0", "System.Runtime"));
        Assert.DoesNotContain("System.Threading.Lock", await DefinedTypes("net8.0", "System.Runtime"));
    }

    [Fact]
    public async Task FrameworkWithoutReferenceAssembliesIsUnresolved()
    {
        Assert.Null(await FrameworkReferences.ResolveAsync("netstandard1.6"));
    }
}
