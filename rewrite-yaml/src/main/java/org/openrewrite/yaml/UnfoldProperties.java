/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.countOccurrences;
import static org.openrewrite.internal.StringUtils.hasLineBreak;
import static org.openrewrite.marker.Markers.EMPTY;
import static org.openrewrite.yaml.tree.Yaml.Scalar.Style.PLAIN;

@Value
@EqualsAndHashCode(callSuper = false)
public class UnfoldProperties extends Recipe {
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    @Option(displayName = "Exclusions",
            description = "An optional list of [JsonPath Plus](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expressions to specify keys that should not be unfolded.",
            example = "$..[org.springframework.security]")
    List<String> exclusions;

    @Option(displayName = "Apply to",
            description = "An optional list of [JsonPath Plus](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expressions that specify which keys the recipe should target only. " +
                    "Only the properties matching these expressions will be unfolded.",
            example = "$..[org.springframework.security]")
    List<String> applyTo;

    public UnfoldProperties(@Nullable final List<String> exclusions, @Nullable final List<String> applyTo) {
        this.exclusions = exclusions == null ? emptyList() : exclusions;
        this.applyTo = applyTo == null ? emptyList() : applyTo;
    }

    @Override
    public String getDisplayName() {
        return "Unfold YAML properties";
    }

    @Override
    public String getDescription() {
        return "Transforms dot-separated property keys in YAML files into nested map hierarchies to enhance clarity and readability, or for compatibility with tools expecting structured YAML.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<JsonPathMatcher> exclusionMatchers = exclusions.stream().map(JsonPathMatcher::new).collect(toList());
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                Yaml.Document doc = super.visitDocument(document, ctx);
                doAfterVisit(new MergeDuplicateSectionsVisitor<>(doc));
                return doc;
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry e, ExecutionContext ctx) {
                Yaml.Mapping.Entry entry = super.visitMappingEntry(e, ctx);

                String key = entry.getKey().getValue();
                if (key.contains(".")) {
                    boolean foundMatch = false;
                    Cursor c = getCursor();
                    while (!foundMatch && !c.isRoot()) {
                        Cursor current = c;
                        foundMatch = exclusionMatchers.stream().anyMatch(it -> it.matches(current));
                        if (foundMatch) {
                            break;
                        } else {
                            c = c.getParent();
                        }
                    }
                    if (!foundMatch) {
                        List<String> parts = getParts(key);
                        if (parts.size() > 1) {
                            Yaml.Mapping.Entry nestedEntry = createNestedEntry(parts, 0, entry.getValue()).withPrefix(entry.getPrefix());
                            Yaml.Mapping.Entry newEntry = maybeAutoFormat(entry, nestedEntry, entry.getValue(), ctx, getCursor());

                            if (shouldShift()) {
                                int identLevel = Math.abs(getIndentLevel(entry) - getIndentLevel(newEntry));
                                if (!hasLineBreak(entry.getPrefix()) && hasLineBreak(newEntry.getPrefix())) {
                                    newEntry = newEntry.withPrefix(substringOfAfterFirstLineBreak(entry.getPrefix()));
                                }
                                doAfterVisit(new ShiftFormatLeftVisitor<>(newEntry, identLevel));
                            }

                            return newEntry;
                        }
                    }
                }

                return entry;
            }

            /**
             * Splits a key into parts while respecting certain exclusion rules.
             * The method ensures certain segments of the key are kept together as defined in the exclusion list.
             * It also considers the applyTo list during the split process.
             *
             * @param key the full key to be split into parts
             * @return a list of strings representing the split parts of the key
             */
            private List<String> getParts(String key) {
                String parentKey = getParentKey();
                List<String> keepTogether = new ArrayList<>();
                for (String ex : exclusions) {
                    keepTogether.addAll(matches(key, ex, parentKey));
                }

                List<String> result = new ArrayList<>();
                List<String> parts = Arrays.asList(key.split("\\."));
                outer:
                for (int i = 0; i < parts.size(); ) {
                    for (String group : keepTogether) {
                        List<String> groupParts = Arrays.asList(group.split("\\."));
                        if (i + groupParts.size() <= parts.size()) {
                            List<String> subList = parts.subList(i, i + groupParts.size());
                            if (subList.equals(groupParts)) {
                                result.add(String.join(".", groupParts));
                                i += groupParts.size();
                                continue outer;
                            }
                        }
                    }
                    result.add(parts.get(i));
                    i++;
                }

                if (!applyTo.isEmpty()) {
                    if (applyTo.stream().allMatch(it -> matches(key, it, parentKey).isEmpty())) {
                        return emptyList();
                    }
                }

                return result;
            }

            private String getParentKey() {
                StringBuilder parentKey = new StringBuilder();
                Cursor c = getCursor().getParent();
                while (c != null) {
                    if (c.getValue() instanceof Yaml.Mapping.Entry) {
                        parentKey.insert(0, ((Yaml.Mapping.Entry) c.getValue()).getKey().getValue() + ".");
                    }
                    c = c.getParent();
                }
                return parentKey.length() == 0 ? "" : parentKey.substring(0, parentKey.length() - 1);
            }

