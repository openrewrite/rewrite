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
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

public class ChangeMethodTargetToVariable extends Recipe {
    private MethodMatcher methodMatcher;
    private String variable;
    private JavaType.Class variableType;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new ChangeMethodTargetToVariableProcessor(methodMatcher, variable, variableType);
    }

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public void setVariableType(String variableType) {
        this.variableType = JavaType.Class.build(variableType);
    }

    @Override
    public Validated validate() {
        return required("method", methodMatcher)
                .and(required("variable", variable))
                .and(required("variableType", variableType.getFullyQualifiedName()));
    }

    private static class ChangeMethodTargetToVariableProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final MethodMatcher methodMatcher;
        private final String variable;
        private final JavaType.Class variableType;

        private ChangeMethodTargetToVariableProcessor(MethodMatcher methodMatcher, String variable, JavaType.Class variableType) {
            this.methodMatcher = methodMatcher;
            this.variable = variable;
            this.variableType = variableType;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(m)) {
                JavaType.Method methodType = null;
                if (m.getType() != null) {
                    Set<Flag> flags = new LinkedHashSet<>(m.getType().getFlags());
                    flags.remove(Flag.Static);
                    methodType = m.getType().withDeclaringType(variableType).withFlags(flags);
                }

                m = m.withSelect(
                        new JRightPadded<>(J.Ident.build(randomId(),
                                m.getSelect() == null ?
                                        Space.EMPTY :
                                        m.getSelect().getElem().getPrefix(),
                                Markers.EMPTY,
                                variable,
                                variableType),
                                Space.EMPTY
                        )
                ).withType(methodType);

                doAfterVisit(new OrderImports());
            }
            return m;
        }
    }
}
