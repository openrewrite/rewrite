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
using NuGet.Frameworks;
using NuGet.ProjectModel;
using OpenRewrite.CSharp.NuGet;

namespace OpenRewrite.Tests;

public class PackageCompileAssetsTests : IDisposable
{
    private readonly string _root = Path.Combine(Path.GetTempPath(), "pca-" + Guid.NewGuid().ToString("N"));

    public void Dispose()
    {
        if (Directory.Exists(_root))
            Directory.Delete(_root, recursive: true);
    }

    private string Package(params string[] files)
    {
        var dir = Path.Combine(_root, "packages", "some.package", "1.0.0");
        foreach (var file in files)
        {
            var path = Path.Combine(dir, file);
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, "");
        }
        return dir;
    }

    private static IEnumerable<string> Relative(string packageDir, IEnumerable<string> paths) =>
        paths.Select(p => Path.GetRelativePath(packageDir, p).Replace('\\', '/'));

    [Fact]
    public void PrefersRefOverLib()
    {
        var dir = Package("lib/net8.0/Some.Package.dll", "ref/net8.0/Some.Package.dll");

        var assets = PackageCompileAssets.CompileAssemblies(dir, NuGetFramework.Parse("net8.0"));

        Assert.Equal(["ref/net8.0/Some.Package.dll"], Relative(dir, assets));
    }

    [Fact]
    public void SelectsNearestCompatibleFramework()
    {
        var dir = Package(
            "lib/netstandard2.0/Some.Package.dll",
            "lib/net6.0/Some.Package.dll",
            "lib/net462/Some.Package.dll");

        Assert.Equal(["lib/net6.0/Some.Package.dll"],
            Relative(dir, PackageCompileAssets.CompileAssemblies(dir, NuGetFramework.Parse("net8.0"))));
        Assert.Equal(["lib/net462/Some.Package.dll"],
            Relative(dir, PackageCompileAssets.CompileAssemblies(dir, NuGetFramework.Parse("net48"))));
    }

    [Fact]
    public void ReadsAssembliesDirectlyUnderLib()
    {
        var dir = Package("lib/Legacy.dll");

        var assets = PackageCompileAssets.CompileAssemblies(dir, NuGetFramework.Parse("net48"));

        Assert.Equal(["lib/Legacy.dll"], Relative(dir, assets));
    }

    [Fact]
    public void PlaceholderGroupHasNoAssemblies()
    {
        var dir = Package("lib/net8.0/_._", "lib/netstandard2.0/Some.Package.dll");

        Assert.Empty(PackageCompileAssets.CompileAssemblies(dir, NuGetFramework.Parse("net8.0")));
    }

    [Fact]
    public void FindsPrereleaseUnderLowercaseInstallPath()
    {
        var installed = Path.Combine(_root, "packages", "my.package", "1.0.0-beta.1");
        Directory.CreateDirectory(installed);

        var found = PackageCompileAssets.FindPackageDirectory(
            "My.Package", "1.0.0-Beta.1", [Path.Combine(_root, "missing"), Path.Combine(_root, "packages")]);

        Assert.Equal(installed, found);
    }

    [Fact]
    public void SearchesPackageFoldersRecordedFromRestore()
    {
        var folder = Path.Combine(_root, "repo-packages");
        var lockFile = new LockFile();
        lockFile.PackageFolders.Add(new LockFileItem(folder));

        PackageCompileAssets.RecordPackageFolders(lockFile);

        Assert.Contains(Path.GetFullPath(folder), PackageCompileAssets.PackageFolders());
    }
}
