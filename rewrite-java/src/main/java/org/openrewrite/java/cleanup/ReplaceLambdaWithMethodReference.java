/*
 * Copyright 2022 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.*;

public class ReplaceLambdaWithMethodReference extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use method references in lambda";
    }

    @Override
    public String getDescription() {
        return "Replaces the single statement lambdas `o -> o instanceOf X`, `o -> (A) o`, `o -> System.out.println(o)`, `o -> o != null`, `o -> o == null` with the equivalent method reference.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1612");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        MethodMatcher printStreamMethod = new MethodMatcher("java.io.PrintStream println(..)");

        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitLambda(J.Lambda lambda, ExecutionContext executionContext) {
                J.Lambda l = (J.Lambda) super.visitLambda(lambda, executionContext);

                String code = "";
                J body = l.getBody();
                if (body instanceof J.Block && ((J.Block) body).getStatements().size() == 1) {
                    Statement statement = ((J.Block) body).getStatements().get(0);
                    if (statement instanceof J.MethodInvocation) {
                        body = statement;
                    }
                } else if (body instanceof J.InstanceOf) {
                    J j = ((J.InstanceOf) body).getClazz();
                    if (j instanceof J.Identifier) {
                        body = j;
                        code = "#{}.class::isInstance";
                    }
                } else if (body instanceof J.TypeCast) {
                    if (!(((J.TypeCast) body).getExpression() instanceof J.MethodInvocation)) {
                        J j = ((J.TypeCast) body).getClazz();
                        if (j != null) {
                            @SuppressWarnings("rawtypes")
                            J tree = ((J.ControlParentheses) j).getTree();
                            if (tree instanceof J.Identifier) {
                                body = tree;
                                code = "#{}.class::cast";
                            }
                        }
                    }
                }

                if (body instanceof J.Identifier) {
                        J.Identifier identifier = (J.Identifier) body;
                        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(identifier.getType());
                        @Language("java")
                        String stub = fullyQualified == null ? "" : "package " + fullyQualified.getPackageName() + "; public class " + fullyQualified.getClassName() + "{}";
                        JavaTemplate template = JavaTemplate
                                .builder(this::getCursor, code)
                                .javaParser(() -> JavaParser.fromJavaVersion()
                                        .dependsOn(stub)
                                        .build())
                                .imports(fullyQualified == null ? "" : fullyQualified.getFullyQualifiedName())
                                .build();
                        return l.withTemplate(template, l.getCoordinates().replace(), identifier.getSimpleName());
                } else if (body instanceof J.Binary) {
                    J.Binary binary = (J.Binary) body;
                    if (isNullCheck(binary.getLeft(), binary.getRight()) || isNullCheck(binary.getRight(), binary.getLeft())) {
                        maybeAddImport("java.util.Objects");
                        code = J.Binary.Type.Equal.equals(binary.getOperator()) ? "Objects::isNull" : "Objects::nonNull";
                        return l.withTemplate(JavaTemplate.builder(this::getCursor, code).imports("java.util.Objects").build(), l.getCoordinates().replace());
                    }
                } else if (body instanceof J.MethodInvocation) {
                    if (printStreamMethod.matches((J.MethodInvocation) body)) {
                        return l.withTemplate(JavaTemplate.builder(this::getCursor, "System.out::println").build(), l.getCoordinates().replace());
                    }
                }

                return l;
            }

            private boolean isNullCheck(J j1, J j2) {
                return j1 instanceof J.Identifier &&
                        j2 instanceof J.Literal && "null".equals(((J.Literal) j2).getValueSource());
            }
        };
    }
}
