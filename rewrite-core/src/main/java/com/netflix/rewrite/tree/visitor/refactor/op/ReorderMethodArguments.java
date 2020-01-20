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
package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.tree.Expression;
import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import lombok.AllArgsConstructor;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class ReorderMethodArguments extends RefactorVisitor<Tr.MethodInvocation> {
    String[] byArgumentNames;

    @NonFinal
    String[] originalParamNames;

    public void setOriginalParamNames(String... originalParamNames) {
        this.originalParamNames = originalParamNames;
    }

    @Override
    protected String getRuleName() {
        return "reorder-method-arguments";
    }

    @Override
    public List<AstTransform<Tr.MethodInvocation>> visitMethodInvocation(Tr.MethodInvocation method) {
        if(method.getType() != null) {
            var paramNames = originalParamNames.length == 0 ? method.getType().getParamNames() :
                    Arrays.asList(originalParamNames);

            if(paramNames == null) {
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
                if(originalArgs.size() > resolvedParamCount && fromPos == resolvedParamCount - 1) {
                    // this is a varargs argument
                    List<Expression> varargs = originalArgs.subList(fromPos, originalArgs.size());
                    reordered.addAll(varargs);
                    originalArgs.subList(i, (i++) + varargs.size()).stream().map(Expression::getFormatting).forEach(formattings::add);
                }
                else if(fromPos >= 0 && originalArgs.size() > fromPos) {
                    reordered.add(originalArgs.get(fromPos));
                    formattings.add(originalArgs.get(i++).getFormatting());
                }
            }

            i = 0;
            for (Expression expression : reordered) {
                reordered.set(i, expression.withFormatting(formattings.get(i++)));
            }

            return transform(m -> m.withArgs(m.getArgs().withArgs(reordered)));
        }

        return super.visitMethodInvocation(method);
    }
}
