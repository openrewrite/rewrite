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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaProcessor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

/**
 * Find places where a type is mentioned explicitly, excluding imports.
 */
public final class FindType extends Recipe {
    private String clazz;

    public FindType() {
        this.processor = () -> new FindTypeProcessor(clazz);
    }

    public void setClass(String clazz) {
        this.clazz = clazz;
    }

    public static Set<NameTree> find(J j, String clazz) {
        return SearchResult.find(new FindTypeProcessor(clazz).visit(j,
                ExecutionContext.builder().build()));
    }

    private static class FindTypeProcessor extends JavaProcessor<ExecutionContext> {
        private final String clazz;

        public FindTypeProcessor(String clazz) {
            this.clazz = clazz;
            setCursoringOn();
        }

        @Override
        public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
            JavaType.Class asClass = TypeUtils.asClass(name.getType());
            if (asClass != null && asClass.getFullyQualifiedName().equals(clazz) &&
                    getCursor().firstEnclosing(J.Import.class) == null) {
                return name.withMarkers(name.getMarkers().add(new SearchResult()));
            }
            return super.visitTypeName(name, ctx);
        }
    }
}
