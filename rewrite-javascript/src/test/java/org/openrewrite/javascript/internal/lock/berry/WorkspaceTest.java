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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The order of a project's workspaces: Yarn sorts the paths fast-glob 3.3.3 reports, which keep a pattern's
 * leading {@code ./} unless a dynamic pattern based at the root merges the walk into one task there. Every
 * expectation is fast-glob's own answer over the same directories ({@code ybf-glob.js}, {@code ybf-glob2.js}).
 */
class WorkspaceTest {

    private static final String[] MEMBERS = {"packages/a", "packages/b", "apps/x", "apps/y", "libs/one"};

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
    void dotSegmentsAreNotPorted() {
        assertThatThrownBy(() -> members("\"././packages/*\"")).isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> members("\"apps/./*\"")).isInstanceOf(CannotKnow.class);
    }

    @Test
    void bracesExpandAsMicromatchDoes() {
        assertThat(GlobPattern.process("a{b,c}d{e,f}")).containsExactly("abde", "abdf", "acde", "acdf");
        assertThat(GlobPattern.process("{a,{b,c}}/x//y")).containsExactly("a/x/y", "b/x/y", "c/x/y");
        assertThat(GlobPattern.base("./packages/*")).isEqualTo("./packages");
        assertThat(GlobPattern.base("./*/a")).isEqualTo(".");
        assertThat(GlobPattern.base("./apps/x/")).isEqualTo("./apps/x");
    }

    private static List<String> members(String patterns) {
        BerryLockRegenerator.Inputs.InputsBuilder inputs = BerryLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\": \"root\", \"private\": true, \"workspaces\": [" + patterns + "]}")
                .now(Instant.EPOCH);
        for (String member : MEMBERS) {
            inputs.manifest(member, "{\"name\": \"" + member.replace('/', '-') + "\"}");
        }
        Project project = new BerryLockRegenerator(inputs.build()).load(YarnRelease.of("4.10.3")).getProject();
        List<String> out = new ArrayList<>();
        for (Workspace workspace : project.workspaces) {
            if (!".".equals(workspace.getRelativeCwd())) {
                out.add(workspace.getRelativeCwd());
            }
        }
        return out;
    }
}
