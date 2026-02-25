/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.internal;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Simplified from Spring's PropertyPlaceholderHelper.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
public class PropertyPlaceholderHelper {
    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    static {
        wellKnownSimplePrefixes.put("}", "{");
        wellKnownSimplePrefixes.put("]", "[");
        wellKnownSimplePrefixes.put(")", "(");
    }

    private final String placeholderPrefix;
    private final String placeholderSuffix;

    private final String simplePrefix;

    @Nullable
    private final String valueSeparator;

    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix,
                                     @Nullable String valueSeparator) {
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
        if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
            this.simplePrefix = simplePrefixForSuffix;
        } else {
            this.simplePrefix = this.placeholderPrefix;
        }
        this.valueSeparator = valueSeparator;
    }

    @Contract("null -> false")
    public boolean hasPlaceholders(@Nullable String value) {
        if (value == null) {
            return false;
        }
        int startIndex = value.indexOf(placeholderPrefix);
        return startIndex > -1 && value.indexOf(placeholderSuffix, startIndex) > startIndex;
    }

    public List<String> getPlaceholders(@Nullable String value) {
        if (value == null) {
            return emptyList();
        }

        List<String> placeholders = new ArrayList<>();
        Matcher matcher = PROPERTY_PATTERN.matcher(value);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    /**
     * Replace placeholders in the given value, resolving each against the supplied properties.
     * A backslash before the placeholder prefix (e.g. {@code \${...}}) escapes it as a literal.
     */
    public String replacePlaceholders(String value, final Properties properties) {
        return replacePlaceholders(value, properties::getProperty);
    }

    /**
     * Replace placeholders in the given value, resolving each via {@code placeholderResolver}.
     * A backslash before the placeholder prefix (e.g. {@code \${...}}) escapes it as a literal.
     */
    public String replacePlaceholders(String value, Function<String, @Nullable String> placeholderResolver) {
        // Support escaping: a backslash before the placeholder prefix produces a literal
        // prefix. E.g., for prefix "${", writing "\${" produces literal "${".
        String escapePrefix = "\\" + placeholderPrefix;
        boolean hasEscaped = value.contains(escapePrefix);
        if (hasEscaped) {
            value = value.replace(escapePrefix, "\u0000\u0001\u0002");
        }
        String result = parseStringValue(value, placeholderResolver, null);
        if (hasEscaped) {
            result = result.replace("\u0000\u0001\u0002", placeholderPrefix);
        }
        return result;
    }

    protected String parseStringValue(String value, Function<String, @Nullable String> placeholderResolver,
                                      @Nullable Set<String> visitedPlaceholders) {
        int startIndex = value.indexOf(placeholderPrefix);
        if (startIndex == -1) {
            return value;
        }

        StringBuilder result = new StringBuilder(value);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(result, startIndex);
            if (endIndex != -1) {
                String placeholder = result.substring(startIndex + placeholderPrefix.length(), endIndex);
                String originalPlaceholder = placeholder;
                if (visitedPlaceholders == null) {
                    visitedPlaceholders = new HashSet<>(4);
                }
                if (visitedPlaceholders.add(originalPlaceholder)) {
                    placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
                }
                // Recursive invocation, parsing placeholders contained in the placeholder key.
                // Now obtain the value for the fully resolved key...
                String propVal = placeholderResolver.apply(placeholder);
                if (propVal == null && valueSeparator != null) {
                    int separatorIndex = placeholder.indexOf(valueSeparator);
                    if (separatorIndex != -1) {
                        String actualPlaceholder = placeholder.substring(0, separatorIndex);
                        String defaultValue = placeholder.substring(separatorIndex + valueSeparator.length());
                        propVal = placeholderResolver.apply(actualPlaceholder);
                        if (propVal == null) {
                            propVal = defaultValue;
                        }
                    }
                }
                if (propVal != null) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
                    result.replace(startIndex, endIndex + placeholderSuffix.length(), propVal);

                    if (propVal.length() < endIndex - startIndex + 1) {
                        endIndex = startIndex + propVal.length();
                    }
                }

                // Proceed with unprocessed value.
                startIndex = result.indexOf(placeholderPrefix, endIndex);
                visitedPlaceholders.remove(originalPlaceholder);
            } else {
                startIndex = -1;
            }
        }
        return result.toString();
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index += placeholderSuffix.length();
                } else {
                    return index;
                }
            } else if (substringMatch(buf, index, simplePrefix)) {
                withinNestedPlaceholder++;
                index += simplePrefix.length();
            } else {
                index++;
            }
        }
        return -1;
    }

    private static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        if (index + substring.length() > str.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); i++) {
            if (str.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
