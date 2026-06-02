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
import org.openrewrite.docker.table.EolDockerImages;
import org.openrewrite.docker.trait.DockerFrom;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.marker.SearchResult;

import java.time.LocalDate;

/**
 * Finds Docker base images that have reached end-of-life.
 * <p>
 * Using EOL base images poses security risks as they no longer receive security patches.
 * This recipe identifies common EOL images including Debian, Ubuntu, Alpine, Python,
 * and Node.js base images.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindEndOfLifeImages extends Recipe {

    transient EolDockerImages eolImages = new EolDockerImages(this);

    @Option(displayName = "Include approaching EOL",
            description = "If true, also flag images that will reach EOL within the next 6 months.",
            example = "true",
            required = false)
    @Nullable
    Boolean includeApproaching;

    @Override
    public String getDisplayName() {
        return "Find end-of-life Docker base images";
    }

    @Override
    public String getDescription() {
        return "Identifies Docker base images that have reached end-of-life. " +
                "Using EOL images poses security risks as they no longer receive security updates. " +
                "Detected images include EOL versions of Debian, Ubuntu, Alpine, Python, and Node.js.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean checkApproaching = Boolean.TRUE.equals(includeApproaching);
        LocalDate cutoffDate = checkApproaching ?
                LocalDate.now().plusMonths(6) : LocalDate.now();

        return new DockerFrom.Matcher()
                .excludeScratch()
                .asVisitor((image, ctx) -> {
                    String imageName = image.getImageName();
                    String tag = image.getTag();
                    Docker.From from = image.getTree();

                    if (imageName == null || tag == null) {
                        return from;
                    }

                    EolImage eol = EolImage.findMatch(imageName, tag);
                    if (eol == null) {
                        return from;
                    }

                    // Check if EOL date is before cutoff
                    if (eol.getEolDate().isAfter(cutoffDate)) {
                        return from;
                    }

                    String stageName = image.getStageName();

                    // Record in data table
                    eolImages.insertRow(ctx, new EolDockerImages.Row(
                            image.getCursor().firstEnclosingOrThrow(Docker.File.class)
                                    .getSourcePath().toString(),
                            stageName,
                            imageName,
                            tag,
                            eol.getEolDate().toString(),
                            eol.getSuggestedReplacement()
                    ));

                    // Build search result message
                    String message = String.format("EOL: %s:%s (ended %s, suggest %s)",
                            imageName, tag, eol.getEolDate(), eol.getSuggestedReplacement());

                    return SearchResult.found(from, message);
                });
    }
}
