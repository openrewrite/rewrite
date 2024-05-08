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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.SearchResult;

import java.util.Iterator;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDeprecatedMethods extends Recipe {
    private static final AnnotationMatcher DEPRECATED_MATCHER = new AnnotationMatcher("java.lang.Deprecated");

    transient MethodCalls deprecatedMethodCalls = new MethodCalls(this);

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "java.util.List add(..)",
            required = false)
    @Nullable
    String methodPattern;

    @Option(displayName = "Ignore deprecated scopes",
            description = "When set to `true` deprecated methods used within deprecated methods or classes will be ignored.",
            required = false)
    @Nullable
    Boolean ignoreDeprecatedScopes;

    @Override
    public String getDisplayName() {
        return "Find uses of deprecated methods";
    }

    @Override
    public String getInstanceNameSuffix() {
        if (methodPattern != null) {
            return "matching `" + methodPattern + "`";
        }
        return super.getInstanceNameSuffix();
    }

    @Override
    public String getDescription() {
        return "Find uses of deprecated methods in any API.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = methodPattern == null || methodPattern.isEmpty() ? null : new MethodMatcher(methodPattern, true);
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
                        if (methodMatcher == null || methodMatcher.matches(method)) {
                            for (JavaType.FullyQualified annotation : method.getAnnotations()) {
                                if (TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                                    return SearchResult.found(cu);
                                }
                            }
                        }
                    }
                }
                return (J) tree;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (method.getMethodType() != null) {
                    for (JavaType.FullyQualified annotation : method.getMethodType().getAnnotations()) {
                        if ((methodMatcher == null || methodMatcher.matches(method)) && TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                            if (Boolean.TRUE.equals(ignoreDeprecatedScopes)) {
                                Iterator<Cursor> cursorPath = getCursor().getPathAsCursors();
                                while (cursorPath.hasNext()) {
                                    Cursor ancestor = cursorPath.next();
                                    if (ancestor.getValue() instanceof J.MethodDeclaration && isDeprecated(ancestor)) {
                                        return m;
                                    }
                                    if (ancestor.getValue() instanceof J.ClassDeclaration && isDeprecated(ancestor)) {
                                        return m;
                                    }
                                }
                            }

                            JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                            if (javaSourceFile != null) {
                                deprecatedMethodCalls.insertRow(ctx, new MethodCalls.Row(
                                        javaSourceFile.getSourcePath().toString(),
                                        method.printTrimmed(getCursor()),
                                        method.getMethodType().getDeclaringType().getFullyQualifiedName(),
                                        method.getSimpleName(),
                                        method.getArguments().stream()
                                                .map(Expression::getType)
                                                .map(String::valueOf)
                                                .collect(Collectors.joining(", "))
                                ));
                            }
                            m = SearchResult.found(m);
                        }
                    }
                }
                return m;
            }

            private boolean isDeprecated(Cursor cursor) {
                return service(AnnotationService.class).matches(cursor, DEPRECATED_MATCHER);
            }
        });
    }
}
