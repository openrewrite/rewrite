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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * Adds a USER instruction to a Dockerfile to run as a non-root user.
 * This helps comply with CIS Docker Benchmark 4.1 and addresses findings
 * from {@link org.openrewrite.docker.search.FindRootUser}.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddUserInstruction extends Recipe {

    @Option(displayName = "User name",
            description = "The username to run as.",
            example = "appuser")
    String userName;

    @Option(displayName = "Group name",
            description = "The group name. If specified, the USER instruction will be `USER user:group`.",
            example = "appgroup",
            required = false)
    @Nullable
    String groupName;

    @Option(displayName = "Stage name",
            description = "Only add the USER instruction to this build stage. If null, adds to the final stage only.",
            example = "final",
            required = false)
    @Nullable
    String stageName;

    @Option(displayName = "Skip if user exists",
            description = "If true (default), skip adding USER if the stage already has a USER instruction. " +
                    "If false, always add the USER instruction at the end of the stage.",
            required = false)
    @Nullable
    Boolean skipIfUserExists;

    @Override
    public String getDisplayName() {
        return "Add USER instruction";
    }

    @Override
    public String getDescription() {
        return "Adds a USER instruction to run the container as a non-root user. " +
                "By default, adds to the final stage only and skips if a USER instruction already exists. " +
                "This helps comply with CIS Docker Benchmark 4.1.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean skip = skipIfUserExists == null || skipIfUserExists;

        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.Stage visitStage(Docker.Stage stage, ExecutionContext ctx) {
                if (!shouldModifyStage(stage, ctx)) {
                    return stage;
                }

                if (skip && hasUserInstruction(stage)) {
                    return stage;
                }

                // Avoid infinite loop: don't add if our target user is already the last instruction
                if (hasTargetUserAsLastInstruction(stage)) {
                    return stage;
                }

                return addUserInstruction(stage);
            }

            private boolean shouldModifyStage(Docker.Stage stage, ExecutionContext ctx) {
                if (stageName != null) {
                    Docker.From.As as = stage.getFrom().getAs();
                    return stageName.equals(as != null ? as.getName().getText() : null);
                }
                // Default: only modify final stage
                List<Docker.Stage> stages = getCursor().getParentTreeCursor().<Docker.File>getValue().getStages();
                return stage == stages.get(stages.size() - 1);
            }

            private boolean hasUserInstruction(Docker.Stage stage) {
                return stage.getInstructions().stream()
                        .anyMatch(inst -> inst instanceof Docker.User);
            }

            private boolean hasTargetUserAsLastInstruction(Docker.Stage stage) {
                // Check if the last instruction is already a USER instruction with our target user
                List<Docker.Instruction> instructions = stage.getInstructions();
                if (instructions.isEmpty()) {
                    return false;
                }
                Docker.Instruction lastInst = instructions.get(instructions.size() - 1);
                if (!(lastInst instanceof Docker.User)) {
                    return false;
                }
                Docker.User lastUser = (Docker.User) lastInst;
                String lastUserName = extractText(lastUser.getUser());
                String lastGroupName = extractText(lastUser.getGroup());

                boolean userMatches = userName.equals(lastUserName);
                boolean groupMatches = (groupName == null || groupName.isEmpty()) ?
                        (lastGroupName == null || lastGroupName.isEmpty()) :
                        groupName.equals(lastGroupName);

                return userMatches && groupMatches;
            }

            private @Nullable String extractText(Docker.@Nullable Argument arg) {
                if (arg == null) {
                    return null;
                }
                StringBuilder builder = new StringBuilder();
                for (Docker.ArgumentContent content : arg.getContents()) {
                    if (content instanceof Docker.Literal) {
                        builder.append(((Docker.Literal) content).getText());
                    }
                }
                return builder.toString();
            }

            private Docker.Stage addUserInstruction(Docker.Stage stage) {
                Docker.User userInstruction = createUserInstruction();
                return stage.withInstructions(ListUtils.concat(stage.getInstructions(), userInstruction));
            }

            private Docker.User createUserInstruction() {
                Docker.Argument userArg = createArgument(userName, Space.SINGLE_SPACE);

                Docker.Argument groupArg = null;
                if (groupName != null && !groupName.isEmpty()) {
                    // Group argument has no space prefix; the colon is added by the printer
                    groupArg = createArgument(groupName, Space.EMPTY);
                }

                return new Docker.User(
                        randomId(),
                        Space.format("\n"),
                        Markers.EMPTY,
                        "USER",
                        userArg,
                        groupArg
                );
            }

            private Docker.Argument createArgument(String text, Space prefix) {
                Docker.ArgumentContent content = new Docker.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        text,
                        null
                );
                return new Docker.Argument(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        singletonList(content)
                );
            }
        };
    }
}
