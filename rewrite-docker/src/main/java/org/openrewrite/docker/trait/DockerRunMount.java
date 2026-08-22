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
import org.openrewrite.docker.internal.FlagOptions;
import org.openrewrite.docker.internal.ImageReferences;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.trait.VisitFunction2;

import java.util.List;
import java.util.Optional;

import static org.openrewrite.Tree.randomId;

/**
 * A trait representing one {@code --mount} flag of a {@code RUN} instruction, whose {@code from}
 * option names where the mounted files come from: an earlier build stage
 * (e.g. {@code RUN --mount=type=bind,from=builder,source=/out,target=/out}), a named build context,
 * or an image (e.g. {@code RUN --mount=type=bind,from=composer:2.5.5,source=/usr/bin/composer,target=/usr/bin/composer}).
 * The image accessors read that option, returning {@code null} where it names a build stage or is
 * absent; use {@link #isStageReference()} to disambiguate.
 * <p>
 * A {@code RUN} may carry several mounts, so unlike {@link DockerFrom} and {@link DockerCopyFrom}
 * the trait stands on the flag rather than on the instruction.
 * <p>
 * Two rules differ from the {@code --from} of a {@code COPY}, both as Docker itself reads them: a
 * number is an image name here rather than a stage index, since only a {@code COPY} resolves a stage
 * by position; and a {@code from} holding a variable reference is an error rather than something to
 * resolve, so one is never taken to be an image.
 */
@RequiredArgsConstructor
public class DockerRunMount implements DockerImageReference<Docker.Flag> {

    @Getter
    private final Cursor cursor;

    private @Nullable Boolean stageReference;
    private boolean componentsComputed;
    private Docker.@Nullable Argument @Nullable [] componentsValue;

    /**
     * The value of the {@code type} option, which Docker defaults to {@code bind} where the mount
     * does not give one.
     */
    public String getType() {
        return getOption("type").orElse("bind");
    }

    /**
     * The value of the {@code source} option (or its {@code src} alias), or empty where the mount
     * carries neither.
     */
    public Optional<String> getSource() {
        Optional<String> source = getOption("source");
        return source.isPresent() ? source : getOption("src");
    }

    /**
     * The value of the {@code target} option (or its {@code dst} and {@code destination} aliases),
     * or empty where the mount carries none.
     */
    public Optional<String> getTarget() {
        Optional<String> target = getOption("target");
        if (target.isPresent()) {
            return target;
        }
        Optional<String> dst = getOption("dst");
        return dst.isPresent() ? dst : getOption("destination");
    }

    /**
     * The value of the named option with environment variable references preserved, or empty where
     * the mount does not carry it.
     */
    public Optional<String> getOption(String key) {
        List<Docker.ArgumentContent> value = optionValue(key);
        return value == null ? Optional.empty() : Optional.of(ArgumentContents.textWithVariables(argument(value)));
    }

    /**
     * Returns true if the {@code from} option refers to a build stage rather than to an image. A
     * named build context is indistinguishable from an image here, since nothing in the file
     * declares one.
     */
    public boolean isStageReference() {
        if (stageReference == null) {
            List<Docker.ArgumentContent> from = optionValue("from");
            String value = from == null ? null : ArgumentContents.text(argument(from));
            stageReference = value != null && Stages.isDeclaredStage(cursor, value);
        }
        return stageReference;
    }

    private @Nullable List<Docker.ArgumentContent> optionValue(String key) {
        Docker.Argument value = getTree().getValue();
        return value == null ? null : FlagOptions.value(value.getContents(), key);
    }

    private Docker.@Nullable Argument @Nullable [] components() {
        if (!componentsComputed) {
            componentsValue = computeComponents();
            componentsComputed = true;
        }
        return componentsValue;
    }

    private Docker.@Nullable Argument @Nullable [] computeComponents() {
        List<Docker.ArgumentContent> from = optionValue("from");
        if (from == null || from.isEmpty() || isStageReference()) {
            return null;
        }
        Docker.Argument argument = argument(from);
        if (ArgumentContents.containsVariable(argument) || ArgumentContents.textWithVariables(argument).isEmpty()) {
            return null;
        }
        return ImageReferences.split(ImageReferences.separated(from), Space.EMPTY);
    }

