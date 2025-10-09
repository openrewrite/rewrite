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
package org.openrewrite.gradle.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;

public class DependencyStringNotationConverter {

    /**
     * @param notation a String in the format group:artifact:version
     * @return A corresponding Dependency or null if the notation could not be parsed
     */
    public static @Nullable Dependency parse(@Nullable String notation) {
        if (notation == null) {
            return null;
        }
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

    private static @Nullable Dependency parse(@Nullable String notation, @Nullable String type) {
        if (notation == null) {
            return null;
        }
        String groupId = null;
        String artifactId = null;
        String version = null;
        String classifier = null;

        int count = 0;
        int idx = 0;
        int cur = 0;
        while (++cur < notation.length()) {
            if (':' == notation.charAt(cur)) {
                String fragment = notation.substring(idx, cur);
                switch (count) {
                    case 0:
                        groupId = fragment;
                        break;
                    case 1:
                        artifactId = fragment;
                        break;
                    case 2:
                        version = fragment;
                        break;
                    case 3:
                        classifier = fragment;
                        break;
                }
                idx = cur + 1;
                count++;
            }
        }
        String fragment = notation.substring(idx, cur);
        switch (count) {
            case 0:
                groupId = fragment;
                break;
            case 1:
                artifactId = fragment;
                break;
            case 2:
                version = fragment;
                break;
            case 3:
                classifier = fragment;
                break;
        }
        count++;

        if (count < 2 || count > 4) {
            return null;
        }

        return Dependency.builder()
                .gav(new GroupArtifactVersion(groupId, artifactId, version))
                .classifier(classifier)
                .type(type)
                .build();
    }
}
