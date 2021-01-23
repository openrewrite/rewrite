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
import org.openrewrite.TreeProcessor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

/**
 * A recipe that will look for a specific method target (using a method pattern) and rename the method. This recipe renames
 * both the method declaration and any invocations/references to the method.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodName extends Recipe {

    private final String methodPattern;
    private final String newMethodName;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new ChangeMethodNameProcessor(new MethodMatcher(methodPattern));
    }

    private class ChangeMethodNameProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private ChangeMethodNameProcessor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
            setCursoringOn();
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl method, ExecutionContext ctx) {
            J.MethodDecl m = super.visitMethod(method, ctx);
            J.ClassDecl classDecl = getCursor().firstEnclosingOrThrow(J.ClassDecl.class);
            if (methodMatcher.matches(method, classDecl)) {
                m = m.withName(m.getName().withName(newMethodName));
            }
            return m;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                m = m.withName(m.getName().withName(newMethodName));
            }
            return m;
        }

        /**
         * The only time field access should be relevant to changing method names is static imports.
         * This exists to turn
         * import static com.abc.B.static1;
         * into
         * import static com.abc.B.static2;
         */
        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);
            if (f.isFullyQualifiedClassReference(methodMatcher)) {
                Expression target = f.getTarget();
                if (target instanceof J.FieldAccess) {
                    String className = target.printTrimmed();
                    String fullyQualified = className + "." + newMethodName;
                    return TypeTree.build(fullyQualified)
                            .withPrefix(f.getPrefix());
                }
            }
            return f;
        }
    }
}
