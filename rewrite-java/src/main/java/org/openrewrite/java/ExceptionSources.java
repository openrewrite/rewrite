/*
 * Copyright 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Optional;

import static java.util.Collections.emptyList;

/**
 * What in a block can throw a checked exception that the methods of a pattern no longer declare: calls of those
 * methods, and everything else that still can, such as a `throw`, another call declaring the exception, a subtype or
 * a supertype of it, the implicit `close()` of a try-with-resources resource, or a call without type information.
 */
final class ExceptionSources {
    final boolean matchingCalls;
    final boolean others;

    private ExceptionSources(boolean matchingCalls, boolean others) {
        this.matchingCalls = matchingCalls;
        this.others = others;
    }

    static ExceptionSources of(J.Block body, MethodMatcher matcher, JavaType.FullyQualified exceptionType) {
        return of(body, null, matcher, exceptionType);
    }

    /**
     * The sources of a `try` statement's own body and resources, without its catches and `finally`.
     */
    static ExceptionSources of(J.Try tryable, MethodMatcher matcher, JavaType.FullyQualified exceptionType) {
        return of(tryable.getBody(), tryable.getResources(), matcher, exceptionType);
    }

    private static ExceptionSources of(J.Block body, @Nullable Iterable<J.Try.Resource> resources, MethodMatcher matcher, JavaType.FullyQualified exceptionType) {
        boolean[] found = new boolean[2];
        JavaIsoVisitor<Integer> visitor = new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                check(method.getMethodType(), matcher.matches(method));
                return super.visitMethodInvocation(method, p);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                check(newClass.getMethodType(), matcher.matches(newClass));
                if (newClass.getBody() == null) {
                    return super.visitNewClass(newClass, p);
                }
                // the methods of an anonymous class are not run here, its arguments and initializers are
                visit(newClass.getEnclosing(), p);
                for (Expression argument : newClass.getArguments()) {
                    visit(argument, p);
                }
                for (Statement statement : newClass.getBody().getStatements()) {
                    if (statement instanceof J.Block || statement instanceof J.VariableDeclarations) {
                        visit(statement, p);
                    }
                }
                return newClass;
            }

            @Override
            public J.Try visitTry(J.Try tryable, Integer p) {
                for (J.Try.Catch c : tryable.getCatches()) {
                    if (TypeUtils.isAssignableTo(c.getParameter().getType(), exceptionType)) {
                        // what the body and resources throw is caught here
                        for (J.Try.Catch other : tryable.getCatches()) {
                            visit(other, p);
                        }
                        visit(tryable.getFinally(), p);
                        return tryable;
                    }
                }
                return super.visitTry(tryable, p);
            }

            @Override
            public J.Throw visitThrow(J.Throw thrown, Integer p) {
                JavaType type = thrown.getException().getType();
                if (type == null || related(type, exceptionType)) {
                    found[1] = true;
                }
                return super.visitThrow(thrown, p);
            }

            @Override
            public J.Try.Resource visitTryResource(J.Try.Resource tryResource, Integer p) {
                if (closeMayThrow(tryResource.getVariableDeclarations().getType(), exceptionType)) {
                    found[1] = true;
                }
                return super.visitTryResource(tryResource, p);
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
                return lambda;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                return classDecl;
            }

            private void check(JavaType.@Nullable Method methodType, boolean matches) {
                if (matches) {
                    // whatever its type still lists, as another recipe may already have removed the exception from it
                    found[0] = true;
                    return;
                }
                if (methodType == null) {
                    found[1] = true;
                    return;
                }
                for (JavaType thrown : methodType.getThrownExceptions()) {
                    if (related(thrown, exceptionType)) {
                        found[1] = true;
                    }
                }
            }
        };
        visitor.visit(body, 0);
        if (resources != null) {
            for (J.Try.Resource resource : resources) {
                visitor.visit(resource, 0);
            }
        }
        return new ExceptionSources(found[0], found[1]);
    }

    /**
     * Whether the exception or one of its subtypes or supertypes is the type.
     */
    private static boolean related(JavaType type, JavaType.FullyQualified exceptionType) {
        return TypeUtils.isAssignableTo(exceptionType, type) || TypeUtils.isAssignableTo(type, exceptionType);
    }

    /**
     * Whether the `close()` a try-with-resources calls on a resource of the type can throw the exception: the
     * nearest `close()` declaration in the type's hierarchy decides, and a type without one, or without type
     * information, is taken to throw it.
     */
    private static boolean closeMayThrow(@Nullable JavaType type, JavaType.FullyQualified exceptionType) {
        Optional<JavaType.Method> close = TypeUtils.findDeclaredMethod(TypeUtils.asFullyQualified(type), "close", emptyList());
        if (!close.isPresent()) {
            return true;
        }
        for (JavaType thrown : close.get().getThrownExceptions()) {
            if (related(thrown, exceptionType)) {
                return true;
            }
        }
        return false;
    }
}
