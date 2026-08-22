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
package org.openrewrite.docker.table;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class StageDependencies extends DataTable<StageDependencies.Row> {

    public StageDependencies(Recipe recipe) {
        super(recipe,
                "Docker build stage dependencies",
                "Records which build stages of a Dockerfile depend on which others, and which of them the image being built needs.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The Dockerfile containing the build stage.")
        String sourcePath;

        @Column(displayName = "Stage name",
                description = "The build stage name (from the `AS` clause), if it has one.")
        @Nullable
        String stageName;

        @Column(displayName = "Stage index",
                description = "The position of the stage in the file, counting from zero, as `--from=<index>` counts it.")
        int stageIndex;

        @Column(displayName = "Base image",
                description = "What the stage's `FROM` names: an image reference, or the earlier stage it extends.")
        String baseImage;

        @Column(displayName = "Registry",
                description = "The registry the base image is pulled from, `docker.io` when the image name does not " +
                              "name one, and empty where the stage extends another stage rather than an image.")
        String registry;

        @Column(displayName = "Referenced by",
                description = "The stages that name this one, comma separated, by stage name where they have one and otherwise by `#index`.")
        String referencedBy;

        @Column(displayName = "Build target",
                description = "Whether something in the repository asks for this stage by name, through a " +
                              "`docker build --target`, a `docker-bake.hcl`, a compose file, a CI workflow, or a " +
                              "comment in the Dockerfile showing how to build it.")
        boolean buildTarget;

        @Column(displayName = "Reachable",
                description = "Whether anything builds this stage, counting both what the file reaches and what the " +
                              "repository names as a build target. A stage in a file holding a reference that cannot " +
                              "be resolved, such as `COPY --from=$BUILDER` or `COPY --from=0`, is reported as " +
                              "reachable because nothing about that file can be concluded.")
        boolean reachable;
    }
}
