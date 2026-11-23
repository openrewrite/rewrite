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
package org.openrewrite.csharp.sln;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;

class RemoveNjsprojFromSolutionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveNjsprojFromSolution());
    }

    @Test
    void removesProjectBlockAndEverythingReferencingIt() {
        rewriteRun(
          text(
            """
              Microsoft Visual Studio Solution File, Format Version 12.00
              # Visual Studio 15
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
              EndProject
              Project("{9092AA53-FB77-4645-B42D-1CCCA6BD08BD}") = "vcc", "vcc\\vcc.njsproj", "{AF92B192-B878-4652-8689-CAA4FBD03FB7}"
              EndProject
              Project("{2150E333-8FDC-42A3-9474-1A3956D46DE8}") = "web", "web", "{22222222-2222-2222-2222-222222222222}"
              EndProject
              Global
                  GlobalSection(SolutionConfigurationPlatforms) = preSolution
                      Debug|Any CPU = Debug|Any CPU
                      Release|Any CPU = Release|Any CPU
                  EndGlobalSection
                  GlobalSection(ProjectConfigurationPlatforms) = postSolution
                      {11111111-1111-1111-1111-111111111111}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
                      {11111111-1111-1111-1111-111111111111}.Debug|Any CPU.Build.0 = Debug|Any CPU
                      {AF92B192-B878-4652-8689-CAA4FBD03FB7}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
                      {AF92B192-B878-4652-8689-CAA4FBD03FB7}.Debug|Any CPU.Build.0 = Debug|Any CPU
                      {AF92B192-B878-4652-8689-CAA4FBD03FB7}.Release|Any CPU.ActiveCfg = Release|Any CPU
                      {AF92B192-B878-4652-8689-CAA4FBD03FB7}.Release|Any CPU.Build.0 = Release|Any CPU
                  EndGlobalSection
                  GlobalSection(NestedProjects) = preSolution
                      {AF92B192-B878-4652-8689-CAA4FBD03FB7} = {22222222-2222-2222-2222-222222222222}
                  EndGlobalSection
              EndGlobal
              """,
            """
              Microsoft Visual Studio Solution File, Format Version 12.00
              # Visual Studio 15
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
              EndProject
              Project("{2150E333-8FDC-42A3-9474-1A3956D46DE8}") = "web", "web", "{22222222-2222-2222-2222-222222222222}"
              EndProject
              Global
                  GlobalSection(SolutionConfigurationPlatforms) = preSolution
                      Debug|Any CPU = Debug|Any CPU
                      Release|Any CPU = Release|Any CPU
                  EndGlobalSection
                  GlobalSection(ProjectConfigurationPlatforms) = postSolution
                      {11111111-1111-1111-1111-111111111111}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
                      {11111111-1111-1111-1111-111111111111}.Debug|Any CPU.Build.0 = Debug|Any CPU
                  EndGlobalSection
                  GlobalSection(NestedProjects) = preSolution
                  EndGlobalSection
              EndGlobal
              """,
            spec -> spec.path("vcc.sln")
          )
        );
    }

    @Test
    void removesProjectSectionsNestedInTheRemovedProject() {
        rewriteRun(
          text(
            """
              Project("{9092AA53-FB77-4645-B42D-1CCCA6BD08BD}") = "vcc", "vcc\\vcc.njsproj", "{AF92B192-B878-4652-8689-CAA4FBD03FB7}"
                  ProjectSection(ProjectDependencies) = postProject
                      {11111111-1111-1111-1111-111111111111} = {11111111-1111-1111-1111-111111111111}
                  EndProjectSection
              EndProject
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
              EndProject
              """,
            """
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
              EndProject
              """,
            spec -> spec.path("vcc.sln")
          )
        );
    }

    @Test
    void removesAnotherProjectsDependencyOnTheRemovedProject() {
        rewriteRun(
          text(
            """
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
                  ProjectSection(ProjectDependencies) = postProject
                      {AF92B192-B878-4652-8689-CAA4FBD03FB7} = {AF92B192-B878-4652-8689-CAA4FBD03FB7}
                  EndProjectSection
              EndProject
              Project("{9092AA53-FB77-4645-B42D-1CCCA6BD08BD}") = "vcc", "vcc\\vcc.njsproj", "{AF92B192-B878-4652-8689-CAA4FBD03FB7}"
              EndProject
              Global
              EndGlobal
              """,
            """
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
                  ProjectSection(ProjectDependencies) = postProject
                  EndProjectSection
              EndProject
              Global
              EndGlobal
              """,
            spec -> spec.path("vcc.sln")
          )
        );
    }

    @Test
    void doesNotSwallowTheRestOfASolutionMissingItsEndProject() {
        rewriteRun(
          text(
            """
              Project("{9092AA53-FB77-4645-B42D-1CCCA6BD08BD}") = "vcc", "vcc\\vcc.njsproj", "{AF92B192-B878-4652-8689-CAA4FBD03FB7}"
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
              EndProject
              Global
                  GlobalSection(SolutionProperties) = preSolution
                      HideSolutionNode = FALSE
                  EndGlobalSection
              EndGlobal
              """,
            """
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
              EndProject
              Global
                  GlobalSection(SolutionProperties) = preSolution
                      HideSolutionNode = FALSE
                  EndGlobalSection
              EndGlobal
              """,
            spec -> spec.path("vcc.sln")
          )
        );
    }

    @Test
    void preservesWindowsLineEndings() {
        rewriteRun(
          text(
            "Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"App\", \"App\\App.csproj\", \"{11111111-1111-1111-1111-111111111111}\"\r\n" +
            "EndProject\r\n" +
            "Project(\"{9092AA53-FB77-4645-B42D-1CCCA6BD08BD}\") = \"vcc\", \"vcc\\vcc.njsproj\", \"{AF92B192-B878-4652-8689-CAA4FBD03FB7}\"\r\n" +
            "EndProject\r\n" +
            "Global\r\n" +
            "EndGlobal\r\n",
            "Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"App\", \"App\\App.csproj\", \"{11111111-1111-1111-1111-111111111111}\"\r\n" +
            "EndProject\r\n" +
            "Global\r\n" +
            "EndGlobal\r\n",
            spec -> spec.path("vcc.sln")
          )
        );
    }

    @Test
    void leavesSolutionsWithoutNodeProjectsAlone() {
        rewriteRun(
          text(
            """
              Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "App", "App\\App.csproj", "{11111111-1111-1111-1111-111111111111}"
              EndProject
              """,
            spec -> spec.path("vcc.sln")
          )
        );
    }

    @Test
    void leavesTextFilesThatAreNotSolutionsAlone() {
        rewriteRun(
          text(
            """
              Project("{9092AA53-FB77-4645-B42D-1CCCA6BD08BD}") = "vcc", "vcc\\vcc.njsproj", "{AF92B192-B878-4652-8689-CAA4FBD03FB7}"
              EndProject
              """,
            spec -> spec.path("notes.txt")
          )
        );
    }

    @Test
    void removesProjectFromSlnx() {
        rewriteRun(
          xml(
            """
              <Solution>
                <Project Path="App/App.csproj" />
                <Project Path="vcc/vcc.njsproj" />
              </Solution>
              """,
            """
              <Solution>
                <Project Path="App/App.csproj" />
              </Solution>
              """,
            spec -> spec.path("vcc.slnx")
          )
        );
    }

    @Test
    void removesProjectNestedInSlnxFolder() {
        rewriteRun(
          xml(
            """
              <Solution>
                <Folder Name="/clients/">
                  <Project Path="App/App.csproj" />
                  <Project Path="vcc/vcc.njsproj" />
                </Folder>
              </Solution>
              """,
            """
              <Solution>
                <Folder Name="/clients/">
                  <Project Path="App/App.csproj" />
                </Folder>
              </Solution>
              """,
            spec -> spec.path("vcc.slnx")
          )
        );
    }

    @Test
    void leavesSlnxWithoutNodeProjectsAlone() {
        rewriteRun(
          xml(
            """
              <Solution>
                <Project Path="App/App.csproj" />
              </Solution>
              """,
            spec -> spec.path("vcc.slnx")
          )
        );
    }
}
