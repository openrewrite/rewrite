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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * The npm configuration arborist reads, under npm's own names and with npm's defaults.
 */
@Value
@Builder(toBuilder = true)
public class NpmOptions {

    public static final NpmOptions DEFAULT = NpmOptions.builder().build();

    /**
     * Thrown where npm would expand an environment variable in the npmrc: the environment is not an input.
     */
    public static final class NeedsEnvironment extends IllegalStateException {
        NeedsEnvironment(String text) {
            super("\"" + text + "\" in the npmrc reads the environment, which is not known");
        }
    }

    private static final Pattern ENV_EXPR = Pattern.compile("(?<!\\\\)(\\\\*)\\$\\{([^${}?]+)(\\?)?\\}");
    private static final Pattern LEADING_INTEGER = Pattern.compile("^[" + Js.WHITESPACE_CLASS + "]*([+-]?\\d+)");
    private static final List<String> KEYS = Arrays.asList(
            "lockfile-version", "legacy-peer-deps", "install-links", "omit-lockfile-registry-resolved");

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
     * The options a project's {@code .npmrc} sets, over npm's defaults, read as npm's config loads the
     * file ({@code ini.parse}, then {@code parseField} per key); keys this type does not hold yet are
     * passed over.
     *
     * @throws NeedsEnvironment for a {@code ${VAR}} reference in the value of a key read, or in a key
     *                          that could expand to one
     */
    public static NpmOptions fromNpmrc(@Nullable String npmrc) {
        NpmOptionsBuilder options = builder();
        if (npmrc == null) {
            return options.build();
        }
        for (Map.Entry<String, @Nullable Object> entry : Ini.decode(npmrc).entrySet()) {
            Pattern expansions = expansions(entry.getKey());
            if (expansions != null) {
                for (String name : KEYS) {
                    if (expansions.matcher(name).matches()) {
                        throw new NeedsEnvironment(entry.getKey());
                    }
                }
                continue;
            }
            String key = envReplace(entry.getKey());
            switch (key) {
                case "lockfile-version":
                    options.lockfileVersion(lockfileVersion(parseField(entry.getValue(), false)));
                    break;
                case "legacy-peer-deps":
                    options.legacyPeerDeps(truthy(parseField(entry.getValue(), true)));
                    break;
                case "install-links":
                    options.installLinks(truthy(parseField(entry.getValue(), true)));
                    break;
                case "omit-lockfile-registry-resolved":
                    options.omitLockfileRegistryResolved(truthy(parseField(entry.getValue(), true)));
                    break;
                default:
                    break;
            }
        }
        return options.build();
    }

    // npm's parseField for a Boolean definition or lockfile-version's literals: neither a list, string nor number type
    private static @Nullable Object parseField(@Nullable Object f, boolean isBool) {
        if (!(f instanceof String)) {
            return f;
        }
        String s = Js.trim((String) f);
        if (isBool && s.isEmpty()) {
            return Boolean.TRUE;
        }
        switch (s) {
            case "true":
                return Boolean.TRUE;
            case "false":
                return Boolean.FALSE;
            case "null":
                return null;
            case "undefined":
                return Js.UNDEFINED;
            default:
                return envReplace(s);
        }
    }

    // npm's envReplace with no environment to read from: a reference is a failure, an escaped one is literal
    private static String envReplace(String f) {
        Matcher m = ENV_EXPR.matcher(f);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String esc = m.group(1);
            if (esc.length() % 2 == 0) {
                throw new NeedsEnvironment(f);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(m.group().substring((esc.length() + 1) / 2)));
        }
        m.appendTail(out);
        return out.toString();
    }

    // every string a key with references could expand to, or null for a key without one
    private static @Nullable Pattern expansions(String key) {
        Matcher m = ENV_EXPR.matcher(key);
        StringBuilder regex = new StringBuilder();
        int end = 0;
        boolean any = false;
        while (m.find()) {
            String esc = m.group(1);
            regex.append(Pattern.quote(key.substring(end, m.start())));
            if (esc.length() % 2 == 0) {
                regex.append(Pattern.quote(esc.substring(esc.length() / 2))).append(".*");
                any = true;
            } else {
                regex.append(Pattern.quote(m.group().substring((esc.length() + 1) / 2)));
            }
            end = m.end();
        }
        regex.append(Pattern.quote(key.substring(end)));
        return any ? Pattern.compile(regex.toString(), Pattern.DOTALL) : null;
    }

    // the definition's flatten (obj[key] && parseInt(obj[key], 10)), then arborist's own check
    private static @Nullable Integer lockfileVersion(@Nullable Object value) {
        if (value instanceof Ini.Section) {
            throw new IllegalArgumentException("Cannot convert object to primitive value");
        }
        Object lfv = truthy(value) ? parseInt(Js.string(value)) : value;
        if (lfv == null || lfv == Js.UNDEFINED) {
            return null;
        }
        if (lfv instanceof Double) {
            double d = (Double) lfv;
            if (d == 1 || d == 2 || d == 3) {
                return (int) d;
            }
        }
        throw new IllegalArgumentException("Invalid lockfileVersion config: " + Js.string(lfv));
    }

    private static Double parseInt(String value) {
        Matcher digits = LEADING_INTEGER.matcher(value);
        return digits.find() ? new BigDecimal(digits.group(1)).doubleValue() : Double.NaN;
    }
}
