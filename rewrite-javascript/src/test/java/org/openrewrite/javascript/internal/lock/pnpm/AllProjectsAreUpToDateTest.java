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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.lockfile;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * lockfile/verification/test/allProjectsAreUpToDate.test.ts of pnpm 11.2.2, less the five local tarball cases, whose
 * integrity needs the tarball's bytes.
 */
class AllProjectsAreUpToDateTest {

    private static final String FOO_MANIFEST = "{'name':'foo','version':'1.0.0'}";

    private static Verification.Project project(String id, String manifest) {
        return new Verification.Project(id, id, obj(manifest));
    }

    private static Map<String, Map<String, Verification.Project>> workspacePackages(Verification.Project... projects) {
        Map<String, Map<String, Verification.Project>> packages = new LinkedHashMap<>();
        for (Verification.Project project : projects) {
            packages.computeIfAbsent(Js.string(project.getManifest().get("name")), n -> new LinkedHashMap<>())
              .put(Js.string(project.getManifest().get("version")), project);
        }
        return packages;
    }

    private static final Map<String, Map<String, Verification.Project>> WORKSPACE_PACKAGES =
      workspacePackages(project("foo", FOO_MANIFEST));

    private static boolean allProjectsAreUpToDate(List<Verification.Project> projects, boolean linkWorkspacePackages,
                                                  String wantedLockfile,
                                                  Map<String, Map<String, Verification.Project>> workspacePackages,
                                                  Map<String, String> files) {
        return Verification.allProjectsAreUpToDate(projects, new LinkedHashMap<>(), false, false,
          linkWorkspacePackages, lockfile(wantedLockfile), workspacePackages, files, "");
    }

    private static boolean allProjectsAreUpToDate(List<Verification.Project> projects, String wantedLockfile) {
        return allProjectsAreUpToDate(projects, true, wantedLockfile, WORKSPACE_PACKAGES, Collections.emptyMap());
    }

    private static List<Verification.Project> barAndFoo(String barManifest) {
        List<Verification.Project> projects = new ArrayList<>();
        projects.add(project("bar", barManifest));
        projects.add(project("foo", FOO_MANIFEST));
        return projects;
    }

    @Test
    void worksWithPackagesLinkedThroughTheWorkspaceProtocolUsingRelativePath() {
        assertThat(allProjectsAreUpToDate(barAndFoo("{'dependencies':{'foo':'workspace:../foo'}}"),
          "{'importers':{'bar':{'dependencies':{'foo':'link:../foo'},'specifiers':{'foo':'workspace:../foo'}}," +
          "'foo':{'specifiers':{}}},'lockfileVersion':'9.0'}")).isTrue();
    }

    @Test
    void worksWithAliasedLocalDependencies() {
        assertThat(allProjectsAreUpToDate(barAndFoo("{'dependencies':{'alias':'npm:foo'}}"),
          "{'importers':{'bar':{'dependencies':{'alias':'link:../foo'},'specifiers':{'alias':'npm:foo'}}," +
          "'foo':{'specifiers':{}}},'lockfileVersion':'9.0'}")).isTrue();
    }

    @Test
    void worksWithAliasedLocalDependenciesThatSpecifyVersions() {
        assertThat(allProjectsAreUpToDate(barAndFoo("{'dependencies':{'alias':'npm:foo@1'}}"),
          "{'importers':{'bar':{'dependencies':{'alias':'link:../foo'},'specifiers':{'alias':'npm:foo@1'}}," +
          "'foo':{'specifiers':{}}},'lockfileVersion':'9.0'}")).isTrue();
    }

    @Test
    void returnsFalseIfTheAliasedDependencyVersionIsOutOfDate() {
        assertThat(allProjectsAreUpToDate(barAndFoo("{'dependencies':{'alias':'npm:foo@0'}}"),
          "{'importers':{'bar':{'dependencies':{'alias':'link:../foo'},'specifiers':{'alias':'npm:foo@0'}}," +
          "'foo':{'specifiers':{}}},'lockfileVersion':'9.0'}")).isFalse();
    }

