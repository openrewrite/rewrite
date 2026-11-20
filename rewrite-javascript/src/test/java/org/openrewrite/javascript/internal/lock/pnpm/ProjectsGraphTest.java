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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * workspace/projects-graph/test/index.ts of pnpm 11.2.2, with the project directories relative to the workspace root
 * ({@code zkochan/src/bar} for {@code /zkochan/src/bar}); a graph is written {@code dir>dep,dep;dir>}.
 */
class ProjectsGraphTest {

    private static final String BAR1 = "zkochan/src/bar";
    private static final String FOO1 = "zkochan/src/foo";
    private static final String BAR2 = "zkochan/src/bar@2";
    private static final String FOO2 = "zkochan/src/foo@2";
    private static final String BAR3 = "zkochan/src/bar@3";
    private static final String BAR4 = "zkochan/src/bar@4";
    private static final String BAR5 = "zkochan/src/bar@5";

    private static Verification.Project project(String rootDir, String manifest) {
        return new Verification.Project(rootDir, rootDir, obj(manifest));
    }

    private static List<Verification.Project> projects(String... dirsAndManifests) {
        List<Verification.Project> projects = new ArrayList<>();
        for (int i = 0; i < dirsAndManifests.length; i += 2) {
            projects.add(project(dirsAndManifests[i], dirsAndManifests[i + 1]));
        }
        return projects;
    }

    private static void assertGraph(ProjectsGraph.Result result, String graph, String unmatched) {
        StringBuilder actual = new StringBuilder();
        result.getGraph().forEach((dir, deps) -> actual.append(actual.length() == 0 ? "" : ";").append(dir)
          .append('>').append(String.join(",", deps)));
        assertThat(actual.toString()).isEqualTo(graph);
        StringBuilder actualUnmatched = new StringBuilder();
        for (ProjectsGraph.Unmatched u : result.getUnmatched()) {
            actualUnmatched.append(actualUnmatched.length() == 0 ? "" : ";").append(u.getPkgName()).append('@')
              .append(u.getRange());
        }
        assertThat(actualUnmatched.toString()).isEqualTo(unmatched);
    }

    private static ProjectsGraph.Result create(List<Verification.Project> projects,
                                               @Nullable Boolean linkWorkspacePackages) {
        return ProjectsGraph.createProjectsGraph(projects, false, linkWorkspacePackages);
    }

    @Test
    void createPackageGraph() {
        ProjectsGraph.Result result = create(projects(
          BAR1, "{'name':'bar','version':'1.0.0','dependencies':{'foo':'^1.0.0','is-positive':'1.0.0'}}",
          FOO1, "{'name':'foo','version':'1.0.0','dependencies':{'bar':'^10.0.0'}}",
          BAR2, "{'name':'bar','version':'2.0.0','dependencies':{'foo':'^2.0.0'}}",
          FOO2, "{'name':'foo','version':'2.0.0'}"), null);
        assertGraph(result, BAR1 + ">" + FOO1 + ";" + FOO1 + ">;" + BAR2 + ">" + FOO2 + ";" + FOO2 + ">",
          "bar@^10.0.0");
    }

    @Test
    void createPackageGraphUsingPeerDependencies() {
        ProjectsGraph.Result result = create(projects(
          BAR1, "{'name':'bar','version':'1.0.0','peerDependencies':{'foo':'^1.0.0','is-positive':'1.0.0'}}",
          FOO1, "{'name':'foo','version':'1.0.0'}"), null);
        assertGraph(result, BAR1 + ">" + FOO1 + ";" + FOO1 + ">", "");
    }

    @Test
    void createPackageGraphForLocalDirectoryDependencies() {
        ProjectsGraph.Result result = create(projects(
          BAR1, "{'name':'bar','version':'1.0.0','dependencies':{'foo':'../foo','is-positive':'1.0.0'," +
                "'weird-dep':':aaaaa'}}",
          FOO1, "{'name':'foo','version':'1.0.0','dependencies':{'bar':'^10.0.0'}}",
          BAR2, "{'name':'bar','version':'2.0.0','dependencies':{'foo':'file:../foo@2'}}",
          FOO2, "{'name':'foo','version':'2.0.0'}"), null);
        assertGraph(result, BAR1 + ">" + FOO1 + ";" + FOO1 + ">;" + BAR2 + ">" + FOO2 + ";" + FOO2 + ">",
          "bar@^10.0.0");
    }

