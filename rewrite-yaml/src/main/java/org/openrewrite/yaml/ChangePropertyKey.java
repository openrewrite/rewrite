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
package org.openrewrite.yaml;

import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

/**
 * When nested YAML mappings are interpreted as dot
 * separated property names, e.g. as Spring Boot
 * interprets application.yml files.
 */
public class ChangePropertyKey extends Recipe {
    private String property;
    private String toProperty;
    private boolean coalesce = true;

    public ChangePropertyKey() {
        this.processor = () -> new ChangePropertyKeyProcessor<>(property, toProperty, coalesce);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setToProperty(String toProperty) {
        this.toProperty = toProperty;
    }

    public void setCoalesce(boolean coalesce) {
        this.coalesce = coalesce;
    }

    @Override
    public Validated validate() {
        return required("property", property)
                .and(required("toProperty", toProperty));
    }

    private static class ChangePropertyKeyProcessor<P> extends YamlProcessor<P> {
        private final String property;
        private final String toProperty;
        private final boolean coalesce;

        public ChangePropertyKeyProcessor(String property, String toProperty, boolean coalesce) {
            this.property = property;
            this.toProperty = toProperty;
            this.coalesce = coalesce;
            setCursoringOn();
        }

        @Override
        public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
            Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) super.visitMappingEntry(entry, p);

            Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                    .filter(Yaml.Mapping.Entry.class::isInstance)
                    .map(Yaml.Mapping.Entry.class::cast)
                    .collect(Collectors.toCollection(ArrayDeque::new));

            String property = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                    .map(e2 -> e2.getKey().getValue())
                    .collect(Collectors.joining("."));

            String propertyToTest = this.toProperty;
            if (property.equals(this.property)) {
                Iterator<Yaml.Mapping.Entry> propertyEntriesLeftToRight = propertyEntries.descendingIterator();
                while (propertyEntriesLeftToRight.hasNext()) {
                    Yaml.Mapping.Entry propertyEntry = propertyEntriesLeftToRight.next();
                    String value = propertyEntry.getKey().getValue();

                    if (!propertyToTest.startsWith(value)) {
                        doAfterVisit(new InsertSubpropertyProcessor<>(
                                propertyEntry,
                                propertyToTest,
                                entry.getValue()
                        ));
                        doAfterVisit(new DeletePropertyProcessor<>(entry));
                        if (coalesce) {
                             maybeCoalesceProperties();
                        }
                        break;
                    }

                    propertyToTest = propertyToTest.substring(value.length() + 1);
                }
            }

            return e;
        }

    }

    private static class InsertSubpropertyProcessor<P> extends YamlProcessor<P> {
        private final Yaml.Mapping.Entry scope;
        private final String subproperty;
        private final Yaml.Block value;

        private InsertSubpropertyProcessor(Yaml.Mapping.Entry scope, String subproperty, Yaml.Block value) {
            this.scope = scope;
            this.subproperty = subproperty;
            this.value = value;
        }

        @Override
        public Yaml visitMapping(Yaml.Mapping mapping, P p) {
            Yaml.Mapping m = (Yaml.Mapping) super.visitMapping(mapping, p);

            if (m.getEntries().contains(scope)) {
                String newEntryFormatting = scope.getPrefix(); // todo, validate simple newEntryFormatting swapout
                if (newEntryFormatting.isEmpty()) {
                    newEntryFormatting = "\n"; // todo, gross?
                }

                m = m.withEntries(Stream.concat(
                        m.getEntries().stream(),
                        Stream.of(
                                new Yaml.Mapping.Entry(randomId(),
                                        new Yaml.Scalar(randomId(), Yaml.Scalar.Style.PLAIN, subproperty,
                                                "", Markers.EMPTY),
                                        scope.getBeforeMappingValueIndicator(), // todo, validate afterKey here
                                        value.copyPaste(),
                                        newEntryFormatting,
                                        Markers.EMPTY
                                )
                        )
                ).collect(toList()));
            }

            return m;
        }
    }

    private static class DeletePropertyProcessor<P> extends YamlProcessor<P> {
        private final Yaml.Mapping.Entry scope;

        private DeletePropertyProcessor(Yaml.Mapping.Entry scope) {
            this.scope = scope;
            setCursoringOn();
        }

        @Override
        public Yaml visitMapping(Yaml.Mapping mapping, P p) {
            Yaml.Mapping m = (Yaml.Mapping) super.visitMapping(mapping, p);

            boolean changed = false;
            List<Yaml.Mapping.Entry> entries = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : m.getEntries()) {
                if (entry == scope || (entry.getValue() instanceof Yaml.Mapping && ((Yaml.Mapping) entry.getValue()).getEntries().isEmpty())) {
                    changed = true;
                } else {
                    entries.add(entry);
                }
            }

            if (changed) {
                m = m.withEntries(entries);

                if (getCursor().getParentOrThrow().getTree() instanceof Yaml.Document) {
                    Yaml.Document document = getCursor().getParentOrThrow().getTree();
                    if (!document.isExplicit()) {
                        m = m.withEntries(m.getEntries()); // todo, firstPrefixFormatting
                    }
                }
            }

            return m;
        }
    }
}
