/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The byte-exact bun {@code bun.lock} (JSONC) serialization primitives, shared by the surgical
 * {@link BunLockPatcher} and the {@link org.openrewrite.javascript.internal.lock.resolve.BunLockWriter}. bun's
 * package entries are compact single-line tuples {@code ["name@ver", "", <metadata>, "<sri>"]} whose metadata is
 * {@code {}} or {@code { "dependencies": { … } }} with the dependency map ASCII-sorted. Both the patcher (splicing
 * one member into a captured LST) and the writer (emitting a whole file) reproduce that shape, so the rules live
 * here once.
 */
public final class BunJson {

    private BunJson() {
    }

    /** A bun {@code packages} tuple: {@code ["<name>@<ver>", "", <metadata>, "<sri>"]}. */
    public static String renderTuple(String name, String version, @Nullable Map<String, String> deps, String integrity) {
        return "[" + quote(name + "@" + version) + ", " + quote("") + ", " +
                renderMetadata(deps) + ", " + quote(integrity) + "]";
    }

    /** bun's compact single-line metadata: {@code {}} or {@code { "dependencies": { "<dep>": "<range>", … } }}. */
    public static String renderMetadata(@Nullable Map<String, String> deps) {
        if (deps == null || deps.isEmpty()) {
            return "{}";
        }
        return "{ " + quote("dependencies") + ": " + renderInlineMap(deps) + " }";
    }

    /** An ASCII-sorted single-line object {@code { "<k>": "<v>", … }}. */
    public static String renderInlineMap(Map<String, String> map) {
        List<String> keys = new ArrayList<>(map.keySet());
        keys.sort(null); // bun sorts the dependency map by name (ASCII)
        StringBuilder sb = new StringBuilder("{ ");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(keys.get(i))).append(": ").append(quote(map.get(keys.get(i))));
        }
        return sb.append(" }").toString();
    }

    /** bun package names, versions, ranges and SRI integrity never contain {@code "}/{@code \\}, so a plain quote suffices. */
    public static String quote(String value) {
        return "\"" + value + "\"";
    }
}
