/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.toolingapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.gradle.toolingapi.OpenRewriteModelBuilder.ForkGradleUserHome;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRewriteModelBuilderTest {

    @Test
    void forkGradleUserHomeSharesTheDefaultHomesDownloads(@TempDir Path tmp) throws IOException {
        Path defaultHome = tmp.resolve("default");
        Files.createDirectories(defaultHome.resolve("caches/modules-2"));

        Path home = ForkGradleUserHome.create(tmp.resolve("homes"), "1", defaultHome);

        assertThat(home.resolve("caches/modules-2")).isSymbolicLink();
        assertThat(Files.isSameFile(home.resolve("caches/modules-2"), defaultHome.resolve("caches/modules-2"))).isTrue();
        // The default home has downloaded no distribution yet, and the fork's first download must still land there
        assertThat(Files.isSameFile(home.resolve("wrapper/dists"), defaultHome.resolve("wrapper/dists"))).isTrue();
    }

    @Test
    void deletingAForkGradleUserHomeKeepsTheSharedDownloads(@TempDir Path tmp) throws IOException {
        Path defaultHome = tmp.resolve("default");
        Path artifact = Files.createDirectories(defaultHome.resolve("caches/modules-2/files-2.1/g/a/1")).resolve("a-1.jar");
        Files.write(artifact, new byte[]{1});
        Path home = ForkGradleUserHome.create(tmp.resolve("homes"), "1", defaultHome);
        Files.createDirectories(home.resolve("caches/9.8.2/groovy-dsl"));

        ForkGradleUserHome.delete(home);

        assertThat(home).doesNotExist();
        assertThat(artifact).exists();
    }
}
