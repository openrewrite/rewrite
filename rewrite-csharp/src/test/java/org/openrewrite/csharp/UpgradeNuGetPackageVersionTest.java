/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.csharp;

import org.junit.jupiter.api.Test;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.openrewrite.csharp.Assertions.csproj;

class UpgradeNuGetPackageVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeNuGetPackageVersion("Newtonsoft.Json", "14.0.1", null));
    }

    @Test
    void upgradeLiteralVersion() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void upgradePropertyVersion() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <PropertyGroup>
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

    @Test
    void noChangeWhenAlreadyAtVersion() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void upgradeVersionRange() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="[13.0.0,14.0.0)" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void upgradeWithGlobPattern() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeNuGetPackageVersion("Newtonsoft.*", "14.0.1", null)),
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  <PackageReference Include="Newtonsoft.Json.Bson" Version="1.0.2" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                  <PackageReference Include="Newtonsoft.Json.Bson" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void upgradeAcrossMultipleProjects() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("ProjectA/ProjectA.csproj")
          ),
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="12.0.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("ProjectB/ProjectB.csproj")
          )
        );
    }

    @Test
    void upgradeVersionAsChildElement() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json">
                    <Version>13.0.3</Version>
                  </PackageReference>
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
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

    @Test
    void noChangeWhenChildElementAlreadyAtVersion() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
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

    @Test
    void upgradeCentralPackageManagement() {
        rewriteRun(
          csproj(
            """
              <Project>
                <ItemGroup>
                  <PackageVersion Include="Newtonsoft.Json" Version="13.0.3" />
                </ItemGroup>
              </Project>
              """,
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

    @Test
    void upgradeConditionalPackageReference() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup Condition="'$(TargetFramework)' == 'net8.0'">
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup Condition="'$(TargetFramework)' == 'net8.0'">
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void upgradeFloatingVersion() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="13.*" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void upgradePreservesOtherAttributes() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" PrivateAssets="all" ExcludeAssets="runtime" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="14.0.1" PrivateAssets="all" ExcludeAssets="runtime" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void upgradeCaseInsensitivePackageName() {
        // matchesGlob uses case-insensitive comparison, so lowercase package name matches
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="newtonsoft.json" Version="13.0.3" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="newtonsoft.json" Version="14.0.1" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void dotStarGlobMatchesSuffixedPackagesOnly() {
        // Pattern "Microsoft.EntityFrameworkCore.*" should match suffixed packages
        // but NOT the base package "Microsoft.EntityFrameworkCore" itself
        rewriteRun(
          spec -> spec.recipe(new UpgradeNuGetPackageVersion("Microsoft.EntityFrameworkCore.*", "10.0.0", null)),
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.0" />
                  <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="8.0.0" />
                  <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="8.0.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
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

    @Test
    void starGlobMatchesBaseAndSuffixedPackages() {
        // Pattern "Microsoft.EntityFrameworkCore*" (no dot before star) should match
        // BOTH the base package and suffixed packages
        rewriteRun(
          spec -> spec.recipe(new UpgradeNuGetPackageVersion("Microsoft.EntityFrameworkCore*", "10.0.0", null)),
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.0" />
                  <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="8.0.0" />
                  <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="8.0.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
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

    @Test
    void starGlobDoesNotMatchUnrelatedPackages() {
        // "Microsoft.EntityFrameworkCore*" should NOT match "Microsoft.Extensions.Logging"
        rewriteRun(
          spec -> spec.recipe(new UpgradeNuGetPackageVersion("Microsoft.EntityFrameworkCore*", "10.0.0", null)),
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Microsoft.EntityFrameworkCore" Version="8.0.0" />
                  <PackageReference Include="Microsoft.Extensions.Logging" Version="8.0.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Microsoft.EntityFrameworkCore" Version="10.0.0" />
                  <PackageReference Include="Microsoft.Extensions.Logging" Version="8.0.0" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void upgradePropertyInDirectoryBuildProps() {
        // Build a custom MSBuildProject marker for the .csproj that knows
        // the NewtonsoftVersion property is defined in Directory.Build.props
        Map<String, MSBuildProject.PropertyValue> properties = new LinkedHashMap<>();
        properties.put("NewtonsoftVersion", new MSBuildProject.PropertyValue("13.0.3", Paths.get("Directory.Build.props")));
        MSBuildProject marker = MSBuildProject.builder()
                .sdk("Microsoft.NET.Sdk")
                .properties(properties)
                .targetFrameworks(Collections.singletonList(
                        MSBuildProject.TargetFramework.builder()
                                .targetFramework("net8.0")
                                .packageReferences(Collections.singletonList(
                                        new MSBuildProject.PackageReference("Newtonsoft.Json", "$(NewtonsoftVersion)", "13.0.3")
                                ))
                                .build()
                ))
                .build();

        rewriteRun(
          // .csproj references the property — should NOT change
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="$(NewtonsoftVersion)" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.mapBeforeRecipe(doc -> doc.withMarkers(doc.getMarkers().setByType(marker)))
          ),
          // Directory.Build.props defines the property — should be updated
          csproj(
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
            spec -> spec.path("Directory.Build.props")
          )
        );
    }
}
