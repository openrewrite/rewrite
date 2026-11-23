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
package org.openrewrite.golang.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnOs(OS.WINDOWS)
class GoExecutorTest {

    private static final String TOOLCHAIN = "/opt/hostedtoolcache/go/1.25.12/x64";

    private static Predicate<Path> installedAt(String... paths) {
        Set<String> installed = Set.of(paths);
        return path -> installed.contains(path.toString());
    }

    @Test
    void pathBeatsWellKnownInstallLocations() {
        // The GitHub Actions layout that broke Go recipe doc generation: the image's default Go is
        // symlinked into /usr/bin, and actions/setup-go prepends a newer toolchain to PATH.
        assertThat(GoExecutor.GO.find(null, TOOLCHAIN + "/bin:/usr/local/bin:/usr/bin",
          installedAt(TOOLCHAIN + "/bin/go", "/usr/bin/go")))
          .isEqualTo(TOOLCHAIN + "/bin/go");
    }

    @Test
    void explicitGoRootBeatsPath() {
        assertThat(GoExecutor.GO.find(TOOLCHAIN, "/usr/bin",
          installedAt(TOOLCHAIN + "/bin/go", "/usr/bin/go")))
          .isEqualTo(TOOLCHAIN + "/bin/go");
    }

    @Test
    void wellKnownInstallLocationsAreATailFallback() {
        assertThat(GoExecutor.GO.find(null, "/usr/local/sbin", installedAt("/usr/local/go/bin/go")))
          .isEqualTo("/usr/local/go/bin/go");
    }

    @Test
    void blankGoRootAndEmptyPathEntriesAreSkipped() {
        assertThat(GoExecutor.GO.find("  ", "::" + TOOLCHAIN + "/bin:", installedAt(TOOLCHAIN + "/bin/go")))
          .isEqualTo(TOOLCHAIN + "/bin/go");
    }

    @Test
    void nullWhenGoIsNotInstalled() {
        assertThat(GoExecutor.GO.find(null, "/usr/bin", installedAt())).isNull();
    }
}
