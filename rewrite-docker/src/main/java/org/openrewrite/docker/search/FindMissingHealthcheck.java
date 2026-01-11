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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.SearchResult;

/**
 * Finds Dockerfiles where the final stage is missing a HEALTHCHECK instruction.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindMissingHealthcheck extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find missing HEALTHCHECK";
    }

    @Override
    public String getDescription() {
        return "Finds Dockerfiles where the final stage is missing a HEALTHCHECK instruction. " +
               "Health checks help container orchestrators determine if a container is healthy and ready to receive traffic.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {

            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                Docker.File f = super.visitFile(file, ctx);

                if (f.getStages().isEmpty()) {
                    return f;
                }

                // Check final stage for missing HEALTHCHECK
                Docker.Stage finalStage = f.getStages().get(f.getStages().size() - 1);

                boolean hasHealthcheck = finalStage.getInstructions().stream()
                        .anyMatch(inst -> inst instanceof Docker.Healthcheck);

                if (!hasHealthcheck) {
                    // Mark the FROM instruction of the final stage
                    f = f.withStages(ListUtils.mapLast(f.getStages(), stage ->
                            stage.withFrom(SearchResult.found(stage.getFrom(), "Missing HEALTHCHECK instruction"))));
                }

                return f;
            }
        };
    }
}
