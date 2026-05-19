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
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Regression coverage for the InstallRecipes RPC, which used to echo wildcard versions
/// (e.g. "*", "*-*") back to the caller because it read the version off the csproj's
/// PackageReference Version attribute instead of the resolved value in project.assets.json.
/// </summary>
public class InstallVersionResolverTests
{
    private const string Pkg = "OpenRewrite.CSharp";

    [Theory]
    [InlineData("*",   "8.81.17")]
    [InlineData("*-*", "8.82.0-snapshot.20260514132053")]
    public void WildcardRequestResolvesToConcreteVersion(string requested, string resolved)
    {
        WithAssetsJson(requested, resolved, projectDir =>
            Assert.Equal(resolved,
                MSBuildProjectHelper.GetResolvedPackageVersion(projectDir, Pkg)));
    }

    [Fact]
    public void PackageNameMatchIsCaseInsensitive()
    {
        // Caller-supplied package name may differ in casing from the ID recorded in
        // project.assets.json; resolution must still succeed.
        WithAssetsJson("*", "8.81.17", projectDir =>
            Assert.Equal("8.81.17",
                MSBuildProjectHelper.GetResolvedPackageVersion(projectDir, Pkg.ToLowerInvariant())));
    }

    [Fact]
    public void UnknownPackageThrows()
    {
        WithAssetsJson("*", "8.81.17", projectDir =>
        {
            var ex = Assert.Throws<InvalidOperationException>(() =>
                MSBuildProjectHelper.GetResolvedPackageVersion(projectDir, "Does.Not.Exist"));
            Assert.Contains("not a direct dependency", ex.Message);
        });
    }

    [Fact]
    public void MissingAssetsJsonThrows()
    {
        WithTempDir(projectDir =>
        {
            var ex = Assert.Throws<InvalidOperationException>(() =>
                MSBuildProjectHelper.GetResolvedPackageVersion(projectDir, Pkg));
            Assert.Contains("project.assets.json not found", ex.Message);
        });
    }

    private static void WithAssetsJson(
        string requestedVersion,
        string resolvedVersion,
        Action<string> assert)
    {
        WithTempDir(projectDir =>
        {
            var objDir = Path.Combine(projectDir, "obj");
            Directory.CreateDirectory(objDir);
            File.WriteAllText(Path.Combine(objDir, "project.assets.json"), $$"""
                {
                  "version": 3,
                  "targets": {
                    "net10.0": {
                      "{{Pkg}}/{{resolvedVersion}}": { "type": "package" }
                    }
                  },
                  "libraries": {
                    "{{Pkg}}/{{resolvedVersion}}": { "type": "package" }
                  },
                  "project": {
                    "frameworks": {
                      "net10.0": {
                        "dependencies": {
                          "{{Pkg}}": {
                            "target": "Package",
                            "version": "[{{requestedVersion}}, )"
                          }
                        }
                      }
                    }
                  }
                }
                """);
            assert(projectDir);
        });
    }

    private static void WithTempDir(Action<string> assert)
    {
        var projectDir = Path.Combine(Path.GetTempPath(),
            "openrewrite-installrecipes-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(projectDir);
        try
        {
            assert(projectDir);
        }
        finally
        {
            try { Directory.Delete(projectDir, recursive: true); } catch { /* best-effort */ }
        }
    }
}
