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
import org.openrewrite.*;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeImageTag extends Recipe {

    @Option(displayName = "Image name pattern",
            description = "Glob pattern to match image names (without tag/digest). " +
                          "For example, `ubuntu` matches only `ubuntu`, while `*` matches any image.",
            example = "ubuntu")
    String imageNamePattern;

    @Option(displayName = "Old tag pattern",
            description = "Only change images with tags matching this glob pattern. If null, matches any tag.",
            example = "20.*",
            required = false)
    @Nullable
    String oldTagPattern;

    @Option(displayName = "Old digest pattern",
            description = "Only change images with digests matching this glob pattern. If null, matches any digest. " +
                          "Cannot be used together with oldTagPattern.",
            example = "sha256:a]*",
            required = false)
    @Nullable
    String oldDigestPattern;

    @Option(displayName = "New tag",
            description = "The new tag to use. Mutually exclusive with newDigest.",
            example = "22.04",
            required = false)
    @Nullable
    String newTag;

    @Option(displayName = "New digest",
            description = "The new digest to use. Mutually exclusive with newTag.",
            example = "sha256:abc123...",
            required = false)
    @Nullable
    String newDigest;

    @Override
    public String getDisplayName() {
        return "Change Docker image tag";
    }

    @Override
    public String getDescription() {
        return "Change the tag or digest of a Docker base image in FROM instructions, preserving the image name.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newTag == null && newDigest == null) {
            validated = validated.and(Validated.invalid("newTag/newDigest", null,
                    "Either newTag or newDigest must be specified"));
        }
        if (newTag != null && newDigest != null) {
            validated = validated.and(Validated.invalid("newTag/newDigest", null,
                    "newTag and newDigest are mutually exclusive"));
        }
        if (oldTagPattern != null && oldDigestPattern != null) {
            validated = validated.and(Validated.invalid("oldTagPattern/oldDigestPattern", null,
                    "oldTagPattern and oldDigestPattern are mutually exclusive"));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                Docker.From f = super.visitFrom(from, ctx);

                // Extract image name text, using wildcard for environment variables
                String imageNameText = extractArgumentText(f.getImageName());
                boolean imageNameHasEnvVar = containsEnvironmentVariable(f.getImageName());

                // Match image name against pattern
                if (imageNameHasEnvVar) {
                    // Bidirectional matching for env vars
                    if (!StringUtils.matchesGlob(imageNameText, imageNamePattern) &&
                        !StringUtils.matchesGlob(imageNamePattern, imageNameText)) {
                        return f;
                    }
                } else if (!StringUtils.matchesGlob(imageNameText, imageNamePattern)) {
                    return f;
                }

                // Extract current tag/digest text
                String currentTagText = f.getTag() != null ? extractArgumentText(f.getTag()) : null;
                String currentDigestText = f.getDigest() != null ? extractArgumentText(f.getDigest()) : null;

                // Match old tag pattern if specified
                if (oldTagPattern != null) {
                    if (currentTagText == null) {
                        return f; // No tag to match
                    }
                    boolean tagHasEnvVar = containsEnvironmentVariable(f.getTag());
                    if (tagHasEnvVar) {
                        if (!StringUtils.matchesGlob(currentTagText, oldTagPattern) &&
                            !StringUtils.matchesGlob(oldTagPattern, currentTagText)) {
                            return f;
                        }
                    } else if (!StringUtils.matchesGlob(currentTagText, oldTagPattern)) {
                        return f;
                    }
                }

                // Match old digest pattern if specified
                if (oldDigestPattern != null) {
                    if (currentDigestText == null) {
                        return f; // No digest to match
                    }
                    boolean digestHasEnvVar = containsEnvironmentVariable(f.getDigest());
                    if (digestHasEnvVar) {
                        if (!StringUtils.matchesGlob(currentDigestText, oldDigestPattern) &&
                            !StringUtils.matchesGlob(oldDigestPattern, currentDigestText)) {
                            return f;
                        }
                    } else if (!StringUtils.matchesGlob(currentDigestText, oldDigestPattern)) {
                        return f;
                    }
                }

                // Check if change is needed
                if (newTag != null) {
                    if (newTag.equals(currentTagText) && f.getDigest() == null) {
                        return f; // Already has the desired tag
                    }
                } else if (newDigest != null) {
                    if (newDigest.equals(currentDigestText) && f.getTag() == null) {
                        return f; // Already has the desired digest
                    }
                }

                // Determine quote style from existing tag/digest or image name
                Docker.Literal.QuoteStyle quoteStyle = getQuoteStyle(f);

                // Apply the new tag or digest
                if (newTag != null) {
                    Docker.Argument newTagArg = createArgument(newTag, quoteStyle);
                    return f.withTag(newTagArg).withDigest(null);
                } else {
                    Docker.Argument newDigestArg = createArgument(newDigest, quoteStyle);
                    return f.withDigest(newDigestArg).withTag(null);
                }
            }
        };
    }

    private String extractArgumentText(Docker.Argument arg) {
        StringBuilder sb = new StringBuilder();
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.Literal) {
                sb.append(((Docker.Literal) content).getText());
            } else if (content instanceof Docker.EnvironmentVariable) {
                sb.append("*");
            }
        }
        return sb.toString();
    }

    private boolean containsEnvironmentVariable(Docker.@Nullable Argument arg) {
        if (arg == null) {
            return false;
        }
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.EnvironmentVariable) {
                return true;
            }
        }
        return false;
    }

    private Docker.Literal.@Nullable QuoteStyle getQuoteStyle(Docker.From from) {
        // Try tag first, then digest, then image name
        if (from.getTag() != null) {
            Docker.Literal.QuoteStyle style = getQuoteStyleFromArg(from.getTag());
            if (style != null) {
                return style;
            }
        }
        if (from.getDigest() != null) {
            Docker.Literal.QuoteStyle style = getQuoteStyleFromArg(from.getDigest());
            if (style != null) {
                return style;
            }
        }
        return getQuoteStyleFromArg(from.getImageName());
    }

    private Docker.Literal.@Nullable QuoteStyle getQuoteStyleFromArg(Docker.Argument arg) {
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.Literal) {
                Docker.Literal.QuoteStyle style = ((Docker.Literal) content).getQuoteStyle();
                if (style != null) {
                    return style;
                }
            }
        }
        return null;
    }

    private Docker.Argument createArgument(String text, Docker.Literal.@Nullable QuoteStyle quoteStyle) {
        return new Docker.Argument(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                singletonList(new Docker.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        text,
                        quoteStyle
                ))
        );
    }
}
