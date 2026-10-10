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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A project's workspaces: the sorted paths fast-glob 3.3.2 reports that hold a package.json, each expectation
 * fast-glob's own answer and, for the negated, globstar and {@code ?} patterns, Yarn 4.10.3's too.
 */
class WorkspaceTest {

    private static final List<String> MEMBERS = Arrays.asList("packages/a", "packages/b", "apps/x", "apps/y",
            "libs/one");

    @Test
    void aLeadingDotSlashSortsBeforeOtherPatterns() {
        assertThat(members("\"./packages/*\", \"apps/*\""))
                .containsExactly("packages/a", "packages/b", "apps/x", "apps/y");
        assertThat(members("\"./packages/**\", \"apps/*\""))
                .containsExactly("packages/a", "packages/b", "apps/x", "apps/y");
        assertThat(members("\"./packages/a\", \"apps/*\""))
                .containsExactly("packages/a", "apps/x", "apps/y");
        assertThat(members("\"./apps/*\", \"packages/*\""))
                .containsExactly("apps/x", "apps/y", "packages/a", "packages/b");
        assertThat(members("\"./{packages,apps}/*\", \"libs/*\""))
                .containsExactly("apps/x", "apps/y", "packages/a", "packages/b", "libs/one");
        assertThat(members("\"./packages/*\", \"!./packages/b\"")).containsExactly("packages/a");
    }

    @Test
    void aDynamicPatternAtTheRootDropsEveryLeadingDotSlash() {
        assertThat(members("\"./{packages,ap*}/*\", \"libs/*\""))
                .containsExactly("apps/x", "apps/y", "libs/one", "packages/a", "packages/b");
        assertThat(members("\"./packages/*\", \"**/one\""))
                .containsExactly("libs/one", "packages/a", "packages/b");
    }

    @Test
    void aStaticPatternIsReportedAsWritten() {
        assertThat(members("\"./apps/x/\", \"apps/*\"")).containsExactly("apps/x", "apps/y");
    }

    @Test
    void twoTasksReportingOneDirectoryRace() {
        assertThatThrownBy(() -> members("\"./packages/*\", \"packages/*\""))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("./packages/a or packages/a");
        assertThatThrownBy(() -> members("\"./packages/*\", \"packages/a\""))
                .isInstanceOf(CannotKnow.class);
    }

    @Test
    void oneStaticTaskReportsTheFirstSpelling() {
        assertThat(members("\"packages/a\", \"./packages/a\", \"tools\"")).containsExactly("packages/a");
    }

    @Test
    void dotSegmentsResolveAfterTheWalk() {
        assertThat(members("\"././packages/*\"")).isEmpty();
        assertThat(members("\"apps/./*\"")).containsExactly("apps/x", "apps/y");
        assertThat(members("\"packages/../apps/*\"")).containsExactly("apps/x", "apps/y");
    }

    @Test
    void aNegatedDirectoryIsNotWalked() {
        assertThat(members(Arrays.asList("packages/a", "packages/a/__fixtures__/proj", "packages/b"),
                "\"packages/**\", \"!packages/**/__fixtures__\"")).containsExactly("packages/a", "packages/b");
        List<String> legacy = Arrays.asList("packages/b", "packages/legacy/sub");
        assertThat(members(legacy, "\"packages/**\", \"!packages/legacy\"")).containsExactly("packages/b");
        assertThat(members(legacy, "\"packages/**\", \"!./packages/legacy\"")).containsExactly("packages/b");
    }

    @Test
    void aTrailingGlobstarMatchesItsOwnDirectory() {
        assertThat(members(Arrays.asList("packages/a", "packages/a/nested"), "\"packages/*/**\""))
                .containsExactly("packages/a", "packages/a/nested");
        assertThat(members(Arrays.asList("tools", "packages/a"), "\"*/**\"")).containsExactly("packages/a", "tools");
        assertThat(members(Arrays.asList("packages/a", "packages/b"), "\"packages/*/\""))
                .containsExactly("packages/a", "packages/b");
    }

    @Test
    void aLeadingQuestionMarkIsTheBaseDirectory() {
        assertThat(members(Arrays.asList("1/z", "a/b"), "\"./?/?\"")).isEmpty();
        assertThat(members(Arrays.asList("a/b", "1/z"), "\"?/[ab]\"")).isEmpty();
        assertThat(members(Arrays.asList("a/b", "1/z"), "\"?/?\"")).isEmpty();
        assertThat(members(Arrays.asList("?/x", "a/x"), "\"?/x\"")).containsExactly("?/x");
        assertThat(members(Arrays.asList("a/b"), "\"[ab]/*\"")).containsExactly("a/b");
    }

    @Test
    void theIgnoredDirectoriesAreNotWalked() {
        assertThat(members(Arrays.asList("packages/a", "node_modules/foo", "packages/node_modules/q"),
                "\"packages/*\", \"node_modules/foo\"")).containsExactly("node_modules/foo", "packages/a");
        assertThat(members(Arrays.asList("packages/a", "packages/.hidden", "packages/.hidden/node_modules/x",
                "packages/.hidden/y"), "\"packages/**\"")).containsExactly("packages/a");
    }