    @Test
    void useLinkAndRegistryVersionIfLinkWorkspacePackagesIsFalse() {
        List<Verification.Project> projects = new ArrayList<>();
        projects.add(project("bar",
          "{'dependencies':{'foo':'workspace:*','foo2':'workspace:~','foo3':'workspace:^'}}"));
        projects.add(project("bar2", "{'dependencies':{'foo':'1.0.0'}}"));
        projects.add(project("foo", FOO_MANIFEST));
        projects.add(project("foo2", "{'name':'foo2','version':'1.0.0'}"));
        projects.add(project("foo3", "{'name':'foo3','version':'1.0.0'}"));
        assertThat(allProjectsAreUpToDate(projects, false,
          "{'importers':{'bar':{'dependencies':{'foo':'link:../foo','foo2':'link:../foo2','foo3':'link:../foo3'}," +
          "'specifiers':{'foo':'workspace:*','foo2':'workspace:~','foo3':'workspace:^'}}," +
          "'bar2':{'dependencies':{'foo':'1.0.0'},'specifiers':{'foo':'1.0.0'}}," +
          "'foo':{'specifiers':{}},'foo2':{'specifiers':{}},'foo3':{'specifiers':{}}},'lockfileVersion':'9.0'}",
          WORKSPACE_PACKAGES, Collections.emptyMap())).isTrue();
    }

    @Test
    void returnsFalseIfDependenciesMetaDiffers() {
        assertThat(allProjectsAreUpToDate(barAndFoo("{'dependencies':{'foo':'workspace:../foo'}," +
                                                    "'dependenciesMeta':{'foo':{'injected':true}}}"),
          "{'importers':{'bar':{'dependencies':{'foo':'link:../foo'},'specifiers':{'foo':'workspace:../foo'}}," +
          "'foo':{'specifiers':{}}},'lockfileVersion':'9.0'}")).isFalse();
    }

    @Test
    void returnsTrueIfDependenciesMetaMatches() {
        assertThat(allProjectsAreUpToDate(barAndFoo("{'dependencies':{'foo':'workspace:../foo'}," +
                                                    "'dependenciesMeta':{'foo':{'injected':true}}}"),
          "{'importers':{'bar':{'dependencies':{'foo':'link:../foo'},'dependenciesMeta':{'foo':{'injected':true}}," +
          "'specifiers':{'foo':'workspace:../foo'}},'foo':{'specifiers':{}}},'lockfileVersion':'9.0'}")).isTrue();
    }

    private static final String LOCAL_FILE_LOCKFILE =
      "{'importers':{'bar':{'dependencies':{'local':'file:./local-dir'},'specifiers':{'local':'file:./local-dir'}}," +
      "'foo':{'specifiers':{}}},'packages':{'local@file:./local-dir':{'resolution':{'directory':'./local-dir'," +
      "'type':'directory'},'version':'1.0.0','dependencies':{'is-positive':'2.0.0'},'dev':false}}," +
      "'lockfileVersion':'9.0'}";

    private static boolean localFileDependencyUpToDate(String localDirDependencies) {
        Map<String, String> files = Collections.singletonMap("local-dir/package.json",
          ("{'name':'local-dir','version':'1.0.0','dependencies':" + localDirDependencies + "}").replace('\'', '"'));
        return allProjectsAreUpToDate(barAndFoo("{'dependencies':{'local':'file:./local-dir'}}"), true,
          LOCAL_FILE_LOCKFILE, WORKSPACE_PACKAGES, files);
    }

    @Test
    void localFileDependencyReturnsTrueIfLocalFileNotChanged() {
        assertThat(localFileDependencyUpToDate("{'is-positive':'2.0.0'}")).isTrue();
    }

    @Test
    void localFileDependencyReturnsFalseIfAddNewDependencyToLocalFile() {
        assertThat(localFileDependencyUpToDate("{'is-positive':'2.0.0','is-odd':'1.0.0'}")).isFalse();
    }

