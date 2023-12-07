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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class WorkingDirectoryExecutionContextView extends DelegatingExecutionContext implements AutoCloseable {
    private static final String WORKING_DIRECTORY = "org.openrewrite.workingDirectory";

    public WorkingDirectoryExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static WorkingDirectoryExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof WorkingDirectoryExecutionContextView) {
            return (WorkingDirectoryExecutionContextView) ctx;
        }
        return new WorkingDirectoryExecutionContextView(ctx);
    }

    public WorkingDirectoryExecutionContextView setWorkingDirectory(Path directory) {
        putMessage(WORKING_DIRECTORY, directory);
        return this;
    }

    public Path getWorkingDirectory() {
        Path workingDirectory = getMessage(WORKING_DIRECTORY);
        if (workingDirectory == null) {
            try {
                workingDirectory = Files.createTempDirectory("recipe-execution-dir");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return workingDirectory;
    }

    public void deleteWorkingDirectory() {
        delete(getWorkingDirectory());
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

    @Override
    public void close() {
        deleteWorkingDirectory();
    }
}
