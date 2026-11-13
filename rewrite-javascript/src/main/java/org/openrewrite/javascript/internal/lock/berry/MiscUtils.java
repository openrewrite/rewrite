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

import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of the parts of @yarnpkg/core sources/miscUtils.ts (Yarn 4.10.3) that reach the lock.
 */
final class MiscUtils {

    private static final Pattern ENV_VARIABLE =
            Pattern.compile("\\$\\{(?<variableName>[\\d\\w_]+)(?<colon>:)?(?:-(?<fallback>[^}]*))?}");

    // Yarn 4.11.0's, where a backslash before ${ keeps the reference as it is
    private static final Pattern ESCAPED_ENV_VARIABLE =
            Pattern.compile("\\\\?\\$\\{(?<variableName>[\\d\\w_]+)(?<colon>:)?(?:-(?<fallback>[^}]*))?}");

    // Yarn 4.13.0's scan
    private static final Pattern SUBSTITUTION = Pattern.compile("\\\\(?<escaped>[\\\\$}])|" +
            "\\$\\{(?<variable>[a-zA-Z]\\w*)(?<operator>:-|-|(?=\\}))|(?<unknown>\\$\\{)|\\}");

    private static final Pattern DURATION = Pattern.compile("(?<num>\\d*\\.?\\d+)(?<unit>ms|s|m|h|d|w)?");

    private static final Map<String, Double> DURATION_UNITS = new LinkedHashMap<>();

    static {
        DURATION_UNITS.put("ms", 1.0);
        DURATION_UNITS.put("s", 1000.0);
        DURATION_UNITS.put("m", 60.0 * 1000);
        DURATION_UNITS.put("h", 60.0 * 60 * 1000);
        DURATION_UNITS.put("d", 24.0 * 60 * 60 * 1000);
        DURATION_UNITS.put("w", 7.0 * 24 * 60 * 60 * 1000);
    }

    private MiscUtils() {
    }

    /**
     * {@code sortMap}: a stable sort by each mapper's string in turn, compared as {@code <} compares strings.
     */
    @SafeVarargs
    static <T> List<T> sortMap(Iterable<T> values, Function<T, String>... mappers) {
        List<T> asArray = new ArrayList<>();
        for (T value : values) {
            asArray.add(value);
        }
        List<List<String>> stringified = new ArrayList<>();
        for (Function<T, String> mapper : mappers) {
            List<String> layer = new ArrayList<>();
            for (T value : asArray) {
                layer.add(mapper.apply(value));
            }
            stringified.add(layer);
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < asArray.size(); i++) {
            indices.add(i);
        }
        indices.sort((a, b) -> {
            for (List<String> layer : stringified) {
                int comparison = Integer.signum(layer.get(a).compareTo(layer.get(b)));
                if (comparison != 0) {
                    return comparison;
                }
            }
            return 0;
        });
        List<T> out = new ArrayList<>();
        for (int index : indices) {
            out.add(asArray.get(index));
        }
        return out;
    }

    /**
     * @throws JsError for anything but the boolean spellings Yarn accepts.
     */
    static boolean parseBoolean(@Nullable Object value) {
        if ("true".equals(value) || "1".equals(value) || Integer.valueOf(1).equals(value) ||
            Boolean.TRUE.equals(value)) {
            return true;
        }
        if ("false".equals(value) || "0".equals(value) || Integer.valueOf(0).equals(value) ||
            Boolean.FALSE.equals(value)) {
            return false;
        }
        throw new JsError(JsError.ERROR, "Couldn't parse \"" + value + "\" as a boolean");
    }

    /**
     * {@code tryParseOptionalBoolean}: a boolean, {@link Syml#UNDEFINED} for undefined, or null when it does not
     * parse.
     */
    static @Nullable Object tryParseOptionalBoolean(@Nullable Object value) {
        if (value == Syml.UNDEFINED) {
            return value;
        }
        try {
            return parseBoolean(value);
        } catch (JsError e) {
            return null;
        }
    }

