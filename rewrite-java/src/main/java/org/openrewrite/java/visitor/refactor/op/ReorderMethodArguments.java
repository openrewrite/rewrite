/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor.refactor.op;

import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Formatting;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.visitor.refactor.AstTransform;
import org.openrewrite.java.visitor.refactor.ScopedRefactorVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReorderMethodArguments extends ScopedRefactorVisitor {
    private final String[] byArgumentNames;
    private String[] originalParamNames;

    public ReorderMethodArguments(J.MethodInvocation scope, String... byArgumentNames) {
        super(scope.getId());
        this.byArgumentNames = byArgumentNames;
        this.originalParamNames = new String[0];
    }

    public ReorderMethodArguments withOriginalParamNames(String... originalParamNames) {
        this.originalParamNames = originalParamNames;
        return this;
    }

    @Override
    public String getRuleName() {
        return "core.ReorderMethodArguments";
    }

    @Override
    public boolean isSingleRun() {
        return true;
    }

    @Override
    public List<AstTransform> visitMethodInvocation(J.MethodInvocation method) {
        return maybeTransform(method,
                isInScope(method) && method.getType() != null,
                super::visitMethodInvocation,
                m -> {
                    if (m.getType() == null) {
                        return m;
                    }

                    var paramNames = originalParamNames.length == 0 ? m.getType().getParamNames() :
                            Arrays.asList(originalParamNames);

                    if (paramNames == null) {
                        throw new IllegalStateException("There is no source attachment for method " + m.getType().getDeclaringType().getFullyQualifiedName() +
                                "." + m.getSimpleName() + "(..). Provide a reference for original parameter names by calling setOriginalParamNames(..)");
                    }

                    List<Expression> originalArgs = m.getArgs().getArgs();

                    var resolvedParamCount = m.getType().getResolvedSignature() == null ? originalArgs.size() :
                            m.getType().getResolvedSignature().getParamTypes().size();

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

                    return m.withArgs(m.getArgs().withArgs(reordered));
                }
        );
    }
}
