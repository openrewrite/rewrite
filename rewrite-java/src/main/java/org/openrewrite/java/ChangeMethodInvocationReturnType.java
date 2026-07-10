/*
 * Copyright 2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodInvocationReturnType extends Recipe {

    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_DESCRIPTION,
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method invocation return type",
            description = "The fully qualified new return type of method invocation. " +
                    "Parameterized types like `java.util.Set<java.lang.String>` are supported; " +
                    "`java.lang` type arguments may use their simple name, e.g. `java.util.List<String>`.",
            example = "long")
    String newReturnType;

    String displayName = "Change method invocation return type";

    String description = "Changes the return type of a method invocation.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);
        return Preconditions.check(new UsesMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {

            private boolean methodUpdated;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = m.getMethodType();
                if (methodMatcher.matches(method) && type != null && !newReturnType.equals(type.getReturnType().toString())) {
                    type = type.withReturnType(JavaType.buildType(newReturnType));
                    m = m.withMethodType(type);
                    if (m.getName().getType() != null) {
                        m = m.withName(m.getName().withType(type));
                    }
                    methodUpdated = true;
                }
                return m;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                methodUpdated = false;
                JavaType.FullyQualified originalType = multiVariable.getTypeAsFullyQualified();
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                boolean initializedByMatch = mv.getVariables().stream()
                        .anyMatch(v -> isInitializedByMatch(v.getInitializer()));
                if (!methodUpdated || !initializedByMatch || mv.getTypeExpression() == null) {
                    return mv;
                }

                JavaTemplate.Builder templateBuilder = JavaTemplate.builder(newReturnType + " __typePlaceholder__").contextSensitive();
                List<String> stubs = synthesizeStubsForTypeAttribution(newReturnType);
                if (!stubs.isEmpty()) {
                    templateBuilder.javaParser(JavaParser.fromJavaVersion().dependsOn(stubs.toArray(new String[0])));
                }
                J.VariableDeclarations resolved = templateBuilder.build()
                        .apply(updateCursor(mv), mv.getCoordinates().replace());
                TypeTree newTypeExpression = resolved.getTypeExpression();
                if (newTypeExpression == null || newTypeExpression.getType() instanceof JavaType.Unknown) {
                    return mv;
                }

                JavaType newType = newTypeExpression.getType();
                maybeRemoveImport(originalType);
                mv = mv.withTypeExpression(newTypeExpression.withPrefix(mv.getTypeExpression().getPrefix()));
                if (!(newTypeExpression instanceof J.Primitive)) {
                    doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(mv.getTypeExpression()));
                }
                return mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                    JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                    if (varType != null && !varType.equals(newType)) {
                        return var.withType(newType).withName(var.getName().withType(newType));
                    }
                    return var;
                }));
            }

            private boolean isInitializedByMatch(@Nullable Expression expression) {
                if (expression == null) {
                    return false;
                }
                Expression unwrapped = expression.unwrap();
                if (unwrapped instanceof J.MethodInvocation) {
                    return methodMatcher.matches((J.MethodInvocation) unwrapped);
                }
                if (unwrapped instanceof J.Ternary) {
                    J.Ternary ternary = (J.Ternary) unwrapped;
                    return isInitializedByMatch(ternary.getTruePart()) ||
                            isInitializedByMatch(ternary.getFalsePart());
                }
                return false;
            }
        });
    }

    private static List<String> synthesizeStubsForTypeAttribution(String type) {
        Map<String, Integer> fqnToArity = new LinkedHashMap<>();
        collectStubTypes(type, fqnToArity);
        List<String> stubs = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : fqnToArity.entrySet()) {
            String fqn = entry.getKey();
            int lastDot = fqn.lastIndexOf('.');
            StringBuilder typeParameters = new StringBuilder();
            for (int i = 0; i < entry.getValue(); i++) {
                typeParameters.append(i == 0 ? "<T" : ", T").append(i);
            }
            if (entry.getValue() > 0) {
                typeParameters.append('>');
            }
            stubs.add("package " + fqn.substring(0, lastDot) + "; public class " +
                    fqn.substring(lastDot + 1) + typeParameters + " {}");
        }
        return stubs;
    }

    private static void collectStubTypes(String type, Map<String, Integer> fqnToArity) {
        type = type.trim();
        while (type.endsWith("[]")) {
            type = type.substring(0, type.length() - 2).trim();
        }
        if (type.startsWith("?")) {
            int extendsIdx = type.indexOf("extends");
            int superIdx = type.indexOf("super");
            int boundIdx = extendsIdx != -1 ? extendsIdx + "extends".length() :
                    superIdx != -1 ? superIdx + "super".length() : -1;
            if (boundIdx != -1) {
                collectStubTypes(type.substring(boundIdx), fqnToArity);
            }
            return;
        }
        int lt = type.indexOf('<');
        String raw = (lt == -1 ? type : type.substring(0, lt)).trim();
        List<String> arguments = lt == -1 ? emptyList() :
                splitTypeArguments(type.substring(lt + 1, type.lastIndexOf('>')));
        if (raw.indexOf('.') != -1 && !raw.startsWith("java.")) {
            fqnToArity.merge(raw, arguments.size(), Math::max);
        }
        for (String argument : arguments) {
            collectStubTypes(argument, fqnToArity);
        }
    }

    private static List<String> splitTypeArguments(String arguments) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < arguments.length(); i++) {
            char c = arguments.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                result.add(arguments.substring(start, i).trim());
                start = i + 1;
            }
        }
        result.add(arguments.substring(start).trim());
        return result;
    }
}
