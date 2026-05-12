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
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveMethodThrows extends Recipe {

    private static final String KOTLIN_THROWS_FQN = "kotlin.jvm.Throws";
    private static final String ANNOTATION_REMOVED_KEY = "kotlinThrowsAnnotationRemoved";

    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_INVOCATIONS_DESCRIPTION,
            example = "java.util.List add(..)")
    String methodPattern;

    @Option(displayName = "Match on overrides",
            description = "When enabled, match methods that are overrides of the method pattern. Default is true.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Option(displayName = "Exception type pattern",
            description = "A type pattern that is used to find matching exception to remove. Use `*` to match all.",
            example = "java.io.IOException")
    String exceptionTypePattern;

    String displayName = "Remove elements from a method declaration `throws` clause";

    String description = "Remove specific, or all exceptions from a method declaration `throws` clause.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides != null ? matchOverrides : true);
        TypeMatcher typeMatcher = new TypeMatcher(exceptionTypePattern, true);
        return Preconditions.check(new DeclaresMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                        // Close the gap if the dropped `@Throws` was the only leading annotation.
                        J.Annotation removedAnnotation = getCursor().pollMessage(ANNOTATION_REMOVED_KEY);
                        List<J.Annotation> originalAnnotations = method.getLeadingAnnotations();
                        if (removedAnnotation != null && originalAnnotations.size() == 1 &&
                                originalAnnotations.get(0) == removedAnnotation) {
                            m = collapseBlankLineLeftByRemovedAnnotation(m);
                        }
                        if (!matchesMethod(m) || m.getThrows() == null) {
                            return m;
                        }
                        return m.withThrows(ListUtils.map(m.getThrows(), nt -> {
                            if (typeMatcher.matches(nt.getType())) {
                                maybeRemoveImport(nt.getType().toString());
                                return null;
                            }
                            return nt;
                        }));
                    }

                    @Override
                    public J.@Nullable Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        J.Annotation a = super.visitAnnotation(annotation, ctx);
                        if (!TypeUtils.isOfClassType(a.getType(), KOTLIN_THROWS_FQN) || a.getArguments() == null) {
                            return a;
                        }
                        J.MethodDeclaration enclosing = getCursor().firstEnclosing(J.MethodDeclaration.class);
                        if (enclosing == null || !matchesMethod(enclosing)) {
                            return a;
                        }
                        List<Expression> originalArgs = a.getArguments();
                        List<Expression> remaining = ListUtils.map(originalArgs, arg -> {
                            if (arg instanceof J.MemberReference) {
                                JavaType referenced = ((J.MemberReference) arg).getContaining().getType();
                                if (referenced != null && typeMatcher.matches(referenced)) {
                                    maybeRemoveImport(referenced.toString());
                                    return null;
                                }
                            }
                            return arg;
                        });
                        if (remaining == null || remaining.isEmpty()) {
                            // Signal parent method for blank-line cleanup.
                            getCursor().dropParentUntil(J.MethodDeclaration.class::isInstance)
                                    .putMessage(ANNOTATION_REMOVED_KEY, annotation);
                            //noinspection DataFlowIssue
                            return null;
                        }
                        if (remaining.size() == originalArgs.size()) {
                            return a;
                        }
                        // If the first argument was removed, the new first argument carries the prefix
                        // it had after the comma (e.g. " ") — adopt the original first arg's prefix
                        // so we don't end up with `( foo`.
                        if (originalArgs.get(0) != remaining.get(0)) {
                            remaining.set(0, remaining.get(0).withPrefix(originalArgs.get(0).getPrefix()));
                        }
                        return a.withArguments(remaining);
                    }

                    @Override
                    public @Nullable JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
                        JavaType jt = super.visitType(javaType, ctx);
                        if (jt instanceof JavaType.Method && methodMatcher.matches((JavaType.Method) jt)) {
                            JavaType.Method mt = (JavaType.Method) jt;
                            return mt.withThrownExceptions(ListUtils.filter(mt.getThrownExceptions(), te -> !typeMatcher.matches(te)));
                        }
                        return jt;
                    }

                    private boolean matchesMethod(J.MethodDeclaration m) {
                        // Top-level Kotlin functions have no enclosing class; fall back to matching by
                        // the method's declaring type from its type attribution.
                        J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        return cd != null ? methodMatcher.matches(m, cd) : methodMatcher.matches(m.getMethodType());
                    }

                    /**
                     * Mirrors {@link RemoveAnnotationVisitor}'s blank-line cleanup, adapted for Kotlin:
                     * skip empty-prefix modifiers (the synthetic {@code final} sits at index 0 with no
                     * whitespace, while {@code fun} sits later with the actual line-leading whitespace),
                     * and for top-level functions also clear the method's own prefix.
                     */
                    private J.MethodDeclaration collapseBlankLineLeftByRemovedAnnotation(J.MethodDeclaration m) {
                        boolean topLevel = getCursor().firstEnclosing(J.ClassDeclaration.class) == null;
                        if (!m.getPrefix().getWhitespace().isEmpty()) {
                            m = m.withPrefix(m.getPrefix().withWhitespace(""));
                            if (!topLevel) {
                                return m;
                            }
                        }
                        for (int i = 0; i < m.getModifiers().size(); i++) {
                            J.Modifier mod = m.getModifiers().get(i);
                            if (!mod.getPrefix().getWhitespace().isEmpty()) {
                                List<J.Modifier> mods = new ArrayList<>(m.getModifiers());
                                mods.set(i, mod.withPrefix(mod.getPrefix().withWhitespace("")));
                                return m.withModifiers(mods);
                            }
                        }
                        if (m.getReturnTypeExpression() != null && !m.getReturnTypeExpression().getPrefix().getWhitespace().isEmpty()) {
                            return m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(m.getReturnTypeExpression().getPrefix().withWhitespace("")));
                        }
                        if (!m.getName().getPrefix().getWhitespace().isEmpty()) {
                            return m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace("")));
                        }
                        return m;
                    }
                }
        );
    }
}
