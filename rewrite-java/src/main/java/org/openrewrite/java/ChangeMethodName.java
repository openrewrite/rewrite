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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodName extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method name",
            description = "The method name that will replace the existing name.",
            example = "any")
    String newMethodName;

    @Override
    public String getDisplayName() {
        return "Change method name";
    }

    @Override
    public String getDescription() {
        return "Rename a method.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesMethod<>(methodPattern));
                doAfterVisit(new DeclaresMethod<>(methodPattern));
                return cu;
            }
        };
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new ChangeMethodNameVisitor(new MethodMatcher(methodPattern));
    }

    private class ChangeMethodNameVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private ChangeMethodNameVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
            if (methodMatcher.matches(method, classDecl)) {
                JavaType.Method type = m.getType();
                if(type != null) {
                    type = type.withName(newMethodName);
                }
                m = m.withName(m.getName().withName(newMethodName))
                        .withType(type);
            }
            return m;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                JavaType.Method type = m.getType();
                if(type != null) {
                    type = type.withName(newMethodName);
                }
                m = m.withName(m.getName().withName(newMethodName))
                        .withType(type);
            }
            return m;
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext context) {
            J.MemberReference m = super.visitMemberReference(memberRef, context);
            if (methodMatcher.matches(m.getReferenceType()) && !m.getReference().getSimpleName().equals(newMethodName)) {
                JavaType type = m.getReferenceType();
                if(type instanceof JavaType.Method) {
                    JavaType.Method mtype = (JavaType.Method) type;
                    type = mtype.withName(newMethodName);
                }
                m = m.withReference(m.getReference().withName(newMethodName))
                        .withReferenceType(type);
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
            if (methodMatcher.isFullyQualifiedClassReference(f)) {
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
