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
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessWorkingDirectoryTest {

    @Test
    void csprojServerRunsFromItsProjectDirectory(@TempDir Path tempDir) {
        Path csproj = tempDir.resolve("tool/OpenRewrite.Tool.csproj");

        assertThat(CSharpRewriteRpc.Builder.processWorkingDirectory(csproj, tempDir.resolve("repo")))
                .isEqualTo(tempDir.resolve("tool").toAbsolutePath().normalize());
    }

    @Test
    void assemblyServerRunsFromTheRequestedDirectory(@TempDir Path tempDir) {
        Path repo = tempDir.resolve("repo");

        assertThat(CSharpRewriteRpc.Builder.processWorkingDirectory(tempDir.resolve("tool/OpenRewrite.Tool.dll"), repo))
                .isEqualTo(repo);
    }

    @Test
    void installedToolRunsFromTheRequestedDirectory(@TempDir Path tempDir) {
        Path repo = tempDir.resolve("repo");

        assertThat(CSharpRewriteRpc.Builder.processWorkingDirectory(null, repo)).isEqualTo(repo);
    }
}
