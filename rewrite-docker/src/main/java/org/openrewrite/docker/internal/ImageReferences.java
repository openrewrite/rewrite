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
 * Splits a parsed image reference (the {@code name:tag@digest} form used by {@code FROM} and by
 * the {@code --from} flag of {@code COPY}/{@code ADD}) into its component {@link Docker.Argument}s.
 * Shared by the parser and the trait layer to keep a single source of truth.
 */
public final class ImageReferences {

    private ImageReferences() {
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
                int colonIndex = text.indexOf(':');

                if (atIndex >= 0 && !foundAt) {
                    foundAt = true;
                    String beforeAt = text.substring(0, atIndex);
                    String digestPart = text.substring(atIndex + 1);

                    int colonInBeforeAt = beforeAt.indexOf(':');
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
}
