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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TreeBuilder;

import static org.openrewrite.Validated.required;

public class ChangeMethodName extends JavaIsoRefactorVisitor {
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
    public J.MethodDecl visitMethod(J.MethodDecl method) {
        J.MethodDecl m = super.visitMethod(method);

        J.ClassDecl classDecl = getCursor().firstEnclosing(J.ClassDecl.class);
        assert classDecl != null;

        if (methodMatcher.matches(method, classDecl)) {
            m = m.withName(m.getName().withName(name));
        }

        return m;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        if (methodMatcher.matches(method)) {
            andThen(new Scoped(method, name));
        }
        return super.visitMethodInvocation(method);
    }

    @Override
    public J.Ident visitIdentifier(J.Ident ident) {
        J.Ident i = super.visitIdentifier(ident);
        return i;
    }

    /**
     * The only time field access should be relevant to changing method names is static imports.
     * This exists to turn
     *  import static com.abc.B.static1;
     * into
     *  import static com.abc.B.static2;
     */
    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess) {
        J.FieldAccess f = super.visitFieldAccess(fieldAccess);
        if (f.isFullyQualifiedClassReference(methodMatcher)) {
            Expression target = f.getTarget();
            if(target instanceof J.FieldAccess) {
                String className = target.printTrimmed();
                String fullyQualified = className + "." + name;
                return TreeBuilder.buildName(fullyQualified, f.getFormatting(), f.getId());
            }
        }
        return f;
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
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
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
            return scope.isScope(method) && !method.getSimpleName().equals(name) ?
                    method.withName(method.getName().withName(name)) :
                    super.visitMethodInvocation(method);
        }
    }
}
