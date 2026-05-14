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

public class RemoveMSBuildPropertyTests : RewriteTest
{
    [Fact]
    public void RemovesProperty()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveMSBuildProperty
            {
                PropertyName = "RuntimeFrameworkVersion"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                    <RuntimeFrameworkVersion>2.1.1</RuntimeFrameworkVersion>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenPropertyAbsent()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveMSBuildProperty
            {
                PropertyName = "RuntimeFrameworkVersion"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void OnlyMatchesInsidePropertyGroup()
    {
        // A child element named identically inside an ItemGroup or elsewhere should not be touched.
        RewriteRun(
            spec => spec.SetRecipe(new RemoveMSBuildProperty
            {
                PropertyName = "RuntimeFrameworkVersion"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <RuntimeFrameworkVersion Include="ignored" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }
}
