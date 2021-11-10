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
package org.openrewrite.properties.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindProperties extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find property";
    }

    @Override
    public String getDescription() {
        return "Finds occurrences of a property key.";
    }

    @Option(displayName = "Property key",
            description = "The property key to look for.",
            example = "management.metrics.binders.files.enabled")
    String propertyKey;

    public static Set<Properties.Entry> find(Properties p, String propertyKey) {
        PropertiesVisitor<Set<Properties.Entry>> findVisitor = new PropertiesVisitor<Set<Properties.Entry>>() {
            @Override
            public Properties visitEntry(Properties.Entry entry, Set<Properties.Entry> ps) {
                if (entry.getKey().equals(propertyKey)) {
                    ps.add(entry);
                }
                return super.visitEntry(entry, ps);
            }
        };

        Set<Properties.Entry> ps = new HashSet<>();
        findVisitor.visit(p, ps);
        return ps;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                if (entry.getKey().equals(propertyKey)) {
                    entry = entry.withValue(entry.getValue().withMarkers(entry.getValue().getMarkers().searchResult()));
                }
                return super.visitEntry(entry, ctx);
            }
        };
    }

}
