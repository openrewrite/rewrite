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
 * Translates between the two forms an image reference takes: the {@code {imageName, tag, digest}} parts
 * a caller works in, and the flat contents of one {@link Docker.Argument}, where the {@code :} and
 * {@code @} separating those parts are contents of their own - the form the {@code IMAGE_REF} lexer
 * modes leave a parsed reference in. A colon inside a quoted name, a variable reference or a registry
 * port is never a content of its own, so reading the parts back needs no knowledge of which separates.
 */
public final class ImageReferences {

    private ImageReferences() {
    }

    public static Docker.@Nullable Argument[] split(String reference, Space prefix) {
        return split(contents(reference), prefix);
    }

    /// `tag` and `digest` are null where the reference does not carry them, and empty where it ends in
    /// a dangling separator.
    public static Docker.@Nullable Argument[] split(List<Docker.ArgumentContent> contents, Space prefix) {
        List<Docker.ArgumentContent> imageName = new ArrayList<>();
        List<Docker.ArgumentContent> tag = null;
        List<Docker.ArgumentContent> digest = null;
        List<Docker.ArgumentContent> part = imageName;

        for (Docker.ArgumentContent content : contents) {
            if (digest == null && isSeparator(content, '@')) {
                part = digest = new ArrayList<>();
            } else if (tag == null && digest == null && isSeparator(content, ':')) {
                part = tag = new ArrayList<>();
            } else {
                part.add(content);
            }
        }

        return new Docker.@Nullable Argument[]{
                new Docker.Argument(randomId(), prefix, Markers.EMPTY, imageName),
                tag == null ? null : new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, tag),
                digest == null ? null : new Docker.Argument(randomId(), Space.EMPTY, Markers.EMPTY, digest)};
    }

    /// Environment variable references are split out first, so a colon inside `${VAR:-default}`
    /// separates nothing.
    public static List<Docker.ArgumentContent> contents(String reference) {
        List<Docker.ArgumentContent> contents = new ArrayList<>();
        boolean tagged = false;
        boolean digested = false;

        for (Docker.ArgumentContent content : ArgumentContents.of(reference, null)) {
            if (!(content instanceof Docker.Literal)) {
                contents.add(content);
                continue;
            }
            String text = ((Docker.Literal) content).getText();
            int separator;
            while (!digested && (separator = separatorIndex(text, tagged)) >= 0) {
                addText(contents, text.substring(0, separator));
                digested = text.charAt(separator) == '@';
                tagged = true;
                contents.add(separator(text.charAt(separator)));
                text = text.substring(separator + 1);
            }
            addText(contents, text);
        }
        return contents;
    }

    public static List<Docker.ArgumentContent> contents(Docker.@Nullable Argument[] parts) {
        List<Docker.ArgumentContent> contents = new ArrayList<>(parts[0].getContents());
        if (parts[1] != null) {
            contents.add(separator(':'));
            contents.addAll(parts[1].getContents());
        }
        if (parts[2] != null) {
            contents.add(separator('@'));
            contents.addAll(parts[2].getContents());
        }
        return contents;
    }

    /// A colon before the last `/` is a registry port rather than a tag separator, and one after the
    /// tag belongs to the tag.
    private static int separatorIndex(String text, boolean tagged) {
        int at = text.indexOf('@');
        if (tagged) {
            return at;
        }
        int colon = text.indexOf(':', text.lastIndexOf('/') + 1);
        if (at >= 0 && colon >= 0) {
            return Math.min(at, colon);
        }
        return at >= 0 ? at : colon;
    }

    private static Docker.Literal separator(char separator) {
        return new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, String.valueOf(separator), null);
    }

    private static boolean isSeparator(Docker.ArgumentContent content, char separator) {
        return content instanceof Docker.Literal && !((Docker.Literal) content).isQuoted() &&
                String.valueOf(separator).equals(((Docker.Literal) content).getText());
    }

    private static void addText(List<Docker.ArgumentContent> contents, String text) {
        if (!text.isEmpty()) {
            contents.add(new Docker.Literal(randomId(), Space.EMPTY, Markers.EMPTY, text, null));
        }
    }
}
