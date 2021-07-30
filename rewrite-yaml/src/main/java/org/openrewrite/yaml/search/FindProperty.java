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
package org.openrewrite.yaml.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindProperty extends Recipe {

    @Option(displayName = "Property key",
            description = "The key to look for.",
            example = "management.metrics.binders.files.enabled")
    String propertyKey;

    @Override
    public String getDisplayName() {
        return "Find YAML properties";
    }

    @Override
    public String getDescription() {
        return "Find a YAML property. Nested YAML mappings are interpreted as dot separated property names, i.e. " +
                " as Spring Boot interprets application.yml files.";
    }

    @Override
    public YamlVisitor<ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);

                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                        .filter(Yaml.Mapping.Entry.class::isInstance)
                        .map(Yaml.Mapping.Entry.class::cast)
                        .collect(Collectors.toCollection(ArrayDeque::new));

                String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                        .map(e2 -> e2.getKey().getValue())
                        .collect(Collectors.joining("."));

                if (prop.equals(propertyKey)) {
                    e = e.withValue(e.getValue().withMarkers(e.getValue().getMarkers().addIfAbsent(new YamlSearchResult(FindProperty.this))));
                }

                return e;
            }
        };
    }

    public static Set<Yaml.Block> find(Yaml y, String propertyKey) {
        YamlVisitor<Set<Yaml.Block>> findVisitor = new YamlIsoVisitor<Set<Yaml.Block>>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Set<Yaml.Block> values) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, values);

                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                        .filter(Yaml.Mapping.Entry.class::isInstance)
                        .map(Yaml.Mapping.Entry.class::cast)
                        .collect(Collectors.toCollection(ArrayDeque::new));

                String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                        .map(e2 -> e2.getKey().getValue())
                        .collect(Collectors.joining("."));

                if (prop.equals(propertyKey)) {
                    values.add(entry.getValue());
                }

                return e;
            }
        };

        Set<Yaml.Block> values = new HashSet<>();
        findVisitor.visit(y, values);
        return values;
    }
}