            /**
             * Matches a key against a JsonPath pattern.
             * It uses a custom JsonPathParser to parse keys with dots, like `logging.level`, and support the @property.match operator.
             *
             * @return found group or empty if no match was found
             */
            private List<String> matches(String key, String pattern, String parentKey) {
                // Recursive descent
                List<String> result = new ArrayList<>();
                if (pattern.startsWith("$..")) {
                    pattern = pattern.substring(3);
                }

                // Starts from root
                if (pattern.startsWith("$.")) {
                    pattern = pattern.replace("$." + parentKey, "");
                    if (pattern.startsWith(".")) {
                        pattern = pattern.substring(1);
                    }
                }

                // Handle parent-child conditions like: `$..[logging.level][?(<condition>)]` and `$..logging.level[?(<condition>)]`
                if (pattern.startsWith("[") && pattern.contains("][")) {
                    int secondBracketStart = pattern.indexOf('[', 1);
                    String secondBracket = pattern.substring(secondBracketStart);
                    String valueOfFirstBracket = pattern.substring(1, secondBracketStart - 1);
                    List<String> firstBracketMatches = matches(key, valueOfFirstBracket, parentKey);
                    for (String firstBracketMatch : firstBracketMatches) {
                        if (key.startsWith(firstBracketMatch) && key.length() > firstBracketMatch.length()) {
                            result.addAll(matches(key.substring(firstBracketMatch.length() + 1), secondBracket, (!parentKey.isEmpty() ? parentKey + "." : parentKey) + valueOfFirstBracket));
                        }
                    }
                    pattern = pattern.substring(1, secondBracketStart - 1) + secondBracket;
                }
                if (!pattern.startsWith("[") && pattern.contains("[") && parentKey.contains(pattern.split("\\[")[0])) {
                    pattern = "[" + pattern.split("\\[")[1];
                }

                // property in brackets
                if (pattern.startsWith("[") && pattern.endsWith("]")) {
                    pattern = pattern.substring(1, pattern.length() - 1);
                }

                // properties can be wrapped in quotes
                if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
                    pattern = pattern.substring(1, pattern.length() - 1);
                } else if (pattern.startsWith("'") && pattern.endsWith("'")) {
                    pattern = pattern.substring(1, pattern.length() - 1);
                }

                if (key.contains(pattern)) {
                    result.add(pattern);
                } else if (pattern.startsWith("?(@property.match(/") && pattern.endsWith("/))")) {
                    pattern = pattern.substring(19, pattern.length() - 3);
                    Matcher m = Pattern.compile(".*(" + pattern + ").*").matcher(key);
                    if (m.matches()) {
                        String match = m.group(1).isEmpty() ? m.group(0) : m.group(1);
                        if (match.endsWith(".")) {
                            match = match.substring(0, match.length() - 1);
                        }
                        result.add(match);
                    }
                }
                return result;
            }

            private Yaml.Mapping.Entry createNestedEntry(List<String> keys, int index, Yaml.Block value) {
                if (index != keys.size() - 1) {
                    Yaml.Mapping.Entry entry = createNestedEntry(keys, index + 1, value);
                    value = new Yaml.Mapping(randomId(), EMPTY, null, singletonList(entry), null, null, null);
                }

                Yaml.Scalar key = new Yaml.Scalar(randomId(), "", EMPTY, PLAIN, null, null, keys.get(index));
                return new Yaml.Mapping.Entry(randomId(), "", EMPTY, key, "", value);
            }

            private int getIndentLevel(Yaml.Mapping.Entry entry) {
                String[] parts = entry.getPrefix().split("\\R");
                return parts.length > 1 ? countOccurrences(parts[1], " ") : 0;
            }

            /**
             * MaybeAutoFormat cannot determine the proper indenting.
             * So shift when the first key with dots is targeted:
             *
             * <pre>
             * {@code
             * a.b:           # <-- shift
             *   b.c:
             * a2.b2: true    # <-- shift
             * a3:
             *  b3:
             *   c3.d4:       # <-- shift
             *    e4.f5.g6:
             *     h3: true
             * }
             * </pre>
             */
            private boolean shouldShift() {
                try {
                    getCursor().dropParentUntil(it -> it instanceof Yaml.Mapping.Entry && ((Yaml.Mapping.Entry) it).getKey().getValue().contains("."));
                    return false;
                } catch (IllegalStateException ignored) {
                    // `IllegalStateException("Expected to find a matching parent")` means no parent with dot in key can be found
                    return true;
                }
            }

            private String substringOfAfterFirstLineBreak(String s) {
                String[] lines = LINE_BREAK.split(s, -1);
                return lines.length > 1 ? String.join("\n", Arrays.copyOfRange(lines, 1, lines.length)) : "";
            }
        };
    }
}
