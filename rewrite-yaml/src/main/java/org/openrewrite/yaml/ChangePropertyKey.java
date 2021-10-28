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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Tree.randomId;

/**
 * Nested YAML mappings are interpreted as dot
 * separated property names, e.g. as Spring Boot
 * interprets application.yml files.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class ChangePropertyKey extends Recipe {

    @Option(displayName = "Old property key",
            description = "The property key to rename.",
            example = "management.metrics.binders.files.enabled")
    String oldPropertyKey;

    @Option(displayName = "New property key",
            description = "The new name for the property key.",
            example = "management.metrics.enable.process.files")
    String newPropertyKey;

    @Incubating(since = "7.8.0")
    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Change property key";
    }

    @Override
    public String getDescription() {
        return "Change a YAML property key leaving the value intact. Nested YAML mappings are " +
                "interpreted as dot separated property names, i.e. as Spring Boot interprets " +
                "application.yml files.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public YamlVisitor<ExecutionContext> getVisitor() {
        return new ChangePropertyKeyVisitor<>();
    }

    private class ChangePropertyKeyVisitor<P> extends YamlIsoVisitor<P> {

        @Override
        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
            Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);

            Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                    .filter(Yaml.Mapping.Entry.class::isInstance)
                    .map(Yaml.Mapping.Entry.class::cast)
                    .collect(Collectors.toCollection(ArrayDeque::new));

            String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                    .map(e2 -> e2.getKey().getValue())
                    .collect(Collectors.joining("."));

            String propertyToTest = newPropertyKey;
            if (prop.equals(oldPropertyKey)) {
                Iterator<Yaml.Mapping.Entry> propertyEntriesLeftToRight = propertyEntries.descendingIterator();
                while (propertyEntriesLeftToRight.hasNext()) {
                    Yaml.Mapping.Entry propertyEntry = propertyEntriesLeftToRight.next();
                    String value = propertyEntry.getKey().getValue();

                    if (!propertyToTest.startsWith(value) || (propertyToTest.startsWith(value) && !propertyEntriesLeftToRight.hasNext())) {
                        doAfterVisit(new InsertSubpropertyVisitor<>(
                                propertyEntry,
                                propertyToTest,
                                entry.getValue()
                        ));
                        doAfterVisit(new DeletePropertyVisitor<>(entry));
                        maybeCoalesceProperties();
                        break;
                    }

                    propertyToTest = propertyToTest.substring(value.length() + 1);
                }
            }

            return e;
        }

    }

    private static class InsertSubpropertyVisitor<P> extends YamlIsoVisitor<P> {
        private final Yaml.Mapping.Entry scope;
        private final String subproperty;
        private final Yaml.Block value;

        private InsertSubpropertyVisitor(Yaml.Mapping.Entry scope, String subproperty, Yaml.Block value) {
            this.scope = scope;
            this.subproperty = subproperty;
            this.value = value;
        }

        @Override
        public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
            Yaml.Mapping m = super.visitMapping(mapping, p);

            if (m.getEntries().contains(scope)) {
                String newEntryPrefix = scope.getPrefix();
                if (newEntryPrefix.isEmpty()) {
                    newEntryPrefix = "\n";
                }

                m = m.withEntries(Stream.concat(
                        m.getEntries().stream(),
                        Stream.of(
                                new Yaml.Mapping.Entry(randomId(),
                                        newEntryPrefix,
                                        Markers.EMPTY,
                                        new Yaml.Scalar(randomId(), "", Markers.EMPTY, null,
                                                Yaml.Scalar.Style.PLAIN, subproperty),
                                        scope.getBeforeMappingValueIndicator(),
                                        value.copyPaste()
                                )
                        )
                ).collect(toList()));
            }

            return m;
        }
    }

    private static class DeletePropertyVisitor<P> extends YamlIsoVisitor<P> {
        private final Yaml.Mapping.Entry scope;

        private DeletePropertyVisitor(Yaml.Mapping.Entry scope) {
            this.scope = scope;
        }

        @Override
        public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
            Yaml.Mapping m = super.visitMapping(mapping, p);

            boolean changed = false;
            List<Yaml.Mapping.Entry> entries = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : m.getEntries()) {
                if (entry == scope || (entry.getValue() instanceof Yaml.Mapping && ((Yaml.Mapping) entry.getValue()).getEntries().isEmpty())) {
                    changed = true;
                } else {
                    entries.add(entry);
                }
            }

            if (entries.size() == 1) {
                entries = ListUtils.map(entries, e -> e.withPrefix(""));
            }

            if (changed) {
                m = m.withEntries(entries);

                if (getCursor().getParentOrThrow().getValue() instanceof Yaml.Document) {
                    Yaml.Document document = getCursor().getParentOrThrow().getValue();
                    if (!document.isExplicit()) {
                        m = m.withEntries(m.getEntries());
                    }
                }
            }
            return m;
        }
    }
}
