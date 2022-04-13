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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Partial implementation of static analysis RSPEC-3740.
 * Recipe is scoped to private method declarations, and check the parameterized types passed into the method invocation.
 * This may be expanded to more use cases when data flow analysis is implemented.
 */
@Incubating(since = "7.22.0")
@EqualsAndHashCode(callSuper = true)
@Value
public class UseGenericTypeMethodParameters extends Recipe {

    @Option(displayName = "Optional `List` of fully qualified names for allowed annotations",
            description = "Enables transformations of methods with the provided annotations.",
            example = "`org.openrewrite.internal.lang.Nullable` or `javax.inject.Inject`",
            required = false)
    @Nullable
    List<String> allowedAnnotations;

    @Override
    public String getDisplayName() {
        return "Use type parameters instead of raw types";
    }

    @Override
    public String getDescription() {
        return "Replaces raw method parameters on private methods with an inferable generic type. " +
                "The raw type will not be updated if a raw type or more than 1 type is passed to the method through method invocations in the class. " +
                "By default, the recipe will not update methods with any annotations on a parameter. I.E. `@Autowired Object arg0`";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-3740");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    public static Set<String> rspecRawTypes = new HashSet<>();

    static {
        rspecRawTypes.add("java.util.Deque");
        rspecRawTypes.add("java.util.List");
        rspecRawTypes.add("java.util.Queue");
        rspecRawTypes.add("java.util.Set");
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
                    if (fq != null && rspecRawTypes.contains(fq.getFullyQualifiedName())) {
                        return super.visitJavaSourceFile(cu, executionContext);
                    }
                }
                return cu;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, executionContext);
                if (method.hasModifier(J.Modifier.Type.Private) && !isOverload(method.getSimpleName()) &&
                        !method.isConstructor() && containsTargetParamType(method)) {

                    Map<Integer, Set<JavaType>> parameterizedArguments = compareMethodInvocations(method);
                    if (parameterizedArguments.isEmpty()) {
                        return m;
                    }

                    // Do not transform the method if any of the invocations passed in a raw type or there is more than 1 type associated to the parameter.
                    if (parameterizedArguments.values().stream().anyMatch(o -> o.size() > 1 ||
                            o.stream().anyMatch(type -> type instanceof JavaType.Unknown))) {
                        return m;
                    }

                    Optional<String> parameterCode = generateTemplateCode(method.getParameters(), parameterizedArguments);
                    if (parameterCode.isPresent()) {
                        m = m.withTemplate(
                                JavaTemplate.builder(this::getCursor, parameterCode.get()).build(),
                                m.getCoordinates().replaceParameters());
                    }
                }
                return m;
            }

            /**
             * Finds the {@link J.MethodInvocation}(s) of the {@link J.MethodDeclaration} and collects the parameterized type
             * from the invocation at each index of a raw type parameter.
             * The `JavaType` will be `Unknown` if a raw type is used in the method invocation.
             *
             * @return Map from the index of each raw type to the type used in each method invocation.
             */
            private Map<Integer, Set<JavaType>> compareMethodInvocations(J.MethodDeclaration method) {
                Cursor searchParent = getCursor().dropParentUntil(is -> is instanceof J.ClassDeclaration);
                // Find all the method invocations in the class to check if there is a consistent type parameter.
                Set<J> methodInvocations = FindMethods.find(searchParent.getValue(), getMethodPattern(method));

                Map<Integer, Set<JavaType>> parameterizedArguments = new HashMap<>();
                for (int i = 0; i < method.getParameters().size(); i++) {
                    Statement currentParameter = method.getParameters().get(i);
                    if (!(currentParameter instanceof J.VariableDeclarations)) {
                        // Safety check.
                        return Collections.emptyMap();
                    }

                    TypeTree typeExpression = ((J.VariableDeclarations) currentParameter).getTypeExpression();
                    if (isRawType(typeExpression)) {
                        // Check each method invocation of the raw type for a consistent type.
                        for (J j : methodInvocations) {
                            if (j instanceof J.MethodInvocation) {
                                J.MethodInvocation methodInvocation = (J.MethodInvocation) j;
                                Expression expression = methodInvocation.getArguments().get(i);
                                if (expression instanceof J.Identifier) {
                                    J.Identifier identifier = (J.Identifier) expression;
                                    Set<JavaType> types = parameterizedArguments.computeIfAbsent(i, k -> new HashSet<>());
                                    if (identifier.getType() instanceof JavaType.Parameterized) {
                                        types.add(((JavaType.Parameterized) identifier.getType()).getTypeParameters().get(0));
                                    } else {
                                        types.add(JavaType.Unknown.getInstance());
                                    }
                                }
                            }
                        }
                    }
                }
                return parameterizedArguments;
            }

            private Optional<String> generateTemplateCode(List<Statement> parameters, Map<Integer, Set<JavaType>> parameterizedArguments) {
                StringJoiner newParameters = new StringJoiner(", ");
                for (int i = 0; i < parameters.size(); i++) {
                    Statement parameter = parameters.get(i);
                    if (parameter instanceof J.VariableDeclarations) {
                        TypeTree typeExpression = ((J.VariableDeclarations) parameter).getTypeExpression();
                        if (isRawType(typeExpression)) {
                            String[] partsOfRawType = parameter.toString().split(" ");
                            JavaType.FullyQualified newType = TypeUtils.asFullyQualified(new ArrayList<>(parameterizedArguments.get(i)).get(0));
                            // A null type cannot be transformed through the template.
                            if (newType == null) {
                                return Optional.empty();
                            }
                            String parameterizedTypeFqn = newType.getFullyQualifiedName();
                            StringBuilder annotations = new StringBuilder();
                            int j = 0;
                            for (; j < partsOfRawType.length; j++) {
                                String s = partsOfRawType[j];
                                if (s.startsWith("@")) {
                                    annotations.append(s).append(" ");
                                } else {
                                    break;
                                }
                            }
                            String param = annotations + partsOfRawType[j] + "<" + parameterizedTypeFqn.substring(parameterizedTypeFqn.lastIndexOf('.') + 1) + "> " + partsOfRawType[j + 1];
                            newParameters.add(param);
                        } else {
                            newParameters.add(parameter.toString());
                        }
                    }
                }
                return Optional.of(newParameters.toString());
            }

            private boolean isRawType(@Nullable TypeTree typeExpression) {
                if (typeExpression instanceof J.Identifier) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(typeExpression.getType());
                    return fq != null && rspecRawTypes.contains(fq.getFullyQualifiedName());
                }
                return false;
            }

            // Generates a method pattern from a `J.MethodDeclaration` to search for invocations in the `CU`.
            private String getMethodPattern(J.MethodDeclaration methodDeclaration) {
                MethodMatcher methodMatcher = new MethodMatcher(methodDeclaration);
                return methodMatcher.getTargetTypePattern() + " " + methodMatcher.getMethodNamePattern() + "(" + methodMatcher.getArgumentPattern() + ")";
            }

            private boolean containsTargetParamType(J.MethodDeclaration methodDeclaration) {
                for (Statement parameter : methodDeclaration.getParameters()) {
                    if (parameter instanceof J.VariableDeclarations) {
                        TypeTree typeExpression = ((J.VariableDeclarations) parameter).getTypeExpression();
                        // Annotations could be relaxed to check for specific annotations like `@Autowired`.
                        // A catch-call to prevent unexpected changes to annotations that inject code.
                        if (validAnnotations(((J.VariableDeclarations) parameter).getAllAnnotations()) && isRawType(typeExpression)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private boolean validAnnotations(List<J.Annotation> annotations) {
                return allowedAnnotations == null && annotations.isEmpty() ||
                        allowedAnnotations != null && allowedAnnotations.containsAll(annotations.stream()
                                // Add "null" to prevent undesired changes if type is null.
                                .map(o -> o.getAnnotationType().getType() == null ? "null" :
                                        o.getAnnotationType().getType().toString()).collect(Collectors.toList()));
            }

            /**
             * Returns true if the current class contains more than 1 method with the specified name.
             */
            private boolean isOverload(String methodName) {
                J.ClassDeclaration classDeclaration = getCursor().dropParentUntil(is -> is instanceof J.ClassDeclaration).getValue();
                JavaIsoVisitor<List<String>> findVisitor = new JavaIsoVisitor<List<String>>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, List<String> matches) {
                        if (method.getSimpleName().equals(methodName)) {
                            matches.add(methodName);
                        }
                        return super.visitMethodDeclaration(method, matches);
                    }
                };

                List<String> matches = new ArrayList<>();
                findVisitor.visit(classDeclaration, matches);
                return matches.size() > 1;
            }
        };
    }
}
