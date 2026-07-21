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
using OpenRewrite.Xml;

namespace OpenRewrite.Tests.Xml;

public class CsprojParserTests
{
    [Fact]
    public void CsprojParserRunsInProcessRestoreAndAttachesFullMarker()
    {
        var parser = new CsprojParser();
        var doc = parser.Parse(
            """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net8.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
              </ItemGroup>
            </Project>
            """);

        var marker = doc.Markers.FindFirst<MSBuildProject>();
        Assert.NotNull(marker);
        Assert.Equal("Microsoft.NET.Sdk", marker.Sdk);
        // The in-process restore (PackageSpec/RestoreRunner -> LockFile) yields full metadata
        Assert.Single(marker.TargetFrameworks);
        Assert.Equal("net8.0", marker.TargetFrameworks[0].TargetFrameworkMoniker);
        Assert.Single(marker.TargetFrameworks[0].PackageReferences);
        var pkgRef = marker.TargetFrameworks[0].PackageReferences[0];
        Assert.Equal("Newtonsoft.Json", pkgRef.Include);
        Assert.Equal("13.0.3", pkgRef.RequestedVersion);
        Assert.Equal("13.0.3", pkgRef.ResolvedVersion);

        // Extended asset data from the lock file
        var resolved = marker.TargetFrameworks[0].ResolvedPackages
            .Single(p => p.Name == "Newtonsoft.Json");
        Assert.Equal("package", resolved.Type);
        Assert.Contains(resolved.CompileAssemblies, p => p.EndsWith("Newtonsoft.Json.dll"));
        Assert.Contains(resolved.RuntimeAssemblies, p => p.EndsWith("Newtonsoft.Json.dll"));
        Assert.Equal(0, resolved.Depth);
    }

    [Fact]
    public void PackagesConfigProjectGetsFullMarker()
    {
        var parser = new CsprojParser();
        var docs = parser.ParseAll(
        [
            (
                """
                <?xml version="1.0" encoding="utf-8"?>
                <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
                  <PropertyGroup>
                    <TargetFrameworkVersion>v4.8</TargetFrameworkVersion>
                    <OutputType>Library</OutputType>
                  </PropertyGroup>
                  <ItemGroup>
                    <Reference Include="Newtonsoft.Json, Version=13.0.0.0, Culture=neutral, PublicKeyToken=30ad4fe6b2a6aeed">
                      <HintPath>..\packages\Newtonsoft.Json.13.0.3\lib\net45\Newtonsoft.Json.dll</HintPath>
                    </Reference>
                  </ItemGroup>
                </Project>
                """,
                "Legacy/Legacy.csproj"
            ),
            (
                """
                <?xml version="1.0" encoding="utf-8"?>
                <packages>
                  <package id="Newtonsoft.Json" version="13.0.3" targetFramework="net48" />
                </packages>
                """,
                "Legacy/packages.config"
            )
        ]);

        var csprojDoc = docs[0];
        var marker = csprojDoc.Markers.FindFirst<MSBuildProject>();
        Assert.NotNull(marker);
        // Legacy packages.config projects get a full dependency graph from the
        // synthesized PackageSpec restore.
        Assert.Single(marker.TargetFrameworks);
        Assert.Equal("net48", marker.TargetFrameworks[0].TargetFrameworkMoniker);
        // PackageReferences stays 1:1 with csproj <PackageReference> items (none here);
        // the packages.config directs appear as depth-0 entries in the resolved graph.
        Assert.Empty(marker.TargetFrameworks[0].PackageReferences);
        var resolved = marker.TargetFrameworks[0].ResolvedPackages
            .Single(p => p.Name == "Newtonsoft.Json");
        Assert.Equal("13.0.3", resolved.ResolvedVersion);
        Assert.Equal(0, resolved.Depth);
        Assert.Contains(resolved.CompileAssemblies, p => p.EndsWith("Newtonsoft.Json.dll"));

        // packages.config itself parses as plain XML without a project marker
        Assert.Null(docs[1].Markers.FindFirst<MSBuildProject>());
    }

