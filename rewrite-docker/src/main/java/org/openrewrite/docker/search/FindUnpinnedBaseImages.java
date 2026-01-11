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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerIsoVisitor;
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
        return new DockerIsoVisitor<ExecutionContext>() {

            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                Docker.From f = super.visitFrom(from, ctx);

                String imageName = extractText(f.getImageName());
                if (imageName == null) {
                    return f; // Contains environment variables
                }

                // Skip "scratch" base image - it's a special case
                if ("scratch".equals(imageName)) {
                    return f;
                }

                String tag = extractText(f.getTag());
                String digest = extractText(f.getDigest());

                // Check for latest tag or no tag (and no digest)
                if (digest == null && (tag == null || "latest".equals(tag))) {
                    String message = tag == null ?
                            "Uses implicit 'latest' tag" :
                            "Uses 'latest' tag";
                    return SearchResult.found(f, message);
                }

                return f;
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
