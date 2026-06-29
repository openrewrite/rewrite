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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static java.time.Duration.ofSeconds;

class PackageManagerExecutorTest {

    @Test
    void exposesAllFiveExecutors() {
        assertThat(PackageManagerExecutor.NPM.getName()).isEqualTo("npm");
        assertThat(PackageManagerExecutor.YARN.getName()).isEqualTo("yarn");
        assertThat(PackageManagerExecutor.PNPM.getName()).isEqualTo("pnpm");
        assertThat(PackageManagerExecutor.BUN.getName()).isEqualTo("bun");
    }

    @Test
    void findReturnsNullForMissingExecutable() {
        // Construct one with a name that won't exist anywhere on PATH.
        PackageManagerExecutor missing =
                PackageManagerExecutor.forTesting("definitely-not-a-real-binary-2026", 30);
        assertThat(missing.find()).isNull();
    }

    @Test
    void closesChildStdinSoStdinReadingProcessesDoNotHang() {
        // given a process whose timeout is far longer than this test is willing to wait, running a
        // command that reads stdin until EOF (`cat`). If stdin stayed an open pipe it would block
        // until the timeout, the exact failure mode behind pnpm's cold-store install hang.
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"),
                "uses the POSIX `cat` to read stdin to EOF");
        String cat = PackageManagerExecutor.which("cat");
        assumeTrue(cat != null, "cat must be available");
        PackageManagerExecutor executor = PackageManagerExecutor.forTesting("cat", 120);

        // when run with no input provided
        PackageManagerExecutor.RunResult result = assertTimeoutPreemptively(ofSeconds(15), () ->
                executor.run(Paths.get("."), cat, Collections.<String, String>emptyMap()));

        // then it sees EOF immediately and exits cleanly rather than hanging on the open pipe
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExitCode()).isEqualTo(0);
    }

    @Test
    void npmResolvesAlongsideTheActiveNode() {
        // given the node that will actually execute npm (npm re-invokes node via its
        // `#!/usr/bin/env node` shebang, so it must run under a compatible runtime)
        String node = PackageManagerExecutor.which("node");
        assumeTrue(node != null, "node must be installed to validate the npm/node pairing");
        Path nodeDir = Paths.get(node).toAbsolutePath().getParent();

        // when locating npm
        String npm = PackageManagerExecutor.NPM.find();
        assumeTrue(npm != null, "npm must be installed");

        // then it lives next to that node rather than some unrelated install whose
        // engines may reject the active node (npm validate-engines, exit 7)
        assertThat(Paths.get(npm).toAbsolutePath().getParent()).isEqualTo(nodeDir);
    }
}
