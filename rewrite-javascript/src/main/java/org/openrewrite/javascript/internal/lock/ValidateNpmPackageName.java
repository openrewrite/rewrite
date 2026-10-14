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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of validate-npm-package-name's errors, all that {@code validForOldPackages} counts: 4.0.0, which pnpm 11.2.2's
 * @pnpm/npm-package-arg calls, 7.0.2, which npm-package-arg 13.0.2 and pnpm's parse-overrides call, and the releases
 * npm 11 bundled before it (6.0.0, 6.0.1, and 6.0.2, whose code 7.0.0 keeps).
 */
public final class ValidateNpmPackageName {

    public enum Release {
        V4_0_0,
        V6_0_0,
        V6_0_1,
        V6_0_2,
        V7_0_2
    }

    private static final Pattern SCOPED_PACKAGE_PATTERN = V8RegExp.of("^(?:@([^/]+?)[/])?([^/]+?)$");

    private ValidateNpmPackageName() {
    }

    public static boolean validForOldPackages(String name, Release release) {
        return errors(name, release).isEmpty();
    }

    public static List<String> errors(String name, Release release) {
        // 7.0.1 refuses a leading hyphen, 6.0.2 rewords the excluded names, 6.0.1 looks at a scoped name's own start
        boolean v7 = release == Release.V7_0_2;
        boolean excludes = v7 || release == Release.V6_0_2;
        boolean scopedPeriod = excludes || release == Release.V6_0_1;
        List<String> errors = new ArrayList<>();
        if (name.isEmpty()) {
            errors.add("name length must be greater than zero");
        }
        if (name.startsWith(".")) {
            errors.add("name cannot start with a period");
        }
        if (v7 && name.startsWith("-")) {
            errors.add("name cannot start with a hyphen");
        }
        if (name.startsWith("_")) {
            errors.add("name cannot start with an underscore");
        }
        if (!EcmaScript.trim(name).equals(name)) {
            errors.add("name cannot contain leading or trailing spaces");
        }
        for (String excludedName : new String[]{"node_modules", "favicon.ico"}) {
            if (name.toLowerCase(Locale.ROOT).equals(excludedName)) {
                errors.add(excludedName + (excludes ? " is not a valid package name" : " is a blacklisted name"));
            }
        }
        if (!UriComponent.encodeURIComponent(name).equals(name)) {
            Matcher nameMatch = SCOPED_PACKAGE_PATTERN.matcher(name);
            if (nameMatch.find()) {
                String user = nameMatch.group(1);
                String pkg = nameMatch.group(2);
                if (scopedPeriod && pkg.startsWith(".")) {
                    errors.add("name cannot start with a period");
                }
                if (user != null && UriComponent.encodeURIComponent(user).equals(user) &&
                    UriComponent.encodeURIComponent(pkg).equals(pkg)) {
                    return errors;
                }
            }
            errors.add("name can only contain URL-friendly characters");
        }
        return errors;
    }
}
