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
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * workspace/projects-reader/test/findPackages.ts of pnpm 11.2.2, over its fixtures' package.json directories. Not
 * ported: the package.yaml and package.json5 case (the host gives package.json files).
 */
class FindPackagesTest {

    private static final List<String> ONE_PKG = singletonList("foo");
    private static final List<String> MANY_PKGS = asList("components/component-1", "components/component-2",
      "libs/foo");
    private static final List<String> MANY_PKGS_2 = asList("", "components/component-1", "components/component-2",
      "libs/foo");

    @Test
    void findsPackage() {
        assertThat(FindPackages.findPackages(ONE_PKG, null, null, false)).containsExactly("foo");
    }

    @Test
    void findsPackagesByPatterns() {
        assertThat(FindPackages.findPackages(MANY_PKGS, singletonList("components/**"), null, false))
          .containsExactly("components/component-1", "components/component-2");
    }

    @Test
    void findsPackagesByStarPattern() {
        assertThat(FindPackages.findPackages(MANY_PKGS_2, asList(".", "components/*"), null, false))
          .containsExactly("", "components/component-1", "components/component-2");
    }

    @Test
    void findsPackagesByDefaultPattern() {
        assertThat(FindPackages.findPackages(MANY_PKGS_2, null, null, false))
          .containsExactly("", "components/component-1", "components/component-2", "libs/foo");
    }

    @Test
    void ignorePackagesByPatterns() {
        assertThat(FindPackages.findPackages(MANY_PKGS, asList("**", "!libs/**"), null, false))
          .containsExactly("components/component-1", "components/component-2");
    }

    /**
     * pnpm finds all three: an absolute ignore pattern matches nothing unless the project sits under it, and the
     * project's absolute path is not an input.
     */
    @Test
    void ignorePackagesByPatternsWithStartsWithBangSlash() {
        assertThatThrownBy(() -> FindPackages.findPackages(MANY_PKGS, asList("**", "!/libs/**"), null, false))
          .isInstanceOf(CannotKnow.class);
    }

    @Test
    void theWorkspaceRootIsAProjectOnlyWithItsOwnManifest() {
        assertThat(FindPackages.findWorkspaceProjects(MANY_PKGS, singletonList("components/*")))
          .containsExactly("components/component-1", "components/component-2");
        assertThat(FindPackages.findWorkspaceProjects(MANY_PKGS_2, singletonList("components/*")))
          .containsExactly("", "components/component-1", "components/component-2");
    }
}
