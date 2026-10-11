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
     * @throws Configuration.UsageError when a variable without a fallback is not set.
     */
    static String replaceEnvVariables(String value, HostEnvironment env) {
        Matcher m = ENV_VARIABLE.matcher(value);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
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
}
