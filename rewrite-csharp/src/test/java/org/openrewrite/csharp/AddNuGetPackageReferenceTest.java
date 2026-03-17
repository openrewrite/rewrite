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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.csharp.Assertions.csproj;

class AddNuGetPackageReferenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddNuGetPackageReference("Serilog", "3.0.0"));
    }

    @Test
    void addToExistingItemGroup() {
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
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  <PackageReference Include="Serilog" Version="3.0.0" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyPresent() {
        rewriteRun(
          csproj(
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

    @Test
    void addToProjectWithNoItemGroup() {
        rewriteRun(
          csproj(
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

    @Test
    void addWithoutVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddNuGetPackageReference("Serilog", null)),
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
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  <PackageReference Include="Serilog" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void addToMultipleProjects() {
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
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  <PackageReference Include="Serilog" Version="3.0.0" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("ProjectA/ProjectA.csproj")
          ),
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="FluentAssertions" Version="6.0.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <PackageReference Include="FluentAssertions" Version="6.0.0" />
                  <PackageReference Include="Serilog" Version="3.0.0" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("ProjectB/ProjectB.csproj")
          )
        );
    }

    @Test
    void addToConditionalItemGroup() {
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
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                  <PackageReference Include="Serilog" Version="3.0.0" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }
}
