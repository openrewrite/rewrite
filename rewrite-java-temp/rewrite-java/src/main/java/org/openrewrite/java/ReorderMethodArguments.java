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

import org.openrewrite.Formatting;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openrewrite.Validated.required;

public class ReorderMethodArguments extends JavaIsoRefactorVisitor {
    private MethodMatcher methodMatcher;
    private String[] order;
    private String[] originalOrder = new String[0];

    @Override
    public boolean isIdempotent() {
        return false;
    }

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    public void setOrder(String... order) {
        this.order = order;
    }

    public void setOriginalOrder(String... originalOrder) {
        this.originalOrder = originalOrder;
    }

    @Override
    public Validated validate() {
        return required("method", methodMatcher)
                .and(required("order", order));
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        if(methodMatcher.matches(method)) {
            andThen(new Scoped(method, order, originalOrder));
        }
        return super.visitMethodInvocation(method);
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.MethodInvocation scope;
        private final String[] order;
        private final String[] originalOrder;

        public Scoped(J.MethodInvocation scope, String[] order, String[] originalOrder) {
            this.scope = scope;
            this.order = order;
            this.originalOrder = originalOrder;
            setCursoringOn();
        }

        @Override
        public boolean isIdempotent() {
            return false;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
            if (scope.isScope(method) && method.getType() != null) {
                List<String> paramNames = originalOrder.length == 0 ? method.getType().getParamNames() :
                        Arrays.asList(originalOrder);

                if (paramNames == null) {
                    throw new IllegalStateException("There is no source attachment for method " + method.getType().getDeclaringType().getFullyQualifiedName() +
                            "." + method.getSimpleName() + "(..). Provide a reference for original parameter names by calling setOriginalParamNames(..)");
                }

                List<Expression> originalArgs = method.getArgs().getArgs();

                int resolvedParamCount = method.getType().getResolvedSignature() == null ? originalArgs.size() :
                        method.getType().getResolvedSignature().getParamTypes().size();

                int i = 0;
                List<Expression> reordered = new ArrayList<>(originalArgs.size());
                List<Formatting> formattings = new ArrayList<>(originalArgs.size());

                for (String name : order) {
                    int fromPos = paramNames.indexOf(name);
                    if (originalArgs.size() > resolvedParamCount && fromPos == resolvedParamCount - 1) {
                        // this is a varargs argument
                        List<Expression> varargs = originalArgs.subList(fromPos, originalArgs.size());
                        reordered.addAll(varargs);
                        originalArgs.subList(i, (i++) + varargs.size()).stream().map(Expression::getFormatting).forEach(formattings::add);
                    } else if (fromPos >= 0 && originalArgs.size() > fromPos) {
                        reordered.add(originalArgs.get(fromPos));
                        formattings.add(originalArgs.get(i++).getFormatting());
                    }
                }

                i = 0;
                for (Expression expression : reordered) {
                    reordered.set(i, expression.withFormatting(formattings.get(i++)));
                }
                if(!method.getArgs().getArgs().equals(reordered)) {
                    return method.withArgs(method.getArgs().withArgs(reordered));
                }
            }

            return super.visitMethodInvocation(method);
        }
    }
}
