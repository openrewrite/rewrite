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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GoRpcSourceExtractorTest {

    @Test
    void extractsBundledSourceTree(@TempDir Path target) throws IOException {
        // when
        GoRpcSourceExtractor.extractTo(target);

        // then
        assertThat(target.resolve("go.mod")).isRegularFile();
        assertThat(target.resolve("go.sum")).isRegularFile();
        assertThat(target.resolve("cmd/rpc/main.go")).isRegularFile();
        assertThat(target.resolve("pkg/rpc")).isDirectory();
        assertThat(target.resolve("pkg/parser")).isDirectory();
        assertThat(target.resolve("vendor")).isDirectory();
    }

    @Test
    void bundledTreeContainsNoTestSources(@TempDir Path target) throws IOException {
        // given
        GoRpcSourceExtractor.extractTo(target);

        // when / then
        try (Stream<Path> walk = Files.walk(target)) {
            assertThat(walk
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith("_test.go")))
                    .as("bundled Go sources must not include *_test.go")
                    .isEmpty();
        }
    }

    @Test
    void extractIsIdempotent(@TempDir Path target) throws IOException {
        // given
        GoRpcSourceExtractor.extractTo(target);
        long firstCount;
        try (Stream<Path> walk = Files.walk(target)) {
            firstCount = walk.filter(Files::isRegularFile).count();
        }

        // when
        GoRpcSourceExtractor.extractTo(target);

        // then
        try (Stream<Path> walk = Files.walk(target)) {
            assertThat(walk.filter(Files::isRegularFile).count()).isEqualTo(firstCount);
        }
    }
}
