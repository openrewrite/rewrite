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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.trait.VisitFunction2;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The image reference carried by the {@code --from} of a {@code COPY}. Where that value names an
 * earlier build stage rather than an external image the image accessors return {@code null}; use
 * {@link #isStageReference()} to tell the two apart.
 */
@RequiredArgsConstructor
public class DockerCopyFrom implements DockerImageReference<Docker.Copy> {

    @Getter
    private final Cursor cursor;

    private @Nullable Boolean stageReference;
    private boolean componentsComputed;
    private Docker.@Nullable Argument @Nullable [] componentsValue;

    private Docker.@Nullable Argument fromArgument() {
        List<Docker.Flag> flags = getTree().getFlags();
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

    private Docker.@Nullable Argument @Nullable [] computeComponents() {
        if (isStageReference()) {
            return null;
        }
        Docker.Argument arg = fromArgument();
        return arg == null ? null : ImageReferences.split(arg.getContents(), arg.getPrefix());
    }

    public Optional<String> getFromValue() {
        Docker.Argument arg = fromArgument();
        return arg == null ? Optional.empty() : Optional.of(ArgumentContents.textWithVariables(arg));
    }

    /// A stage named by its `AS` alias or by its numeric index, rather than an external image.
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
        String value = ArgumentContents.text(arg);
        if (value == null) {
            return false;
        }
        if (isNonNegativeInteger(value)) {
            return true;
        }
        return stageNames().contains(value.toLowerCase(Locale.ROOT));
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

    /// The `AS` alias of every stage of the file, lowercased as Docker records and looks them up,
    /// so `COPY --from=Builder` finds `AS builder`.
    private Set<String> stageNames() {
        Docker.File file = cursor.firstEnclosing(Docker.File.class);
        Set<String> names = new HashSet<>();
        if (file != null) {
            for (Docker.Stage stage : file.getStages()) {
                Docker.From.As as = stage.getFrom().getAs();
                if (as != null) {
                    names.add(as.getName().getText().toLowerCase(Locale.ROOT));
                }
            }
        }
        return names;
    }

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

    /// Unchanged where there is no `--from` flag.
    @Override
    public Docker.Copy withImageReference(String reference) {
        Docker.Argument arg = fromArgument();
        if (arg == null) {
            return getTree();
        }
        return withFromValue(arg.withContents(ImageReferences.contents(reference)));
    }

    @Override
    public Docker.Copy withImageNameArgument(Docker.Argument imageName) {
        Docker.@Nullable Argument[] parts = components();
        Docker.Argument arg = fromArgument();
        if (parts == null || arg == null) {
            return getTree();
        }
        return withFromValue(arg.withContents(
          ImageReferences.contents(new Docker.Argument[]{imageName, parts[1], parts[2]})));
    }

    private Docker.Copy withFromValue(Docker.Argument value) {
        Docker.Copy copy = getTree();
        return copy.withFlags(ListUtils.map(copy.getFlags(), f ->
          "from".equals(f.getName()) ? f.withValue(value) : f));
    }

    @Override
    public Docker.Copy withTag(String tag) {
        Optional<String> name = getImageName();
        if (!name.isPresent()) {
            return getTree();
        }
        String suffix = getDigest().map(d -> "@" + d).orElse("");
        return withImageReference(name.get() + ":" + tag + suffix);
    }

    public static class Matcher extends DockerTraitMatcher<DockerCopyFrom> {
        private @Nullable String imageNamePattern;
        private @Nullable String tagPattern;
        private @Nullable String digestPattern;
        private boolean excludeStageReferences;

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

        /// Match only external image references.
        @Contract("-> this")
        public Matcher excludeStageReferences() {
            this.excludeStageReferences = true;
            return this;
        }

        @Override
        protected @Nullable DockerCopyFrom test(Cursor cursor) {
            if (!(cursor.getValue() instanceof Docker.Copy)) {
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
            };
        }
    }
}
