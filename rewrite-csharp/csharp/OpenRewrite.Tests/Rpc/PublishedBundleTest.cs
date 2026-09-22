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
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Verifies that a publish directory describes its own bundle through <c>Recipes.deps.json</c>:
/// the package the staging project referenced, its resolved version, and only that package's
/// own runtime assemblies — not transitive dependency DLLs.
/// </summary>
public class PublishedBundleTest : IDisposable
{
    private readonly string _publishDir = Path.Combine(Path.GetTempPath(), "published-bundle-test",
        Guid.NewGuid().ToString("N")[..8]);

    public PublishedBundleTest() => Directory.CreateDirectory(_publishDir);

    public void Dispose()
    {
        try { Directory.Delete(_publishDir, true); } catch { /* best-effort */ }
    }

    [Fact]
    public void ReadsPackageVersionAndOwnAssembliesFromDepsJson()
    {
        File.WriteAllText(Path.Combine(_publishDir, "Recipes.deps.json"), """
            {
              "runtimeTarget": { "name": ".NETCoreApp,Version=v10.0", "signature": "" },
              "targets": {
                ".NETCoreApp,Version=v10.0": {
                  "Recipes/1.0.0": {
                    "dependencies": { "My.Package": "1.2.3" },
                    "runtime": { "Recipes.dll": {} }
                  },
                  "My.Package/1.2.3": {
                    "dependencies": { "Dep.Package": "2.0.0" },
                    "runtime": {
                      "lib/net10.0/Primary.dll": {},
                      "lib/net10.0/Primary.Extras.dll": {}
                    }
                  },
                  "Dep.Package/2.0.0": {
                    "runtime": { "lib/netstandard2.0/DepLib.dll": {} }
                  }
                }
              },
              "libraries": {
                "Recipes/1.0.0": { "type": "project", "serviceable": false, "sha512": "" },
                "My.Package/1.2.3": { "type": "package", "serviceable": true, "sha512": "", "path": "my.package/1.2.3" },
                "Dep.Package/2.0.0": { "type": "package", "serviceable": true, "sha512": "", "path": "dep.package/2.0.0" }
              }
            }
            """);

        var bundle = PublishedBundle.Read(_publishDir);

        Assert.Equal("My.Package", bundle.PackageName);
        Assert.Equal("1.2.3", bundle.Version);
        Assert.Equal(["Primary", "Primary.Extras"], bundle.OwnAssemblyNames.Order());
        Assert.Contains("primary.extras", bundle.OwnAssemblyNames);
        Assert.DoesNotContain("DepLib", bundle.OwnAssemblyNames);
    }

    [Fact]
    public void DirectoryWithoutDepsJsonIsRejected()
    {
        var ex = Assert.Throws<InvalidOperationException>(() => PublishedBundle.Read(_publishDir));
        Assert.Contains(_publishDir, ex.Message);
    }
}
