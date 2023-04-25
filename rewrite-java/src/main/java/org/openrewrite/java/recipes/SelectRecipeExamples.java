/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.recipes;

import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Comparator.comparing;

public class SelectRecipeExamples extends Recipe {

    private static final String DOCUMENT_EXAMPLE_ANNOTATION_FQN = "org.openrewrite.internal.DocumentExample";
    private static final AnnotationMatcher TEST_ANNOTATION_MATCHER = new AnnotationMatcher("@org.junit.jupiter.api" +
                                                                                           ".Test");
    private static final AnnotationMatcher ISSUE_ANNOTATION_MATCHER = new AnnotationMatcher("@org.openrewrite.Issue");
    private static final AnnotationMatcher DISABLED_ANNOTATION_MATCHER = new AnnotationMatcher("@org.junit.jupiter" +
                                                                                               ".api.Disabled");
    private static final AnnotationMatcher DOCUMENT_EXAMPLE_ANNOTATION_MATCHER =
        new AnnotationMatcher("@" + DOCUMENT_EXAMPLE_ANNOTATION_FQN);
    private static final MethodMatcher REWRITE_RUN_METHOD_MATCHER =
        new MethodMatcher("org.openrewrite.test.RewriteTest rewriteRun(..)");
    private static final String REWRITE_TEST_FQN = "org.openrewrite.test.RewriteTest";

    @Override
    public String getDisplayName() {
        return "Automatically select recipe examples from the unit test cases of a recipe";
    }

    @Override
    public String getDescription() {
        return "Add `@DocumentExample` to the first non-issue and not a disabled unit test of a recipe as an example," +
               " if there are not any examples yet.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.not(new UsesType<>(DOCUMENT_EXAMPLE_ANNOTATION_FQN, false));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext executionContext) {
                if (classDecl.getImplements() != null && !classDecl.getImplements().isEmpty()) {
                    if (!TypeUtils.isOfClassType(classDecl.getImplements().get(0).getType(), REWRITE_TEST_FQN)) {
                        return classDecl;
                    }
                }
                return super.visitClassDeclaration(classDecl, executionContext);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                              ExecutionContext executionContext) {
                List<J.Annotation> annotations = method.getLeadingAnnotations();

                boolean isTest = annotations.stream().anyMatch(a -> TEST_ANNOTATION_MATCHER.matches(a));
                if (!isTest) {
                    return method;
                }

                boolean hasIssueOrDisabledAnnotation =
                    annotations.stream().anyMatch(a -> ISSUE_ANNOTATION_MATCHER.matches(a) ||
                                                       DISABLED_ANNOTATION_MATCHER.matches(a) ||
                                                       DOCUMENT_EXAMPLE_ANNOTATION_MATCHER.matches(a)
                    );

                if (hasIssueOrDisabledAnnotation) {
                    return method;
                }

                boolean rewriteRunMethodCalled = new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                                    AtomicBoolean called) {
                        if (REWRITE_RUN_METHOD_MATCHER.matches(method)) {
                            called.set(true);
                        }
                        return method;
                    }
                }.reduce(method, new AtomicBoolean()).get();

                if (!rewriteRunMethodCalled) {
                    return method;
                }

                JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@DocumentExample")
                    .imports(DOCUMENT_EXAMPLE_ANNOTATION_FQN)
                    .javaParser(JavaParser.fromJavaVersion()
                        .classpath(JavaParser.runtimeClasspath()))
                    .build();

                maybeAddImport(DOCUMENT_EXAMPLE_ANNOTATION_FQN);

                return method.withTemplate(
                    t,
                    method.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
            }
        };
    }
}
