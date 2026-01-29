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
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

/**
 * Converts shell form ENTRYPOINT and CMD instructions to exec form (JSON array).
 * <p>
 * Exec form is preferred because:
 * <ul>
 *   <li>It does not invoke a command shell, avoiding shell string munging</li>
 *   <li>The command runs as PID 1 and properly receives Unix signals (like SIGTERM)</li>
 *   <li>Docker can properly stop the container on `docker stop`</li>
 * </ul>
 * <p>
 * Shell form wraps the command in {@code /bin/sh -c "command"}, which can cause:
 * <ul>
 *   <li>The shell process (not your application) receives signals</li>
 *   <li>The application may not shut down gracefully</li>
 *   <li>Environment variable substitution behavior differences</li>
 * </ul>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UseExecFormEntrypoint extends Recipe {

    // Pattern to split command into tokens, respecting quoted strings
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\"([^\"]*)\"|'([^']*)'|(\\S+)"
    );

    @Option(displayName = "Convert ENTRYPOINT",
            description = "Whether to convert ENTRYPOINT instructions. Defaults to true.",
            required = false)
    @Nullable
    Boolean convertEntrypoint;

    @Option(displayName = "Convert CMD",
            description = "Whether to convert CMD instructions. Defaults to true.",
            required = false)
    @Nullable
    Boolean convertCmd;

    @Override
    public String getDisplayName() {
        return "Use exec form for ENTRYPOINT and CMD";
    }

    @Override
    public String getDescription() {
        return "Converts shell form ENTRYPOINT and CMD instructions to exec form (JSON array). " +
                "Exec form is preferred because it runs the command as PID 1, allowing it to receive " +
                "Unix signals properly. Shell form wraps commands in '/bin/sh -c' which can cause signal handling issues.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean doEntrypoint = convertEntrypoint == null || convertEntrypoint;
        boolean doCmd = convertCmd == null || convertCmd;

        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.Entrypoint visitEntrypoint(Docker.Entrypoint entrypoint, ExecutionContext ctx) {
                if (!doEntrypoint) {
                    return entrypoint;
                }

                if (!(entrypoint.getCommand() instanceof Docker.ShellForm)) {
                    return entrypoint;
                }

                Docker.ShellForm shellForm = (Docker.ShellForm) entrypoint.getCommand();
                Docker.ExecForm execForm = convertToExecForm(shellForm);

                return entrypoint.withCommand(execForm);
            }

            @Override
            public Docker.Cmd visitCmd(Docker.Cmd cmd, ExecutionContext ctx) {
                if (!doCmd) {
                    return cmd;
                }

                if (!(cmd.getCommand() instanceof Docker.ShellForm)) {
                    return cmd;
                }

                Docker.ShellForm shellForm = (Docker.ShellForm) cmd.getCommand();
                Docker.ExecForm execForm = convertToExecForm(shellForm);

                return cmd.withCommand(execForm);
            }

            private Docker.ExecForm convertToExecForm(Docker.ShellForm shellForm) {
                String commandText = shellForm.getArgument().getText();
                List<String> tokens = tokenize(commandText);

                List<Docker.Literal> arguments = new ArrayList<>();
                for (int i = 0; i < tokens.size(); i++) {
                    Space prefix = i == 0 ? Space.EMPTY : Space.SINGLE_SPACE;
                    arguments.add(new Docker.Literal(
                            randomId(),
                            prefix,
                            Markers.EMPTY,
                            tokens.get(i),
                            Docker.Literal.QuoteStyle.DOUBLE
                    ));
                }

                return new Docker.ExecForm(
                        randomId(),
                        shellForm.getPrefix(),
                        Markers.EMPTY,
                        arguments,
                        Space.EMPTY
                );
            }

            private List<String> tokenize(String command) {
                List<String> tokens = new ArrayList<>();
                Matcher matcher = TOKEN_PATTERN.matcher(command.trim());

                while (matcher.find()) {
                    // Group 1: double-quoted string content
                    // Group 2: single-quoted string content
                    // Group 3: unquoted token
                    if (matcher.group(1) != null) {
                        tokens.add(matcher.group(1));
                    } else if (matcher.group(2) != null) {
                        tokens.add(matcher.group(2));
                    } else if (matcher.group(3) != null) {
                        tokens.add(matcher.group(3));
                    }
                }

                return tokens;
            }
        };
    }
}
