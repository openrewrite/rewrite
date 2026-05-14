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

public class FindNuGetPackageReferenceTests : RewriteTest
{
    [Fact]
    public void FindsPackageWhenPresent()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindNuGetPackageReference
            {
                PackageName = "Swashbuckle.AspNetCore"
            }),
            CsProj(
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
                """
                <!--~~>--><Project Sdk="Microsoft.NET.Sdk.Web">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Swashbuckle.AspNetCore" Version="6.4.0" />
                    <PackageReference Include="Microsoft.AspNetCore.OpenApi" Version="7.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void FindNoMatchWhenPackageAbsent()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindNuGetPackageReference
            {
                PackageName = "Swashbuckle.AspNetCore"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk.Worker">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.Extensions.Hosting" Version="9.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void FindSupportsGlobPattern()
    {
        RewriteRun(
            spec => spec.SetRecipe(new FindNuGetPackageReference
            {
                PackageName = "Swashbuckle.*"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk.Web">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Swashbuckle.AspNetCore.SwaggerGen" Version="6.4.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <!--~~>--><Project Sdk="Microsoft.NET.Sdk.Web">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Swashbuckle.AspNetCore.SwaggerGen" Version="6.4.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }
}
