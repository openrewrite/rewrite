/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml.tree;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

public interface TomlKey extends Toml {

    /**
     * The canonical key path: the list of unquoted segment names. A simple
     * {@link Toml.Identifier} returns a singleton list; a {@link Toml.DottedKey}
     * returns one entry per segment. Quoted segments are returned without their
     * surrounding quotes, so {@code site."google.com"} yields
     * {@code ["site", "google.com"]} (length 2) and is distinguishable from
     * {@code site.google.com} which yields {@code ["site", "google", "com"]}
     * (length 3).
     */
    default List<String> getPath() {
        if (this instanceof Toml.DottedKey) {
            List<TomlRightPadded<Toml.Identifier>> segments = ((Toml.DottedKey) this).getPadding().getNames();
            List<String> path = new ArrayList<>(segments.size());
            for (TomlRightPadded<Toml.Identifier> segment : segments) {
                path.add(segment.getElement().getName());
            }
            return path;
        }
        return singletonList(((Toml.Identifier) this).getName());
    }

    /**
     * The dot-joined unquoted segment names. For a simple {@link Toml.Identifier}
     * this is the bare or unquoted name; for a {@link Toml.DottedKey} the segments
     * are joined with {@code '.'}. This loses the distinction between a quoted
     * segment containing a dot ({@code site."google.com"}) and a sequence of bare
     * segments ({@code site.google.com}); use {@link #getPath()} when that
     * distinction matters.
     */
    default String getName() {
        return String.join(".", getPath());
    }
}
