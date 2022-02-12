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
package org.openrewrite.java;

import lombok.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.java.tree.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Alex Boyko
 */
public class ChangeMethodInvocationParameters extends Recipe {

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class ParameterInfo {
        private int oldParamIndex;
        private String typeFQN;
        private String template;
        private Collection<String> imports = Collections.emptyList();
    }

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;
    private List<ParameterInfo> parameterInfos; // order of parameters

    public ChangeMethodInvocationParameters(String methodPattern, List<ParameterInfo> parameterInfos) {
        this.methodPattern = methodPattern;
        this.parameterInfos = parameterInfos;
    }

    @Override
    public String getDisplayName() {
        return "Change method invocation parameter";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, true);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation m = super.visitMethodInvocation(method, context);
                if (methodMatcher.matches(method)) {

                    // Create new arguments as a string
                    String newParamsStr = parameterInfos.stream().filter(info -> info.template != null).map(info -> {
                        if (info.oldParamIndex > 0 && info.oldParamIndex <= method.getArguments().size()) {
                            String oldParam = method.getArguments().get(info.oldParamIndex - 1).printTrimmed();
                            return info.template.replaceAll("\\{\\}", oldParam);
                        } else {
                            return info.template;
                        }
                    }).collect(Collectors.joining(","));

                    // Assemble all types used by the new arguments
                    Set<String> allImports = parameterInfos.stream().filter(info -> info.imports != null).flatMap(info -> info.imports.stream()).collect(Collectors.toSet());

                    // Set new args on the method
                    JavaType.Method type = method.getMethodType();
                    JavaTemplate template = JavaTemplate.builder(() -> getCursor(), newParamsStr).imports(allImports.toArray(new String[allImports.size()])).build();
                    m = m.withTemplate(template, m.getCoordinates().replaceArguments());

                    // Change method signature based on the new args types. Do this after parsing new args thus types resolved
                    List<JavaType> newParamTypes = m.getArguments().stream()
                            .map(e -> e.getType())
                            .collect(Collectors.toList());
                    m = m.withMethodType(m.getMethodType().withParameterTypes(newParamTypes));

                    // Add the imports for the types used in new args
                    for (String i : allImports) {
                        maybeAddImport(i);
                    }

                    // Remove likely unused imports
                    for (Expression arg : method.getArguments()) {
                        // Find all referenced type in original method arguments and try to remove them
                        Set<NameTree> types = FindTypes.findAssignable(arg, "java.lang.Object");
                        for (NameTree t : types) {
                            JavaType.FullyQualified foundType = TypeUtils.asFullyQualified(t.getType());
                            if (foundType != null) {
                                maybeRemoveImport(foundType.getFullyQualifiedName());
                            }
                        }
                    }
                }
                return m;
            }
        };
    }
}
