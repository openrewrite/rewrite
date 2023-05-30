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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeleteProperty extends Recipe {
    @Option(displayName = "Property key",
            description = "The key to be deleted.",
            example = "management.metrics.binders.files.enabled")
    String propertyKey;

    @Deprecated
    @Option(displayName = "Coalesce",
            description = "(Deprecated: in a future version, this recipe will always use the `false` behavior)"
                    + " Simplify nested map hierarchies into their simplest dot separated property form.",
            required = false)
    @Nullable
    Boolean coalesce;

    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Default is `true`. Set to `false`  to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Override
    public String getDisplayName() {
        return "Delete property";
    }

    @Override
    public String getDescription() {
        return "Delete a YAML property. Nested YAML mappings are interpreted as dot separated property names, i.e. " +
                " as Spring Boot interprets application.yml files like `a.b.c.d` or `a.b.c:d`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {

            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext executionContext) {
                // TODO: Update DeleteProperty to support documents having Anchor / Alias Pairs
                if (documents != new ReplaceAliasWithAnchorValueVisitor<ExecutionContext>().visit(documents, executionContext)) {
                    return documents;
                }
                return super.visitDocuments(documents, executionContext);
            }

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

                if (!Boolean.FALSE.equals(relaxedBinding) ? NameCaseConvention.equalsRelaxedBinding(prop, propertyKey) : prop.equals(propertyKey)) {
                    doAfterVisit(new DeletePropertyVisitor<>(entry));
                    if (Boolean.TRUE.equals(coalesce)) {
                        maybeCoalesceProperties();
                    }
                }

                return e;
            }
        };
    }

    private static class DeletePropertyVisitor<P> extends YamlVisitor<P> {
        private final Yaml.Mapping.Entry scope;

        private DeletePropertyVisitor(Yaml.Mapping.Entry scope) {
            this.scope = scope;
        }

        @Override
        public Yaml visitMapping(Yaml.Mapping mapping, P p) {
            Yaml.Mapping m = (Yaml.Mapping) super.visitMapping(mapping, p);

            boolean changed = false;
            List<Yaml.Mapping.Entry> entries = new ArrayList<>();
            String deletedPrefix = null;
            for (Yaml.Mapping.Entry entry : m.getEntries()) {
                if (entry == scope || (entry.getValue() instanceof Yaml.Mapping && ((Yaml.Mapping) entry.getValue()).getEntries().isEmpty())) {
                    deletedPrefix = entry.getPrefix();
                    changed = true;
                } else {
                    if (deletedPrefix != null) {
                        entry = entry.withPrefix(deletedPrefix);
                        deletedPrefix = null;
                    }
                    entries.add(entry);
                }
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
