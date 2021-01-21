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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openrewrite.Validated.required;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ReorderMethodArguments extends Recipe {
    private final String methodPattern;
    private final String[] order;
    private String[] originalOrder = new String[0];

    public ReorderMethodArguments(String methodPattern, String[] order) {
        this.methodPattern = methodPattern;
        this.order = order;
    }

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new ReorderMethodArgumentsProcessor(new MethodMatcher(methodPattern));
    }

    @Override
    public Validated validate() {
        return required("methodPattern", methodPattern)
                .and(required("order", order));
    }

    private class ReorderMethodArgumentsProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private ReorderMethodArgumentsProcessor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            if (methodMatcher.matches(m) && m.getType() != null) {
                List<String> paramNames = originalOrder.length == 0 ? m.getType().getParamNames() :
                        Arrays.asList(originalOrder);

                if (paramNames == null) {
                    throw new IllegalStateException("There is no source attachment for method " + m.getType().getDeclaringType().getFullyQualifiedName() +
                            "." + m.getSimpleName() + "(..). Provide a reference for original parameter names by calling setOriginalParamNames(..)");
                }

                List<JRightPadded<Expression>> originalArgs = m.getArgs().getElem();

                int resolvedParamCount = m.getType().getResolvedSignature() == null ? originalArgs.size() :
                        m.getType().getResolvedSignature().getParamTypes().size();

                int i = 0;
                List<JRightPadded<Expression>> reordered = new ArrayList<>(originalArgs.size());
                List<Space> formattings = new ArrayList<>(originalArgs.size());
                List<Space> rightFormattings = new ArrayList<>(originalArgs.size());

                for (String name : order) {
                    int fromPos = paramNames.indexOf(name);
                    if (originalArgs.size() > resolvedParamCount && fromPos == resolvedParamCount - 1) {
                        // this is a varargs argument
                        List<JRightPadded<Expression>> varargs = originalArgs.subList(fromPos, originalArgs.size());
                        reordered.addAll(varargs);
                        for (JRightPadded<Expression> exp : originalArgs.subList(i, (i++) + varargs.size())) {
                            formattings.add(exp.getElem().getPrefix());
                            rightFormattings.add(exp.getAfter());
                        }
                    } else if (fromPos >= 0 && originalArgs.size() > fromPos) {
                        reordered.add(originalArgs.get(fromPos));
                        formattings.add(originalArgs.get(i).getElem().getPrefix());
                        rightFormattings.add(originalArgs.get(i++).getAfter());
                    }
                }

                boolean changed = false;
                i = 0;
                for (JRightPadded<Expression> expression : reordered) {
                    final int index = i;
                    reordered.set(i, expression
                            .map(e -> e.withPrefix(formattings.get(index)))
                            .withAfter(rightFormattings.get(index)));
                    if (reordered.get(i) != originalArgs.get(i)) {
                        changed = true;
                    }
                    i++;
                }

                if (changed) {
                    m = m.withArgs(m.getArgs().withElem(reordered));
                }
            }
            return m;
        }
    }
}
