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
 * Translates between the two forms an image reference takes: the {@code {imageName, tag, digest}}
 * parts a caller works in, and the flat contents of one {@link Docker.Argument}, where the {@code :}
 * and {@code @} separating those parts are contents of their own.
 * <p>
 * That is the form the {@code IMAGE_REF} and {@code FLAG_IMAGE_REF} lexer modes leave a parsed
 * reference in, so a reference a recipe supplies as text is modelled here the same way a parsed one
 * is, and reading the parts back needs no knowledge of which colon separates a tag: a colon inside a
 * quoted name, a variable reference or a registry port is never a content of its own.
 */
public final class ImageReferences {

    private ImageReferences() {
    }

    /**
     * Splits a reference a recipe supplied as text, such as {@code "nginx:1.25"}, into
     * {@code {imageName, tag, digest}}.
     */
    public static Docker.@Nullable Argument[] split(String reference, Space prefix) {
        return split(contents(reference), prefix);
    }

    /**
     * Groups contents into {@code {imageName, tag, digest}}, with {@code tag} and {@code digest}
     * null where the reference does not carry them, and empty where it ends in a dangling separator.
     */
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

    /**
     * The contents of a reference given as text. Environment variable references are split out
     * first, so a colon inside {@code ${VAR:-default}} separates nothing.
     */
    public static List<Docker.ArgumentContent> contents(String reference) {
        return separated(ArgumentContents.of(reference, null));
    }

    /**
     * Re-reads contents that were parsed as ordinary text as an image reference, leaving the
     * {@code :} and {@code @} separating its parts as contents of their own. This is what a
     * reference the lexer did not read in an image reference mode needs before {@link
     * #split(List, Space)} can group it, as in the {@code from=} option of a {@code --mount}.
     * A quoted literal is left whole, since a quote makes the whole of it one name.
     */
    public static List<Docker.ArgumentContent> separated(List<Docker.ArgumentContent> source) {
        List<Docker.ArgumentContent> contents = new ArrayList<>();
        boolean tagged = false;
        boolean digested = false;

        for (Docker.ArgumentContent content : source) {
            if (!(content instanceof Docker.Literal) || ((Docker.Literal) content).isQuoted()) {
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

    /**
     * Joins what {@link #separated(List)} split, merging adjacent unquoted literals back into one.
     * A reference the lexer did not read in an image reference mode has to go back into the tree
     * this way, since a reparse of the printed file would read it as one literal again.
     */
    public static List<Docker.ArgumentContent> joined(List<Docker.ArgumentContent> source) {
        List<Docker.ArgumentContent> contents = new ArrayList<>();
        for (Docker.ArgumentContent content : source) {
            Docker.Literal previous = lastUnquotedLiteral(contents);
            if (previous != null && content instanceof Docker.Literal && !((Docker.Literal) content).isQuoted()) {
                contents.set(contents.size() - 1,
                        previous.withText(previous.getText() + ((Docker.Literal) content).getText()));
            } else {
                contents.add(content);
            }
        }
        return contents;
    }

    private static Docker.@Nullable Literal lastUnquotedLiteral(List<Docker.ArgumentContent> contents) {
        if (contents.isEmpty()) {
            return null;
        }
        Docker.ArgumentContent last = contents.get(contents.size() - 1);
        return last instanceof Docker.Literal && !((Docker.Literal) last).isQuoted() ? (Docker.Literal) last : null;
    }

    /**
     * The contents of a reference held as its parts, the inverse of {@link #split(List, Space)}.
     */
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

    /// The index of the first separator still to come, or `-1` when the text holds none. A colon
    /// before the last `/` is a registry port rather than a tag separator, and one after the tag
    /// belongs to the tag.
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
