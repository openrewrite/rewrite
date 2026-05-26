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
using OpenRewrite.Xml;

namespace OpenRewrite.Tests.Xml;

public class CsprojParserTests
{
    [Fact]
    public void CsprojParserRunsRestoreAndAttachesFullMarker()
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
        // Without rootDir, CsprojParser runs dotnet restore in a temp dir to generate
        // project.assets.json, so the marker has full metadata
        Assert.Single(marker.TargetFrameworks);
        Assert.Equal("net8.0", marker.TargetFrameworks[0].TargetFrameworkMoniker);
        Assert.Single(marker.TargetFrameworks[0].PackageReferences);
        var pkgRef = marker.TargetFrameworks[0].PackageReferences[0];
        Assert.Equal("Newtonsoft.Json", pkgRef.Include);
        Assert.Equal("13.0.3", pkgRef.RequestedVersion);
        Assert.Equal("13.0.3", pkgRef.ResolvedVersion);
    }

    [Fact]
    public void MarkerCreatedFromAssetsJson()
    {
        // Set up a temp directory with a hand-crafted project.assets.json
        // to test MSBuildProjectHelper.CreateMarker directly
        var tempDir = Path.Combine(Path.GetTempPath(), "openrewrite-test-" + Guid.NewGuid().ToString("N")[..8]);
        var projectDir = Path.Combine(tempDir, "src");
        var objDir = Path.Combine(projectDir, "obj");
        Directory.CreateDirectory(objDir);

        try
        {
            var assetsJson = """
                {
                  "version": 3,
                  "targets": {
                    "net8.0": {
                      "Newtonsoft.Json/13.0.3": {
                        "type": "package",
                        "compile": {},
                        "runtime": {}
                      }
                    }
                  },
                  "libraries": {
                    "Newtonsoft.Json/13.0.3": {
                      "type": "package",
                      "path": "newtonsoft.json/13.0.3"
                    }
                  },
                  "projectFileDependencyGroups": {
                    "net8.0": ["Newtonsoft.Json >= 13.0.3"]
                  },
                  "packageFolders": {},
                  "project": {
                    "version": "1.0.0",
                    "restore": {
                      "projectName": "TestProject",
                      "originalTargetFrameworks": ["net8.0"],
                      "sources": {},
                      "frameworks": {
                        "net8.0": { "targetAlias": "net8.0" }
                      }
                    },
                    "frameworks": {
                      "net8.0": {
                        "targetAlias": "net8.0",
                        "dependencies": {
                          "Newtonsoft.Json": {
                            "target": "Package",
                            "version": "[13.0.3, )"
                          }
                        }
                      }
                    }
                  }
                }
                """;
            File.WriteAllText(Path.Combine(objDir, "project.assets.json"), assetsJson);

            var xmlParser = new XmlParser();
            var doc = xmlParser.Parse(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  </ItemGroup>
                </Project>
                """,
                sourcePath: "src/TestProject.csproj");

            var marker = MSBuildProjectHelper.CreateMarker(doc, tempDir);
            Assert.NotNull(marker);
            Assert.Equal("Microsoft.NET.Sdk", marker.Sdk);
            Assert.Single(marker.TargetFrameworks);
            Assert.Equal("net8.0", marker.TargetFrameworks[0].TargetFrameworkMoniker);

            // Package reference with correct requested and resolved versions
            Assert.Single(marker.TargetFrameworks[0].PackageReferences);
            var pkgRef = marker.TargetFrameworks[0].PackageReferences[0];
            Assert.Equal("Newtonsoft.Json", pkgRef.Include);
            Assert.Equal("13.0.3", pkgRef.RequestedVersion);
            Assert.Equal("13.0.3", pkgRef.ResolvedVersion);

            // Resolved packages include the package
            Assert.Single(marker.TargetFrameworks[0].ResolvedPackages);
            Assert.Equal("Newtonsoft.Json", marker.TargetFrameworks[0].ResolvedPackages[0].Name);
            Assert.Equal("13.0.3", marker.TargetFrameworks[0].ResolvedPackages[0].ResolvedVersion);
        }
        finally
        {
            Directory.Delete(tempDir, true);
        }
    }
}
