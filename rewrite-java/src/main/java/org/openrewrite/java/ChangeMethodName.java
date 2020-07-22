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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.J;

import static org.openrewrite.Validated.required;

public class ChangeMethodName extends JavaRefactorVisitor {
    private MethodMatcher methodMatcher;
    private String name;

    public ChangeMethodName() {
        setCursoringOn();
    }

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Validated validate() {
        return required("method", methodMatcher)
                .and(required("name", name));
    }

    @Override
    public J visitMethod(J.MethodDecl method) {
        J.MethodDecl m = refactor(method, super::visitMethod);

        J.ClassDecl classDecl = getCursor().firstEnclosing(J.ClassDecl.class);
        assert classDecl != null;

        if (methodMatcher.matches(method, classDecl)) {
            m = m.withName(m.getName().withName(name));
        }

        return m;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        if (methodMatcher.matches(method)) {
            andThen(new Scoped(method, name));
        }
        return super.visitMethodInvocation(method);
    }

    public static class Scoped extends JavaRefactorVisitor {
        private final J.MethodInvocation scope;
        private final String name;

        public Scoped(J.MethodInvocation scope, String name) {
            this.scope = scope;
            this.name = name;
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("name", name);
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method) {
            return scope.isScope(method) && !method.getSimpleName().equals(name) ?
                    method.withName(method.getName().withName(name)) :
                    super.visitMethodInvocation(method);
        }
    }
}
