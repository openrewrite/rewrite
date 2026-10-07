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
package org.openrewrite.javascript.internal.lock.berry;

import java.util.ArrayList;
import java.util.List;

/**
 * Node.js {@code path.posix.normalize}, which @yarnpkg/fslib's {@code ppath} is.
 */
final class PortablePath {

    private PortablePath() {
    }

    static String normalize(String path) {
        if (path.isEmpty()) {
            return ".";
        }
        boolean isAbsolute = path.charAt(0) == '/';
        boolean trailingSeparator = path.charAt(path.length() - 1) == '/';
        List<String> out = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!out.isEmpty() && !"..".equals(out.get(out.size() - 1))) {
                    out.remove(out.size() - 1);
                } else if (!isAbsolute) {
                    out.add("..");
                }
                continue;
            }
            out.add(segment);
        }
        String normalized = String.join("/", out);
        if (normalized.isEmpty() && !isAbsolute) {
            normalized = ".";
        }
        if (!normalized.isEmpty() && trailingSeparator) {
            normalized += "/";
        }
        return isAbsolute ? "/" + normalized : normalized;
    }
}
