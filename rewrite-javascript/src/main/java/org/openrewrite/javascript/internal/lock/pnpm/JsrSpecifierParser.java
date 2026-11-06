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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.ValidateNpmPackageName;

/**
 * Port of @pnpm/resolving.jsr-specifier-parser src/index.ts (pnpm 11.2.2).
 */
final class JsrSpecifierParser {

    private JsrSpecifierParser() {
    }

    @Value
    static class JsrSpec {
        String jsrPkgName;
        String npmPkgName;

        @Nullable
        String versionSelector;
    }

    static @Nullable JsrSpec parseJsrSpecifier(String rawSpecifier, @Nullable String alias) {
        return parseJsrSpecifier(rawSpecifier, alias, PnpmRelease.PORTED);
    }

    static @Nullable JsrSpec parseJsrSpecifier(String rawSpecifier, @Nullable String alias, PnpmRelease release) {
        if (!rawSpecifier.startsWith("jsr:")) {
            return null;
        }
        rawSpecifier = rawSpecifier.substring("jsr:".length());
        if (!rawSpecifier.isEmpty() && rawSpecifier.charAt(0) == '@') {
            int index = rawSpecifier.lastIndexOf('@');
            if (index == 0) {
                return new JsrSpec(rawSpecifier, jsrToNpmPackageName(rawSpecifier, release), null);
            }
            String jsrPkgName = rawSpecifier.substring(0, index);
            return new JsrSpec(jsrPkgName, jsrToNpmPackageName(jsrPkgName, release),
                    rawSpecifier.substring(index + 1));
        }
        if (rawSpecifier.contains("@")) {
            throw new PnpmError("MISSING_JSR_PACKAGE_SCOPE", "Package names from JSR must have a scope");
        }
        if (!Js.truthy(alias)) {
            throw new PnpmError("INVALID_JSR_SPECIFIER", "JSR specifier '" + rawSpecifier +
                                                         "' is missing a package name");
        }
        return new JsrSpec(alias, jsrToNpmPackageName(alias, release), rawSpecifier);
    }

    // from 11.10.0 a name validate-npm-package-name 7.0.2 rejects is invalid; before, only one without a slash
    // (PnpmRelease#validatesAJsrPackageName)
    private static String jsrToNpmPackageName(String jsrPkgName, PnpmRelease release) {
        if (jsrPkgName.isEmpty() || jsrPkgName.charAt(0) != '@') {
            throw new PnpmError("MISSING_JSR_PACKAGE_SCOPE", "Package names from JSR must have a scope");
        }
        int sepIndex = jsrPkgName.indexOf('/');
        if (sepIndex == -1 || !ValidateNpmPackageName.validForOldPackages(jsrPkgName,
                ValidateNpmPackageName.Release.V7_0_2) && release.validatesAJsrPackageName()) {
            throw new PnpmError("INVALID_JSR_PACKAGE_NAME", "The package name '" + jsrPkgName + "' is invalid");
        }
        String scope = jsrPkgName.substring(0, sepIndex);
        String name = jsrPkgName.substring(sepIndex + 1);
        return "@jsr/" + scope.substring(1) + "__" + name;
    }
}
