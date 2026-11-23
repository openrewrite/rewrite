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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Port of yarn src/util/normalize-pattern.js (yarn 1.22.22): a pattern {@code name@range} split into its name and
 * range, {@code latest} when it names no range and {@code *} when the range is empty.
 */
@Value
class NormalizedPattern {
    boolean hasVersion;
    String name;
    String range;

    static NormalizedPattern normalizePattern(String pattern) {
        boolean hasVersion = false;
        String range = "latest";
        String name = pattern;

        boolean isScoped = false;
        if (name.startsWith("@")) {
            isScoped = true;
            name = name.substring(1);
        }

        List<String> parts = new ArrayList<>(Arrays.asList(name.split("@", -1)));
        if (parts.size() > 1) {
            name = parts.remove(0);
            range = String.join("@", parts);
            if (!range.isEmpty()) {
                hasVersion = true;
            } else {
                range = "*";
            }
        }

        if (isScoped) {
            name = "@" + name;
        }
        return new NormalizedPattern(hasVersion, name, range);
    }
}
