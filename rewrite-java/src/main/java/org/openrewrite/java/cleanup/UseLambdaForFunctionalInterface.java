/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;

public class UseLambdaForFunctionalInterface extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use lambdas where possible";
    }

    @Override
    public String getDescription() {
        return "Instead of anonymous class declarations, use a lambda where possible.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-1604");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (n.getBody() != null &&
                        n.getBody().getStatements().size() == 1 &&
                        n.getBody().getStatements().get(0) instanceof J.MethodDeclaration &&
                        n.getClazz() != null) {
                    JavaType.Class type = TypeUtils.asClass(n.getClazz().getType());
                    if (type != null && type.getKind().equals(JavaType.Class.Kind.Interface)) {
                        JavaType.Method sam = null;
                        for (JavaType.Method method : type.getMethods()) {
                            if (method.hasFlags(Flag.Default) || method.hasFlags(Flag.Static)) {
                                continue;
                            }
                            if (sam != null) {
                                return n;
                            }
                            sam = method;
                        }
                        if (sam == null) {
                            return n;
                        }

                        JavaType.Method.Signature signature = sam.getResolvedSignature();

                        if (signature != null) {
                            StringBuilder templateBuilder = new StringBuilder();
                            J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) n.getBody().getStatements().get(0);

                            templateBuilder.append(methodDeclaration.getParameters().stream()
                                    .map(param -> ((J.VariableDeclarations) param).getVariables().get(0).getSimpleName())
                                    .collect(Collectors.joining(",", "(", ") -> {")));

                            JavaType returnType = signature.getReturnType();
                            if (!JavaType.Primitive.Void.equals(returnType)) {
                                templateBuilder.append("return ").append(valueOfType(returnType)).append('.');
                            }
                            templateBuilder.append('}');

                            J.Lambda lambda = n.withTemplate(
                                    JavaTemplate.builder(this::getCursor, templateBuilder.toString())
                                            .build(),
                                    n.getCoordinates().replace()
                            );

                            lambda = (J.Lambda) new UnnecessaryParenthesesVisitor<ExecutionContext>(Checkstyle.unnecessaryParentheses())
                                    .visitNonNull(lambda, ctx);

                            J.Block lambdaBody = methodDeclaration.getBody();
                            assert lambdaBody != null;

                            lambda = lambda.withBody(lambdaBody.withPrefix(Space.format(" ")));

                            lambda = (J.Lambda) new LambdaBlockToExpression().getVisitor().visitNonNull(lambda, ctx);

                            return autoFormat(lambda, ctx, getCursor().getParentOrThrow());
                        }
                    }
                }
                return n;
            }

            private String valueOfType(@Nullable JavaType type) {
                JavaType.Primitive primitive = TypeUtils.asPrimitive(type);
                if (primitive != null) {
                    switch (primitive) {
                        case Boolean:
                            return "true";
                        case Byte:
                        case Char:
                        case Int:
                        case Double:
                        case Float:
                        case Long:
                        case Short:
                            return "0";
                        case String:
                        case Null:
                            return "null";
                        case None:
                        case Wildcard:
                        case Void:
                        default:
                            return "";
                    }
                }

                return "null";
            }
        };
    }
}
