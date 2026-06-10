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
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Integration tests for parsing .csproj files via the Java RPC bridge.
/// Exercises the Parse handler, CsprojParser, and MSBuildProject marker
/// round-tripping through the RPC layer.
/// </summary>
[Collection("RPC")]
public class CsprojParseTests : RpcRewriteTest
{
    public CsprojParseTests(RpcFixture fixture) : base(fixture) { }

    [Fact]
    public void ParseSimpleCsproj()
    {
        RewriteRun(
            CsProj("""
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """)
        );
    }

    [Fact]
    public void ParseCsprojWithPackageReferences()
    {
        RewriteRun(
            CsProj("""
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                    <PackageReference Include="Serilog" Version="3.1.1" />
                  </ItemGroup>
                </Project>
                """)
        );
    }

    [Fact]
    public void ParseCsprojWithMultipleTargetFrameworks()
    {
        RewriteRun(
            CsProj("""
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFrameworks>net8.0;net9.0</TargetFrameworks>
                  </PropertyGroup>
                </Project>
                """)
        );
    }

    [Fact]
    public void ParseCsprojWithProjectReferences()
    {
        RewriteRun(
            CsProj("""
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <ProjectReference Include="..\Shared\Shared.csproj" />
                  </ItemGroup>
                </Project>
                """)
        );
    }

    [Fact]
    public void ParseCsprojAlongsideCSharpSource()
    {
        RewriteRun(
            CsProj("""
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """),
            CSharp("""
                namespace MyApp;

                public class Program
                {
                    public static void Main(string[] args)
                    {
                    }
                }
                """)
        );
    }

    [Fact]
    public void CsprojHasMSBuildProjectMarker()
    {
        var rpc = RewriteRpcServer.Current!;
        var tree = rpc.ParseOnRemote("project.csproj", """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net8.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
              </ItemGroup>
            </Project>
            """, "org.openrewrite.xml.tree.Xml$Document");

        var msbuild = tree.Markers.MarkerList.OfType<MSBuildProject>().FirstOrDefault();
        Assert.NotNull(msbuild);
        Assert.Equal("Microsoft.NET.Sdk", msbuild.Sdk);
        Assert.NotEmpty(msbuild.TargetFrameworks);
        Assert.Equal("net8.0", msbuild.TargetFrameworks[0].TargetFrameworkMoniker);

        // Verify package references
        var packageRefs = msbuild.TargetFrameworks[0].PackageReferences;
        Assert.Contains(packageRefs, pr => pr.Include == "Newtonsoft.Json");
    }
}
