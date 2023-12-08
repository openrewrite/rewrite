/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.internal.lang.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class WorkingDirectoryExecutionContextView extends DelegatingExecutionContext implements AutoCloseable {
    @Nullable
    private final Path existing;

    private WorkingDirectoryExecutionContextView(ExecutionContext delegate, @Nullable Path existing) {
        super(delegate);
        this.existing = existing;
        try {
            if (existing != null) {
                if (!Files.isDirectory(existing)) {
                    Files.createDirectories(existing);
                }
                putMessage(WORKING_DIRECTORY, existing);
            } else {
                putMessage(WORKING_DIRECTORY, Files.createTempDirectory("recipe-wd"));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a new {@link WorkingDirectoryExecutionContextView} which will create a new temporary directory for the
     * working directory. This temporary working directory will automatically get deleted once this object is closed.
     * <p>
     * For recipes which only want to access the provided working directory, use {@link #getWorkingDirectory(ExecutionContext)}.
     */
    public static WorkingDirectoryExecutionContextView createNew(ExecutionContext ctx) {
        if (ctx instanceof WorkingDirectoryExecutionContextView) {
            return (WorkingDirectoryExecutionContextView) ctx;
        }
        return new WorkingDirectoryExecutionContextView(ctx, null);
    }

    /**
     * Create a new {@link WorkingDirectoryExecutionContextView} using a provided working directory. While the directory
     * will get created if it doesn't already exist, it is the responsibility of the caller to delete it. Thus, the
     * {@link #close()} method is a no-op.
     * <p>
     * For recipes which only want to access the provided working directory, use {@link #getWorkingDirectory(ExecutionContext)}.
     */
    public static WorkingDirectoryExecutionContextView useProvided(ExecutionContext ctx, Path existing) {
        if (ctx instanceof WorkingDirectoryExecutionContextView) {
            return (WorkingDirectoryExecutionContextView) ctx;
        }
        return new WorkingDirectoryExecutionContextView(ctx, existing);
    }

    public static Path getWorkingDirectory(ExecutionContext ctx) {
        return Optional.ofNullable(ctx.<Path>getMessage(WORKING_DIRECTORY))
                .orElseThrow(() -> new IllegalStateException("Working directory not set"));
    }

    @Override
    public void close() {
        if (existing == null) {
            delete(Objects.requireNonNull(getMessage(WORKING_DIRECTORY)));
        }
    }

    private void delete(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> files = Files.list(path)) {
                    files.forEach(this::delete);
                }
            }
            Files.delete(path);
        } catch (IOException ignore) {
        }
    }
}