    private static Docker.Argument argument(List<Docker.ArgumentContent> contents) {
        return new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, contents);
    }

    /**
     * Returns the image name the {@code from} option carries, or {@code null} if it names a build
     * stage or is absent.
     */
    @Override
    public Docker.@Nullable Argument getImageNameArgument() {
        Docker.@Nullable Argument[] components = components();
        return components == null ? null : components[0];
    }

    @Override
    public Docker.@Nullable Argument getTagArgument() {
        Docker.@Nullable Argument[] components = components();
        return components == null ? null : components[1];
    }

    @Override
    public Docker.@Nullable Argument getDigestArgument() {
        Docker.@Nullable Argument[] components = components();
        return components == null ? null : components[2];
    }

    /**
     * Returns the mount with the value of its {@code from} option replaced by {@code reference}
     * (e.g. {@code "nginx:1.25"}), or unchanged if it carries no {@code from}.
     */
    @Override
    public Docker.Flag withImageReference(String reference) {
        return withFrom(ImageReferences.contents(reference));
    }

    /**
     * Returns the mount with the image name of its {@code from} option replaced by
     * {@code imageName}, preserving any tag and digest. Unchanged for stage references.
     */
    @Override
    public Docker.Flag withImageNameArgument(Docker.Argument imageName) {
        Docker.@Nullable Argument[] parts = components();
        if (parts == null) {
            return getTree();
        }
        return withFrom(ImageReferences.contents(new Docker.Argument[]{imageName, parts[1], parts[2]}));
    }

    /**
     * Returns the mount with the tag of its image reference replaced by {@code tag}, preserving the
     * image name and any digest. Unchanged for stage references.
     */
    @Override
    public Docker.Flag withTag(String tag) {
        Optional<String> name = getImageName();
        if (!name.isPresent()) {
            return getTree();
        }
        String suffix = getDigest().map(d -> "@" + d).orElse("");
        return withImageReference(name.get() + ":" + tag + suffix);
    }

    private Docker.Flag withFrom(List<Docker.ArgumentContent> from) {
        Docker.Flag flag = getTree();
        Docker.Argument value = flag.getValue();
        if (value == null || optionValue("from") == null) {
            return flag;
        }
        return flag.withValue(value.withContents(
                FlagOptions.withValue(value.getContents(), "from", ImageReferences.joined(from))));
    }

    /**
     * Matcher for {@link DockerRunMount} traits with builder-style configuration.
     */
    public static class Matcher extends DockerTraitMatcher<DockerRunMount> {
        private @Nullable String imageNamePattern;
        private @Nullable String tagPattern;
        private @Nullable String digestPattern;
        private boolean excludeStageReferences;
        private boolean onlyWithFrom;

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

        /**
         * Only match mounts carrying a {@code from} option, whether it names an image or a build
         * stage. Without it a mount takes its files from the build context.
         */
        @Contract("-> this")
        public Matcher onlyWithFrom() {
            this.onlyWithFrom = true;
            return this;
        }

        /**
         * Exclude {@code from} values that reference a build stage, matching only external image
         * references.
         */
        @Contract("-> this")
        public Matcher excludeStageReferences() {
            this.excludeStageReferences = true;
            return this;
        }

        @Override
        protected @Nullable DockerRunMount test(Cursor cursor) {
            if (!(cursor.getValue() instanceof Docker.Flag) ||
                    !"mount".equals(((Docker.Flag) cursor.getValue()).getName()) ||
                    cursor.firstEnclosing(Docker.Run.class) == null) {
                return null;
            }
            DockerRunMount mount = new DockerRunMount(cursor);

            if ((onlyWithFrom || excludeStageReferences) && mount.optionValue("from") == null) {
                return null;
            }
            if (excludeStageReferences && mount.isStageReference()) {
                return null;
            }

            if (imageNamePattern != null && !mount.imageNameMatches(imageNamePattern)) {
                return null;
            }
            if (tagPattern != null && !mount.tagMatches(tagPattern)) {
                return null;
            }
            if (digestPattern != null && !mount.digestMatches(digestPattern)) {
                return null;
            }

            return mount;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<DockerRunMount, P> visitor) {
            return new DockerVisitor<P>() {
                @Override
                public Docker visitFlag(Docker.Flag flag, P p) {
                    DockerRunMount mount = test(getCursor());
                    if (mount != null) {
                        return (Docker) visitor.visit(mount, p);
                    }
                    return super.visitFlag(flag, p);
                }
            };
        }
    }
}
