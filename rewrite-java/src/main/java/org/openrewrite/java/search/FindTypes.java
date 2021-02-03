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
package org.openrewrite.java.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

/**
 * This recipe will find all references to a type matching the fully qualified type name and mark those fields with
 * {@link SearchResult} markers.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class FindTypes extends Recipe {
    private final String fullyQualifiedTypeName;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindTypesVisitor();
    }

    public static Set<NameTree> find(J j, String fullyQualifiedClassName) {
        //noinspection ConstantConditions
        return ((FindTypesVisitor) new FindTypes(fullyQualifiedClassName).getVisitor())
                .visit(j, ExecutionContext.builder().build())
                .findMarkedWith(SearchResult.class);
    }

    private class FindTypesVisitor extends JavaVisitor<ExecutionContext> {

        public FindTypesVisitor() {
            setCursoringOn();
        }

        @Override
        public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
            N n = super.visitTypeName(name, ctx);
            JavaType.Class asClass = TypeUtils.asClass(n.getType());
            if (asClass != null && asClass.getFullyQualifiedName().equals(fullyQualifiedTypeName) &&
                    getCursor().firstEnclosing(J.Import.class) == null) {
                return n.withMarker(new SearchResult(null));
            }
            return n;
        }
    }
}
