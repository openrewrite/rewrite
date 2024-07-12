/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.shell.exec;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Incubating(since = "8.30.0")
@SuppressWarnings("unused")
public interface ShellExecutor {

    @SuppressWarnings("unused")
    default void init() {
    }

    @SuppressWarnings("unused")
    default Path exec(List<String> command, Path workingDirectory, Map<String, String> environment, ExecutionContext ctx) {
        return exec(command, workingDirectory, environment, Duration.ofMinutes(5), ctx);
    }

    default Path exec(List<String> command, Path workingDirectory, Map<String, String> environment, Duration timeout, ExecutionContext ctx) {
        Path stdOut = null, stdErr = null;
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);
            builder.directory(workingDirectory.toFile());
            builder.environment().putAll(environment);

            stdOut = Files.createTempFile(WorkingDirectoryExecutionContextView.view(ctx).getWorkingDirectory(), "shell",
                    null);
            stdErr = Files.createTempFile(WorkingDirectoryExecutionContextView.view(ctx).getWorkingDirectory(), "shell",
                    null);
            builder.redirectOutput(ProcessBuilder.Redirect.to(stdOut.toFile()));
            builder.redirectError(ProcessBuilder.Redirect.to(stdErr.toFile()));
            Process process = builder.start();

            process.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                String error = "Command failed:" + String.join(" ", command);
                if (Files.exists(stdErr)) {
                    error += "\n" + new String(Files.readAllBytes(stdErr));
                }
                throw new RuntimeException(error);
            }
            return stdOut;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (stdOut != null) {
                // noinspection ResultOfMethodCallIgnored
                stdOut.toFile().delete();
            }
            if (stdErr != null) {
                // noinspection ResultOfMethodCallIgnored
                stdErr.toFile().delete();
            }
        }
    }

    @SuppressWarnings("unused")
    default void postExec() {
    }
}