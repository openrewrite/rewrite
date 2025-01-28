/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.remote;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.marker.Markers;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A remote resource that can be fetched from an {@link java.io.InputStream} which
 * could be, for example, a classpath resource.
 */
@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class RemoteResource implements Remote {
    @EqualsAndHashCode.Include
    UUID id;

    Path sourcePath;
    Markers markers;
    InputStream inputStream;

    @Nullable
    Charset charset;

    boolean charsetBomMarked;

    @Nullable
    FileAttributes fileAttributes;

    @Language("markdown")
    @Nullable
    String description;

    @Nullable
    Checksum checksum;

    /**
     * Note that this method can only be called once, consuming the
     * {@link InputStream} in the process.
     *
     * @param ctx Unused in this implementation of {@link Remote}.
     * @return The data of the file.
     */
    @Override
    public InputStream getInputStream(ExecutionContext ctx) {
        return inputStream;
    }
}
