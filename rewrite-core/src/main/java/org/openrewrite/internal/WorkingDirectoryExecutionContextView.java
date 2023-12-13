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
package org.openrewrite.internal;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class WorkingDirectoryExecutionContextView extends DelegatingExecutionContext {

    private WorkingDirectoryExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static WorkingDirectoryExecutionContextView view(ExecutionContext ctx) {
        return new WorkingDirectoryExecutionContextView(ctx);
    }

    public void setRoot(Path path) {
        if (getRoot().isPresent()) {
            throw new IllegalStateException("Working directory root already set");
        }
        putMessage(WORKING_DIRECTORY_ROOT, path);
    }

    public void setWorkingDirectory(String directory) {
        putMessage(WORKING_DIRECTORY, directory);
    }

    public void delete() {
        getRoot().ifPresent(WorkingDirectoryExecutionContextView::delete);
    }

    private Optional<Path> getRoot() {
        return Optional.ofNullable(getMessage(WORKING_DIRECTORY_ROOT));
    }

    private static void delete(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> files = Files.list(path)) {
                    files.forEach(WorkingDirectoryExecutionContextView::delete);
                }
            }
            Files.delete(path);
        } catch (IOException ignore) {
        }
    }
}
