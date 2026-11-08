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

import org.openrewrite.javascript.semver.NodeSemver;

/**
 * Port of yarn src/util/semver.js satisfiesWithPrereleases (yarn 1.22.22).
 */
final class Semver {
    private Semver() {
    }

    static boolean satisfiesWithPrereleases(String version, String range, boolean loose) {
        NodeSemver.Options options = loose ? Config.LOOSE_SEMVER : Config.SEMVER;
        String formatted = NodeSemver.validRange(range, options);
        if (formatted == null || version.isEmpty() || NodeSemver.valid(version, options) == null) {
            return false;
        }
        // Test each comparator directly, without Range.test's exclusion of prereleases. For an exclusive
        // upper bound without a prerelease, Yarn first increments the bound's prerelease to 0.
        for (String set : formatted.split("\\|\\|", -1)) {
            boolean matches = true;
            for (String comparator : set.trim().split(" +")) {
                if (comparator.isEmpty() || "*".equals(comparator)) {
                    continue;
                }
                int i = 0;
                while (i < comparator.length() && "<>=!".indexOf(comparator.charAt(i)) >= 0) {
                    i++;
                }
                String operator = comparator.substring(0, i);
                String bound = comparator.substring(i);
                if ("<".equals(operator) && NodeSemver.prerelease(bound, options) == null) {
                    int build = bound.indexOf('+');
                    bound = (build < 0 ? bound : bound.substring(0, build)) + "-0";
                }
                int comparison = NodeSemver.compare(version, bound, options);
                switch (operator) {
                    case "<": matches = comparison < 0; break;
                    case "<=": matches = comparison <= 0; break;
                    case ">": matches = comparison > 0; break;
                    case ">=": matches = comparison >= 0; break;
                    default: matches = comparison == 0;
                }
                if (!matches) {
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }
}
