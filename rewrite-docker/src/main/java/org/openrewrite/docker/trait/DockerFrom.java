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
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

/**
 * A trait representing a Docker base image from a FROM instruction.
 * Provides semantic access to image name, tag, digest, platform, and stage name,
 * along with matching capabilities that handle environment variables.
 */
@RequiredArgsConstructor
public class DockerFrom implements Trait<Docker.From> {

    @Getter
    private final Cursor cursor;

    /**
     * Returns the image name (without tag or digest).
     * Environment variables are preserved in their original form.
     *
     * @return The image name, or null if it contains unresolvable variables
     */
    public @Nullable String getImageName() {
        return new Matcher().extractTextWithVariables(getTree().getImageName());
    }

    /**
     * Returns the tag, or null if no tag is specified.
     * Environment variables are preserved in their original form.
     *
     * @return The tag, or null
     */
    public @Nullable String getTag() {
        return new Matcher().extractTextWithVariables(getTree().getTag());
    }

    /**
     * Returns the digest, or null if no digest is specified.
     * Environment variables are preserved in their original form.
     *
     * @return The digest, or null
     */
    public @Nullable String getDigest() {
        return new Matcher().extractTextWithVariables(getTree().getDigest());
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
        Matcher m = new Matcher();
        for (Docker.Flag flag : from.getFlags()) {
            if ("platform".equals(flag.getName()) && flag.getValue() != null) {
                return m.extractTextWithVariables(flag.getValue());
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
     * Returns true if this is the special "scratch" base image.
     *
     * @return true if the image is "scratch"
     */
    public boolean isScratch() {
        String name = new Matcher().extractText(getTree().getImageName());
        return "scratch".equals(name);
    }

    /**
     * Returns true if this image is unpinned (no tag, uses "latest" tag, or has no digest).
     * Images with a digest are considered pinned regardless of tag.
     *
     * @return true if the image is unpinned
     */
    public boolean isUnpinned() {
        return getUnpinnedReason() != null;
    }

    /**
     * Reasons why an image may be considered unpinned.
     */
    public enum UnpinnedReason {
        /**
         * No tag specified, which defaults to "latest".
         */
        IMPLICIT_LATEST,
        /**
         * Explicit "latest" tag specified.
         */
        EXPLICIT_LATEST
    }

    /**
     * Returns the reason this image is unpinned, or null if it's pinned.
     * Images with a digest are considered pinned. Images with environment variables
     * in the tag are conservatively considered pinned (we can't determine the value).
     *
     * @return The reason for being unpinned, or null if pinned
     */
    public @Nullable UnpinnedReason getUnpinnedReason() {
        Docker.From from = getTree();
        // Images with digest are pinned
        if (from.getDigest() != null) {
            return null;
        }
        // No tag means implicit "latest"
        if (from.getTag() == null) {
            return UnpinnedReason.IMPLICIT_LATEST;
        }
        // Explicit "latest" tag is unpinned (if it's a literal, not env var)
        String tag = new Matcher().extractText(from.getTag());
        if ("latest".equals(tag)) {
            return UnpinnedReason.EXPLICIT_LATEST;
        }
        // Has a specific tag (or env var tag) - considered pinned
        return null;
    }

    /**
     * Returns the quote style used for the image name, if any.
     *
     * @return The quote style, or null if unquoted
     */
    public Docker.Literal.@Nullable QuoteStyle getQuoteStyle() {
        return new Matcher().getQuoteStyle(getTree().getImageName());
    }

    /**
     * Checks if the image name (without tag/digest) matches the given glob pattern.
     *
     * @param pattern The glob pattern to match against
     * @return true if the image name matches
     */
    public boolean imageNameMatches(String pattern) {
        Matcher m = new Matcher();
        String text = m.extractTextForMatching(getTree().getImageName());
        return m.matchesBidirectional(text, pattern, m.hasEnvironmentVariables(getTree().getImageName()));
    }

    /**
     * Checks if the tag matches the given glob pattern.
     *
     * @param pattern The glob pattern to match against
     * @return true if the tag matches, false if no tag or doesn't match
     */
    public boolean tagMatches(String pattern) {
        Docker.Argument tag = getTree().getTag();
        if (tag == null) {
            return false;
        }
        Matcher m = new Matcher();
        String text = m.extractTextForMatching(tag);
        return m.matchesBidirectional(text, pattern, m.hasEnvironmentVariables(tag));
    }

    /**
     * Checks if the digest matches the given glob pattern.
     *
     * @param pattern The glob pattern to match against
     * @return true if the digest matches, false if no digest or doesn't match
     */
    public boolean digestMatches(String pattern) {
        Docker.Argument digest = getTree().getDigest();
        if (digest == null) {
            return false;
        }
        Matcher m = new Matcher();
        String text = m.extractTextForMatching(digest);
        return m.matchesBidirectional(text, pattern, m.hasEnvironmentVariables(digest));
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
