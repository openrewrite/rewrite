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
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * pnpm 11.2.2's workspace/root-finder over the files a project holds: find-up from where pnpm runs, unless
 * NPM_CONFIG_WORKSPACE_DIR (or its lowercase form) names the directory.
 */
class WorkspaceRootFinderTest {

    private static final Set<String> HELD = new HashSet<>(Arrays.asList("pnpm-workspace.yaml",
      "packages/a/package.json", "tools/pnpm-workspace.yaml", "tools/x/package.json"));

    private static @Nullable String find(String cwd, @Nullable Map<String, String> env) {
        return WorkspaceRootFinder.findWorkspaceDir(HELD::contains, cwd, env);
    }

    @Test
    void findsTheNearestWorkspaceManifestUp() {
        assertThat(find("packages/a", null)).isEqualTo("");
        assertThat(find("tools/x", Collections.emptyMap())).isEqualTo("tools");
        assertThat(WorkspaceRootFinder.findWorkspaceDir(path -> false, "packages/a", null)).isNull();
    }

    @Test
    void theEnvironmentNamesTheWorkspaceDirectory() {
        assertThat(find("packages/a", Collections.singletonMap("NPM_CONFIG_WORKSPACE_DIR", "../../tools")))
          .isEqualTo("tools");
        assertThat(find("tools/x", Collections.singletonMap("npm_config_workspace_dir", "../..")))
          .isEqualTo("");
        assertThat(find("tools/x", Collections.singletonMap("NPM_CONFIG_WORKSPACE_DIR", ""))).isEqualTo("tools");
    }

    @Test
    void aWorkspaceDirectoryOutsideTheProject() {
        assertThatThrownBy(() -> find("", Collections.singletonMap("NPM_CONFIG_WORKSPACE_DIR", "/srv/ws")))
          .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> find("", Collections.singletonMap("NPM_CONFIG_WORKSPACE_DIR", "..")))
          .isInstanceOf(CannotKnow.class);
    }

    /**
     * root-finder's 'finds overridden workspace dir': the directory named, which need not hold a workspace manifest.
     */
    @Test
    void findsOverriddenWorkspaceDir() {
        assertThat(find("", Collections.singletonMap("NPM_CONFIG_WORKSPACE_DIR", "FAKE_PATH"))).isEqualTo("FAKE_PATH");
        assertThat(find("tools/x", Collections.singletonMap("NPM_CONFIG_WORKSPACE_DIR", "."))).isEqualTo("tools/x");
    }
}
