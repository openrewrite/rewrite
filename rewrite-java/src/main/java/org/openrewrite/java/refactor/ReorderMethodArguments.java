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
package org.openrewrite.java.refactor;

import org.openrewrite.Formatting;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReorderMethodArguments extends JavaRefactorVisitor {
    private final J.MethodInvocation scope;
    private final String[] byArgumentNames;
    private String[] originalParamNames;

    public ReorderMethodArguments(J.MethodInvocation scope, String... byArgumentNames) {
        this.scope = scope;
        this.byArgumentNames = byArgumentNames;
        this.originalParamNames = new String[0];
        setCursoringOn();
    }

    public ReorderMethodArguments withOriginalParamNames(String... originalParamNames) {
        this.originalParamNames = originalParamNames;
        return this;
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        if (scope.isScope(method) && method.getType() != null) {
            var paramNames = originalParamNames.length == 0 ? method.getType().getParamNames() :
                    Arrays.asList(originalParamNames);

            if (paramNames == null) {
                throw new IllegalStateException("There is no source attachment for method " + method.getType().getDeclaringType().getFullyQualifiedName() +
                        "." + method.getSimpleName() + "(..). Provide a reference for original parameter names by calling setOriginalParamNames(..)");
            }

            List<Expression> originalArgs = method.getArgs().getArgs();

            var resolvedParamCount = method.getType().getResolvedSignature() == null ? originalArgs.size() :
                    method.getType().getResolvedSignature().getParamTypes().size();

            int i = 0;
            List<Expression> reordered = new ArrayList<>(originalArgs.size());
            List<Formatting> formattings = new ArrayList<>(originalArgs.size());

            for (String name : byArgumentNames) {
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

            return method.withArgs(method.getArgs().withArgs(reordered));
        }

        return super.visitMethodInvocation(method);
    }
}
