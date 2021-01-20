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
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

import static org.openrewrite.Validated.required;

public class FindField extends Recipe {
    private String fullyQualifiedName;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new FindFieldsProcessor(fullyQualifiedName);
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    @Override
    public Validated validate() {
        return required("fullyQualifiedName", fullyQualifiedName);
    }

    public static Set<J.VariableDecls> find(J j, String clazz) {
        //noinspection ConstantConditions
        return new FindFieldsProcessor(clazz)
                .visit(j, ExecutionContext.builder().build())
                .findMarkedWith(SearchResult.class);
    }

    private static final class FindFieldsProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final String fullyQualifiedName;

        public FindFieldsProcessor(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        @Override
        public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable, ExecutionContext ctx) {
            if (multiVariable.getTypeExpr() instanceof J.MultiCatch) {
                return multiVariable;
            }
            if (multiVariable.getTypeExpr() != null && TypeUtils.hasElementType(multiVariable.getTypeExpr()
                    .getType(), fullyQualifiedName)) {
                return multiVariable.mark(new SearchResult());
            }
            return multiVariable;
        }
    }
}
