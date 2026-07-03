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
package org.openrewrite.csharp.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.csharp.Assertions.csproj;
import static org.openrewrite.xml.Assertions.xml;

class NugetDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new NugetDependency.Matcher().asVisitor((dep, ctx) ->
          SearchResult.found(dep.getTree(), dep.getPackageId() + ":" + String.join(",", dep.getVersions())))));
    }

    @Test
    void matchesPackageReferenceWithDeclaredVersion() {
        rewriteRun(
          //language=xml
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                      <TargetFramework>net6.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                      <PackageReference Include="bootstrap" Version="4.6.2" />
                  </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                      <TargetFramework>net6.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                      <!--~~(bootstrap:4.6.2)~~>--><PackageReference Include="bootstrap" Version="4.6.2" />
                  </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void conditionalTagsMatchTheirOwnDeclaredVersion() {
        rewriteRun(
          //language=xml
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                      <TargetFrameworks>net472;net6.0</TargetFrameworks>
                  </PropertyGroup>
                  <ItemGroup>
                      <PackageReference Include="bootstrap" Version="4.6.2" Condition="'$(TargetFramework)'=='net472'" />
                      <PackageReference Include="bootstrap" Version="5.3.3" Condition="'$(TargetFramework)'=='net6.0'" />
                  </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                      <TargetFrameworks>net472;net6.0</TargetFrameworks>
                  </PropertyGroup>
                  <ItemGroup>
                      <!--~~(bootstrap:4.6.2)~~>--><PackageReference Include="bootstrap" Version="4.6.2" Condition="'$(TargetFramework)'=='net472'" />
                      <!--~~(bootstrap:5.3.3)~~>--><PackageReference Include="bootstrap" Version="5.3.3" Condition="'$(TargetFramework)'=='net6.0'" />
                  </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void versionsFromMarkerWhenTagHasNoVersionAttribute() {
        MSBuildProject marker = MSBuildProject.builder()
          .sdk("Microsoft.NET.Sdk")
          .targetFrameworks(List.of(MSBuildProject.TargetFramework.builder()
            .targetFramework("net6.0")
            .packageReferences(List.of(MSBuildProject.PackageReference.builder()
              .include("bootstrap").requestedVersion("4.6.2").resolvedVersion("4.6.2").build()))
            .build()))
          .build();
        rewriteRun(
          //language=xml
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                      <PackageReference Include="bootstrap" />
                  </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                      <!--~~(bootstrap:4.6.2)~~>--><PackageReference Include="bootstrap" />
                  </ItemGroup>
              </Project>
              """,
            spec -> spec.path("demo.csproj").markers(marker)
          )
        );
    }

    @Test
    void doesNotMatchOutsideProjectItemGroup() {
        rewriteRun(
          //language=xml
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                      <TargetFramework>net6.0</TargetFramework>
                  </PropertyGroup>
                  <Target Name="AddBuildTimePackages">
                      <ItemGroup>
                          <PackageReference Include="bootstrap" Version="4.6.2" />
                      </ItemGroup>
                  </Target>
              </Project>
              """
          )
        );
    }

    @Test
    void doesNotMatchWithoutMSBuildProjectMarker() {
        rewriteRun(
          //language=xml
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                  <ItemGroup>
                      <PackageReference Include="bootstrap" Version="4.6.2" />
                  </ItemGroup>
              </Project>
              """,
            spec -> spec.path("demo.csproj")
          )
        );
    }
}
