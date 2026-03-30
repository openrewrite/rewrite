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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.csharp.Assertions.csproj;

/**
 * Tests the Swashbuckle → OpenAPI migration pattern: FindNuGetPackageReference as a
 * precondition gates AddNuGetPackageReference, while RemoveNuGetPackageReference runs
 * unconditionally. This mirrors the C# MigrateSwashbuckleToOpenApi composite recipe.
 */
class MigrateSwashbuckleToOpenApiTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // Mirrors the C# MigrateSwashbuckleToOpenApi composite recipe's csproj-level behavior:
        // - Preconditioned add: only adds OpenApi if Swashbuckle is present
        // - Unconditional removals: always remove Swashbuckle packages
        spec.recipes(
                new PreconditionedAddOpenApi(),
                new RemoveNuGetPackageReference("Swashbuckle.AspNetCore"),
                new RemoveNuGetPackageReference("Swashbuckle.AspNetCore.SwaggerGen"),
                new RemoveNuGetPackageReference("Swashbuckle.AspNetCore.SwaggerUI"),
                new RemoveNuGetPackageReference("Swashbuckle.AspNetCore.Swagger")
        );
    }

    /**
     * Recipe that adds Microsoft.AspNetCore.OpenApi only if Swashbuckle.AspNetCore is present.
     */
    static class PreconditionedAddOpenApi extends Recipe {
        @Override
        public String getDisplayName() {
            return "Add OpenApi (preconditioned on Swashbuckle)";
        }

        @Override
        public String getDescription() {
            return "Adds Microsoft.AspNetCore.OpenApi only to projects that reference Swashbuckle.AspNetCore.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(
                    new FindNuGetPackageReference("Swashbuckle.AspNetCore"),
                    new AddNuGetPackageReference("Microsoft.AspNetCore.OpenApi", "9.0.0").getVisitor()
            );
        }
    }

    @Test
    void addsOpenApiAndRemovesSwashbuckleWhenPresent() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk.Web">
                <PropertyGroup>
                  <TargetFramework>net9.0</TargetFramework>
                </PropertyGroup>
                <ItemGroup>
                  <PackageReference Include="Swashbuckle.AspNetCore" Version="6.4.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk.Web">
                <PropertyGroup>
                  <TargetFramework>net9.0</TargetFramework>
                </PropertyGroup>
                <ItemGroup>
                  <PackageReference Include="Microsoft.AspNetCore.OpenApi" Version="9.0.0" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void noChangesWhenSwashbuckleNotPresent() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk.Worker">
                <PropertyGroup>
                  <TargetFramework>net9.0</TargetFramework>
                </PropertyGroup>
                <ItemGroup>
                  <PackageReference Include="Microsoft.Extensions.Hosting" Version="9.0.0" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void onlyAffectsProjectWithSwashbuckleInMultiProjectSolution() {
        rewriteRun(
          // Web project WITH Swashbuckle — should be migrated
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk.Web">
                <ItemGroup>
                  <PackageReference Include="Swashbuckle.AspNetCore" Version="6.4.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk.Web">
                <ItemGroup>
                  <PackageReference Include="Microsoft.AspNetCore.OpenApi" Version="9.0.0" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("WebApi/WebApi.csproj")
          ),
          // Worker project WITHOUT Swashbuckle — should NOT be affected
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk.Worker">
                <ItemGroup>
                  <PackageReference Include="Microsoft.Extensions.Hosting" Version="9.0.0" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("Worker/Worker.csproj")
          )
        );
    }
}
