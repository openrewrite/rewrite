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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.RecipeSearchResult;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds fields that have a matching type.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FindFields extends Recipe {
    private final String fullyQualifiedTypeName;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable, ExecutionContext ctx) {
                if (multiVariable.getTypeExpr() instanceof J.MultiCatch) {
                    return multiVariable;
                }
                if (multiVariable.getTypeExpr() != null && TypeUtils.hasElementType(multiVariable.getTypeExpr()
                        .getType(), fullyQualifiedTypeName)) {
                    return multiVariable.withMarker(new RecipeSearchResult(FindFields.this));
                }
                return multiVariable;
            }
        };
    }

    public static Set<J.VariableDecls> find(J j, String fullyQualifiedTypeName) {
        JavaIsoVisitor<Set<J.VariableDecls>> findVisitor = new JavaIsoVisitor<Set<J.VariableDecls>>() {
            @Override
            public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable, Set<J.VariableDecls> vs) {
                if (multiVariable.getTypeExpr() instanceof J.MultiCatch) {
                    return multiVariable;
                }
                if (multiVariable.getTypeExpr() != null && TypeUtils.hasElementType(multiVariable.getTypeExpr()
                        .getType(), fullyQualifiedTypeName)) {
                    vs.add(multiVariable);
                }
                return multiVariable;
            }
        };

        Set<J.VariableDecls> vs = new HashSet<>();
        findVisitor.visit(j, vs);
        return vs;
    }
}
