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

import java.util.regex.Pattern;

/**
 * Port of glob-parent on POSIX: 2.0.0 over is-glob 2.0.1, which yarn 1.22.22 bundles for glob-base 0.3.0, and 5.1.2
 * over is-glob 4.0.3, which Yarn 4.10.3's fast-glob 3.3.2 calls.
 */
public final class GlobParent {

    public enum Release {
        V2_0_0,
        V5_1_2
    }

    private static final Pattern ENCLOSURE = V8RegExp.of("[\\{\\[].*[\\}\\]]$");
    private static final Pattern GLOBBY = V8RegExp.of("(^|[^\\\\])([\\{\\[]|\\([^\\)]+$)");
    private static final Pattern ESCAPED = V8RegExp.of("\\\\([\\!\\*\\?\\|\\[\\]\\(\\)\\{\\}])");

    private GlobParent() {
    }

    public static String globParent(String str, Release release) {
        if (release == Release.V2_0_0) {
            str += "a";
            do {
                str = PosixPath.dirname(str);
            } while (IsGlob.isGlob(str, IsGlob.Release.V2_0_1));
            return str;
        }
        if (ENCLOSURE.matcher(str).find()) {
            str += "/";
        }
        str += "a";
        do {
            str = PosixPath.dirname(str);
        } while (IsGlob.isGlob(str, IsGlob.Release.V4_0_3) || GLOBBY.matcher(str).find());
        return V8RegExp.replace(str, ESCAPED, true, "$1");
    }
}
