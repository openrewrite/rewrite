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

class ChangeDotNetTargetFrameworkTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeDotNetTargetFramework("net8.0", "net9.0"));
    }

    @Test
    void changeSingleTargetFramework() {
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
                  <TargetFramework>net9.0</TargetFramework>
                </PropertyGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void changeMultiTargetFrameworks() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <PropertyGroup>
                  <TargetFrameworks>net8.0;netstandard2.0</TargetFrameworks>
                </PropertyGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <PropertyGroup>
                  <TargetFrameworks>net9.0;netstandard2.0</TargetFrameworks>
                </PropertyGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void noChangeWhenDifferentTfm() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <PropertyGroup>
                  <TargetFramework>net7.0</TargetFramework>
                </PropertyGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void changeMultiTfmOnlyMatchingFramework() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDotNetTargetFramework("net8.0", "net10.0")),
          csproj(
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

    @Test
    void changeAcrossMultipleProjects() {
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
                  <TargetFramework>net9.0</TargetFramework>
                </PropertyGroup>
              </Project>
              """,
            spec -> spec.path("ProjectA/ProjectA.csproj")
          ),
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
                  <TargetFramework>net9.0</TargetFramework>
                </PropertyGroup>
              </Project>
              """,
            spec -> spec.path("ProjectB/ProjectB.csproj")
          )
        );
    }

    @Test
    void noChangeWhenTfmNotPresent() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk"></Project>
              """
          )
        );
    }
}
