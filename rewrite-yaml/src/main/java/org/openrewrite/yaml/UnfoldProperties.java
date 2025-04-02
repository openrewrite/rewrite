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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
            description = "The keys which you do not want to unfold",
            example = "org.springframework.security")
    List<String> exclusions;

    public UnfoldProperties(@Nullable final List<String> exclusions) {
        this.exclusions = exclusions == null ? emptyList() : exclusions;
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
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry e, ExecutionContext ctx) {
                Yaml.Mapping.Entry entry = super.visitMappingEntry(e, ctx);

                String key = entry.getKey().getValue();
                if (key.contains(".") && !exclusions.contains(key)) {
                    String[] parts = key.split("\\.");
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

                return entry;
            }

            private Yaml.Mapping.Entry createNestedEntry(String[] keys, int index, Yaml.Block value) {
                if (index != keys.length - 1) {
                    Yaml.Mapping.Entry entry = createNestedEntry(keys, index + 1, value);
                    value = new Yaml.Mapping(randomId(), EMPTY, null, singletonList(entry), null, null, null);
                }

                Yaml.Scalar key = new Yaml.Scalar(randomId(), "", EMPTY, PLAIN, null, null, keys[index]);
                return new Yaml.Mapping.Entry(randomId(), "", EMPTY, key, "", value);
            }

            private int getIndentLevel(Yaml.Mapping.Entry entry) {
                String[] parts = entry.getPrefix().split("\\R");
                return parts.length > 1 ? countOccurrences(parts[1], " "): 0;
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
