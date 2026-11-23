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
using NuGet.ProjectModel;
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Regression coverage for the InstallRecipes RPC, which used to echo wildcard versions
/// (e.g. "*", "*-*") back to the caller because it read the version off the csproj's
/// PackageReference Version attribute instead of the version NuGet actually resolved.
/// The resolved version now comes from the in-memory <see cref="LockFile"/> of the
/// in-process restore.
/// </summary>
public class InstallVersionResolverTests
{
    private const string Pkg = "OpenRewrite.CSharp";

    [Theory]
    [InlineData("*",   "8.81.17")]
    [InlineData("*-*", "8.82.0-snapshot.20260514132053")]
    public void WildcardRequestResolvesToConcreteVersion(string requested, string resolved)
    {
        var lockFile = LockFileFor(requested, resolved);
        Assert.Equal(resolved, MSBuildProjectHelper.GetResolvedPackageVersion(lockFile, Pkg));
    }

    [Fact]
    public void PackageNameMatchIsCaseInsensitive()
    {
        // Caller-supplied package name may differ in casing from the resolved ID;
        // resolution must still succeed.
        var lockFile = LockFileFor("*", "8.81.17");
        Assert.Equal("8.81.17",
            MSBuildProjectHelper.GetResolvedPackageVersion(lockFile, Pkg.ToLowerInvariant()));
    }

    [Fact]
    public void UnknownPackageThrows()
    {
        var lockFile = LockFileFor("*", "8.81.17");
        var ex = Assert.Throws<InvalidOperationException>(() =>
            MSBuildProjectHelper.GetResolvedPackageVersion(lockFile, "Does.Not.Exist"));
        Assert.Contains("not a direct dependency", ex.Message);
    }

    [Fact]
    public void ProjectWithoutCsprojThrows()
    {
        var projectDir = Path.Combine(Path.GetTempPath(),
            "openrewrite-installrecipes-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(projectDir);
        try
        {
            var ex = Assert.Throws<InvalidOperationException>(() =>
                MSBuildProjectHelper.GetResolvedPackageVersion(projectDir, Pkg));
            Assert.Contains("No .csproj found", ex.Message);
        }
        finally
        {
            try { Directory.Delete(projectDir, recursive: true); } catch { /* best-effort */ }
        }
    }

    private static LockFile LockFileFor(string requestedVersion, string resolvedVersion)
    {
        // Same JSON shape the restore produces; parsed straight into the LockFile object model.
        return new LockFileFormat().Parse($$"""
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
            """, "in-memory");
    }
}
