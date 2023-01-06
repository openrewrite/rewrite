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
import org.openrewrite.*;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindProperty extends Recipe {

    @Option(displayName = "Property key",
            description = "The key to look for. Glob is supported.",
            example = "management.metrics.binders.*.enabled")
    String propertyKey;

    @Incubating(since = "7.17.0")
    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Default is `true`. Set to `false`  to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Override
    public String getDisplayName() {
        return "Find YAML properties";
    }

    @Override
    public String getDescription() {
        return "Find a YAML property. Nested YAML mappings are interpreted as dot separated property names, i.e. " +
                " as Spring Boot interprets `application.yml` files.";
    }

    @Override
    public YamlVisitor<ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                String prop = getProperty(getCursor());
                if (!Boolean.FALSE.equals(relaxedBinding) ?
                        NameCaseConvention.matchesGlobRelaxedBinding(prop, propertyKey) :
                        StringUtils.matchesGlob(prop, propertyKey)) {
                    e = e.withValue(SearchResult.found(e.getValue()));
                }
                return e;
            }
        };
    }

    public static Set<Yaml.Block> find(Yaml y, String propertyKey, @Nullable Boolean relaxedBinding) {
        YamlVisitor<Set<Yaml.Block>> findVisitor = new YamlIsoVisitor<Set<Yaml.Block>>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Set<Yaml.Block> values) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, values);
                String prop = getProperty(getCursor());
                if (!Boolean.FALSE.equals(relaxedBinding) ?
                        NameCaseConvention.matchesGlobRelaxedBinding(prop, propertyKey) :
                        StringUtils.matchesGlob(prop, propertyKey)) {
                    values.add(entry.getValue());
                }
                return e;
            }
        };

        Set<Yaml.Block> values = new HashSet<>();
        findVisitor.visit(y, values);
        return values;
    }

    public static boolean matches(Cursor cursor, String propertyKey, @Nullable Boolean relaxedBinding) {
        String prop = getProperty(cursor);
        return !Boolean.FALSE.equals(relaxedBinding) ?
                NameCaseConvention.matchesGlobRelaxedBinding(prop, propertyKey) :
                StringUtils.matchesGlob(prop, propertyKey);
    }

    private static String getProperty(Cursor cursor) {
        StringBuilder asProperty = new StringBuilder();
        Iterator<Object> path = cursor.getPath();
        int i = 0;
        while (path.hasNext()) {
            Object next = path.next();
            if (next instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) next;
                if (i++ > 0) {
                    asProperty.insert(0, '.');
                }
                asProperty.insert(0, entry.getKey().getValue());
            }
        }
        return asProperty.toString();
    }
}
