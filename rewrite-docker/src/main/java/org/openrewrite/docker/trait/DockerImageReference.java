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

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerVisitor;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.Optional;

/**
 * A trait representing an image reference anywhere in a Dockerfile: the base image of a
 * {@code FROM} instruction (see {@link DockerFrom}) or the image carried by the {@code --from}
 * flag of a {@code COPY} / {@code ADD} instruction (see {@link DockerCopyFrom}).
 * <p>
 * This is the common contract shared by both concrete traits, providing semantic access to
 * the image name, tag, and digest, pinning classification, glob matching, and the ability to
 * update the reference. Location-specific concepts (platform, stage name, {@code scratch},
 * stage references) live on the concrete traits.
 * <p>
 * Use {@link Matcher} to find and update image references regardless of where they occur.
 *
 * @param <T> The instruction type carrying the image reference ({@link Docker.From} for a
 *            {@code FROM}, or {@link Docker.Instruction} for a {@code COPY}/{@code ADD}).
 */
public interface DockerImageReference<T extends Docker.Instruction> extends Trait<T> {

    /**
     * Returns the image name (without tag or digest), or empty if the reference does not
     * resolve to an external image (e.g. a build-stage reference).
     */
    Optional<String> getImageName();

    /**
     * Returns the tag, or empty if no tag is specified.
     */
    Optional<String> getTag();

    /**
     * Returns the digest, or empty if no digest is specified.
     */
    Optional<String> getDigest();

    /**
     * Returns true if the referenced image is pinned by digest.
     */
    boolean isDigestPinned();

    /**
     * Returns true if the referenced image is unpinned (no tag or an explicit "latest" tag).
     */
    boolean isUnpinned();

    /**
     * Returns the reason the referenced image is unpinned, or empty if it is pinned.
     */
    Optional<UnpinnedReason> getUnpinnedReason();

    /**
     * Checks if the image name matches the given glob pattern.
     */
    boolean imageNameMatches(String pattern);

    /**
     * Checks if the tag matches the given glob pattern.
     */
    boolean tagMatches(String pattern);

    /**
     * Checks if the digest matches the given glob pattern.
     */
    boolean digestMatches(String pattern);

    /**
     * Returns the instruction with its image reference replaced by {@code reference}
     * (e.g. {@code "nginx:1.25"}).
     */
    T withImageReference(String reference);

    /**
     * Returns the instruction with the tag of its image reference replaced by {@code tag},
     * preserving the image name and any digest.
     */
    T withTag(String tag);

    /**
     * Reasons why an image may be considered unpinned.
     */
    enum UnpinnedReason {
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
     * Matcher that finds image references in {@code FROM}, {@code COPY --from}, and
     * {@code ADD --from} alike, yielding the shared {@link DockerImageReference} contract.
     * Build-stage references (e.g. {@code COPY --from=builder}) are not images and are skipped.
     * <p>
     * Only the options common to all locations are offered here; use {@link DockerFrom.Matcher}
     * or {@link DockerCopyFrom.Matcher} directly for location-specific options such as
     * {@code platform}, {@code excludeScratch}, or {@code onlyUnpinned}.
     */
    class Matcher extends DockerTraitMatcher<DockerImageReference<?>> {
        private @Nullable String imageNamePattern;
        private @Nullable String tagPattern;
        private @Nullable String digestPattern;

        /**
         * Only match images with names matching this glob pattern.
         */
        @Contract("_ -> this")
        public Matcher imageName(String pattern) {
            this.imageNamePattern = pattern;
            return this;
        }

        /**
         * Only match images with tags matching this glob pattern.
         */
        @Contract("_ -> this")
        public Matcher tag(String pattern) {
            this.tagPattern = pattern;
            return this;
        }

        /**
         * Only match images with digests matching this glob pattern.
         */
        @Contract("_ -> this")
        public Matcher digest(String pattern) {
            this.digestPattern = pattern;
            return this;
        }

        private DockerFrom.Matcher fromMatcher() {
            DockerFrom.Matcher m = new DockerFrom.Matcher();
            if (imageNamePattern != null) {
                m.imageName(imageNamePattern);
            }
            if (tagPattern != null) {
                m.tag(tagPattern);
            }
            if (digestPattern != null) {
                m.digest(digestPattern);
            }
            return m;
        }

        private DockerCopyFrom.Matcher copyFromMatcher() {
            DockerCopyFrom.Matcher m = new DockerCopyFrom.Matcher().excludeStageReferences();
            if (imageNamePattern != null) {
                m.imageName(imageNamePattern);
            }
            if (tagPattern != null) {
                m.tag(tagPattern);
            }
            if (digestPattern != null) {
                m.digest(digestPattern);
            }
            return m;
        }

        @Override
        protected @Nullable DockerImageReference<?> test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Docker.From) {
                return fromMatcher().test(cursor);
            }
            if (value instanceof Docker.Copy || value instanceof Docker.Add) {
                return copyFromMatcher().test(cursor);
            }
            return null;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<DockerImageReference<?>, P> visitor) {
            return new DockerVisitor<P>() {
                @Override
                public Docker visitFrom(Docker.From from, P p) {
                    DockerImageReference<?> ref = test(getCursor());
                    if (ref != null) {
                        return (Docker) visitor.visit(ref, p);
                    }
                    return super.visitFrom(from, p);
                }

                @Override
                public Docker visitCopy(Docker.Copy copy, P p) {
                    DockerImageReference<?> ref = test(getCursor());
                    if (ref != null) {
                        return (Docker) visitor.visit(ref, p);
                    }
                    return super.visitCopy(copy, p);
                }

                @Override
                public Docker visitAdd(Docker.Add add, P p) {
                    DockerImageReference<?> ref = test(getCursor());
                    if (ref != null) {
                        return (Docker) visitor.visit(ref, p);
                    }
                    return super.visitAdd(add, p);
                }
            };
        }
    }
}
