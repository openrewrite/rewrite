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

/**
 * Finds occurrences of a property key.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FindProperties extends Recipe {

    @Option(displayName = "Property key",
            description = "A property glob expression that properties are matched against.",
            example = "guava*")
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
    public String getDisplayName() {
        return "Find property";
    }

    @Override
    public String getDescription() {
        return "Find uses of a property by key or keys by pattern.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                Properties p = super.visitEntry(entry, ctx);
                if (entry.getKey().equals(propertyKey)) {
                    p = p.withMarkers(p.getMarkers().searchResult());
                }
                return p;
            }
        };
    }
}
