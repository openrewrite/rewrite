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

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the value of one option out of the comma-separated option list a flag carries, as in
 * {@code --mount=type=bind,from=alpine,target=/opt}. The {@code ,} between one option and the next
 * and the {@code =} between a key and its value are contents of their own, so an option is found by
 * walking contents rather than by scanning text, and a value holding a quote or a variable reference
 * is returned as the contents it was parsed into.
 * <p>
 * As Docker does, a key is matched without regard to case, and only the first {@code =} of an option
 * separates it from its value, so the {@code =} of {@code env=A=B} belongs to the value.
 */
public final class FlagOptions {

    private FlagOptions() {
    }

    /**
     * The contents of the value of {@code key}, empty where it carries none, or {@code null} where
     * the option list does not hold the key at all.
     */
    public static @Nullable List<Docker.ArgumentContent> value(List<Docker.ArgumentContent> contents, String key) {
        int[] range = range(contents, key);
        return range == null ? null : new ArrayList<>(contents.subList(range[0], range[1]));
    }

    /**
     * The option list with the value of {@code key} replaced by {@code value}, or unchanged where
     * the list does not hold the key.
     */
    public static List<Docker.ArgumentContent> withValue(List<Docker.ArgumentContent> contents, String key,
                                                         List<Docker.ArgumentContent> value) {
        int[] range = range(contents, key);
        if (range == null) {
            return contents;
        }
        List<Docker.ArgumentContent> replaced = new ArrayList<>(contents.subList(0, range[0]));
        replaced.addAll(value);
        replaced.addAll(contents.subList(range[1], contents.size()));
        return replaced;
    }

    /// The `{start, end}` indices of the value of `key` in `contents`, or `null` where the option
    /// list holds no such key or holds it without a value.
    private static int @Nullable [] range(List<Docker.ArgumentContent> contents, String key) {
        int i = 0;
        while (i < contents.size()) {
            StringBuilder name = new StringBuilder();
            while (i < contents.size() && !isSeparator(contents.get(i), '=') && !isSeparator(contents.get(i), ',')) {
                if (contents.get(i) instanceof Docker.Literal) {
                    name.append(((Docker.Literal) contents.get(i)).getText());
                }
                i++;
            }
            if (i < contents.size() && isSeparator(contents.get(i), '=')) {
                int start = ++i;
                while (i < contents.size() && !isSeparator(contents.get(i), ',')) {
                    i++;
                }
                if (key.equalsIgnoreCase(name.toString())) {
                    return new int[]{start, i};
                }
            }
            i++;
        }
        return null;
    }

    private static boolean isSeparator(Docker.ArgumentContent content, char separator) {
        return content instanceof Docker.Literal && !((Docker.Literal) content).isQuoted() &&
                String.valueOf(separator).equals(((Docker.Literal) content).getText());
    }
}
