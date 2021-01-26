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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;

import java.util.Set;

import static org.openrewrite.Validated.required;

/**
 * This recipe will find all occurrences of the property key and mark those properties with {@link SearchResult} markers.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FindProperty extends Recipe {

    private final String propertyKey;

    public static Set<Properties> find(Properties p, String propertyKey) {
        //noinspection ConstantConditions
        return ((FindPropertyVisitor) new FindProperty(propertyKey).getVisitor())
                .visit(p, ExecutionContext.builder().build())
                .findMarkedWith(SearchResult.class);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindPropertyVisitor();
    }

    public class FindPropertyVisitor extends PropertiesVisitor<ExecutionContext> {

        @Override
        public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
            Properties p = super.visitEntry(entry, ctx);
            if (entry.getKey().equals(propertyKey)) {
                p = p.mark(new SearchResult());
            }
            return p;
        }
    }
}

