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
package org.openrewrite.javascript.internal.lock.bun;

import java.util.ArrayList;
import java.util.List;

/**
 * The posix path arithmetic of bun's src/resolver/resolve_path.zig ({@code joinAbsString}, {@code relative})
 * on paths relative to the project root, which stands in for bun's top-level directory.
 */
final class PosixPaths {

    private PosixPaths() {
    }

    /**
     * A path relative to the root, without {@code .} segments, {@code ..} resolved, no trailing slash; {@code ""} is
     * the root, and a path that climbs above it keeps its leading {@code ..} segments.
     */
    static String normalize(String path) {
        List<String> out = new ArrayList<>();
        for (String segment : path.replace('\\', '/').split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment) && !out.isEmpty() && !"..".equals(out.get(out.size() - 1))) {
                out.remove(out.size() - 1);
            } else {
                out.add(segment);
            }
        }
        return String.join("/", out);
    }

    static String join(String dir, String path) {
        if (dir.isEmpty()) {
            return normalize(path);
        }
        return normalize(dir + "/" + path);
    }

    /**
     * {@code relative(from, to)} for two root-relative paths.
     */
    static String relative(String from, String to) {
        String[] f = normalize(from).isEmpty() ? new String[0] : normalize(from).split("/");
        String[] t = normalize(to).isEmpty() ? new String[0] : normalize(to).split("/");
        int common = 0;
        while (common < f.length && common < t.length && f[common].equals(t[common])) {
            common++;
        }
        List<String> out = new ArrayList<>();
        for (int i = common; i < f.length; i++) {
            out.add("..");
        }
        for (int i = common; i < t.length; i++) {
            out.add(t[i]);
        }
        return String.join("/", out);
    }
}
