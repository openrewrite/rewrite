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
import org.openrewrite.docker.trait.DockerImage;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
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

                // Use DockerImage trait for matching
                DockerImage image = new DockerImage.Matcher().require(getCursor());

                // Match image name against pattern
                if (!image.imageNameMatches(imageNamePattern)) {
                    return f;
                }

                // Match old tag pattern if specified
                if (oldTagPattern != null) {
                    if (f.getTag() == null || !image.tagMatches(oldTagPattern)) {
                        return f;
                    }
                }

                // Match old digest pattern if specified
                if (oldDigestPattern != null) {
                    if (f.getDigest() == null || !image.digestMatches(oldDigestPattern)) {
                        return f;
                    }
                }

                // Check if change is needed
                String currentTagText = image.getTagForMatching();
                String currentDigestText = image.getDigestForMatching();

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
                Docker.Literal.QuoteStyle quoteStyle = image.getPreferredQuoteStyle();

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
