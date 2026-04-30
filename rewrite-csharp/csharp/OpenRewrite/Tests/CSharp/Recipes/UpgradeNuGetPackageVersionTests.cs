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

public class UpgradeNuGetPackageVersionTests : RewriteTest
{
    [Fact]
    public void UpgradePackageVersion()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
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
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradePropertyVersion()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <NewtonsoftVersion>13.0.3</NewtonsoftVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="$(NewtonsoftVersion)" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <NewtonsoftVersion>14.0.1</NewtonsoftVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="$(NewtonsoftVersion)" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradePackageVersionAlreadyCurrent()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeVersionRange()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="[13.0.0,14.0.0)" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeWithGlobPattern()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.*",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                    <PackageReference Include="Newtonsoft.Json.Bson" Version="1.0.2" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                    <PackageReference Include="Newtonsoft.Json.Bson" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeAcrossMultipleProjects()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
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
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
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
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="12.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """,
                sourcePath: "ProjectB/ProjectB.csproj"
            )
        );
    }

    [Fact]
    public void UpgradeVersionAsChildElement()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json">
                      <Version>13.0.3</Version>
                    </PackageReference>
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json">
                      <Version>14.0.1</Version>
                    </PackageReference>
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeNoChangeWhenChildElementAlreadyAtVersion()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json">
                      <Version>14.0.1</Version>
                    </PackageReference>
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeCentralPackageManagement()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project>
                  <ItemGroup>
                    <PackageVersion Include="Newtonsoft.Json" Version="13.0.3" />
                  </ItemGroup>
                </Project>
                """
                ,
                """
                <Project>
                  <ItemGroup>
                    <PackageVersion Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeConditionalPackageReference()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup Condition="'$(TargetFramework)' == 'net8.0'">
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup Condition="'$(TargetFramework)' == 'net8.0'">
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeFloatingVersion()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.*" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradePreservesOtherAttributes()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="13.0.3" PrivateAssets="all" ExcludeAssets="runtime" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" PrivateAssets="all" ExcludeAssets="runtime" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeCaseInsensitivePackageName()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="newtonsoft.json" Version="13.0.3" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="newtonsoft.json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void DotStarGlobMatchesSuffixedPackagesOnly()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Microsoft.EntityFrameworkCore.*",
                NewVersion = "10.0.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.0" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="8.0.0" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="8.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.0" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="10.0.0" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="10.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void StarGlobMatchesBaseAndSuffixedPackages()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Microsoft.EntityFrameworkCore*",
                NewVersion = "10.0.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.0" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="8.0.0" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="8.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="10.0.0" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="10.0.0" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="10.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void StarGlobDoesNotMatchUnrelatedPackages()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Microsoft.EntityFrameworkCore*",
                NewVersion = "10.0.0"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.0" />
                    <PackageReference Include="Microsoft.Extensions.Logging" Version="8.0.0" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="10.0.0" />
                    <PackageReference Include="Microsoft.Extensions.Logging" Version="8.0.0" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradePropertyInSameFile()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <NewtonsoftVersion>13.0.3</NewtonsoftVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="$(NewtonsoftVersion)" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <NewtonsoftVersion>14.0.1</NewtonsoftVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="$(NewtonsoftVersion)" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradePropertyInDirectoryBuildProps()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            // Directory.Build.props defines the property — should be updated
            CsProj(
                """
                <Project>
                  <PropertyGroup>
                    <NewtonsoftVersion>13.0.3</NewtonsoftVersion>
                  </PropertyGroup>
                </Project>
                """,
                """
                <Project>
                  <PropertyGroup>
                    <NewtonsoftVersion>14.0.1</NewtonsoftVersion>
                  </PropertyGroup>
                </Project>
                """,
                sourcePath: "Directory.Build.props"
            ),
            // .csproj references the property — should NOT change
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="$(NewtonsoftVersion)" />
                  </ItemGroup>
                </Project>
                """,
                sourcePath: "src/MyProject/MyProject.csproj"
            )
        );
    }

    [Fact]
    public void UpgradeCentralPackageManagementCrossFile()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            // Directory.Packages.props with CPM — version should be updated
            CsProj(
                """
                <Project>
                  <PropertyGroup>
                    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageVersion Include="Newtonsoft.Json" Version="13.0.3" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project>
                  <PropertyGroup>
                    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageVersion Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """,
                sourcePath: "Directory.Packages.props"
            ),
            // .csproj has no version (CPM manages it) — should NOT change
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" />
                  </ItemGroup>
                </Project>
                """,
                sourcePath: "src/MyProject/MyProject.csproj"
            )
        );
    }

    [Fact]
    public void DoNotUpgradePropertyDefinedInMultipleFiles()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            // Property defined in Directory.Build.props — should NOT change (ambiguous)
            CsProj(
                """
                <Project>
                  <PropertyGroup>
                    <NewtonsoftVersion>13.0.3</NewtonsoftVersion>
                  </PropertyGroup>
                </Project>
                """,
                sourcePath: "Directory.Build.props"
            ),
            // Same property also defined in .csproj — should NOT change (ambiguous)
            // But the PackageReference still uses the property, so no direct version change either
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <NewtonsoftVersion>13.0.3</NewtonsoftVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="$(NewtonsoftVersion)" />
                  </ItemGroup>
                </Project>
                """,
                sourcePath: "src/MyProject/MyProject.csproj"
            )
        );
    }

    [Fact]
    public void DoNotUpgradePropertyUsedByNonTargetedPackage()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            // Shared property used by both targeted and non-targeted packages — should NOT change
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <SharedVersion>13.0.3</SharedVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="$(SharedVersion)" />
                    <PackageReference Include="Some.Other.Package" Version="$(SharedVersion)" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void DoNotUpgradePropertyThatIsPropertyReference()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            // Property value is itself a property reference — should NOT change
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <BaseVersion>13.0.3</BaseVersion>
                    <NewtonsoftVersion>$(BaseVersion)</NewtonsoftVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="$(NewtonsoftVersion)" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradePropertyUsedByMultipleTargetedPackages()
    {
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Microsoft.EntityFrameworkCore*",
                NewVersion = "10.0.0"
            }),
            // Property used by multiple packages all matching the glob — should be updated
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <EfCoreVersion>8.0.0</EfCoreVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="$(EfCoreVersion)" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="$(EfCoreVersion)" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="$(EfCoreVersion)" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                    <EfCoreVersion>10.0.0</EfCoreVersion>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Microsoft.EntityFrameworkCore" Version="$(EfCoreVersion)" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="$(EfCoreVersion)" />
                    <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="$(EfCoreVersion)" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void UpgradeFloatingWildcardVersion()
    {
        // Source csproj uses NuGet's floating "8.0.*" syntax. The recipe should
        // still upgrade it to the exact target version.
        RewriteRun(
            spec => spec.SetRecipe(new UpgradeNuGetPackageVersion
            {
                PackageName = "Newtonsoft.Json",
                NewVersion = "14.0.1"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="8.0.*" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net8.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

}
