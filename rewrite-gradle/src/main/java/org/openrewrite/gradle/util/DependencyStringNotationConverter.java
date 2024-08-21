/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.util;

import org.jspecify.annotations.Nullable;

public class DependencyStringNotationConverter {

    /**
     * @param notation a String in the format group:artifact:version
     * @return A corresponding Dependency or null if the notation could not be parsed
     */
    public static @Nullable Dependency parse(String notation) {
        int idx = notation.lastIndexOf('@');
        if (idx == -1) {
            return parse(notation, null);
        }

        int versionIdx = notation.lastIndexOf(':');
        if (versionIdx < idx) {
            return parse(notation.substring(0, idx), notation.substring(idx + 1));
        }

        return parse(notation, null);
    }

    private static @Nullable Dependency parse(String notation, @Nullable String ext) {
        Dependency dependency = new Dependency(null, null, null, null, ext);

        int count = 0;
        int idx = 0;
        int cur = 0;
        while (++cur < notation.length()) {
            if (':' == notation.charAt(cur)) {
                String fragment = notation.substring(idx, cur);
                dependency = assignValue(dependency, count, fragment);
                idx = cur + 1;
                count++;
            }
        }
        dependency = assignValue(dependency, count, notation.substring(idx, cur));
        count++;

        if (count < 2 || count > 4) {
            return null;
        }

        return dependency;
    }

    private static Dependency assignValue(Dependency dependency, int count, String fragment) {
        switch (count) {
            case 0:
                return dependency.withGroupId(fragment);
            case 1:
                return dependency.withArtifactId(fragment);
            case 2:
                return dependency.withVersion(fragment);
            case 3:
                return dependency.withClassifier(fragment);
            default:
                throw new IllegalArgumentException("Invalid count parameter: " + count);
        }
    }
}
