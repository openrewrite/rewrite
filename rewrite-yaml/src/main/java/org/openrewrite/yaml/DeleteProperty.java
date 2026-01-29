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
                List<Yaml.Sequence.Entry> entries = s.getEntries();
                if (entries.isEmpty()) {
                    return s;
                }

                entries = ListUtils.map(entries, entry -> ToBeRemoved.hasMarker(entry) ? null : entry);
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

                boolean changed = false;
                List<Yaml.Mapping.Entry> entries = new ArrayList<>();
                String firstDeletedPrefix = null;
                for (Yaml.Mapping.Entry entry : m.getEntries()) {
                    if (ToBeRemoved.hasMarker(entry.getValue()) ||
                        ToBeRemoved.hasMarker(entry) ||
                        (entry.getValue() instanceof Yaml.Mapping && ((Yaml.Mapping) entry.getValue()).getEntries().isEmpty())) {
                        // Entry is being deleted - capture prefix from the first deleted entry before any kept entries
                        if (entries.isEmpty() && firstDeletedPrefix == null) {
                            firstDeletedPrefix = entry.getPrefix();
                        }
                        changed = true;
                    } else {
                        if (entries.isEmpty() && firstDeletedPrefix != null && containsOnlyWhitespace(entry.getPrefix())) {
                            // This is the first kept entry and there were deleted entries before it
                            entry = entry.withPrefix(firstDeletedPrefix);
                        }
                        entries.add(entry);
                    }
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
