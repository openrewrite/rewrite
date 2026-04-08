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

public class RemoveNuGetPackageReferenceTests : RewriteTest
{
    [Fact]
    public void RemovePackageReference()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveNuGetPackageReference
            {
                PackageName = "Newtonsoft.Json"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void RemovePackageNoChangeWhenNotPresent()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveNuGetPackageReference
            {
                PackageName = "Newtonsoft.Json"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void RemovePackageWithGlob()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveNuGetPackageReference
            {
                PackageName = "Newtonsoft.*"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void RemoveConditionalPackageReference()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveNuGetPackageReference
            {
                PackageName = "Newtonsoft.Json"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup Condition="'$(TargetFramework)' == 'net8.0'">
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup Condition="'$(TargetFramework)' == 'net8.0'">
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void RemovePackageReferenceWithChildElements()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveNuGetPackageReference
            {
                PackageName = "Newtonsoft.Json"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json">
                      <Version>13.0.3</Version>
                    </PackageReference>
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void RemoveFromMultipleProjects()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveNuGetPackageReference
            {
                PackageName = "Newtonsoft.Json"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                    <PackageReference Include="Serilog" Version="3.0.0" />
                  </ItemGroup>
                </Project>
                """,
                sourcePath: "ProjectA/ProjectA.csproj"
            ),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                    <PackageReference Include="AutoMapper" Version="12.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                    <PackageReference Include="AutoMapper" Version="12.0.0" />
                  </ItemGroup>
                </Project>
                """,
                sourcePath: "ProjectB/ProjectB.csproj"
            )
        );
    }
}
