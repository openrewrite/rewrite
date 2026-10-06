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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The npm configuration arborist reads, under npm's own names and with npm's defaults.
 */
@Value
@Builder(toBuilder = true)
public class NpmOptions {

    public static final NpmOptions DEFAULT = NpmOptions.builder().build();

    private static final Pattern LEADING_INTEGER = Pattern.compile("^[+-]?\\d+");

    /**
     * {@code lockfile-version}: the version to write, or null to keep the lock's own (v1 is upgraded).
     */
    @Nullable
    Integer lockfileVersion;

    /**
     * {@code legacy-peer-deps}
     */
    boolean legacyPeerDeps;

    /**
     * {@code install-links}
     */
    boolean installLinks;

    /**
     * {@code omit-lockfile-registry-resolved}
     */
    boolean omitLockfileRegistryResolved;

    /**
     * The options a project's {@code .npmrc} sets, over npm's defaults; keys this type does not hold
     * yet are passed over.
     */
    public static NpmOptions fromNpmrc(@Nullable String npmrc) {
        NpmOptionsBuilder options = builder();
        if (npmrc == null) {
            return options.build();
        }
        for (String line : npmrc.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            String key = (eq < 0 ? trimmed : trimmed.substring(0, eq)).trim();
            String value = eq < 0 ? "true" : trimmed.substring(eq + 1).trim();
            switch (key) {
                case "lockfile-version":
                    options.lockfileVersion(lockfileVersion(value));
                    break;
                case "legacy-peer-deps":
                    options.legacyPeerDeps(bool(value));
                    break;
                case "install-links":
                    options.installLinks(bool(value));
                    break;
                case "omit-lockfile-registry-resolved":
                    options.omitLockfileRegistryResolved(bool(value));
                    break;
                default:
                    break;
            }
        }
        return options.build();
    }

    // npm's parseField maps only these four words; any other value stays a string, which arborist tests for truthiness
    private static boolean bool(String value) {
        return !"false".equals(value) && !"null".equals(value) && !"undefined".equals(value);
    }

    // npm's parseField, then the definition's flatten (obj[key] && parseInt(obj[key], 10)), then arborist's own check
    private static @Nullable Integer lockfileVersion(String value) {
        if ("null".equals(value) || "undefined".equals(value)) {
            return null;
        }
        String lfv = value.isEmpty() || "false".equals(value) ? value : "true".equals(value) ? "NaN" : parseInt(value);
        if ("1".equals(lfv) || "2".equals(lfv) || "3".equals(lfv)) {
            return Integer.parseInt(lfv);
        }
        throw new IllegalArgumentException("Invalid lockfileVersion config: " + lfv);
    }

    private static String parseInt(String value) {
        Matcher digits = LEADING_INTEGER.matcher(value);
        return digits.find() ? Js.numberToString(new BigDecimal(digits.group()).doubleValue()) : "NaN";
    }
}
