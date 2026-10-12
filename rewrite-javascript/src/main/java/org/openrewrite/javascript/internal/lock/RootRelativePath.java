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

import java.util.ArrayList;
import java.util.List;

/**
 * Paths relative to the project's root, standing in for the absolute directory a package manager joins paths onto:
 * {@code ""} is the root, and a path above it keeps its leading {@code ..} segments.
 */
public final class RootRelativePath {

    private RootRelativePath() {
    }

    /**
     * Without {@code .} segments, {@code ..} resolved, no trailing slash.
     */
    public static String normalize(String path) {
        List<String> out = new ArrayList<>();
        for (String segment : path.split("/")) {
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

    public static String join(String dir, String path) {
        return dir.isEmpty() ? normalize(path) : normalize(dir + "/" + path);
    }

    public static String relative(String from, String to) {
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

    public static String dirname(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    public static boolean climbsAboveRoot(String normalized) {
        return "..".equals(normalized) || normalized.startsWith("../");
    }
}
