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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitLambda(J.Lambda lambda, ExecutionContext executionContext) {
                J.Lambda l = (J.Lambda) super.visitLambda(lambda, executionContext);

                if (TypeUtils.isOfClassType(lambda.getType(), "groovy.lang.Closure")) {
                    return l;
                }

                String code = "";
                J body = l.getBody();
                if (body instanceof J.Block && ((J.Block) body).getStatements().size() == 1) {
                    Statement statement = ((J.Block) body).getStatements().get(0);
                    if (statement instanceof J.MethodInvocation) {
                        body = statement;
                    } else if (statement instanceof J.Return &&
                            (((J.Return) statement).getExpression()) instanceof MethodCall) {
                        body = ((J.Return) statement).getExpression();
                    }
                } else if (body instanceof J.InstanceOf) {
                    J j = ((J.InstanceOf) body).getClazz();
                    if (j instanceof J.Identifier) {
                        body = j;
                        code = "#{}.class::isInstance";
                    }
                } else if (body instanceof J.TypeCast) {
                    if (!(((J.TypeCast) body).getExpression() instanceof J.MethodInvocation)) {
                        J.ControlParentheses<TypeTree> j = ((J.TypeCast) body).getClazz();
                        if (j != null) {
                            @SuppressWarnings("rawtypes") J tree = ((J.ControlParentheses) j).getTree();
                            if (tree instanceof J.Identifier &&
                                    !(j.getType() instanceof JavaType.GenericTypeVariable)) {
                                body = tree;
                                code = "#{}.class::cast";
                            }
                        }
                    }
                }

                if (body instanceof J.Identifier && !code.isEmpty()) {
                    J.Identifier identifier = (J.Identifier) body;
                    JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(identifier.getType());
                    @Language("java") String stub = fullyQualified == null ? "" :
                            "package " + fullyQualified.getPackageName() + "; public class " +
                                    fullyQualified.getClassName();
                    JavaTemplate template = JavaTemplate.builder(this::getCursor, code)
                            .javaParser(() -> JavaParser.fromJavaVersion().dependsOn(stub).build())
                            .imports(fullyQualified == null ? "" : fullyQualified.getFullyQualifiedName()).build();
                    return l.withTemplate(template, l.getCoordinates().replace(), identifier.getSimpleName());
                } else if (body instanceof J.Binary) {
                    J.Binary binary = (J.Binary) body;
                    if (isNullCheck(binary.getLeft(), binary.getRight()) ||
                            isNullCheck(binary.getRight(), binary.getLeft())) {
                        maybeAddImport("java.util.Objects");
                        code = J.Binary.Type.Equal.equals(binary.getOperator()) ? "Objects::isNull" :
                                "Objects::nonNull";
                        return l.withTemplate(
                                JavaTemplate.builder(this::getCursor, code).imports("java.util.Objects").build(),
                                l.getCoordinates().replace());
                    }
                } else if (body instanceof MethodCall) {
                    MethodCall method = (MethodCall) body;
                    if (method instanceof J.NewClass && ((J.NewClass) method).getBody() != null ||
                            multipleMethodInvocations(method) ||
                            !methodArgumentsMatchLambdaParameters(method, lambda)) {
                        return l;
                    }

                    Expression select =
                            method instanceof J.MethodInvocation ? ((J.MethodInvocation) method).getSelect() : null;
                    JavaType.Method methodType = method.getMethodType();
                    if (methodType != null) {
                        JavaType.FullyQualified declaringType = methodType.getDeclaringType();
                        if (methodType.hasFlags(Flag.Static) ||
                                methodSelectMatchesFirstLambdaParameter(method, lambda)) {
                            maybeAddImport(declaringType);
                            return l.withTemplate(JavaTemplate.builder(this::getCursor, "#{}::#{}").build(),
                                    l.getCoordinates().replace(), declaringType.getClassName(),
                                    method.getMethodType().getName());
                        } else if (method instanceof J.NewClass) {
                            return l.withTemplate(JavaTemplate.builder(this::getCursor, "#{}::new").build(),
                                    l.getCoordinates().replace(), className((J.NewClass) method));
                        } else {
                            String templ = select == null ? "#{}::#{}" :
                                    "#{any(" + declaringType.getFullyQualifiedName() + ")}::#{}";
                            return l.withTemplate(JavaTemplate.builder(this::getCursor, templ).build(),
                                    l.getCoordinates().replace(), select == null ? "this" : select,
                                    method.getMethodType().getName());
                        }
                    }
                }

                return l;
            }

            // returns the class name as given in the source code (qualified or unqualified)
            private String className(J.NewClass method) {
                TypeTree clazz = method.getClazz();
                return clazz instanceof J.ParameterizedType ? ((J.ParameterizedType) clazz).getClazz().toString() :
                        Objects.toString(clazz);
            }

            private boolean multipleMethodInvocations(MethodCall method) {
                return method instanceof J.MethodInvocation &&
                        ((J.MethodInvocation) method).getSelect() instanceof J.MethodInvocation;
            }

            private boolean methodArgumentsMatchLambdaParameters(MethodCall method, J.Lambda lambda) {
                JavaType.Method methodType = method.getMethodType();
                if (methodType == null) {
                    return false;
                }
                boolean statik = methodType.hasFlags(Flag.Static);
                List<Expression> methodArgs = method.getArguments().stream().filter(a -> !(a instanceof J.Empty))
                        .collect(Collectors.toList());
                List<J.VariableDeclarations.NamedVariable> lambdaParameters = lambda.getParameters().getParameters()
                        .stream().filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast).map(v -> v.getVariables().get(0))
                        .collect(Collectors.toList());
                if (methodArgs.isEmpty() && lambdaParameters.isEmpty()) {
                    return true;
                }
                if (!statik && methodSelectMatchesFirstLambdaParameter(method, lambda)) {
                    methodArgs.add(0, ((J.MethodInvocation) method).getSelect());
                }
                if (methodArgs.size() != lambdaParameters.size()) {
                    return false;
                }
                for (int i = 0; i < lambdaParameters.size(); i++) {
                    JavaType lambdaParam = lambdaParameters.get(i).getVariableType();
                    if (!(methodArgs.get(i) instanceof J.Identifier)) {
                        return false;
                    }
                    JavaType methodArgument = ((J.Identifier) methodArgs.get(i)).getFieldType();
                    if (lambdaParam != methodArgument) {
                        return false;
                    }
                }
                return true;
            }

            private boolean methodSelectMatchesFirstLambdaParameter(MethodCall method, J.Lambda lambda) {
                if (!(method instanceof J.MethodInvocation) ||
                        !(((J.MethodInvocation) method).getSelect() instanceof J.Identifier) ||
                        !(lambda.getParameters().getParameters().get(0) instanceof J.VariableDeclarations)) {
                    return false;
                }
                J.VariableDeclarations firstLambdaParameter = (J.VariableDeclarations) lambda.getParameters()
                        .getParameters().get(0);
                return ((J.Identifier) ((J.MethodInvocation) method).getSelect()).getFieldType() ==
                        firstLambdaParameter.getVariables().get(0).getVariableType();
            }

            private boolean isNullCheck(J j1, J j2) {
                return j1 instanceof J.Identifier && j2 instanceof J.Literal &&
                        "null".equals(((J.Literal) j2).getValueSource());
            }
        };
    }
}
