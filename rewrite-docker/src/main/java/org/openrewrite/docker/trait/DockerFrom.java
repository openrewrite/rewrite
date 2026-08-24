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
package org.openrewrite.docker.trait;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerVisitor;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.internal.ImageReferences;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.trait.VisitFunction2;

import java.util.Optional;

import static org.openrewrite.Tree.randomId;

/**
 * A trait representing a Docker base image from a FROM instruction.
 * Provides semantic access to image name, tag, digest, platform, and stage name,
 * along with matching capabilities that handle environment variables.
 */
@RequiredArgsConstructor
public class DockerFrom implements DockerImageReference<Docker.From> {

    @Getter
    private final Cursor cursor;

    @Override
    public Docker.Argument getImageNameArgument() {
        return getTree().getImageName();
    }

    @Override
    public Docker.@Nullable Argument getTagArgument() {
        return getTree().getTag();
    }

    @Override
    public Docker.@Nullable Argument getDigestArgument() {
        return getTree().getDigest();
    }

    /**
     * Returns the platform flag value, or null if not specified.
     *
     * @return The platform (e.g., "linux/amd64"), or null
     */
    public @Nullable String getPlatform() {
        Docker.From from = getTree();
        if (from.getFlags() == null) {
            return null;
        }
        for (Docker.Flag flag : from.getFlags()) {
            if ("platform".equals(flag.getName()) && flag.getValue() != null) {
                return ArgumentContents.textWithVariables(flag.getValue());
            }
        }
        return null;
    }

    /**
     * Returns the stage name (AS alias), or null if not specified.
     *
     * @return The stage name, or null
     */
    public @Nullable String getStageName() {
        Docker.From.As as = getTree().getAs();
        return as != null ? as.getName().getText() : null;
    }

    /**
     * Returns the quote style used for the image name, if any.
     *
     * @return The quote style, or null if unquoted
     */
    public Docker.Literal.@Nullable QuoteStyle getQuoteStyle() {
        return ArgumentContents.quoteStyle(getTree().getImageName());
    }

    /**
     * Returns the FROM instruction with its image reference replaced by {@code reference}
     * (e.g. {@code "nginx:1.25"}), decomposing it into the structured image name, tag, and
     * digest while preserving the original prefix.
     */
    @Override
    public Docker.From withImageReference(String reference) {
        Docker.From from = getTree();
        Docker.@Nullable Argument[] parts = ImageReferences.split(reference, from.getImageName().getPrefix());
        return from.withImageName(parts[0]).withTag(parts[1]).withDigest(parts[2]);
    }

    /**
     * Returns the FROM instruction with the tag of its image reference replaced by {@code tag},
     * preserving the image name and any digest.
     */
    @Override
    public Docker.From withTag(String tag) {
        return getTree().withTag(new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY,
                ArgumentContents.of(tag, getQuoteStyle())));
    }

    /**
     * Returns the FROM instruction with its image name replaced by {@code imageName}, preserving
     * any tag and digest.
     */
    @Override
    public Docker.From withImageNameArgument(Docker.Argument imageName) {
        return getTree().withImageName(imageName);
    }

    /**
     * Matcher for DockerImage traits with builder-style configuration.
     */
    public static class Matcher extends DockerTraitMatcher<DockerFrom> {
        private @Nullable String imageNamePattern;
        private @Nullable String tagPattern;
        private @Nullable String digestPattern;
        private @Nullable String platformPattern;
        private boolean excludeScratch;
        private boolean onlyUnpinned;

        /**
         * Only match images with names matching this glob pattern.
         *
         * @param pattern The glob pattern for image name
         * @return this matcher for chaining
         */
        @Contract("_ -> this")
        public Matcher imageName(String pattern) {
            this.imageNamePattern = pattern;
            return this;
        }

        /**
         * Only match images with tags matching this glob pattern.
         *
         * @param pattern The glob pattern for tag
         * @return this matcher for chaining
         */
        @Contract("_ -> this")
        public Matcher tag(String pattern) {
            this.tagPattern = pattern;
            return this;
        }

        /**
         * Only match images with digests matching this glob pattern.
         *
         * @param pattern The glob pattern for digest
         * @return this matcher for chaining
         */
        @Contract("_ -> this")
        public Matcher digest(String pattern) {
            this.digestPattern = pattern;
            return this;
        }

        /**
         * Only match images with platform flags matching this glob pattern.
         *
         * @param pattern The glob pattern for platform
         * @return this matcher for chaining
         */
        @Contract("_ -> this")
        public Matcher platform(String pattern) {
            this.platformPattern = pattern;
            return this;
        }

        /**
         * Exclude the special "scratch" base image from matches.
         *
         * @return this matcher for chaining
         */
        @Contract("-> this")
        public Matcher excludeScratch() {
            this.excludeScratch = true;
            return this;
        }

        /**
         * Only match unpinned images (no tag, "latest" tag, or no digest).
         *
         * @return this matcher for chaining
         */
        @Contract("-> this")
        public Matcher onlyUnpinned() {
            this.onlyUnpinned = true;
            return this;
        }

        @Override
        protected @Nullable DockerFrom test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof Docker.From)) {
                return null;
            }
            Docker.From from = (Docker.From) value;
            DockerFrom image = new DockerFrom(cursor);

            // Check exclusions
            if (excludeScratch && image.isScratch()) {
                return null;
            }

            if (onlyUnpinned && !image.isUnpinned()) {
                return null;
            }

            // Check image name pattern
            if (imageNamePattern != null && !image.imageNameMatches(imageNamePattern)) {
                return null;
            }

            // Check tag pattern
            if (tagPattern != null) {
                if (from.getTag() == null) {
                    return null;
                }
                if (!image.tagMatches(tagPattern)) {
                    return null;
                }
            }

            // Check digest pattern
            if (digestPattern != null) {
                if (from.getDigest() == null) {
                    return null;
                }
                if (!image.digestMatches(digestPattern)) {
                    return null;
                }
            }

            // Check platform pattern
            if (platformPattern != null) {
                String platform = image.getPlatform();
                if (platform == null || !StringUtils.matchesGlob(platform, platformPattern)) {
                    return null;
                }
            }

            return image;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<DockerFrom, P> visitor) {
            return new DockerVisitor<P>() {
                @Override
                public Docker visitFrom(Docker.From from, P p) {
                    DockerFrom image = test(getCursor());
                    if (image != null) {
                        return (Docker) visitor.visit(image, p);
                    }
                    return super.visitFrom(from, p);
                }
            };
        }
    }
}