    @Test
    void localFileDependencyReturnsFalseIfUpdateDependencyInLocalFile() {
        assertThat(localFileDependencyUpToDate("{'is-positive':'3.0.0'}")).isFalse();
    }

    @Test
    void localFileDependencyReturnsFalseIfRemoveDependencyInLocalFile() {
        assertThat(localFileDependencyUpToDate("{}")).isFalse();
    }

    @Test
    void returnsTrueIfWorkspaceDependencysVersionTypeIsTag() {
        assertThat(allProjectsAreUpToDate(barAndFoo("{'dependencies':{'foo':'unpublished-tag'}}"),
          "{'importers':{'bar':{'dependencies':{'foo':'link:../foo'},'specifiers':{'foo':'unpublished-tag'}}," +
          "'foo':{'specifiers':{}}},'lockfileVersion':'9.0'}")).isTrue();
    }

    private static final String IMPORTER_MISSING_LOCKFILE =
      "{'importers':{'bar':{'dependencies':{'is-even':'1.0.0'},'specifiers':{'is-even':'1.0.0'}}}," +
      "'lockfileVersion':'9.0'}";

    @Test
    void returnsFalseIfOneOfTheImportersIsNotPresentInTheLockfile() {
        Verification.Project foo = project("foo", "{'name':'foo','version':'1.0.0'," +
                                                  "'dependencies':{'is-odd':'1.0.0'}}");
        Verification.Project bar = project("bar", "{'name':'bar','version':'1.0.0'," +
                                                  "'dependencies':{'is-even':'1.0.0'}}");
        List<Verification.Project> projects = new ArrayList<>();
        projects.add(bar);
        projects.add(foo);
        assertThat(allProjectsAreUpToDate(projects, true, IMPORTER_MISSING_LOCKFILE, workspacePackages(foo, bar),
          Collections.emptyMap())).isFalse();
    }

    @Test
    void returnsTrueIfOneOfTheImportersIsNotPresentInTheLockfileButTheImporterHasNoDependencies() {
        Verification.Project foo = project("foo", "{'name':'foo','version':'1.0.0'}");
        Verification.Project bar = project("bar", "{'name':'bar','version':'1.0.0'," +
                                                  "'dependencies':{'is-even':'1.0.0'}}");
        List<Verification.Project> projects = new ArrayList<>();
        projects.add(bar);
        projects.add(foo);
        assertThat(allProjectsAreUpToDate(projects, true, IMPORTER_MISSING_LOCKFILE, workspacePackages(foo, bar),
          Collections.emptyMap())).isTrue();
    }

    @Test
    void returnsTrueForInjectedSelfReferencingFileDependencyResolvedAsLink() {
        List<Verification.Project> projects = Collections.singletonList(project("can-link",
          "{'name':'can-link','version':'2.0.0','dependenciesMeta':{'can-link':{'injected':true}}," +
          "'devDependencies':{'can-link':'file:'}}"));
        assertThat(allProjectsAreUpToDate(projects, false,
          "{'importers':{'can-link':{'dependenciesMeta':{'can-link':{'injected':true}}," +
          "'devDependencies':{'can-link':'link:'},'specifiers':{'can-link':'file:'}}},'lockfileVersion':'9.0'}",
          new LinkedHashMap<>(), Collections.emptyMap())).isTrue();
    }

    @Test
    void returnsFalseIfTheLockfileIsBrokenTheResolvedVersionsDoNotSatisfyTheRanges() {
        List<Verification.Project> projects = Collections.singletonList(project(".",
          "{'dependencies':{'@apollo/client':'3.3.7'}}"));
        assertThat(allProjectsAreUpToDate(projects,
          "{'importers':{'.':{'dependencies':{'@apollo/client':'3.13.8(@types/react@18.3.23)(graphql@15.8.0)" +
          "(react-dom@17.0.2(react@17.0.2))(react@17.0.2)(subscriptions-transport-ws@0.11.0(graphql@15.8.0))'}," +
          "'specifiers':{'@apollo/client':'3.3.7'}}},'lockfileVersion':'9.0'}")).isFalse();
    }
}
