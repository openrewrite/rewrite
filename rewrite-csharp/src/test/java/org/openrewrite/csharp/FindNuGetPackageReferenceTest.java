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

class FindNuGetPackageReferenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindNuGetPackageReference("Swashbuckle.AspNetCore"));
    }

    @Test
    void findsPackageWhenPresent() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk.Web">
                <ItemGroup>
                  <PackageReference Include="Swashbuckle.AspNetCore" Version="6.4.0" />
                  <PackageReference Include="Microsoft.AspNetCore.OpenApi" Version="7.0.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <!--~~>--><Project Sdk="Microsoft.NET.Sdk.Web">
                <ItemGroup>
                  <PackageReference Include="Swashbuckle.AspNetCore" Version="6.4.0" />
                  <PackageReference Include="Microsoft.AspNetCore.OpenApi" Version="7.0.0" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void noMatchWhenPackageAbsent() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk.Worker">
                <ItemGroup>
                  <PackageReference Include="Microsoft.Extensions.Hosting" Version="9.0.0" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }

    @Test
    void supportsGlobPattern() {
        rewriteRun(
          spec -> spec.recipe(new FindNuGetPackageReference("Swashbuckle.*")),
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk.Web">
                <ItemGroup>
                  <PackageReference Include="Swashbuckle.AspNetCore.SwaggerGen" Version="6.4.0" />
                </ItemGroup>
              </Project>
              """,
            """
              <!--~~>--><Project Sdk="Microsoft.NET.Sdk.Web">
                <ItemGroup>
                  <PackageReference Include="Swashbuckle.AspNetCore.SwaggerGen" Version="6.4.0" />
                </ItemGroup>
              </Project>
              """
          )
        );
    }
}
