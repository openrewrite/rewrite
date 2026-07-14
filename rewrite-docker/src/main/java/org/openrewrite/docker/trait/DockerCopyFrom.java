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
import org.openrewrite.docker.internal.ImageReferences;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * A trait representing the image reference carried by the {@code --from} flag of a
 * {@code COPY} or {@code ADD} instruction. Provides the same semantic access to image name,
 * tag, and digest as {@link DockerFrom}, while distinguishing an external image reference
 * (e.g. {@code COPY --from=nginx:latest}) from a reference to an earlier build stage
 * (e.g. {@code COPY --from=builder} or {@code COPY --from=0}). When the value refers to a
 * build stage the image accessors return {@code null}; use {@link #isStageReference()} to
 * disambiguate.
 */
@RequiredArgsConstructor
public class DockerCopyFrom implements Trait<Docker.Instruction>, DockerImageReference<Docker.Instruction> {

    @Getter
    private final Cursor cursor;

    private @Nullable List<Docker.Flag> flags() {
        Docker.Instruction instruction = getTree();
        if (instruction instanceof Docker.Copy) {
            return ((Docker.Copy) instruction).getFlags();
        }
        if (instruction instanceof Docker.Add) {
            return ((Docker.Add) instruction).getFlags();
        }
        return null;
    }

    private Docker.@Nullable Argument fromArgument() {
        List<Docker.Flag> flags = flags();
        if (flags == null) {
            return null;
        }
        for (Docker.Flag flag : flags) {
            if ("from".equals(flag.getName())) {
                return flag.getValue();
            }
        }
        return null;
    }

    private Docker.@Nullable Argument @Nullable [] components() {
        if (isStageReference()) {
            return null;
        }
        Docker.Argument arg = fromArgument();
        if (arg == null) {
            return null;
        }
        return ImageReferences.split(arg.getContents(), arg.getPrefix());
    }

    /**
     * Returns the raw {@code --from} value with environment variable references preserved,
     * or null if there is no {@code --from} flag.
     */
    public @Nullable String getFromValue() {
        return new Matcher().extractTextWithVariables(fromArgument());
    }

    /**
     * Returns true if the {@code --from} value refers to an earlier build stage (by name or
     * numeric index) rather than an external image.
     */
    public boolean isStageReference() {
        Docker.Argument arg = fromArgument();
        if (arg == null) {
            return false;
        }
        String value = new Matcher().extractText(arg);
        if (value == null) {
            return false;
        }
        if (isNonNegativeInteger(value)) {
            return true;
        }
        return stageNames().contains(value);
    }

    private static boolean isNonNegativeInteger(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private Set<String> stageNames() {
        Docker.File file = cursor.firstEnclosing(Docker.File.class);
        Set<String> names = new HashSet<>();
        if (file != null) {
            for (Docker.Stage stage : file.getStages()) {
                Docker.From.As as = stage.getFrom().getAs();
                if (as != null) {
                    names.add(as.getName().getText());
                }
            }
        }
        return names;
    }

    /**
     * Returns the image name (without tag or digest), or null if this is a stage reference
     * or there is no {@code --from} flag.
     */
    @Override
    public @Nullable String getImageName() {
        Docker.@Nullable Argument[] components = components();
        return components == null ? null : new Matcher().extractTextWithVariables(components[0]);
    }

    /**
     * Returns the tag, or null if no tag is specified or this is a stage reference.
     */
    @Override
    public @Nullable String getTag() {
        Docker.@Nullable Argument[] components = components();
        return components == null ? null : new Matcher().extractTextWithVariables(components[1]);
    }

    /**
     * Returns the digest, or null if no digest is specified or this is a stage reference.
     */
    @Override
    public @Nullable String getDigest() {
        Docker.@Nullable Argument[] components = components();
        return components == null ? null : new Matcher().extractTextWithVariables(components[2]);
    }

    /**
     * Returns true if the referenced external image is pinned by digest.
     */
    @Override
    public boolean isDigestPinned() {
        Docker.@Nullable Argument[] components = components();
        return components != null && components[2] != null;
    }

    /**
     * Returns true if the referenced external image is unpinned (no tag or an explicit
     * "latest" tag). Stage references and digest-pinned images are considered pinned.
     */
    @Override
    public boolean isUnpinned() {
        return getUnpinnedReason() != null;
    }

    /**
     * Returns the reason the referenced external image is unpinned, or null if it's pinned
     * or this is a stage reference.
     */
    @Override
    public @Nullable UnpinnedReason getUnpinnedReason() {
        Docker.@Nullable Argument[] components = components();
        if (components == null) {
            return null;
        }
        if (components[2] != null) {
            return null;
        }
        if (components[1] == null) {
            return UnpinnedReason.IMPLICIT_LATEST;
        }
        String tag = new Matcher().extractText(components[1]);
        if ("latest".equals(tag)) {
            return UnpinnedReason.EXPLICIT_LATEST;
        }
        return null;
    }

    /**
     * Returns the instruction with its {@code --from} value replaced by {@code reference}
     * (e.g. {@code "nginx:1.25"}), or unchanged if there is no {@code --from} flag.
     */
    @Override
    public Docker.Instruction withImageReference(String reference) {
        Docker.Argument arg = fromArgument();
        if (arg == null) {
            return getTree();
        }
        Docker.Argument newValue = arg.withContents(singletonList(
          new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, reference, null)));
        List<Docker.Flag> newFlags = ListUtils.map(flags(), f ->
          "from".equals(f.getName()) ? f.withValue(newValue) : f);
        Docker.Instruction instruction = getTree();
        if (instruction instanceof Docker.Copy) {
            return ((Docker.Copy) instruction).withFlags(newFlags);
        }
        if (instruction instanceof Docker.Add) {
            return ((Docker.Add) instruction).withFlags(newFlags);
        }
        return instruction;
    }

    /**
     * Returns the instruction with the tag of its external image reference replaced by
     * {@code tag}, preserving the image name and any digest. Unchanged for stage references.
     */
    @Override
    public Docker.Instruction withTag(String tag) {
        String name = getImageName();
        if (name == null) {
            return getTree();
        }
        String digest = getDigest();
        return withImageReference(name + ":" + tag + (digest != null ? "@" + digest : ""));
    }

    /**
     * Checks if the image name matches the given glob pattern; always false for stage references.
     */
    @Override
    public boolean imageNameMatches(String pattern) {
        Docker.@Nullable Argument[] components = components();
        if (components == null) {
            return false;
        }
        Matcher m = new Matcher();
        String text = m.extractTextForMatching(components[0]);
        return m.matchesBidirectional(text, pattern, m.hasEnvironmentVariables(components[0]));
    }

    /**
     * Checks if the tag matches the given glob pattern; false for stage references or when absent.
     */
    @Override
    public boolean tagMatches(String pattern) {
        Docker.@Nullable Argument[] components = components();
        if (components == null || components[1] == null) {
            return false;
        }
        Matcher m = new Matcher();
        String text = m.extractTextForMatching(components[1]);
        return m.matchesBidirectional(text, pattern, m.hasEnvironmentVariables(components[1]));
    }

    /**
     * Checks if the digest matches the given glob pattern; false for stage references or when absent.
     */
    @Override
    public boolean digestMatches(String pattern) {
        Docker.@Nullable Argument[] components = components();
        if (components == null || components[2] == null) {
            return false;
        }
        Matcher m = new Matcher();
        String text = m.extractTextForMatching(components[2]);
        return m.matchesBidirectional(text, pattern, m.hasEnvironmentVariables(components[2]));
    }

    /**
     * Matcher for {@link DockerCopyFrom} traits with builder-style configuration.
     */
    public static class Matcher extends DockerTraitMatcher<DockerCopyFrom> {
        private @Nullable String imageNamePattern;
        private @Nullable String tagPattern;
        private @Nullable String digestPattern;
        private boolean excludeStageReferences;

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
         * Exclude {@code --from} values that reference an earlier build stage, matching only
         * external image references.
         */
        @Contract("-> this")
        public Matcher excludeStageReferences() {
            this.excludeStageReferences = true;
            return this;
        }

        @Override
        protected @Nullable DockerCopyFrom test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof Docker.Copy) && !(value instanceof Docker.Add)) {
                return null;
            }
            DockerCopyFrom copyFrom = new DockerCopyFrom(cursor);

            if (copyFrom.fromArgument() == null) {
                return null;
            }

            if (excludeStageReferences && copyFrom.isStageReference()) {
                return null;
            }

            if (imageNamePattern != null && !copyFrom.imageNameMatches(imageNamePattern)) {
                return null;
            }
            if (tagPattern != null && !copyFrom.tagMatches(tagPattern)) {
                return null;
            }
            if (digestPattern != null && !copyFrom.digestMatches(digestPattern)) {
                return null;
            }

            return copyFrom;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<DockerCopyFrom, P> visitor) {
            return new DockerVisitor<P>() {
                @Override
                public Docker visitCopy(Docker.Copy copy, P p) {
                    DockerCopyFrom copyFrom = test(getCursor());
                    if (copyFrom != null) {
                        return (Docker) visitor.visit(copyFrom, p);
                    }
                    return super.visitCopy(copy, p);
                }

                @Override
                public Docker visitAdd(Docker.Add add, P p) {
                    DockerCopyFrom copyFrom = test(getCursor());
                    if (copyFrom != null) {
                        return (Docker) visitor.visit(copyFrom, p);
                    }
                    return super.visitAdd(add, p);
                }
            };
        }
    }
}
