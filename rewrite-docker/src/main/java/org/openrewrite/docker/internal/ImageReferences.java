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
 * Splits an image reference a recipe supplied as text, such as {@code "nginx:1.25"}, into its
 * component {@link Docker.Argument}s. A reference read from a Dockerfile is split by the grammar
 * instead, in the {@code IMAGE_REF} and {@code FLAG_IMAGE_REF} lexer modes, so this is only reached
 * for text that was never parsed.
 */
public final class ImageReferences {

    private ImageReferences() {
    }

    /**
     * Splits {@code reference} into {@code {imageName, tag, digest}}, with {@code tag} and
     * {@code digest} null when the reference does not carry them. Environment variable references
     * are split out first, so a colon inside {@code ${VAR:-default}} separates nothing.
     */
    public static Docker.@Nullable Argument[] split(String reference, Space prefix) {
        List<Docker.ArgumentContent> imageName = new ArrayList<>();
        List<Docker.ArgumentContent> tag = null;
        List<Docker.ArgumentContent> digest = null;
        List<Docker.ArgumentContent> part = imageName;

        for (Docker.ArgumentContent content : ArgumentContents.of(reference, null)) {
            if (!(content instanceof Docker.Literal)) {
                part.add(content);
                continue;
            }
            String text = ((Docker.Literal) content).getText();
            int separator;
            while ((separator = separatorIndex(text, tag != null, digest != null)) >= 0) {
                addText(part, text.substring(0, separator));
                part = text.charAt(separator) == '@' ? (digest = new ArrayList<>()) : (tag = new ArrayList<>());
                text = text.substring(separator + 1);
            }
            addText(part, text);
        }

        return new Docker.@Nullable Argument[]{
                new Docker.Argument(randomId(), prefix, Markers.EMPTY, imageName),
                tag == null ? null : new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, tag),
                digest == null ? null : new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, digest)};
    }

    /// The index of the first separator still to come, or `-1` when the text holds none. A colon
    /// before the last `/` is a registry port rather than a tag separator, and one after a `@` or a
    /// tag that was already found belongs to the part that holds it.
    private static int separatorIndex(String text, boolean taggedAlready, boolean digestedAlready) {
        int at = digestedAlready ? -1 : text.indexOf('@');
        int colon = taggedAlready || digestedAlready ? -1 : text.indexOf(':', text.lastIndexOf('/') + 1);
        if (at >= 0 && colon >= 0) {
            return Math.min(at, colon);
        }
        return at >= 0 ? at : colon;
    }

    private static void addText(List<Docker.ArgumentContent> part, String text) {
        if (!text.isEmpty()) {
            part.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text, null));
        }
    }
}
