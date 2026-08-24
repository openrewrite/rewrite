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
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.Optional;

import static org.openrewrite.docker.trait.DockerTraitMatcher.partMatches;

/**
 * An image reference anywhere in a Dockerfile: the base image of a {@code FROM} (see
 * {@link DockerFrom}) or the image carried by the {@code --from} of a {@code COPY} (see
 * {@link DockerCopyFrom}). Location-specific concepts such as platform, stage name and
 * {@code scratch} live on the concrete traits.
 *
 * @param <T> The instruction type carrying the image reference.
 */
public interface DockerImageReference<T extends Docker.Instruction> extends Trait<T> {

    /**
     * The image name as written, or {@code null} where the instruction names no image at all: a
     * {@code COPY} without a {@code --from}, or one whose {@code --from} names an earlier stage.
     * The tag and digest are {@code null} whenever this is.
     */
    Docker.@Nullable Argument getImageNameArgument();

    Docker.@Nullable Argument getTagArgument();

    Docker.@Nullable Argument getDigestArgument();

    /// The image name without tag or digest, with environment variable references left as written.
    default Optional<String> getImageName() {
        Docker.Argument imageName = getImageNameArgument();
        return imageName == null ? Optional.empty() : Optional.of(ArgumentContents.textWithVariables(imageName));
    }

    /// As [#getImageName()], decomposed into registry and path, which also gives the other spellings
    /// of the same image.
    default Optional<ImageName> getImage() {
        return getImageName().map(ImageName::parse);
    }

    /// The registry as written; for the one an image is pulled from either way, see
    /// [ImageName#getResolvedRegistry()].
    default Optional<String> getRegistry() {
        return getImage().map(ImageName::getRegistry);
    }

    /// The shortest name that resolves to the same image, so `docker.io/library/ubuntu` reads as `ubuntu`.
    default Optional<String> getFamiliarImageName() {
        return getImage().map(ImageName::getFamiliar);
    }

    /// The fully qualified name, so `ubuntu` reads as `docker.io/library/ubuntu`.
    default Optional<String> getCanonicalImageName() {
        return getImage().map(ImageName::getCanonical);
    }

    default Optional<String> getTag() {
        Docker.Argument tag = getTagArgument();
        return tag == null ? Optional.empty() : Optional.of(ArgumentContents.textWithVariables(tag));
    }

    default Optional<String> getDigest() {
        Docker.Argument digest = getDigestArgument();
        return digest == null ? Optional.empty() : Optional.of(ArgumentContents.textWithVariables(digest));
    }

    /// Pinned by digest, whatever its tag.
    default boolean isDigestPinned() {
        return getDigestArgument() != null;
    }

    default boolean isUnpinned() {
        return getUnpinnedReason().isPresent();
    }

    /// Empty if pinned. A name that is an unresolved environment variable cannot be classified, so it
    /// is conservatively taken to be pinned.
    default Optional<UnpinnedReason> getUnpinnedReason() {
        Docker.Argument imageName = getImageNameArgument();
        if (imageName == null || getDigestArgument() != null) {
            return Optional.empty();
        }
        Docker.Argument tag = getTagArgument();
        if (tag == null) {
            if (ArgumentContents.containsVariable(imageName)) {
                return Optional.empty();
            }
            return Optional.of(UnpinnedReason.IMPLICIT_LATEST);
        }
        if ("latest".equals(ArgumentContents.text(tag))) {
            return Optional.of(UnpinnedReason.EXPLICIT_LATEST);
        }
        return Optional.empty();
    }

    /// Matches in whichever spelling the pattern was written: `ubuntu` matches
    /// `docker.io/library/ubuntu` and the other way round.
    default boolean imageNameMatches(String pattern) {
        Docker.Argument imageName = getImageNameArgument();
        return imageName != null && DockerTraitMatcher.imageNameMatches(imageName, pattern);
    }

    default boolean tagMatches(String pattern) {
        Docker.Argument tag = getTagArgument();
        return tag != null && partMatches(tag, pattern);
    }

    default boolean digestMatches(String pattern) {
        Docker.Argument digest = getDigestArgument();
        return digest != null && partMatches(digest, pattern);
    }

    /// The instruction with its image reference replaced, as by `nginx:1.25`.
    T withImageReference(String reference);

    /// The instruction with its tag replaced, keeping the image name and any digest.
    T withTag(String tag);

    /// Takes an argument rather than a string so that a name holding an environment variable reference
    /// can be edited without flattening it back into text.
    T withImageNameArgument(Docker.Argument imageName);

    /// The `AS` alias of the stage this reference stands in, which for a `FROM` is its own.
    default @Nullable String getStageName() {
        Docker.Stage stage = getCursor().firstEnclosing(Docker.Stage.class);
        if (stage == null) {
            return null;
        }
        Docker.From.As as = stage.getFrom().getAs();
        return as == null ? null : as.getName().getText();
    }

    /// The special `scratch` image, which cannot be pulled, updated or scanned.
    default boolean isScratch() {
        Docker.Argument imageName = getImageNameArgument();
        return imageName != null && "scratch".equals(ArgumentContents.text(imageName));
    }

    enum UnpinnedReason {
        IMPLICIT_LATEST,
        EXPLICIT_LATEST
    }

    /**
     * Finds image references in {@code FROM} and {@code COPY --from} alike, skipping build-stage
     * references. Offers only the options common to both; use {@link DockerFrom.Matcher} or
     * {@link DockerCopyFrom.Matcher} for location-specific ones.
     */
    class Matcher extends DockerTraitMatcher<DockerImageReference<?>> {
        private @Nullable String imageNamePattern;
        private @Nullable String tagPattern;
        private @Nullable String digestPattern;
        private boolean excludeScratch;

        @Contract("_ -> this")
        public Matcher imageName(String pattern) {
            this.imageNamePattern = pattern;
            return this;
        }

        @Contract("_ -> this")
        public Matcher tag(String pattern) {
            this.tagPattern = pattern;
            return this;
        }

        @Contract("_ -> this")
        public Matcher digest(String pattern) {
            this.digestPattern = pattern;
            return this;
        }

        /// Exclude the special `scratch` image.
        @Contract("-> this")
        public Matcher excludeScratch() {
            this.excludeScratch = true;
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
            DockerImageReference<?> reference = null;
            if (value instanceof Docker.From) {
                reference = fromMatcher().test(cursor);
            } else if (value instanceof Docker.Copy) {
                reference = copyFromMatcher().test(cursor);
            }
            return reference == null || excludeScratch && reference.isScratch() ? null : reference;
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
            };
        }
    }
}
