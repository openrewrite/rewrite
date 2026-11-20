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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of is-extglob: 1.0.0, which yarn 1.22.22 bundles for micromatch 2.3.11, and 2.1.1, which Yarn 4.10.3's
 * fast-glob reaches through is-glob 4.0.3.
 */
public final class IsExtglob {

    public enum Release {
        V1_0_0,
        V2_1_1
    }

    private static final Pattern EXTGLOB_1_0_0 = V8RegExp.of("[@?!+*]\\(");
    private static final Pattern EXTGLOB_2_1_1 = V8RegExp.of("(\\\\).|([@?!+*]\\(.*\\))");

    private IsExtglob() {
    }

    public static boolean isExtglob(String str, Release release) {
        if (release == Release.V1_0_0) {
            return EXTGLOB_1_0_0.matcher(str).find();
        }
        if (str.isEmpty()) {
            return false;
        }
        while (true) {
            Matcher match = EXTGLOB_2_1_1.matcher(str);
            if (!match.find()) {
                return false;
            }
            if (match.group(2) != null) {
                return true;
            }
            str = str.substring(match.end());
        }
    }
}
