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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class FileAttributesTest {

    @Test
    void fromPathAssignsTimestampsInConstructorOrder(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("hello.txt"), "hello");
        FileTime modified = FileTime.from(Instant.parse("2020-01-02T03:04:05Z"));
        FileTime accessed = FileTime.from(Instant.parse("2021-06-07T08:09:10Z"));
        requireNonNull(Files.getFileAttributeView(file, BasicFileAttributeView.class)).setTimes(modified, accessed, null);

        FileAttributes attributes = requireNonNull(FileAttributes.fromPath(file));

        assertThat(requireNonNull(attributes.getLastModifiedTime()).toInstant()).isEqualTo(modified.toInstant());
        assertThat(requireNonNull(attributes.getLastAccessTime()).toInstant()).isEqualTo(accessed.toInstant());
    }
}
