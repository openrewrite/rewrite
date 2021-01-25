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
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * This recipe finds method invocations matching a method pattern and uses a zero-based argument index to determine
 * which argument is removed.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeleteMethodArgument extends Recipe {

    private final String methodPattern;
    private final Integer argumentIndex;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DeleteMethodArgumentVisitor(new MethodMatcher(methodPattern));
    }

    private class DeleteMethodArgumentVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        public DeleteMethodArgumentVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            List<JRightPadded<Expression>> originalArgs = m.getArgs().getElem();
            if (methodMatcher.matches(m) && originalArgs.stream()
                    .filter(a -> !(a.getElem() instanceof J.Empty))
                    .count() >= argumentIndex + 1) {
                List<JRightPadded<Expression>> args = new ArrayList<>(m.getArgs().getElem());

                args.remove((int) argumentIndex);
                if (args.isEmpty()) {
                    args = singletonList(new JRightPadded<>(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), Space.EMPTY, Markers.EMPTY));
                }

                m = m.withArgs(m.getArgs().withElem(args));
            }
            return m;
        }
    }
}
