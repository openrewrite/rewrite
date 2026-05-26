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
using OpenRewrite.CSharp.Recipes;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp.Recipes;

public class ChangeDotNetFrameworkVersionTests(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    private static SourceSpec AppConfig(string before, string? after = null, string sourcePath = "app.config") =>
        new(before, after, sourcePath, "org.openrewrite.xml.tree.Xml$Document");

    [Fact]
    public void ChangeLegacyCsprojTargetFrameworkVersion()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetFrameworkVersion
            {
                OldVersion = "v4.7.2",
                NewVersion = "v4.8"
            }),
            CsProj(
                """
                <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
                  <PropertyGroup>
                    <TargetFrameworkVersion>v4.7.2</TargetFrameworkVersion>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
                  <PropertyGroup>
                    <TargetFrameworkVersion>v4.8</TargetFrameworkVersion>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void ChangeAppConfigSupportedRuntimeSku()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetFrameworkVersion
            {
                OldVersion = "v4.7.2",
                NewVersion = "v4.8"
            }),
            AppConfig(
                """
                <configuration>
                  <startup>
                    <supportedRuntime version="v4.0" sku=".NETFramework,Version=v4.7.2" />
                  </startup>
                </configuration>
                """,
                """
                <configuration>
                  <startup>
                    <supportedRuntime version="v4.0" sku=".NETFramework,Version=v4.8" />
                  </startup>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void AcceptsVersionWithoutVPrefix()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetFrameworkVersion
            {
                OldVersion = "4.7.2",
                NewVersion = "4.8"
            }),
            CsProj(
                """
                <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
                  <PropertyGroup>
                    <TargetFrameworkVersion>v4.7.2</TargetFrameworkVersion>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
                  <PropertyGroup>
                    <TargetFrameworkVersion>v4.8</TargetFrameworkVersion>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void ChangesBothCsprojAndAppConfigInSingleRun()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetFrameworkVersion
            {
                OldVersion = "v4.6.2",
                NewVersion = "v4.7.2"
            }),
            CsProj(
                """
                <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
                  <PropertyGroup>
                    <TargetFrameworkVersion>v4.6.2</TargetFrameworkVersion>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
                  <PropertyGroup>
                    <TargetFrameworkVersion>v4.7.2</TargetFrameworkVersion>
                  </PropertyGroup>
                </Project>
                """
            ),
            AppConfig(
                """
                <configuration>
                  <startup>
                    <supportedRuntime version="v4.0" sku=".NETFramework,Version=v4.6.2" />
                  </startup>
                </configuration>
                """,
                """
                <configuration>
                  <startup>
                    <supportedRuntime version="v4.0" sku=".NETFramework,Version=v4.7.2" />
                  </startup>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenVersionDoesNotMatch()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetFrameworkVersion
            {
                OldVersion = "v4.6.2",
                NewVersion = "v4.8"
            }),
            CsProj(
                """
                <Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
                  <PropertyGroup>
                    <TargetFrameworkVersion>v4.7.2</TargetFrameworkVersion>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenSupportedRuntimeMissing()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetFrameworkVersion
            {
                OldVersion = "v4.7.2",
                NewVersion = "v4.8"
            }),
            AppConfig(
                """
                <configuration>
                  <appSettings>
                    <add key="foo" value="bar" />
                  </appSettings>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void IgnoresSdkStyleTargetFrameworkElement()
    {
        // SDK-style csproj uses <TargetFramework>net472</TargetFramework>.
        // This recipe deliberately does not touch it; ChangeDotNetTargetFramework owns that.
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetFrameworkVersion
            {
                OldVersion = "v4.7.2",
                NewVersion = "v4.8"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net472</TargetFramework>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void IgnoresUnrelatedSkuValuesInSupportedRuntime()
    {
        // Only sku entries with the .NETFramework,Version= prefix should match.
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetFrameworkVersion
            {
                OldVersion = "v4.7.2",
                NewVersion = "v4.8"
            }),
            AppConfig(
                """
                <configuration>
                  <startup>
                    <supportedRuntime version="v4.0" sku="Client" />
                  </startup>
                </configuration>
                """
            )
        );
    }
}
