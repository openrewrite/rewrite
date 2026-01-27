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
public class DockerImage implements Trait<Docker.From> {

    @Getter
    private final Cursor cursor;

    /**
     * Returns the image reference with environment variables replaced by wildcards,
     * suitable for glob matching. Format: name[:tag][@digest]
     *
     * @return The image reference for matching purposes
     */
    public String getImageReferenceForMatching() {
        Docker.From from = getTree();
        StringBuilder sb = new StringBuilder();
        sb.append(new Matcher().extractTextForMatching(from.getImageName()));

        if (from.getTag() != null) {
            sb.append(":");
            sb.append(new Matcher().extractTextForMatching(from.getTag()));
        }
        if (from.getDigest() != null) {
            sb.append("@");
            sb.append(new Matcher().extractTextForMatching(from.getDigest()));
        }
        return sb.toString();
    }

    /**
     * Returns the full image reference with environment variables preserved.
     * Format: name[:tag][@digest]
     *
     * @return The image reference with environment variables in original form, or null if extraction fails
     */
    public @Nullable String getImageReference() {
        Docker.From from = getTree();
        Matcher m = new Matcher();
        String name = m.extractTextWithVariables(from.getImageName());
        if (name == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(name);
        if (from.getTag() != null) {
            String tag = m.extractTextWithVariables(from.getTag());
            if (tag != null) {
                sb.append(":").append(tag);
            }
        }
        if (from.getDigest() != null) {
            String digest = m.extractTextWithVariables(from.getDigest());
            if (digest != null) {
                sb.append("@").append(digest);
            }
        }
        return sb.toString();
    }

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
     * Returns the image name for matching purposes, with environment variables
     * replaced by wildcards.
     *
     * @return The image name suitable for glob matching
     */
    public String getImageNameForMatching() {
        return new Matcher().extractTextForMatching(getTree().getImageName());
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
     * Returns the tag for matching purposes, with environment variables
     * replaced by wildcards.
     *
     * @return The tag suitable for glob matching, or null if no tag
     */
    public @Nullable String getTagForMatching() {
        Docker.Argument tag = getTree().getTag();
        return tag != null ? new Matcher().extractTextForMatching(tag) : null;
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
     * Returns the digest for matching purposes, with environment variables
     * replaced by wildcards.
     *
     * @return The digest suitable for glob matching, or null if no digest
     */
    public @Nullable String getDigestForMatching() {
        Docker.Argument digest = getTree().getDigest();
        return digest != null ? new Matcher().extractTextForMatching(digest) : null;
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
     * Returns true if the image reference contains any environment variables.
     *
     * @return true if environment variables are present
     */
    public boolean hasEnvironmentVariables() {
        Docker.From from = getTree();
        Matcher m = new Matcher();
        return m.hasEnvironmentVariables(from.getImageName()) ||
               m.hasEnvironmentVariables(from.getTag()) ||
               m.hasEnvironmentVariables(from.getDigest());
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
     * Returns true if the image name contains environment variables.
     * This is useful for skipping analysis when the image name cannot be statically determined.
     *
     * @return true if the image name contains environment variables
     */
    public boolean imageNameHasEnvironmentVariables() {
        return new Matcher().hasEnvironmentVariables(getTree().getImageName());
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
     * Returns the quote style from the first available source: tag, digest, or image name.
     * This is useful when creating new tag/digest arguments that should match existing style.
     *
     * @return The quote style from tag, digest, or image name (in that order), or null if all are unquoted
     */
    public Docker.Literal.@Nullable QuoteStyle getPreferredQuoteStyle() {
        Docker.From from = getTree();
        Matcher m = new Matcher();
        if (from.getTag() != null) {
            Docker.Literal.QuoteStyle style = m.getQuoteStyle(from.getTag());
            if (style != null) {
                return style;
            }
        }
        if (from.getDigest() != null) {
            Docker.Literal.QuoteStyle style = m.getQuoteStyle(from.getDigest());
            if (style != null) {
                return style;
            }
        }
        return m.getQuoteStyle(from.getImageName());
    }

    /**
     * Checks if the full image reference matches the given glob pattern.
     * Handles environment variables by performing bidirectional matching.
     *
     * @param pattern The glob pattern to match against (e.g., "ubuntu:*", "registry/alpine:3.*")
     * @return true if the image matches the pattern
     */
    public boolean matches(String pattern) {
        String text = getImageReferenceForMatching();
        return new Matcher().matchesBidirectional(text, pattern, hasEnvironmentVariables());
    }

    /**
     * Checks if the image name (without tag/digest) matches the given glob pattern.
     *
     * @param pattern The glob pattern to match against
     * @return true if the image name matches
     */
    public boolean imageNameMatches(String pattern) {
        String text = getImageNameForMatching();
        Matcher m = new Matcher();
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
    public static class Matcher extends DockerTraitMatcher<DockerImage> {
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
        public Matcher platform(String pattern) {
            this.platformPattern = pattern;
            return this;
        }

        /**
         * Exclude the special "scratch" base image from matches.
         *
         * @return this matcher for chaining
         */
        public Matcher excludeScratch() {
            this.excludeScratch = true;
            return this;
        }

        /**
         * Only match unpinned images (no tag, "latest" tag, or no digest).
         *
         * @return this matcher for chaining
         */
        public Matcher onlyUnpinned() {
            this.onlyUnpinned = true;
            return this;
        }

        @Override
        protected @Nullable DockerImage test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof Docker.From)) {
                return null;
            }
            Docker.From from = (Docker.From) value;
            DockerImage image = new DockerImage(cursor);

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
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<DockerImage, P> visitor) {
            return new DockerVisitor<P>() {
                @Override
                public Docker visitFrom(Docker.From from, P p) {
                    DockerImage image = test(getCursor());
                    if (image != null) {
                        return (Docker) visitor.visit(image, p);
                    }
                    return super.visitFrom(from, p);
                }
            };
        }
    }
}
