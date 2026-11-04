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

public class EolDockerImages extends DataTable<EolDockerImages.Row> {

    public EolDockerImages(Recipe recipe) {
        super(recipe,
                "End-of-life Docker images",
                "Records Docker base images that have reached end-of-life.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The Dockerfile containing the EOL base image.")
        String sourceFile;

        @Column(displayName = "Stage name",
                description = "The build stage name (from AS clause), if specified.")
        @Nullable
        String stageName;

        @Column(displayName = "Image name",
                description = "The name of the base image.")
        String imageName;

        @Column(displayName = "Tag",
                description = "The image tag.")
        String tag;

        @Column(displayName = "EOL date",
                description = "The date when the image reached end-of-life.")
        String eolDate;

        @Column(displayName = "Suggested replacement",
                description = "Recommended newer version to migrate to.")
        String suggestedReplacement;
    }
}
