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
using OpenRewrite.CSharp.Recipes;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp.Recipes;

public class ChangeDotNetTargetFrameworkTests : RewriteTest
{
    [Fact]
    public void ChangeSingleTargetFramework()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetTargetFramework
            {
                OldTargetFramework = "net8.0",
                NewTargetFramework = "net9.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net9.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void ChangeMultiTargetFramework()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetTargetFramework
            {
                OldTargetFramework = "net8.0",
                NewTargetFramework = "net9.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFrameworks>net7.0;net8.0</TargetFrameworks>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFrameworks>net7.0;net9.0</TargetFrameworks>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void ChangeTargetFrameworkNoMatch()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetTargetFramework
            {
                OldTargetFramework = "net6.0",
                NewTargetFramework = "net9.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void ChangeMultiTfmOnlyMatchingFramework()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetTargetFramework
            {
                OldTargetFramework = "net8.0",
                NewTargetFramework = "net10.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFrameworks>net8.0;net9.0</TargetFrameworks>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFrameworks>net10.0;net9.0</TargetFrameworks>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void ChangeAcrossMultipleProjects()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetTargetFramework
            {
                OldTargetFramework = "net8.0",
                NewTargetFramework = "net9.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net9.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """,
                sourcePath: "ProjectA/ProjectA.csproj"
            ),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net9.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """,
                sourcePath: "ProjectB/ProjectB.csproj"
            )
        );
    }

    [Fact]
    public void DeduplicatesMultiTfmAfterReplacement()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetTargetFramework
            {
                OldTargetFramework = "netstandard2.0",
                NewTargetFramework = "net6.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFrameworks>net40;netstandard2.0;net6.0</TargetFrameworks>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFrameworks>net40;net6.0</TargetFrameworks>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenTfmNotPresent()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeDotNetTargetFramework
            {
                OldTargetFramework = "net8.0",
                NewTargetFramework = "net9.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk"></Project>
                """
            )
        );
    }
}
