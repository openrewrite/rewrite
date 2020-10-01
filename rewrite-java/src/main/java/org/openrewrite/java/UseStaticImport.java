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

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class UseStaticImport extends JavaIsoRefactorVisitor {
    private MethodMatcher methodMatcher;

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        if (methodMatcher.matches(method) && method.getSelect() != null) {
            andThen(new Scoped(method));
        }
        return super.visitMethodInvocation(method);
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.MethodInvocation scope;

        public Scoped(J.MethodInvocation scope) {
            this.scope = scope;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
            if (scope.isScope(method) && method.getSelect() != null) {
                if (method.getType() != null) {
                    JavaType.FullyQualified receiverType = method.getType().getDeclaringType();
                    maybeRemoveImport(receiverType);

                    AddImport addStatic = new AddImport();
                    addStatic.setType(receiverType.getFullyQualifiedName());
                    addStatic.setStaticMethod(method.getSimpleName());
                    if (!andThen().contains(addStatic)) {
                        andThen(addStatic);
                    }
                }

                return method
                        .withSelect(null)
                        .withName(method.getName().withFormatting(method.getSelect().getFormatting()));
            }
            return super.visitMethodInvocation(method);
        }
    }
}
