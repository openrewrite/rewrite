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
package org.openrewrite.csharp.msbuild;

import org.junit.jupiter.api.Test;
import org.openrewrite.csharp.table.PathCasingMismatches;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;

class AlignPathCasingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AlignPathCasing());
    }

    @Test
    void alignsProjectReferenceDirectoryCasing() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="..\\..\\assemblies\\WPFToolkit\\WPFToolkit.csproj" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="..\\..\\assemblies\\Wpftoolkit\\WPFToolkit.csproj" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("src/App/App.csproj")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("assemblies/Wpftoolkit/WPFToolkit.csproj")
          )
        );
    }

    @Test
    void alignsFileNameCasing() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="../lib/MSDesktop.ConfigurationManager.LightWeight.csproj" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="../lib/MsDesktop.ConfigurationManager.LightWeight.csproj" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("src/App.csproj")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("lib/MsDesktop.ConfigurationManager.LightWeight.csproj")
          )
        );
    }

    @Test
    void alignsSolutionProjectEntry() {
        rewriteRun(
          text(
            """
              Microsoft Visual Studio Solution File, Format Version 12.00
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "WPFToolkit", "assemblies\\WPFToolkit\\WPFToolkit.csproj", "{2C1E1B0F-6A5D-4E97-9C4C-1B3E0F1D2A11}"
              EndProject
              Global
              EndGlobal
              """,
            """
              Microsoft Visual Studio Solution File, Format Version 12.00
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "WPFToolkit", "assemblies\\Wpftoolkit\\WPFToolkit.csproj", "{2C1E1B0F-6A5D-4E97-9C4C-1B3E0F1D2A11}"
              EndProject
              Global
              EndGlobal
              """,
            spec -> spec.path("Everything.sln")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("assemblies/Wpftoolkit/WPFToolkit.csproj")
          )
        );
    }

    @Test
    void alignsSolutionItems() {
        rewriteRun(
          text(
            """
              Project("{2150E333-8FDC-42A3-9474-1A3956D46DE8}") = "Solution Items", "Solution Items", "{A1}"
              \tProjectSection(SolutionItems) = preProject
              \t\tbuild\\Common.props = build\\Common.props
              \tEndProjectSection
              EndProject
              """,
            """
              Project("{2150E333-8FDC-42A3-9474-1A3956D46DE8}") = "Solution Items", "Solution Items", "{A1}"
              \tProjectSection(SolutionItems) = preProject
              \t\tBuild\\common.props = Build\\common.props
              \tEndProjectSection
              EndProject
              """,
            spec -> spec.path("Everything.sln")
          ),
          xml(
            """
              <Project />
              """,
            spec -> spec.path("Build/common.props")
          )
        );
    }

    @Test
    void alignsSlnxProjectPath() {
        rewriteRun(
          xml(
            """
              <Solution>
                <Project Path="src/APP/App.csproj" />
              </Solution>
              """,
            """
              <Solution>
                <Project Path="src/app/App.csproj" />
              </Solution>
              """,
            spec -> spec.path("Everything.slnx")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("src/app/App.csproj")
          )
        );
    }

    @Test
    void alignsHintPath() {
        rewriteRun(
          xml(
            """
              <Project>
                <ItemGroup>
                  <Reference Include="Widgets">
                    <HintPath>..\\LIB\\Widgets.dll</HintPath>
                  </Reference>
                </ItemGroup>
              </Project>
              """,
            """
              <Project>
                <ItemGroup>
                  <Reference Include="Widgets">
                    <HintPath>..\\lib\\Widgets.dll</HintPath>
                  </Reference>
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("src/App.csproj")
          ),
          text(
            "assembly",
            spec -> spec.path("lib/Widgets.dll")
          )
        );
    }

    @Test
    void alignsOnlyTheLiteralPrefixOfAGlob() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <Content Include="VIEWS\\**\\*.cshtml" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <Content Include="Views\\**\\*.cshtml" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("App.csproj")
          ),
          text(
            "<div/>",
            spec -> spec.path("Views/Shared/_Layout.cshtml")
          )
        );
    }

    @Test
    void alignsEachEntryOfASemicolonSeparatedList() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <Compile Include="SRC\\One.cs; SRC\\Two.cs" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <Compile Include="src\\One.cs; src\\Two.cs" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("App.csproj")
          ),
          text("class One;", spec -> spec.path("src/One.cs")),
          text("class Two;", spec -> spec.path("src/Two.cs"))
        );
    }

    @Test
    void leavesExactMatchesAlone() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="..\\lib\\Lib.csproj" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("src/App.csproj")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("lib/Lib.csproj")
          )
        );
    }

    @Test
    void leavesUnresolvablePathsAlone() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="..\\missing\\Missing.csproj" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("src/App.csproj")
          )
        );
    }

    @Test
    void leavesAmbiguousCasingAlone() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="..\\LIB\\Lib.csproj" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("src/App.csproj")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("lib/Lib.csproj")
          ),
          text("readme", spec -> spec.path("Lib/readme.txt"))
        );
    }

    @Test
    void leavesAssemblyAndPackageIdentitiesAlone() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <Reference Include="MSDesktop.Core" />
                  <PackageReference Include="NEWTONSOFT.Json" Version="13.0.3" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("App.csproj")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("MsDesktop.Core/MsDesktop.Core.csproj")
          ),
          text("readme", spec -> spec.path("Newtonsoft.json/readme.txt"))
        );
    }

    @Test
    void leavesMsBuildPropertiesAlone() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="$(RepoRoot)\\LIB\\Lib.csproj" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("src/App.csproj")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("lib/Lib.csproj")
          )
        );
    }

    @Test
    void leavesPathsOutsideTheRepositoryAlone() {
        rewriteRun(
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <Import Project="..\\..\\Shared\\Common.props" />
              </Project>
              """,
            spec -> spec.path("src/App.csproj")
          )
        );
    }

    @Test
    void recordsMismatchesInADataTable() {
        rewriteRun(
          spec -> spec.dataTable(PathCasingMismatches.Row.class, rows -> {
              assertThat(rows).hasSize(1);
              PathCasingMismatches.Row row = rows.get(0);
              assertThat(row.getSourcePath()).isEqualTo("src/App.csproj");
              assertThat(row.getLocation()).isEqualTo("ProjectReference/@Include");
              assertThat(row.getReference()).isEqualTo("..\\LIB\\Lib.csproj");
              assertThat(row.getAlignedReference()).isEqualTo("..\\lib\\Lib.csproj");
          }),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="..\\LIB\\Lib.csproj" />
                </ItemGroup>
              </Project>
              """,
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <ItemGroup>
                  <ProjectReference Include="..\\lib\\Lib.csproj" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.path("src/App.csproj")
          ),
          xml(
            """
              <Project Sdk="Microsoft.NET.Sdk" />
              """,
            spec -> spec.path("lib/Lib.csproj")
          )
        );
    }
}
