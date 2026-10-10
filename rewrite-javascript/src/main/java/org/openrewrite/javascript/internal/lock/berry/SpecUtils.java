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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Port of corepack sources/specUtils.ts {@code loadSpec} and {@code parseSpec} (corepack 0.35.0).
 */
final class SpecUtils {

    private static final Pattern NODE_MODULES = Pattern.compile("[\\\\/]node_modules[\\\\/](@[^\\\\/]*[\\\\/])?" +
                                                                "([^@\\\\/][^\\\\/]*)$");

    private SpecUtils() {
    }

    /**
     * The package.json whose {@code packageManager} corepack reads, by its path from where corepack runs.
     */
    @Value
    static class Found {
        String source;
        @Nullable Object packageManager;
    }

    /**
     * From {@code initialCwd} up to the project's root, by directory from that root, the first package.json that names
     * a packageManager; null when none up to the root does. {@code manifest} is a directory's package.json text.
     */
    static @Nullable Found loadSpec(String initialCwd, Function<String, @Nullable String> manifest) {
        String currCwd = initialCwd;
        String up = "";
        while (true) {
            String text = NODE_MODULES.matcher("/" + currCwd).find() ? null : manifest.apply(currCwd);
            if (text != null) {
                String source = up + "package.json";
                Object data;
                try {
                    data = Js.jsonParse(text);
                } catch (Js.SyntaxError e) {
                    data = null;
                }
                if (!(data instanceof Map) && !(data instanceof List)) {
                    throw corepackRejects("Invalid package.json in " + source);
                }
                Object packageManager = Js.get(data, "packageManager");
                if (packageManager != Syml.UNDEFINED && Js.truthy(packageManager)) {
                    return new Found(source, packageManager);
                }
            }
            if (currCwd.isEmpty()) {
                return null;
            }
            currCwd = currCwd.contains("/") ? PosixPath.dirname(currCwd) : "";
            up += "../";
        }
    }

    /**
     * {@code parseSpec} with {@code enforceExactVersion}, which throws Rejected as corepack words its usage error.
     */
    static void parseSpec(@Nullable Object raw, String source) {
        if (!(raw instanceof String)) {
            throw corepackRejects("Invalid package manager specification in " + source + "; expected a string");
        }
        String spec = (String) raw;
        int atIndex = spec.indexOf('@');
        if (atIndex == -1 || atIndex == spec.length() - 1) {
            throw corepackRejects("No version specified for " + spec + " in \"packageManager\" of " + source);
        }
        String name = spec.substring(0, atIndex);
        String range = spec.substring(atIndex + 1);
        boolean supported = "npm".equals(name) || "pnpm".equals(name) || "yarn".equals(name);
        if (WhatwgUrl.tryParse(range) == null) {
            if (!SemverUtils.valid(range)) {
                throw corepackRejects("Invalid package manager specification in " + source + " (" + spec + "); " +
                                      "expected a semver version");
            }
            if (!supported) {
                throw corepackRejects("Unsupported package manager specification (" + spec + ")");
            }
        } else if (supported) {
            throw corepackRejects("Illegal use of URL for known package manager. Instead, select a specific " +
                                  "version, or set COREPACK_ENABLE_UNSAFE_CUSTOM_URLS=1 in your environment (" + spec +
                                  ")");
        }
    }

    private static Rejected corepackRejects(String message) {
        return Rejected.rejects(Reason.MALFORMED_MANIFEST, message + " (corepack)");
    }
}
