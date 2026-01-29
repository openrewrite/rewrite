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

import static org.openrewrite.Tree.randomId;

/**
 * Combines consecutive RUN instructions into a single RUN instruction.
 * <p>
 * This reduces the number of layers in the resulting Docker image, which:
 * <ul>
 *   <li>Decreases image size</li>
 *   <li>Improves build and pull performance</li>
 *   <li>Reduces the attack surface</li>
 * </ul>
 * <p>
 * Only shell form RUN instructions without flags are combined. Exec form and
 * instructions with flags (like --mount) are preserved as-is.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class CombineRunInstructions extends Recipe {

    @Option(displayName = "Separator",
            description = "The separator to use between combined commands. Defaults to ' && '.",
            example = " && \\\\\\n    ",
            required = false)
    @Nullable
    String separator;

    @Override
    public String getDisplayName() {
        return "Combine consecutive RUN instructions";
    }

    @Override
    public String getDescription() {
        return "Combines consecutive RUN instructions into a single instruction to reduce image layers. " +
                "Only shell form RUN instructions without flags are combined.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String sep = separator != null ? separator : " && ";

        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.Stage visitStage(Docker.Stage stage, ExecutionContext ctx) {
                List<Docker.Instruction> instructions = stage.getInstructions();
                List<Docker.Instruction> newInstructions = new ArrayList<>();

                List<Docker.Run> consecutiveRuns = new ArrayList<>();
                Space firstRunPrefix = null;

                for (Docker.Instruction instruction : instructions) {
                    if (instruction instanceof Docker.Run && canCombine((Docker.Run) instruction)) {
                        Docker.Run run = (Docker.Run) instruction;
                        if (consecutiveRuns.isEmpty()) {
                            firstRunPrefix = run.getPrefix();
                        }
                        consecutiveRuns.add(run);
                    } else {
                        // Flush any accumulated RUN instructions
                        if (!consecutiveRuns.isEmpty()) {
                            newInstructions.add(combineRuns(consecutiveRuns, firstRunPrefix, sep));
                            consecutiveRuns.clear();
                            firstRunPrefix = null;
                        }
                        newInstructions.add(instruction);
                    }
                }

                // Flush any remaining RUN instructions
                if (!consecutiveRuns.isEmpty()) {
                    newInstructions.add(combineRuns(consecutiveRuns, firstRunPrefix, sep));
                }

                if (newInstructions.size() == instructions.size()) {
                    // No changes made
                    return stage;
                }

                return stage.withInstructions(newInstructions);
            }

            private boolean canCombine(Docker.Run run) {
                // Only combine shell form RUN instructions
                if (!(run.getCommand() instanceof Docker.ShellForm)) {
                    return false;
                }
                // Don't combine if there are flags (like --mount, --network, etc.)
                return run.getFlags() == null || run.getFlags().isEmpty();
            }

            private Docker.Run combineRuns(List<Docker.Run> runs, @Nullable Space prefix, String sep) {
                if (runs.size() == 1) {
                    return runs.get(0);
                }

                StringBuilder combined = new StringBuilder();
                for (int i = 0; i < runs.size(); i++) {
                    Docker.Run run = runs.get(i);
                    Docker.ShellForm shellForm = (Docker.ShellForm) run.getCommand();
                    String commandText = shellForm.getArgument().getText();

                    if (i > 0) {
                        combined.append(sep);
                    }
                    combined.append(commandText);
                }

                Docker.Literal combinedLiteral = new Docker.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        combined.toString(),
                        null
                );

                Docker.ShellForm combinedShellForm = new Docker.ShellForm(
                        randomId(),
                        Space.SINGLE_SPACE,
                        Markers.EMPTY,
                        combinedLiteral
                );

                return new Docker.Run(
                        randomId(),
                        prefix != null ? prefix : runs.get(0).getPrefix(),
                        Markers.EMPTY,
                        "RUN",
                        null,
                        combinedShellForm
                );
            }
        };
    }
}
