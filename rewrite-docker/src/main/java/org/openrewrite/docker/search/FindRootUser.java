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
package org.openrewrite.docker.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.SearchResult;

/**
 * Finds containers that run as root, either explicitly via USER root/0
 * or implicitly by not having a USER instruction in the final stage.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindRootUser extends Recipe {

    @Option(displayName = "Include missing `USER`",
            description = "When true, also marks the final stage if no USER instruction is present (defaults to root). " +
                    "When false, only marks explicit `USER root/0` instructions.",
            required = false)
    @Nullable
    Boolean includeMissingUser;

    @Override
    public String getDisplayName() {
        return "Find containers running as root";
    }

    @Override
    public String getDescription() {
        return "Finds containers that run as root user (CIS Docker Benchmark 4.1). " +
                "This includes explicit `USER root` or `USER 0` instructions, " +
                "and optionally containers with no `USER` instruction in the final stage (which default to root).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean checkMissing = includeMissingUser == null || includeMissingUser;

        return new DockerIsoVisitor<ExecutionContext>() {

            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                Docker.File f = super.visitFile(file, ctx);

                if (!checkMissing || f.getStages().isEmpty()) {
                    return f;
                }

                // Check final stage for missing USER instruction
                Docker.Stage finalStage = f.getStages().get(f.getStages().size() - 1);

                boolean hasUserInstruction = finalStage.getInstructions().stream()
                        .anyMatch(inst -> inst instanceof Docker.User);

                if (!hasUserInstruction) {
                    // Mark the FROM instruction of the final stage
                    f = f.withStages(ListUtils.mapLast(f.getStages(), stage ->
                            stage.withFrom(SearchResult.found(stage.getFrom(), "No USER instruction, runs as root"))));
                }

                return f;
            }

            @Override
            public Docker.User visitUser(Docker.User user, ExecutionContext ctx) {
                Docker.User u = super.visitUser(user, ctx);

                String userName = extractText(u.getUser());
                if ("root".equals(userName) || "0".equals(userName)) {
                    return SearchResult.found(u, "Explicitly runs as root");
                }

                return u;
            }

            private @Nullable String extractText(Docker.@Nullable Argument arg) {
                if (arg == null) {
                    return null;
                }
                StringBuilder builder = new StringBuilder();
                for (Docker.ArgumentContent content : arg.getContents()) {
                    if (content instanceof Docker.Literal) {
                        builder.append(((Docker.Literal) content).getText());
                    } else if (content instanceof Docker.EnvironmentVariable) {
                        return null;
                    }
                }
                return builder.toString();
            }
        };
    }
}