    @Test
    void bracesClassesAndExtglobs() {
        assertThat(members(Arrays.asList("packages/a", "packages/b", "v1", "v2", "v4"),
                "\"packages/{a,b}\", \"v{1..3}\"")).containsExactly("packages/a", "packages/b", "v1", "v2");
        assertThat(members(Arrays.asList("packages/a", "packages/b", "packages/c"),
                "\"packages/*\", \"!packages/[ab]\"")).containsExactly("packages/c");
        assertThat(members("\"@(apps|libs)/*\"")).containsExactly("apps/x", "apps/y", "libs/one");
        assertThat(members("\"!(packages)/*\"")).containsExactly("apps/x", "apps/y", "libs/one");
        assertThat(members("\"packages/!(a)\"")).containsExactly("packages/b");
    }

    @Test
    void whatFastGlobThrows() {
        assertThatThrownBy(() -> members("\"constructor\"")).isInstanceOf(JsError.class)
                .hasMessage("output.startsWith is not a function");
        assertThatThrownBy(() -> members("\"packages/*\", \"!__proto__\"")).isInstanceOf(JsError.class)
                .hasMessage("output.startsWith is not a function");
        assertThatThrownBy(() -> members("\"hasOwnProperty/*\"")).isInstanceOf(JsError.class)
                .hasMessage("collection[base].push is not a function");
        assertThatThrownBy(() -> members("\"\"")).isInstanceOf(JsError.class)
                .hasMessage("Patterns must be a string (non empty) or an array of strings");
        assertThatThrownBy(() -> members("\"{x}{(a)\"")).isInstanceOf(JsError.class)
                .hasMessage("Cannot read properties of undefined (reading 'push')");
        assertThatThrownBy(() -> members("\"v{1..1001}\"")).isInstanceOf(JsError.class)
                .hasMessageStartingWith("expanded array length exceeds range limit");
        assertThatThrownBy(() -> members("\"packages/a/package.json/*\"")).isInstanceOf(JsError.class)
                .hasMessageStartingWith("ENOTDIR: not a directory, scandir");
        assertThat(members("\"!\"")).isEmpty();
    }

