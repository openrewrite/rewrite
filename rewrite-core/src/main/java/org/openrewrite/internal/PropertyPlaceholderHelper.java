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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Simplified from Spring's PropertyPlaceholderHelper.
 *
 * <p>The same input strings are resolved repeatedly when used by callers
 * such as {@code JavaTemplate}, where every recipe matcher visit feeds
 * the same template string back through {@code replacePlaceholders}.
 * To avoid re-scanning for placeholder boundaries on every call, we cache
 * the parsed segment list per {@code (prefix, suffix, value)} tuple.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
public class PropertyPlaceholderHelper {
    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<>(4);
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Cache of parsed templates, scoped first by the {@code prefix + suffix}
     * configuration so that lookup is a single map operation per call. The
     * inner map is keyed by the input string. Parsing depends only on
     * prefix and suffix; the value separator is consulted at resolution
     * time and is therefore not part of the cache key.
     */
    private static final ConcurrentMap<String, ConcurrentMap<String, ParsedTemplate>> SEGMENT_CACHES =
            new ConcurrentHashMap<>();

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

    private final String escapePrefix;

    private final ConcurrentMap<String, ParsedTemplate> segmentCache;

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
        this.escapePrefix = "\\" + placeholderPrefix;
        this.segmentCache = SEGMENT_CACHES.computeIfAbsent(
                placeholderPrefix + '\u0001' + placeholderSuffix, k -> new ConcurrentHashMap<>());
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
        if (value.indexOf(placeholderPrefix) == -1) {
            return value;
        }

        ParsedTemplate parsed = segmentCache.get(value);
        if (parsed == null) {
            parsed = parseTemplate(value);
            // putIfAbsent keeps the first published instance to avoid replacing it under contention.
            ParsedTemplate existing = segmentCache.putIfAbsent(value, parsed);
            if (existing != null) {
                parsed = existing;
            }
        }

        if (parsed.segments.length == 1 && parsed.segments[0] instanceof String) {
            return (String) parsed.segments[0];
        }

        StringBuilder out = new StringBuilder(value.length());
        Set<String> visited = visitedPlaceholders;
        for (Object seg : parsed.segments) {
            if (seg instanceof String) {
                out.append((String) seg);
                continue;
            }
            Placeholder ph = (Placeholder) seg;
            String originalKey = ph.key;

            // Common case: no nested key. Resolve and inspect the value before
            // touching `visited` — for typical JavaTemplate-style use the
            // resolved value is a leaf string and we skip the HashSet
            // allocation entirely.
            if (!ph.nestedKey) {
                String propVal = resolveWithDefault(originalKey, placeholderResolver);
                if (propVal == null) {
                    out.append(placeholderPrefix).append(originalKey).append(placeholderSuffix);
                    continue;
                }
                if (propVal.indexOf(placeholderPrefix) < 0) {
                    out.append(propVal);
                    continue;
                }
                // Resolved value re-introduces a placeholder — recurse with
                // cycle protection.
                if (visited == null) {
                    visited = new HashSet<>(4);
                }
                if (!visited.add(originalKey)) {
                    out.append(placeholderPrefix).append(originalKey).append(placeholderSuffix);
                    continue;
                }
                try {
                    out.append(parseStringValue(propVal, placeholderResolver, visited));
                } finally {
                    visited.remove(originalKey);
                }
                continue;
            }

            // Nested key (rare; e.g. `${a${b}c}`). Track `originalKey` across
            // both the nested-key resolution and the propVal recursion below
            // to preserve the original cycle-detection scope.
            if (visited == null) {
                visited = new HashSet<>(4);
            }
            if (!visited.add(originalKey)) {
                out.append(placeholderPrefix).append(originalKey).append(placeholderSuffix);
                continue;
            }
            try {
                String resolvedKey = parseStringValue(originalKey, placeholderResolver, visited);
                String propVal = resolveWithDefault(resolvedKey, placeholderResolver);
                if (propVal == null) {
                    out.append(placeholderPrefix).append(resolvedKey).append(placeholderSuffix);
                } else if (propVal.indexOf(placeholderPrefix) >= 0) {
                    out.append(parseStringValue(propVal, placeholderResolver, visited));
                } else {
                    out.append(propVal);
                }
            } finally {
                visited.remove(originalKey);
            }
        }
        return out.toString();
    }

    private @Nullable String resolveWithDefault(String key, Function<String, @Nullable String> placeholderResolver) {
        String propVal = placeholderResolver.apply(key);
        if (propVal == null && valueSeparator != null) {
            int separatorIndex = key.indexOf(valueSeparator);
            if (separatorIndex != -1) {
                String actualPlaceholder = key.substring(0, separatorIndex);
                String defaultValue = key.substring(separatorIndex + valueSeparator.length());
                propVal = placeholderResolver.apply(actualPlaceholder);
                if (propVal == null) {
                    propVal = defaultValue;
                }
            }
        }
        return propVal;
    }

    private ParsedTemplate parseTemplate(String value) {
        int firstPrefixAt = value.indexOf(placeholderPrefix);
        if (firstPrefixAt == -1) {
            return new ParsedTemplate(new Object[]{value});
        }
        List<Object> segments = new ArrayList<>();
        int cursor = 0;
        int placeholderStart = firstPrefixAt;
        while (placeholderStart != -1) {
            int placeholderEnd = findPlaceholderEndIndex(value, placeholderStart);
            if (placeholderEnd == -1) {
                break;
            }
            if (placeholderStart > cursor) {
                segments.add(value.substring(cursor, placeholderStart));
            }
            String key = value.substring(placeholderStart + placeholderPrefix.length(), placeholderEnd);
            boolean nestedKey = key.indexOf(placeholderPrefix) >= 0;
            segments.add(new Placeholder(key, nestedKey));
            cursor = placeholderEnd + placeholderSuffix.length();
            placeholderStart = value.indexOf(placeholderPrefix, cursor);
        }
        if (cursor < value.length()) {
            segments.add(value.substring(cursor));
        }
        return new ParsedTemplate(segments.toArray());
    }

    private int findPlaceholderEndIndex(String buf, int startIndex) {
        int index = startIndex + placeholderPrefix.length();
        int suffixLen = placeholderSuffix.length();
        int simplePrefixLen = simplePrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (buf.regionMatches(index, placeholderSuffix, 0, suffixLen)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index += suffixLen;
                } else {
                    return index;
                }
            } else if (buf.regionMatches(index, simplePrefix, 0, simplePrefixLen)) {
                withinNestedPlaceholder++;
                index += simplePrefixLen;
            } else {
                index++;
            }
        }
        return -1;
    }

    /** Test hook: returns the cache size for the current {@code (prefix, suffix)} configuration. */
    int parsedTemplateCacheSize() {
        return segmentCache.size();
    }

    /** Test hook: clears the cache for the current {@code (prefix, suffix)} configuration. */
    void clearParsedTemplateCache() {
        segmentCache.clear();
    }

    private static final class ParsedTemplate {
        /**
         * Mixed array of {@link String} (literal text) and {@link Placeholder}
         * (an unresolved placeholder reference). Order matches the input text.
         */
        final Object[] segments;

        ParsedTemplate(Object[] segments) {
            this.segments = segments;
        }
    }

    private static final class Placeholder {
        final String key;
        /** {@code true} when {@link #key} itself contains the placeholder prefix and needs nested resolution. */
        final boolean nestedKey;

        Placeholder(String key, boolean nestedKey) {
            this.key = key;
            this.nestedKey = nestedKey;
        }
    }
}