    [Fact]
    public void MarkerCreatedFromLockFile()
    {
        // Hand-crafted lock file (same JSON shape as project.assets.json) parsed with
        // LockFileFormat, verifying the LockFile -> marker mapping without any restore.
        var lockFileJson = """
            {
              "version": 3,
              "targets": {
                "net8.0": {
                  "Newtonsoft.Json/13.0.3": {
                    "type": "package",
                    "compile": {
                      "lib/net6.0/Newtonsoft.Json.dll": {}
                    },
                    "runtime": {
                      "lib/net6.0/Newtonsoft.Json.dll": {}
                    },
                    "build": {
                      "build/Newtonsoft.Json.targets": {}
                    }
                  },
                  "Placeholder.Pkg/1.0.0": {
                    "type": "package",
                    "dependencies": {
                      "Newtonsoft.Json": "13.0.1"
                    },
                    "compile": {
                      "lib/net8.0/_._": {}
                    }
                  }
                }
              },
              "libraries": {
                "Newtonsoft.Json/13.0.3": {
                  "type": "package",
                  "path": "newtonsoft.json/13.0.3",
                  "files": [
                    "analyzers/dotnet/cs/Newtonsoft.Json.Analyzer.dll",
                    "tools/install.ps1",
                    "content/web.config.transform",
                    "content/scripts/app.js",
                    "lib/net6.0/Newtonsoft.Json.dll"
                  ]
                },
                "Placeholder.Pkg/1.0.0": {
                  "type": "package",
                  "path": "placeholder.pkg/1.0.0",
                  "files": [ "lib/net8.0/_._" ]
                }
              },
              "projectFileDependencyGroups": {
                "net8.0": ["Placeholder.Pkg >= 1.0.0"]
              },
              "packageFolders": {},
              "project": {
                "version": "1.0.0",
                "restore": {
                  "projectName": "TestProject",
                  "projectStyle": "PackageReference",
                  "originalTargetFrameworks": ["net8.0"],
                  "frameworks": {
                    "net8.0": { "targetAlias": "net8.0" }
                  }
                },
                "frameworks": {
                  "net8.0": {
                    "targetAlias": "net8.0",
                    "dependencies": {
                      "Placeholder.Pkg": {
                        "target": "Package",
                        "version": "[1.0.0, )"
                      }
                    }
                  }
                }
              }
            }
            """;
        var lockFile = new LockFileFormat().Parse(lockFileJson, "in-memory");

        var marker = MSBuildProjectHelper.CreateFromLockFile(
            "Microsoft.NET.Sdk", lockFile, Path.GetTempPath());

        Assert.Single(marker.TargetFrameworks);
        var tf = marker.TargetFrameworks[0];
        Assert.Equal("net8.0", tf.TargetFrameworkMoniker);

        // Declared reference resolves via the graph
        var pkgRef = Assert.Single(tf.PackageReferences);
        Assert.Equal("Placeholder.Pkg", pkgRef.Include);
        Assert.Equal("1.0.0", pkgRef.RequestedVersion);
        Assert.Equal("1.0.0", pkgRef.ResolvedVersion);

        Assert.Equal(2, tf.ResolvedPackages.Count);

        var placeholder = tf.ResolvedPackages.Single(p => p.Name == "Placeholder.Pkg");
        Assert.Equal(0, placeholder.Depth); // direct
        Assert.Empty(placeholder.CompileAssemblies); // _._ placeholder stripped
        var edge = Assert.Single(placeholder.Dependencies);
        Assert.Equal("Newtonsoft.Json", edge.Name);
        Assert.Equal("13.0.3", edge.ResolvedVersion); // edge links to the RESOLVED node

        var newtonsoft = tf.ResolvedPackages.Single(p => p.Name == "Newtonsoft.Json");
        Assert.Equal(1, newtonsoft.Depth); // transitive
        Assert.Contains("lib/net6.0/Newtonsoft.Json.dll", newtonsoft.CompileAssemblies);
        Assert.Contains("lib/net6.0/Newtonsoft.Json.dll", newtonsoft.RuntimeAssemblies);
        Assert.Contains("build/Newtonsoft.Json.targets", newtonsoft.BuildFiles);
        Assert.Contains("analyzers/dotnet/cs/Newtonsoft.Json.Analyzer.dll", newtonsoft.AnalyzerAssemblies);
        Assert.True(newtonsoft.HasInstallScripts);
        Assert.True(newtonsoft.HasXdtTransforms);
        Assert.True(newtonsoft.HasLegacyContentFolder);
    }

    // When an in-process restore is unavailable (offline CI, restore-graph failure), the marker
    // must still expose declared <PackageReference> items from the XML, or the Find/Upgrade/Remove
    // NuGet recipes silently no-op.
    [Fact]
    public void MarkerExposesDeclaredPackageReferencesWithoutRestore()
    {
        var doc = new XmlParser().Parse(
            """
            <Project Sdk="Microsoft.NET.Sdk.Web">
              <PropertyGroup>
                <TargetFramework>net8.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <PackageReference Include="Swashbuckle.AspNetCore" Version="6.4.0" />
                <PackageReference Include="Microsoft.AspNetCore.OpenApi" Version="7.0.0" />
              </ItemGroup>
            </Project>
            """,
            "project.csproj");

        // No rootDir -> no restore is attempted; the marker is built from the XML alone.
        var marker = MSBuildProjectHelper.CreateMarker(doc);

        Assert.NotNull(marker);
        Assert.Equal("Microsoft.NET.Sdk.Web", marker.Sdk);
        var tfm = Assert.Single(marker.TargetFrameworks);
        Assert.Equal("net8.0", tfm.TargetFrameworkMoniker);
        Assert.Equal(2, tfm.PackageReferences.Count);
        Assert.Contains(tfm.PackageReferences, p => p.Include == "Swashbuckle.AspNetCore" && p.RequestedVersion == "6.4.0");
        Assert.Contains(tfm.PackageReferences, p => p.Include == "Microsoft.AspNetCore.OpenApi" && p.RequestedVersion == "7.0.0");
    }

    [Fact]
    public void MarkerExposesDeclaredPackageReferencesForMultiTargetWithoutRestore()
    {
        var doc = new XmlParser().Parse(
            """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFrameworks>net8.0;net9.0</TargetFrameworks>
              </PropertyGroup>
              <ItemGroup>
                <PackageReference Include="Newtonsoft.Json">
                  <Version>13.0.3</Version>
                </PackageReference>
              </ItemGroup>
            </Project>
            """,
            "project.csproj");

        var marker = MSBuildProjectHelper.CreateMarker(doc);

        Assert.NotNull(marker);
        Assert.Equal(2, marker.TargetFrameworks.Count);
        foreach (var tfm in marker.TargetFrameworks)
        {
            var pkgRef = Assert.Single(tfm.PackageReferences);
            Assert.Equal("Newtonsoft.Json", pkgRef.Include);
            Assert.Equal("13.0.3", pkgRef.RequestedVersion);
        }
    }
}
