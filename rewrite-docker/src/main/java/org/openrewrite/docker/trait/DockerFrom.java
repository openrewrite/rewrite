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
 * The base image of a {@code FROM} instruction, along with its platform flag and stage name.
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

    public @Nullable String getStageName() {
        Docker.From.As as = getTree().getAs();
        return as != null ? as.getName().getText() : null;
    }

    public Docker.Literal.@Nullable QuoteStyle getQuoteStyle() {
        return ArgumentContents.quoteStyle(getTree().getImageName());
    }

    /// Decomposes `reference` into image name, tag and digest, keeping the original prefix.
    @Override
    public Docker.From withImageReference(String reference) {
        Docker.From from = getTree();
        Docker.@Nullable Argument[] parts = ImageReferences.split(reference, from.getImageName().getPrefix());
        return from.withImageName(parts[0]).withTag(parts[1]).withDigest(parts[2]);
    }

    @Override
    public Docker.From withTag(String tag) {
        return getTree().withTag(new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY,
                ArgumentContents.of(tag, getQuoteStyle())));
    }

    @Override
    public Docker.From withImageNameArgument(Docker.Argument imageName) {
        return getTree().withImageName(imageName);
    }

    public static class Matcher extends DockerTraitMatcher<DockerFrom> {
        private @Nullable String imageNamePattern;
        private @Nullable String tagPattern;
        private @Nullable String digestPattern;
        private @Nullable String platformPattern;
        private boolean excludeScratch;
        private boolean onlyUnpinned;

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

        @Contract("_ -> this")
        public Matcher platform(String pattern) {
            this.platformPattern = pattern;
            return this;
        }

        @Contract("-> this")
        public Matcher excludeScratch() {
            this.excludeScratch = true;
            return this;
        }

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

            if (excludeScratch && image.isScratch()) {
                return null;
            }

            if (onlyUnpinned && !image.isUnpinned()) {
                return null;
            }

            if (imageNamePattern != null && !image.imageNameMatches(imageNamePattern)) {
                return null;
            }

            if (tagPattern != null) {
                if (from.getTag() == null) {
                    return null;
                }
                if (!image.tagMatches(tagPattern)) {
                    return null;
                }
            }

            if (digestPattern != null) {
                if (from.getDigest() == null) {
                    return null;
                }
                if (!image.digestMatches(digestPattern)) {
                    return null;
                }
            }

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
