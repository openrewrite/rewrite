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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodTargetToVariable extends Recipe {

    @NonNull
    private final String methodPattern;

    @NonNull
    private final String variableName;

    @NonNull
    private final String variableType;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new ChangeMethodTargetToVariableProcessor(new MethodMatcher(methodPattern), JavaType.Class.build(variableType));
    }

    private class ChangeMethodTargetToVariableProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final MethodMatcher methodMatcher;
        private final JavaType.Class variableType;

        private ChangeMethodTargetToVariableProcessor(MethodMatcher methodMatcher, JavaType.Class variableType) {
            this.methodMatcher = methodMatcher;
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
                    methodType = m.getType().withDeclaringType(this.variableType).withFlags(flags);
                }

                m = m.withSelect(
                        new JRightPadded<>(J.Ident.build(randomId(),
                                m.getSelect() == null ?
                                        Space.EMPTY :
                                        m.getSelect().getElem().getPrefix(),
                                Markers.EMPTY,
                                variableName,
                                this.variableType),
                                Space.EMPTY,
                                Markers.EMPTY
                        )
                ).withType(methodType);

                doAfterVisit(new OrderImports());
            }
            return m;
        }
    }
}