    @Test
    void aPatternOutsideTheProjectIsNotKnown() {
        assertThatThrownBy(() -> members("\"../x/*\"")).isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> members("\"/abs/*\"")).isInstanceOf(CannotKnow.class);
    }

    /**
     * Yarn 4.10.3's {@code yarn workspaces list}: a workspace claims the members it lists that no workspace has
     * claimed when its setup ends, and each workspace is followed by the members it claimed.
     */
    @Test
    void eachWorkspaceIsFollowedByTheMembersItClaimed() {
        assertThat(workspaces(tree("=[\"packages/*\"]", "packages/a=[\"../c\"]", "packages/b", "packages/c")))
                .containsExactly(".", "packages/a", "packages/b", "packages/c");
        assertThat(workspaces(tree("=[\"packages/*\"]", "packages/a=[\"../c\"]", "packages/b",
                "packages/c=[\"sub/*\"]", "packages/c/sub/x")))
                .containsExactly(".", "packages/a", "packages/b", "packages/c", "packages/c/sub/x");
        assertThat(workspaces(tree("=[\"packages/*\"]", "packages/a=[\"sub/*\"]", "packages/a/sub/x",
                "packages/a/sub/y", "packages/b")))
                .containsExactly(".", "packages/a", "packages/a/sub/x", "packages/a/sub/y", "packages/b");
        assertThat(workspaces(tree("=[\"packages/*\", \"packages/a/sub/*\"]", "packages/a=[\"sub/*\"]",
                "packages/a/sub/x", "packages/b")))
                .containsExactly(".", "packages/a", "packages/a/sub/x", "packages/b");
        assertThat(workspaces(tree("=[\"packages/*\"]", "packages/a=[\"../b\"]", "packages/b", "packages/c")))
                .containsExactly(".", "packages/a", "packages/b", "packages/c");
        assertThat(workspaces(tree("=[\"packages/*\"]", "packages/a=[\".\", \"../..\", \"../b\"]", "packages/b")))
                .containsExactly(".", "packages/a", "packages/b");
    }

    /**
     * Yarn 4.10.3 lists {@code shared} after {@code packages/a}, and after {@code packages/b} when {@code packages/a}'s
     * glob walks four thousand directories; likewise {@code packages/b/deep} and {@code packages/a/sub/x}.
     */
    @Test
    void twoWorkspacesWhoseSetupsMayEndInEitherOrderListingOneMemberRace() {
        assertThatThrownBy(() -> workspaces(tree("=[\"packages/*\"]", "packages/a=[\"../../shared\"]",
                "packages/b=[\"../../shared\"]", "shared")))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("packages/a and packages/b both list shared");
        assertThatThrownBy(() -> workspaces(tree("=[\"packages/*\"]", "packages/a=[\"sub/*\"]",
                "packages/a/sub/x=[\"../../../b/deep\"]", "packages/b=[\"deep\"]", "packages/b/deep")))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("packages/b and packages/a/sub/x both list packages/b/deep");
    }

    /**
     * Yarn 4.10.3 on a case-sensitive volume and on macOS's default one, which finds {@code Packages/a}, {@code
     * libs/MyLib} and {@code packages/A} under the pattern's spelling; a deeper segment matches the names held on both.
     */
    @Test
    void aPathInAnotherCaseIsUnknownWhereTheFileSystemMayIgnoreCase() {
        Host linux = Host.builder().os("linux").build();
        assertThat(workspaces(tree("=[\"Packages/*\"]", "packages/a", "packages/b").host(linux)))
                .containsExactly(".");
        assertThat(workspaces(tree("=[\"packages/*\", \"libs/MyLib\"]", "packages/a", "libs/mylib").host(linux)))
                .containsExactly(".", "packages/a");
        assertThat(workspaces(tree("=[\"packages/A\"]", "packages/a").host(linux))).containsExactly(".");

        for (Host host : Arrays.asList(Host.builder().os("darwin").build(), Host.builder().os("win32").build(),
                Host.UNKNOWN)) {
            assertThatThrownBy(() -> workspaces(tree("=[\"Packages/*\"]", "packages/a", "packages/b").host(host)))
                    .isInstanceOf(CannotKnow.class)
                    .hasMessageContaining("finds Packages as packages");
            assertThatThrownBy(() -> workspaces(tree("=[\"packages/*\", \"libs/MyLib\"]", "packages/a",
                    "libs/mylib").host(host)))
                    .isInstanceOf(CannotKnow.class)
                    .hasMessageContaining("finds libs/MyLib as libs/mylib");
            assertThatThrownBy(() -> workspaces(tree("=[\"packages/A\"]", "packages/a").host(host)))
                    .isInstanceOf(CannotKnow.class);
        }

        for (Host host : Arrays.asList(linux, Host.UNKNOWN)) {
            assertThat(workspaces(tree("=[\"packages/*/Lib\"]", "packages/a/lib", "packages/b/Lib").host(host)))
                    .containsExactly(".", "packages/b/Lib");
        }
    }

    @Test
    void thePathsTheHostHoldsAreTheFileSystem() {
        Set<String> paths = new HashSet<>(Arrays.asList("package.json", "packages/a/package.json",
                "packages/docs/README.md", "README.md"));
        assertThat(workspaces(tree("=[\"packages/*\"]", "packages/a").paths(paths)))
                .containsExactly(".", "packages/a");
        assertThatThrownBy(() -> workspaces(tree("=[\"README.md/*\"]", "packages/a").paths(paths)))
                .isInstanceOf(JsError.class)
                .hasMessageStartingWith("ENOTDIR: not a directory, scandir");

        paths.add("packages/b/package.json");
        assertThatThrownBy(() -> workspaces(tree("=[\"packages/*\"]", "packages/a").paths(paths)))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("packages/b/package.json (its contents were not read)");
    }

    private static List<String> workspaces(YarnBerryLockRegenerator.Inputs.InputsBuilder inputs) {
        Project project = new YarnBerryLockRegenerator(inputs.now(Instant.EPOCH).build(), BerryScenarios.NO_REGISTRY)
                .load(YarnRelease.of("4.10.3"), Collections.emptyMap()).getProject();
        List<String> out = new ArrayList<>();
        for (Workspace workspace : project.workspaces) {
            out.add(workspace.getRelativeCwd());
        }
        return out;
    }

    // a directory's manifest, with the workspaces after an = ("" the root's)
    private static YarnBerryLockRegenerator.Inputs.InputsBuilder tree(String... directories) {
        YarnBerryLockRegenerator.Inputs.InputsBuilder inputs = YarnBerryLockRegenerator.Inputs.builder();
        for (String directory : directories) {
            int eq = directory.indexOf('=');
            String location = eq < 0 ? directory : directory.substring(0, eq);
            inputs.manifest(location, "{\"name\": \"" + (location.isEmpty() ? "root" :
                    "w-" + location.replace('/', '-').toLowerCase(Locale.ROOT)) + "\"" +
                    (eq < 0 ? "" : ", \"private\": true, \"workspaces\": " + directory.substring(eq + 1)) + "}");
        }
        return inputs;
    }

    private static List<String> members(String patterns) {
        return members(MEMBERS, patterns);
    }

    private static List<String> members(List<String> directories, String patterns) {
        YarnBerryLockRegenerator.Inputs.InputsBuilder inputs = YarnBerryLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\": \"root\", \"private\": true, \"workspaces\": [" + patterns + "]}")
                .now(Instant.EPOCH);
        for (String member : directories) {
            inputs.manifest(member, "{\"name\": \"" + member.replace('/', '-') + "\"}");
        }
        Project project = new YarnBerryLockRegenerator(inputs.build(), BerryScenarios.NO_REGISTRY)
                .load(YarnRelease.of("4.10.3"), Collections.emptyMap()).getProject();
        List<String> out = new ArrayList<>();
        for (Workspace workspace : project.workspaces) {
            if (!".".equals(workspace.getRelativeCwd())) {
                out.add(workspace.getRelativeCwd());
            }
        }
        return out;
    }
}
