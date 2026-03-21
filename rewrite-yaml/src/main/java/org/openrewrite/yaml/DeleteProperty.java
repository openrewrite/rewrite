/*
 * Copyright 2021 the original author or authors.
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
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.NameCaseConvention.Compiled;
import org.openrewrite.marker.Marker;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteProperty extends Recipe {
    @Option(displayName = "Property key",
            description = "The key to be deleted. Supports glob patterns.",
            example = "management.metrics.binders.files.*")
    String propertyKey;

    @Deprecated
    @Option(displayName = "Coalesce",
            description = "(Deprecated: in a future version, this recipe will always use the `false` behavior)" +
                    " Simplify nested map hierarchies into their simplest dot separated property form.",
            required = false)
    @Nullable
    Boolean coalesce;

    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Defaults to `true`. If you want to use exact matching in your search, set this to `false`.",
            required = false)
    @Nullable
    Boolean relaxedBinding;


    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    String displayName = "Delete property";

    String description = "Delete a YAML property. Nested YAML mappings are interpreted as dot separated property names, i.e. " +
                "as Spring Boot interprets application.yml files like `a.b.c.d` or `a.b.c:d`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Compiled keyMatcher = (!Boolean.FALSE.equals(relaxedBinding) ?
                NameCaseConvention.LOWER_CAMEL :
                NameCaseConvention.EXACT).compile(propertyKey);

        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {

            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                if (documents != new ReplaceAliasWithAnchorValueVisitor<ExecutionContext>().visit(documents, ctx)) {
                    return documents;
                }
                return super.visitDocuments(documents, ctx);
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);

                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                        .filter(Yaml.Mapping.Entry.class::isInstance)
                        .map(Yaml.Mapping.Entry.class::cast)
                        .collect(toCollection(ArrayDeque::new));

                String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                        .map(e2 -> e2.getKey().getValue())
                        .collect(joining("."));

                if (keyMatcher.matchesGlob(prop)) {
                    e = ToBeRemoved.withMarker(e);
                    if (Boolean.TRUE.equals(coalesce)) {
                        maybeCoalesceProperties();
                    }
                }

                return e;
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                Yaml.Sequence s = super.visitSequence(sequence, ctx);
                boolean childModified = (s != sequence);
                List<Yaml.Sequence.Entry> entries = s.getEntries();
                if (entries.isEmpty()) {
                    return s;
                }

                boolean changed = false;
                entries = ListUtils.map(entries, entry -> ToBeRemoved.hasMarker(entry) ? null : entry);
                if (entries.size() != s.getEntries().size()) {
                    changed = true;
                }

                if ((changed || childModified) && s.getOpeningBracketPrefix() == null) {
                    List<Yaml.Sequence.Entry> fixedEntries = null;
                    for (int i = 1; i < entries.size(); i++) {
                        Yaml.Sequence.Entry entry = entries.get(i);
                        Yaml.Sequence.Entry prevEntry = entries.get(i - 1);
                        if (!containsNewline(entry.getPrefix()) && !endsWithBlockScalar(prevEntry.getBlock())) {
                            if (fixedEntries == null) {
                                fixedEntries = new ArrayList<>(entries);
                            }
                            fixedEntries.set(i, entry.withPrefix("\n" + entry.getPrefix()));
                        }
                    }
                    if (fixedEntries != null) {
                        entries = fixedEntries;
                    }
                }

                return entries.isEmpty() ? ToBeRemoved.withMarker(s) : s.withEntries(entries);
            }

            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, ExecutionContext ctx) {
                Yaml.Sequence.Entry e = super.visitSequenceEntry(entry, ctx);
                if (e.getBlock() instanceof Yaml.Mapping) {
                    Yaml.Mapping m = (Yaml.Mapping) e.getBlock();
                    if (ToBeRemoved.hasMarker(m)) {
                        return ToBeRemoved.withMarker(e);
                    }
                }
                return e;
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                boolean childModified = (m != mapping);

                boolean changed = false;
                List<Yaml.Mapping.Entry> entries = new ArrayList<>();
                String firstDeletedPrefix = null;
                boolean previousWasDeleted = false;
                String trailingInlineComment = null;
                for (Yaml.Mapping.Entry entry : m.getEntries()) {
                    if (ToBeRemoved.hasMarker(entry.getValue()) ||
                        ToBeRemoved.hasMarker(entry) ||
                        (entry.getValue() instanceof Yaml.Mapping && ((Yaml.Mapping) entry.getValue()).getEntries().isEmpty())) {
                        // Entry is being deleted - capture prefix from the first deleted entry before any kept entries
                        if (entries.isEmpty() && firstDeletedPrefix == null) {
                            firstDeletedPrefix = entry.getPrefix();
                        }
                        // Capture inline comment from the first deleted entry after kept entries
                        if (trailingInlineComment == null && !entries.isEmpty()) {
                            trailingInlineComment = extractInlineComment(entry.getPrefix());
                        }
                        changed = true;
                        previousWasDeleted = true;
                    } else {
                        if (entries.isEmpty() && firstDeletedPrefix != null && containsOnlyWhitespace(entry.getPrefix())) {
                            entry = entry.withPrefix(firstDeletedPrefix);
                        } else if (previousWasDeleted && !entries.isEmpty() && !containsNewline(entry.getPrefix())) {
                            entry = entry.withPrefix("\n" + entry.getPrefix());
                        }
                        entries.add(entry);
                        previousWasDeleted = false;
                        trailingInlineComment = null;
                    }
                }

                // Preserve inline comment from deleted trailing entries on the last kept entry
                if (trailingInlineComment != null) {
                    String comment = trailingInlineComment;
                    entries = ListUtils.mapLast(entries, lastKept -> {
                        if (lastKept.getValue() instanceof Yaml.Scalar &&
                            ((Yaml.Scalar) lastKept.getValue()).getStyle() == Yaml.Scalar.Style.PLAIN) {
                            Yaml.Scalar scalar = (Yaml.Scalar) lastKept.getValue();
                            return lastKept.withValue(
                                    scalar.withValue(scalar.getValue() + comment));
                        }
                        return lastKept;
                    });
                }

                if (changed) {
                    m = m.withEntries(entries);
                    if (entries.isEmpty()) {
                        m = ToBeRemoved.withMarker(m);
                    }

                    if (getCursor().getParentOrThrow().getValue() instanceof Yaml.Document) {
                        Yaml.Document document = getCursor().getParentOrThrow().getValue();
                        if (!document.isExplicit()) {
                            m = m.withEntries(m.getEntries());
                        }
                    }
                }

                if ((changed || childModified) && m.getOpeningBracePrefix() == null) {
                    List<Yaml.Mapping.Entry> currentEntries = m.getEntries();
                    List<Yaml.Mapping.Entry> fixedEntries = null;
                    for (int i = 1; i < currentEntries.size(); i++) {
                        Yaml.Mapping.Entry entry = currentEntries.get(i);
                        Yaml.Mapping.Entry prevEntry = currentEntries.get(i - 1);
                        if (!containsNewline(entry.getPrefix()) && !endsWithBlockScalar(prevEntry)) {
                            if (fixedEntries == null) {
                                fixedEntries = new ArrayList<>(currentEntries);
                            }
                            fixedEntries.set(i, entry.withPrefix("\n" + entry.getPrefix()));
                        }
                    }
                    if (fixedEntries != null) {
                        m = m.withEntries(fixedEntries);
                    }
                }

                return m;
            }
        });
    }

    private static boolean containsOnlyWhitespace(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }

        return true;
    }

    private static boolean containsNewline(@Nullable String str) {
        return str != null && str.indexOf('\n') >= 0;
    }

    /**
     * Extract an inline comment from a prefix string. Inline comments appear before the
     * first newline in the prefix (e.g. {@code " # comment\n  "} → {@code " # comment"}).
     */
    private static @Nullable String extractInlineComment(String prefix) {
        int newlineIndex = prefix.indexOf('\n');
        String beforeNewline = newlineIndex >= 0 ? prefix.substring(0, newlineIndex) : prefix;
        return beforeNewline.contains("#") ? beforeNewline : null;
    }

    private static boolean endsWithBlockScalar(Yaml.Mapping.Entry entry) {
        return endsWithBlockScalar(entry.getValue());
    }

    private static boolean endsWithBlockScalar(Yaml.Block block) {
        if (block instanceof Yaml.Scalar) {
            Yaml.Scalar.Style style = ((Yaml.Scalar) block).getStyle();
            return style == Yaml.Scalar.Style.FOLDED || style == Yaml.Scalar.Style.LITERAL;
        } else if (block instanceof Yaml.Mapping) {
            List<Yaml.Mapping.Entry> entries = ((Yaml.Mapping) block).getEntries();
            if (!entries.isEmpty()) {
                return endsWithBlockScalar(entries.get(entries.size() - 1));
            }
        } else if (block instanceof Yaml.Sequence) {
            List<Yaml.Sequence.Entry> entries = ((Yaml.Sequence) block).getEntries();
            if (!entries.isEmpty()) {
                return endsWithBlockScalar(entries.get(entries.size() - 1).getBlock());
            }
        }
        return false;
    }

    @Value
    @With
    private static class ToBeRemoved implements Marker {
        UUID id;
        static <Y2 extends Yaml> Y2 withMarker(Y2 y) {
            return y.withMarkers(y.getMarkers().addIfAbsent(new ToBeRemoved(randomId())));
        }
        static boolean hasMarker(Yaml y) {
            return y.getMarkers().findFirst(ToBeRemoved.class).isPresent();
        }
    }
}
