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
import org.openrewrite.marker.Markers;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

public class DeleteMethodArgument extends JavaIsoRefactorVisitor {
    private MethodMatcher methodMatcher;
    private Integer index;

    @Override
    public boolean isIdempotent() {
        return false;
    }

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public Validated validate() {
        return required("method", methodMatcher)
                .and(required("index", index));
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        if(methodMatcher.matches(method)) {
            andThen(new Scoped(method, index));
        }
        return super.visitMethodInvocation(method);
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.MethodInvocation scope;
        private final int index;

        public Scoped(J.MethodInvocation scope, int index) {
            this.scope = scope;
            this.index = index;
        }

        @Override
        public boolean isIdempotent() {
            return false;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
            List<Expression> originalArgs = method.getArgs().getArgs();
            if (scope.isScope(method) && originalArgs.stream()
                    .filter(a -> !(a instanceof J.Empty))
                    .count() >= index + 1) {
                List<Expression> args = new ArrayList<>(method.getArgs().getArgs());

                args.remove(index);
                if (args.isEmpty()) {
                    args = singletonList(new J.Empty(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY));
                }

                return method.withArgs(method.getArgs().withArgs(args));
            }

            return super.visitMethodInvocation(method);
        }
    }
}
