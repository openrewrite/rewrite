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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

/**
 * Adds cleanup commands to apt-get RUN instructions to reduce image size.
 * <p>
 * When apt-get is used, it leaves behind cached package lists and other files
 * that are not needed at runtime. Adding cleanup commands removes these files
 * and can significantly reduce the final image size.
 * <p>
 * The cleanup commands added are:
 * <ul>
 *   <li>{@code rm -rf /var/lib/apt/lists/*} - removes cached package lists</li>
 * </ul>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddAptGetCleanup extends Recipe {

    private static final String DEFAULT_CLEANUP = " && rm -rf /var/lib/apt/lists/*";

    // Pattern to match apt-get install commands
    private static final Pattern APT_GET_INSTALL = Pattern.compile(
            "\\bapt-get\\s+(-[a-zA-Z]+\\s+)*install\\b"
    );

    // Pattern to check if cleanup is already present
    private static final Pattern CLEANUP_PATTERN = Pattern.compile(
            "rm\\s+(-[a-zA-Z]+\\s+)*/var/lib/apt/lists"
    );

    @Option(displayName = "Cleanup command",
            description = "The cleanup command to append. Defaults to ' && rm -rf /var/lib/apt/lists/*'.",
            example = " && apt-get clean && rm -rf /var/lib/apt/lists/*",
            required = false)
    @Nullable
    String cleanupCommand;

    @Override
    public String getDisplayName() {
        return "Add apt-get cleanup";
    }

    @Override
    public String getDescription() {
        return "Adds cleanup commands to apt-get RUN instructions to reduce Docker image size. " +
               "By default, adds 'rm -rf /var/lib/apt/lists/*' to remove cached package lists.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String cleanup = cleanupCommand != null ? cleanupCommand : DEFAULT_CLEANUP;

        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.Run visitRun(Docker.Run run, ExecutionContext ctx) {
                run = (Docker.Run) super.visitRun(run, ctx);

                if (!(run.getCommand() instanceof Docker.ShellForm)) {
                    return run;
                }

                Docker.ShellForm shellForm = (Docker.ShellForm) run.getCommand();
                String commandText = shellForm.getArgument().getText();

                // Check if this contains apt-get install
                Matcher installMatcher = APT_GET_INSTALL.matcher(commandText);
                if (!installMatcher.find()) {
                    return run;
                }

                // Check if cleanup is already present
                Matcher cleanupMatcher = CLEANUP_PATTERN.matcher(commandText);
                if (cleanupMatcher.find()) {
                    return run;
                }

                // Add cleanup to the command
                String newCommandText = commandText + cleanup;

                Docker.Literal newLiteral = new Docker.Literal(
                        randomId(),
                        shellForm.getArgument().getPrefix(),
                        shellForm.getArgument().getMarkers(),
                        newCommandText,
                        shellForm.getArgument().getQuoteStyle()
                );

                Docker.ShellForm newShellForm = new Docker.ShellForm(
                        randomId(),
                        shellForm.getPrefix(),
                        Markers.EMPTY,
                        newLiteral
                );

                return run.withCommand(newShellForm);
            }
        };
    }
}
