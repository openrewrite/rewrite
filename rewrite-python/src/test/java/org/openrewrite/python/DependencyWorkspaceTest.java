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
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.openrewrite.python.internal.PackageManagerExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DependencyWorkspaceTest {

    @Test
    void rebuildsWorkspaceWhenStaleDirectoryOccupiesTarget() throws IOException {
        assumeTrue(PackageManagerExecutor.UV.find() != null, "uv is not installed");

        String requirements = "six==1.17.0\n";
        Path workspace = DependencyWorkspace.getOrCreateRequirementsWorkspace(requirements, null);
        assumeTrue(workspace != null, "could not create workspace (uv install failed?)");

        // Simulate macOS periodic tmp cleanup, which deletes files older than three
        // days from $TMPDIR but leaves the directory skeleton (including .venv) in
        // place. The leftover directory occupies the path the rebuild moves into.
        Files.deleteIfExists(workspace.resolve("freeze.txt"));
        Files.deleteIfExists(workspace.resolve("version.txt"));
        // A fresh JVM would not have the (now invalid) workspace in the in-memory cache.
        DependencyWorkspace.clearCache();

        Path rebuilt = DependencyWorkspace.getOrCreateRequirementsWorkspace(requirements, null);
        assertThat(rebuilt).isNotNull();
        assertThat(rebuilt.resolve("freeze.txt")).exists();
        assertThat(rebuilt.resolve("version.txt")).exists();
    }
}
