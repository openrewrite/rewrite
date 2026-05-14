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

public class AddFrameworkReferenceTests : RewriteTest
{
    [Fact]
    public void AddsWhenTriggerMatches()
    {
        RewriteRun(
            spec => spec.SetRecipe(new AddFrameworkReference
            {
                FrameworkName = "Microsoft.AspNetCore.App",
                TriggerPackageGlob = "Microsoft.AspNetCore.*"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.AspNetCore.Mvc" Version="1.1.3" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.AspNetCore.Mvc" Version="1.1.3" />
                  </ItemGroup>
                  <ItemGroup>
                    <FrameworkReference Include="Microsoft.AspNetCore.App" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenSdkImplicitlyImports()
    {
        // Microsoft.NET.Sdk.Web already imports Microsoft.AspNetCore.App
        RewriteRun(
            spec => spec.SetRecipe(new AddFrameworkReference
            {
                FrameworkName = "Microsoft.AspNetCore.App"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk.Web">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenAlreadyPresent()
    {
        RewriteRun(
            spec => spec.SetRecipe(new AddFrameworkReference
            {
                FrameworkName = "Microsoft.AspNetCore.App"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <FrameworkReference Include="Microsoft.AspNetCore.App" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenTriggerNotMatched()
    {
        RewriteRun(
            spec => spec.SetRecipe(new AddFrameworkReference
            {
                FrameworkName = "Microsoft.AspNetCore.App",
                TriggerPackageGlob = "Microsoft.AspNetCore.*"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }
}
