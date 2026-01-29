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
import org.openrewrite.docker.trait.DockerImage;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.marker.SearchResult;

/**
 * Finds FROM instructions that use unpinned base images (either no tag or the 'latest' tag).
 * Images pinned by digest are considered acceptable.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindUnpinnedBaseImages extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find unpinned base images";
    }

    @Override
    public String getDescription() {
        return "Finds FROM instructions that use unpinned base images. " +
                "Images without an explicit tag default to 'latest', which is not reproducible. " +
                "Images pinned by digest are considered acceptable.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerImage.Matcher()
                .excludeScratch()
                .onlyUnpinned()
                .asVisitor(image -> {
                    // Skip images with environment variables in the name (can't analyze statically)
                    if (image.imageNameHasEnvironmentVariables()) {
                        return image.getTree();
                    }

                    // Get the reason for being unpinned
                    DockerImage.UnpinnedReason reason = image.getUnpinnedReason();

                    String message = reason == DockerImage.UnpinnedReason.IMPLICIT_LATEST ?
                            "Uses implicit 'latest' tag" :
                            "Uses 'latest' tag";
                    return SearchResult.found(image.getTree(), message);
                });
    }
}
