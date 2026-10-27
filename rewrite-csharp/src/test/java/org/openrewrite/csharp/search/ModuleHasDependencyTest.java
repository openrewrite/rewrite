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
package org.openrewrite.csharp.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.csharp.marker.DotNetProject;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;

class ModuleHasDependencyTest implements RewriteTest {

    private static final String CSPROJ = """
      <Project Sdk="Microsoft.NET.Sdk">
      </Project>
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModuleHasDependency("Newtonsoft.*", "13.x", null));
    }

    @DocumentExample
    @Test
    void marksAllSourcesOfProjectWithDependency() {
        rewriteRun(
          xml(
            CSPROJ,
            """
              <!--~~(Module has dependency: Newtonsoft.*:13.x)~~>--><Project Sdk="Microsoft.NET.Sdk">
              </Project>
              """,
            spec -> spec.path("src/App/App.csproj").markers(project(resolved("Newtonsoft.Json", "13.0.3", 0)))
          ),
          text(
            "class A {}",
            "~~(Module has dependency: Newtonsoft.*:13.x)~~>class A {}",
            spec -> spec.path("src/App/Services/A.cs").markers(dotNetProject("App"))
          ),
          xml(
            CSPROJ,
            spec -> spec.path("src/Other/Other.csproj").markers(project(resolved("Serilog", "3.1.1", 0)))
          ),
          text(
            "class B {}",
            spec -> spec.path("src/Other/B.cs").markers(dotNetProject("Other"))
          ),
          text(
            "class C {}",
            spec -> spec.path("tools/C.cs")
          )
        );
    }

    @Test
    void versionOutsideRangeDoesNotMatch() {
        rewriteRun(
          xml(
            CSPROJ,
            spec -> spec.path("App/App.csproj").markers(project(resolved("Newtonsoft.Json", "12.0.3", 0)))
          ),
          text(
            "class A {}",
            spec -> spec.path("App/A.cs")
          )
        );
    }

    @Test
    void packageIdIsCaseInsensitive() {
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("newtonsoft.json", null, null)),
          xml(
            CSPROJ,
            "<!--~~(Module has dependency: newtonsoft.json)~~>-->" + CSPROJ,
            spec -> spec.path("App.csproj").markers(project(resolved("Newtonsoft.Json", "13.0.3", 0)))
          ),
          text(
            "class A {}",
            "~~(Module has dependency: newtonsoft.json)~~>class A {}",
            spec -> spec.path("A.cs")
          )
        );
    }

    @Test
    void transitiveDependencyMatchesUnlessOnlyDirect() {
        MSBuildProject marker = project(resolved("Newtonsoft.Json", "13.0.3", 1));
        rewriteRun(
          xml(
            CSPROJ,
            "<!--~~(Module has dependency: Newtonsoft.*:13.x)~~>-->" + CSPROJ,
            spec -> spec.path("App/App.csproj").markers(marker)
          )
        );
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("Newtonsoft.*", "13.x", true)),
          xml(
            CSPROJ,
            spec -> spec.path("App/App.csproj").markers(marker)
          )
        );
    }

    @Test
    void referencedProjectsAreNotPackages() {
        rewriteRun(
          spec -> spec.recipe(new ModuleHasDependency("Newtonsoft.Json", null, null)),
          xml(
            CSPROJ,
            spec -> spec.path("App/App.csproj").markers(project(MSBuildProject.ResolvedPackage.builder()
              .name("Newtonsoft.Json").resolvedVersion("1.0.0").depth(0).type("project").build()))
          )
        );
    }

    @Test
    void fallsBackToPackageReferencesWithoutRestoreGraph() {
        MSBuildProject marker = MSBuildProject.builder()
          .targetFrameworks(singletonList(MSBuildProject.TargetFramework.builder()
            .targetFramework("net8.0")
            .packageReferences(singletonList(new MSBuildProject.PackageReference("Newtonsoft.Json", "13.0.1", null)))
            .build()))
          .build();
        rewriteRun(
          xml(
            CSPROJ,
            "<!--~~(Module has dependency: Newtonsoft.*:13.x)~~>-->" + CSPROJ,
            spec -> spec.path("App/App.csproj").markers(marker)
          )
        );
    }

    @Test
    void nestedProjectOwnsItsOwnDirectory() {
        rewriteRun(
          xml(
            CSPROJ,
            "<!--~~(Module has dependency: Newtonsoft.*:13.x)~~>-->" + CSPROJ,
            spec -> spec.path("App/App.csproj").markers(project(resolved("Newtonsoft.Json", "13.0.3", 0)))
          ),
          xml(
            CSPROJ,
            spec -> spec.path("App/Tests/Tests.csproj").markers(project())
          ),
          text(
            "class T {}",
            spec -> spec.path("App/Tests/Unit/T.cs")
          )
        );
    }

    @Test
    void projectsSharingADirectoryAreToldApartByDotNetProject() {
        rewriteRun(
          xml(
            CSPROJ,
            "<!--~~(Module has dependency: Newtonsoft.*:13.x)~~>-->" + CSPROJ,
            spec -> spec.path("App/App.csproj").markers(project(resolved("Newtonsoft.Json", "13.0.3", 0)))
          ),
          xml(
            CSPROJ,
            spec -> spec.path("App/App.Tests.csproj").markers(project())
          ),
          text(
            "class A {}",
            "~~(Module has dependency: Newtonsoft.*:13.x)~~>class A {}",
            spec -> spec.path("App/A.cs").markers(dotNetProject("App"))
          ),
          text(
            "class T {}",
            spec -> spec.path("App/T.cs").markers(dotNetProject("App.Tests"))
          )
        );
    }

    @Test
    void buildPropsFilesAreNotProjects() {
        rewriteRun(
          xml(
            CSPROJ,
            spec -> spec.path("Directory.Build.props").markers(project(resolved("Newtonsoft.Json", "13.0.3", 0)))
          ),
          text(
            "class A {}",
            spec -> spec.path("src/A.cs")
          )
        );
    }

    @Test
    void noProjectFilesMarksNothing() {
        rewriteRun(
          text(
            "class A {}",
            spec -> spec.path("A.cs").markers(dotNetProject("App"))
          )
        );
    }

    private static MSBuildProject project(MSBuildProject.ResolvedPackage... resolved) {
        return MSBuildProject.builder()
          .targetFrameworks(singletonList(MSBuildProject.TargetFramework.builder()
            .targetFramework("net8.0")
            .resolvedPackages(List.of(resolved))
            .build()))
          .build();
    }

    private static MSBuildProject.ResolvedPackage resolved(String name, String version, int depth) {
        return MSBuildProject.ResolvedPackage.builder().name(name).resolvedVersion(version).depth(depth).build();
    }

    private static DotNetProject dotNetProject(String name) {
        return new DotNetProject(randomId(), name, singletonList("net8.0"), "Microsoft.NET.Sdk");
    }
}
