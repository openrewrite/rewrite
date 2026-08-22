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
import org.openrewrite.trait.VisitFunction2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
public class DockerCopyFrom implements DockerImageReference<Docker.Instruction> {

    @Getter
    private final Cursor cursor;

    private @Nullable Boolean stageReference;
    private boolean componentsComputed;
    private Docker.@Nullable Argument @Nullable [] componentsValue;

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
        if (!componentsComputed) {
            componentsValue = computeComponents();
            componentsComputed = true;
        }
        return componentsValue;
    }

    /// The `name`, `tag` and `digest` of the `--from` value, read off the separators the
    /// `FLAG_IMAGE_REF` lexer mode left in its contents. Only a separator is a content of its own, so
    /// a colon that belongs to a registry port, a quoted name or a variable reference stays inside the
    /// part that holds it without this having to know the rule that put it there.
    private Docker.@Nullable Argument @Nullable [] computeComponents() {
        if (isStageReference()) {
            return null;
        }
        Docker.Argument arg = fromArgument();
        if (arg == null) {
            return null;
        }

        List<Docker.ArgumentContent> imageName = new ArrayList<>();
        List<Docker.ArgumentContent> tag = null;
        List<Docker.ArgumentContent> digest = null;
        List<Docker.ArgumentContent> part = imageName;
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (digest == null && isSeparator(content, "@")) {
                part = digest = new ArrayList<>();
            } else if (tag == null && digest == null && isSeparator(content, ":")) {
                part = tag = new ArrayList<>();
            } else {
                part.add(content);
            }
        }

        return new Docker.@Nullable Argument[]{
                new Docker.Argument(randomId(), arg.getPrefix(), Markers.EMPTY, imageName),
                tag == null ? null : new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, tag),
                digest == null ? null : new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, digest)};
    }

    private static boolean isSeparator(Docker.ArgumentContent content, String separator) {
        return content instanceof Docker.Literal && !((Docker.Literal) content).isQuoted() &&
                separator.equals(((Docker.Literal) content).getText());
    }

    /// The contents of a reference given as text, with the separators kept as contents of their own
    /// so that a value a recipe writes is read back the way a parsed one is.
    private static List<Docker.ArgumentContent> referenceContents(String reference) {
        Docker.@Nullable Argument[] parts = ImageReferences.split(reference, Space.EMPTY);
        List<Docker.ArgumentContent> contents = new ArrayList<>(parts[0].getContents());
        if (parts[1] != null) {
            contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, ":", null));
            contents.addAll(parts[1].getContents());
        }
        if (parts[2] != null) {
            contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "@", null));
            contents.addAll(parts[2].getContents());
        }
        return contents;
    }

    /**
     * Returns the raw {@code --from} value with environment variable references preserved,
     * or empty if there is no {@code --from} flag.
     */
    public Optional<String> getFromValue() {
        Docker.Argument arg = fromArgument();
        return arg == null ? Optional.empty() : Optional.of(arg.getTextWithVariables());
    }

    /**
     * Returns true if the {@code --from} value refers to an earlier build stage (by name or
     * numeric index) rather than an external image.
     */
    public boolean isStageReference() {
        if (stageReference == null) {
            stageReference = computeStageReference();
        }
        return stageReference;
    }

    private boolean computeStageReference() {
        Docker.Argument arg = fromArgument();
        if (arg == null) {
            return false;
        }
        String value = arg.getText();
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
     * Returns the image name of the referenced external image, or {@code null} if the
     * {@code --from} names an earlier build stage or is absent.
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
     * Returns the instruction with its {@code --from} value replaced by {@code reference}
     * (e.g. {@code "nginx:1.25"}), or unchanged if there is no {@code --from} flag.
     */
    @Override
    public Docker.Instruction withImageReference(String reference) {
        Docker.Argument arg = fromArgument();
        if (arg == null) {
            return getTree();
        }
        Docker.Argument newValue = arg.withContents(referenceContents(reference));
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
        Optional<String> name = getImageName();
        if (!name.isPresent()) {
            return getTree();
        }
        String suffix = getDigest().map(d -> "@" + d).orElse("");
        return withImageReference(name.get() + ":" + tag + suffix);
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