    @Test
    void createPackageGraphIgnoringTheWorkspaceProtocol() {
        ProjectsGraph.Result result = create(projects(
          BAR1, "{'name':'bar','version':'1.0.0','dependencies':{'foo':'workspace:^1.0.0','is-positive':'1.0.0'}}",
          FOO1, "{'name':'foo','version':'1.0.0','dependencies':{'bar':'^10.0.0'}}",
          BAR2, "{'name':'bar','version':'2.0.0','dependencies':{'foo':'workspace:^2.0.0'}}",
          FOO2, "{'name':'foo','version':'2.0.0'}",
          BAR3, "{'name':'bar','version':'3.0.0','dependencies':{'foo':'workspace:^'}}",
          BAR4, "{'name':'bar','version':'4.0.0','dependencies':{'foo':'workspace:~'}}"), null);
        assertGraph(result, BAR1 + ">" + FOO1 + ";" + FOO1 + ">;" + BAR2 + ">" + FOO2 + ";" + FOO2 + ">;" + BAR3 +
                            ">" + FOO2 + ";" + BAR4 + ">" + FOO2, "bar@^10.0.0");
    }

    @Test
    void createPackageGraphRespectsLinkedWorkspacePackagesFalse() {
        ProjectsGraph.Result result = create(projects(
          BAR1, "{'dependencies':{'foo':'workspace:*'},'name':'bar','version':'1.0.0'}",
          FOO1, "{'dependencies':{'bar':'^10.0.0'},'name':'foo','version':'1.0.1'}",
          BAR2, "{'dependencies':{'foo':'1.0.1'},'name':'bar','version':'2.0.0'}",
          BAR3, "{'dependencies':{'foo':'workspace:~1.0.0'},'name':'bar','version':'3.0.0'}",
          BAR4, "{'dependencies':{'foo':'workspace:^'},'name':'bar','version':'4.0.0'}",
          BAR5, "{'dependencies':{'foo':'workspace:~'},'name':'bar','version':'5.0.0'}"), false);
        assertGraph(result, BAR1 + ">" + FOO1 + ";" + FOO1 + ">;" + BAR2 + ">;" + BAR3 + ">" + FOO1 + ";" + BAR4 +
                            ">" + FOO1 + ";" + BAR5 + ">" + FOO1, "bar@^10.0.0;foo@1.0.1");
    }

    @Test
    void createPackageGraphRespectsIgnoreDevDeps() {
        ProjectsGraph.Result result = ProjectsGraph.createProjectsGraph(projects(
          BAR1, "{'name':'bar','version':'1.0.0','dependencies':{'is-positive':'1.0.0'}," +
                "'devDependencies':{'foo':'^1.0.0'}}",
          FOO1, "{'name':'foo','version':'1.0.0','dependencies':{'bar':'^10.0.0'}}",
          BAR2, "{'name':'bar','version':'2.0.0','dependencies':{'foo':'^2.0.0'}}",
          FOO2, "{'name':'foo','version':'2.0.0'}"), true, null);
        assertGraph(result, BAR1 + ">;" + FOO1 + ">;" + BAR2 + ">" + FOO2 + ";" + FOO2 + ">", "bar@^10.0.0");
    }

    @Test
    void starMatchesPrereleaseVersions() {
        ProjectsGraph.Result result = create(projects(
          BAR1, "{'dependencies':{'foo':'*'},'name':'bar','version':'1.0.0'}",
          FOO1, "{'name':'foo','version':'1.0.0-0'}"), null);
        assertGraph(result, BAR1 + ">" + FOO1 + ";" + FOO1 + ">", "");
    }

    @Test
    void aWorkspacePackageWithNoVersion() {
        ProjectsGraph.Result result = create(projects(
          BAR1, "{'dependencies':{'foo':'workspace:*'},'name':'bar','version':'1.0.0'}",
          FOO1, "{'name':'foo'}"), null);
        assertGraph(result, BAR1 + ">" + FOO1 + ";" + FOO1 + ">", "");
    }

    @Test
    void workspaceAliasSyntax() {
        ProjectsGraph.Result result = create(projects(
          BAR1, "{'dependencies':{'foo-alias':'workspace:foo@*'},'name':'bar','version':'1.0.0'}",
          FOO1, "{'name':'foo'}"), null);
        assertGraph(result, BAR1 + ">" + FOO1 + ";" + FOO1 + ">", "");
    }

    @Test
    void aLinkProtocolIsNoEdge() {
        ProjectsGraph.Result result = create(projects(
          "packages/a", "{'name':'a','version':'1.0.0','dependencies':{'b':'link:../b'}}",
          "packages/b", "{'name':'b','version':'1.0.0'}"), false);
        assertGraph(result, "packages/a>;packages/b>", "");
    }

    @Test
    void aDirectoryOutsideTheWorkspaceIsUnknown() {
        ProjectsGraph.Result result = create(projects(
          "packages/a", "{'name':'a','version':'1.0.0','dependencies':{'b':'file:../../../elsewhere/b'}}"), false);
        assertGraph(result, "packages/a>", "");
        assertThat(result.getUnknown()).containsExactly("file:../../../elsewhere/b");
    }
}
