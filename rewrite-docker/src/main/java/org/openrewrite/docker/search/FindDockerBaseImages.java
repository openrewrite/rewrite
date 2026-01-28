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
import org.openrewrite.docker.table.DockerBaseImages;
import org.openrewrite.docker.trait.DockerImage;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDockerBaseImages extends Recipe {

    transient DockerBaseImages dockerBaseImages = new DockerBaseImages(this);

    @Option(displayName = "Image name pattern",
            description = "A glob pattern to match against base image names. If not specified, all base images are matched.",
            example = "ubuntu*",
            required = false)
    @Nullable
    String imageNamePattern;

    @Override
    public String getDisplayName() {
        return "Find Docker base images";
    }

    @Override
    public String getDescription() {
        return "Find all base images (FROM instructions) in Dockerfiles.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        DockerImage.Matcher matcher = new DockerImage.Matcher();
        if (imageNamePattern != null) {
            matcher.imageName(imageNamePattern);
        }

        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                Docker.From f = super.visitFrom(from, ctx);

                // Use DockerImage trait for accessing image components
                DockerImage image = matcher.get(getCursor()).orElse(null);
                if (image == null) {
                    return f;
                }

                String imageName = image.getImageName();
                if (imageName == null) {
                    return f;
                }

                String tag = image.getTag();
                String digest = image.getDigest();
                String platform = image.getPlatform();
                String stageName = image.getStageName();

                // Insert row into data table
                dockerBaseImages.insertRow(ctx, new DockerBaseImages.Row(
                        getCursor().firstEnclosingOrThrow(Docker.File.class).getSourcePath().toString(),
                        stageName,
                        imageName,
                        tag,
                        digest,
                        platform
                ));

                // Build message with image reference
                String message = imageName;
                if (digest != null) {
                    message += "@" + digest;
                } else if (tag != null) {
                    message += ":" + tag;
                }

                // Mark the FROM instruction as a search result
                return SearchResult.found(f, message);
            }
        };
    }
}
