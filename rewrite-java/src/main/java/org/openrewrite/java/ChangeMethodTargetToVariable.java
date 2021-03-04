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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Option;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodTargetToVariable extends Recipe {

    /**
     * A method pattern, expressed as a pointcut expression, that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern, expressed as a pointcut expression, that is used to find matching method invocations.",
            example = "org.mycorp.A method(..)")
    String methodPattern;

    @Option(displayName = "Variable name", description = "Name of variable to use as target for the modified method invocation.")
    String variableName;

    @Option(displayName = "Variable type",
            description = "Type attribution to use for the return type of the modified method invocation.")
    String variableType;

    @Override
    public String getDisplayName() {
        return "Change method target to variable";
    }

    @Override
    public String getDescription() {
        return "Change method invocations to method calls on a variable.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeMethodTargetToVariableVisitor(new MethodMatcher(methodPattern), JavaType.Class.build(variableType));
    }

    private class ChangeMethodTargetToVariableVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;
        private final JavaType.Class variableType;

        private ChangeMethodTargetToVariableVisitor(MethodMatcher methodMatcher, JavaType.Class variableType) {
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

                m = m.withSelect(J.Identifier.build(randomId(),
                        m.getSelect() == null ?
                                Space.EMPTY :
                                m.getSelect().getPrefix(),
                        Markers.EMPTY,
                        variableName,
                        this.variableType)
                ).withType(methodType);

                doAfterVisit(new OrderImports());
            }
            return m;
        }
    }
}
