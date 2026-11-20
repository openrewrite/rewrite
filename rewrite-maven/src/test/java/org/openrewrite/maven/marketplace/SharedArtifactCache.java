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
package org.openrewrite.maven.marketplace;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.util.Comparator.reverseOrder;

/**
 * The recipe jars and dependencies these tests download, kept in one directory for all the test classes of a JVM
 * rather than downloaded again into each test's own.
 */
final class SharedArtifactCache {
    private static @Nullable LocalMavenArtifactCache cache;

    private SharedArtifactCache() {
    }

    static synchronized LocalMavenArtifactCache get() {
        if (cache == null) {
            try {
                Path dir = Files.createTempDirectory("rewrite-maven-test-artifacts");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> delete(dir)));
                cache = new LocalMavenArtifactCache(dir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return cache;
    }

    private static void delete(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(reverseOrder()).forEach(file -> file.toFile().delete());
        } catch (IOException | UncheckedIOException ignored) {
            // Best effort as the JVM exits, like File.deleteOnExit
        }
    }
}
