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
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

public class InsertMethodArgument extends JavaIsoRefactorVisitor {
    private MethodMatcher methodMatcher;
    private Integer index;
    private String source;

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public Validated validate() {
        return required("method", methodMatcher)
                .and(required("pos", index))
                .and(required("source", source));
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        if(methodMatcher.matches(method)) {
            andThen(new Scoped(method, index, source));
        }
        return super.visitMethodInvocation(method);
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.MethodInvocation scope;
        private final int index;
        private final String source;

        public Scoped(J.MethodInvocation scope, int index, String source) {
            this.scope = scope;
            this.index = index;
            this.source = source;
        }

        @Override
        public boolean isIdempotent() {
            return false;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
            if (scope.isScope(method)) {
                List<Expression> modifiedArgs = method.getArgs().getArgs().stream()
                        .filter(a -> !(a instanceof J.Empty))
                        .collect(Collectors.toCollection(ArrayList::new));
                modifiedArgs.add(index,
                        new J.UnparsedSource(randomId(),
                                source,
                                Collections.emptyList(),
                                index == 0 ?
                                        modifiedArgs.stream().findFirst().map(Tree::getFormatting).orElse(Formatting.EMPTY) :
                                        format(" "),
                                Markers.EMPTY
                        )
                );

                if (index == 0 && modifiedArgs.size() > 1) {
                    // this argument previously did not occur after a comma, and now does, so let's introduce a bit of space
                    modifiedArgs.set(1, modifiedArgs.get(1).withFormatting(format(" ")));
                }

                return method.withArgs(method.getArgs().withArgs(modifiedArgs));
            }

            return super.visitMethodInvocation(method);
        }
    }
}