    /**
     * {@code replaceEnvVariables} as the release reads a setting: Yarn 4.10.3's, a backslash before {@code ${} keeping
     * it from 4.11.0, and from 4.13.0 a scan with nested defaults and escapes.
     *
     * @throws Configuration.UsageError when a variable without a fallback is not set, or for a substitution 4.13.0
     *                                  does not read.
     */
    static String replaceEnvVariables(String value, HostEnvironment env, YarnRelease release) {
        if (release.scansEnvironmentSubstitutions()) {
            return scanEnvVariables(value, env);
        }
        return replaceEnvVariables(value, env, release.escapesEnvironmentSubstitutions());
    }

    /**
     * Yarn 4.10.3's {@code replaceEnvVariables}.
     *
     * @throws Configuration.UsageError when a variable without a fallback is not set.
     */
    static String replaceEnvVariables(String value, HostEnvironment env) {
        return replaceEnvVariables(value, env, false);
    }

    private static String replaceEnvVariables(String value, HostEnvironment env, boolean escapes) {
        Matcher m = (escapes ? ESCAPED_ENV_VARIABLE : ENV_VARIABLE).matcher(value);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            if (escapes && m.group().startsWith("\\")) {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group().substring(1)));
                continue;
            }
            String variableName = m.group("variableName");
            String variableValue = env.reference(variableName);
            boolean variableExist = env.known().containsKey(variableName);
            String replacement;
            if (variableValue != null && !variableValue.isEmpty()) {
                replacement = variableValue;
            } else if (variableExist && m.group("colon") == null) {
                replacement = variableValue;
            } else if (m.group("fallback") != null) {
                replacement = m.group("fallback");
            } else {
                throw new Configuration.UsageError("Environment variable not found (" + variableName + ")");
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement == null ? "undefined" : replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    // Yarn 4.13.0's replaceEnvVariables: one scan of the input, whose skip shares the scan
    private static String scanEnvVariables(String input, HostEnvironment env) {
        StringBuilder output = new StringBuilder();
        int current = 0;
        int[] depth = {0};
        Matcher iterator = SUBSTITUTION.matcher(input);
        while (iterator.find()) {
            String match = iterator.group();
            output.append(input, current, iterator.start());
            current = iterator.end();
            String escaped = iterator.group("escaped");
            String variable = iterator.group("variable");
            if (escaped != null) {
                output.append(escaped);
            } else if (variable != null) {
                String operator = iterator.group("operator");
                String value = env.reference(variable);
                depth[0]++;
                if (operator.isEmpty() && value != null || ":-".equals(operator) && value != null &&
                                                             !value.isEmpty() ||
                    "-".equals(operator) && value != null) {
                    output.append(value);
                    current = skip(iterator, input, depth);
                } else if (operator.isEmpty()) {
                    throw new Configuration.UsageError("Environment variable not found (" + variable + ")");
                }
            } else if ("}".equals(match)) {
                if (depth[0] == 0) {
                    output.append(match);
                } else {
                    depth[0]--;
                }
            } else if (iterator.group("unknown") != null) {
                throw new Configuration.UsageError("Invalid environment variable substitution syntax: " + input);
            }
        }
        if (depth[0] > 0) {
            throw new Configuration.UsageError("Incomplete variable substitution in input: " + input);
        }
        return output.append(input.substring(current)).toString();
    }

    // the scan past a default whose variable is set, to the brace that closes it
    private static int skip(Matcher iterator, String input, int[] depth) {
        int limit = depth[0];
        while (iterator.find()) {
            if (iterator.group("variable") != null) {
                depth[0]++;
            } else if ("}".equals(iterator.group())) {
                if (--depth[0] < limit) {
                    return iterator.end();
                }
            }
        }
        return input.length();
    }

    /**
     * {@code parseDuration} (Yarn 4.11.0): a number in {@code unit}, or one with its own unit converted to it.
     *
     * @throws JsError where the value is no duration.
     */
    static Object parseDuration(String value, String unit) {
        Matcher match = DURATION.matcher(value);
        if (!match.matches()) {
            throw new JsError(JsError.ERROR, "Couldn't parse \"" + value + "\" as a duration");
        }
        double num = Double.parseDouble(match.group("num"));
        String matchUnit = match.group("unit");
        if (matchUnit == null) {
            return number(num);
        }
        return number(num * DURATION_UNITS.get(matchUnit) / DURATION_UNITS.get(unit));
    }

    // a number as the settings hold one, an int when it is one
    private static Object number(double d) {
        return d == (int) d ? (Object) (int) d : (Object) d;
    }
}
