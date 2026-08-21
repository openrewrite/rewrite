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
package org.openrewrite.docker.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;

/**
 * Splits an image reference (the {@code name:tag@digest} form) into its component
 * {@link Docker.Argument}s. Used where the reference reaches the trait layer as text rather than as
 * a parse tree: the value of the {@code --from} flag of {@code COPY}/{@code ADD}, which the lexer
 * keeps as one token, and a reference handed in by a recipe. A {@code FROM} instruction is split by
 * the grammar instead, in the {@code IMAGE_REF} lexer mode.
 */
public final class ImageReferences {

    private ImageReferences() {
    }

    /**
     * Splits an image reference a recipe supplied as text, such as {@code "nginx:1.25"}, into
     * {@code {imageName, tag, digest}}. The parts are unquoted literals, so that a colon in the
     * text separates the tag rather than being kept as part of a quoted name.
     */
    public static Docker.@Nullable Argument[] split(String reference, Space prefix) {
        return split(ArgumentContents.of(reference, null), prefix);
    }

    /**
     * Splits image reference contents into {@code {imageName, tag, digest}}, with {@code tag} and
     * {@code digest} null when absent. A single quoted string is kept intact as the image name.
     */
    public static Docker.@Nullable Argument[] split(List<Docker.ArgumentContent> contents, Space prefix) {
        if (contents.size() == 1 && contents.get(0) instanceof Docker.Literal && ((Docker.Literal) contents.get(0)).isQuoted()) {
            Docker.Argument imageName = new Docker.Argument(randomId(), prefix, Markers.EMPTY, contents);
            return new Docker.@Nullable Argument[]{imageName, null, null};
        }

        List<Docker.ArgumentContent> imageNameContents = new ArrayList<>();
        List<Docker.ArgumentContent> tagContents = new ArrayList<>();
        List<Docker.ArgumentContent> digestContents = new ArrayList<>();

        boolean foundColon = false;
        boolean foundAt = false;

        for (Docker.ArgumentContent content : contents) {
            if (content instanceof Docker.Literal && !((Docker.Literal) content).isQuoted()) {
                String text = ((Docker.Literal) content).getText();

                int atIndex = text.indexOf('@');
                int colonIndex = tagColonIndex(text);

                if (atIndex >= 0 && !foundAt) {
                    foundAt = true;
                    String beforeAt = text.substring(0, atIndex);
                    String digestPart = text.substring(atIndex + 1);

                    int colonInBeforeAt = tagColonIndex(beforeAt);
                    if (colonInBeforeAt >= 0 && !foundColon) {
                        foundColon = true;
                        String imagePart = beforeAt.substring(0, colonInBeforeAt);
                        String tagPart = beforeAt.substring(colonInBeforeAt + 1);

                        if (!imagePart.isEmpty()) {
                            imageNameContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, imagePart, null));
                        }
                        if (!tagPart.isEmpty()) {
                            tagContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, tagPart, null));
                        }
                    } else {
                        if (!beforeAt.isEmpty()) {
                            if (foundColon) {
                                tagContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, beforeAt, null));
                            } else {
                                imageNameContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, beforeAt, null));
                            }
                        }
                    }
                    if (!digestPart.isEmpty()) {
                        digestContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, digestPart, null));
                    }
                } else if (colonIndex >= 0 && !foundColon && !foundAt) {
                    foundColon = true;
                    String imagePart = text.substring(0, colonIndex);
                    String tagPart = text.substring(colonIndex + 1);

                    if (!imagePart.isEmpty()) {
                        imageNameContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, imagePart, null));
                    }
                    if (!tagPart.isEmpty()) {
                        tagContents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, tagPart, null));
                    }
                } else {
                    if (foundAt) {
                        digestContents.add(content);
                    } else if (foundColon) {
                        tagContents.add(content);
                    } else {
                        imageNameContents.add(content);
                    }
                }
            } else {
                if (foundAt) {
                    digestContents.add(content);
                } else if (foundColon) {
                    tagContents.add(content);
                } else {
                    imageNameContents.add(content);
                }
            }
        }

        Docker.Argument imageName = new Docker.Argument(randomId(), prefix, Markers.EMPTY, imageNameContents);
        Docker.Argument tag = tagContents.isEmpty() ? null :
                new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, tagContents);
        Docker.Argument digest = digestContents.isEmpty() ? null :
                new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, digestContents);

        return new Docker.@Nullable Argument[]{imageName, tag, digest};
    }

    /**
     * The index of the colon that separates the tag, or {@code -1} when there is none. A colon
     * before the last {@code /} is a registry port rather than a tag separator.
     */
    private static int tagColonIndex(String text) {
        return text.indexOf(':', text.lastIndexOf('/') + 1);
    }
}
