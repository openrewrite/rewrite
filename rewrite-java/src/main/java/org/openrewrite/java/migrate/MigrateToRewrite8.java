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
package org.openrewrite.java.migrate;

import io.micrometer.core.instrument.search.Search;
import lombok.Value;
import lombok.With;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.J.Modifier.Type.Protected;
import static org.openrewrite.java.tree.J.Modifier.Type.Public;

public class MigrateToRewrite8 extends Recipe {

    private static final String REWRITE_RECIPE_FQN = "org.openrewrite.Recipe";
    private static final MethodMatcher GET_SINGLE_SOURCE_APPLICABLE_TEST_METHOD_MATCHER = new MethodMatcher("org.openrewrite.Recipe getSingleSourceApplicableTest()", true);
    private static final MethodMatcher GET_VISITOR_METHOD_MATCHER = new MethodMatcher("org.openrewrite.Recipe getVisitor()", true);
    private static final MethodMatcher VISIT_METHOD_MATCHER = new MethodMatcher("org.openrewrite.Recipe visit(..)", true);
    private static final AnnotationMatcher OVERRIDE_ANNOTATION_MATCHER = new AnnotationMatcher("@java.lang.Override");

    @Override
    public String getDisplayName() {
        return "Migrate Rewrite recipes from version 7 to 8";
    }

    @Override
    public String getDescription() {
        return "Rewrite Recipe Migration to version 8. While most parts can be automatically migrated, there are some" +
               " complex and open-ended scenarios that require manual attention. In those cases, this recipe will add" +
               " a comment to the code and request a human to review and handle it manually.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            boolean hasSingleSourceApplicableTest = false;
            List<Statement> singleSourceApplicableTestMethodStatements = new ArrayList<>();

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext ctx) {
                if (classDecl.getExtends() != null) {
                    if (!TypeUtils.isOfClassType(classDecl.getExtends().getType(), REWRITE_RECIPE_FQN)) {
                        return classDecl;
                    }
                }

                // find `getSingleSourceApplicableTest` method
                J.MethodDeclaration singleSourceApplicableTestMethod = findSingleSourceApplicableTest(classDecl);
                if (singleSourceApplicableTestMethod != null) {
                    hasSingleSourceApplicableTest = true;
                    List<Statement> statements = singleSourceApplicableTestMethod.getBody().getStatements();
                    singleSourceApplicableTestMethodStatements.addAll(statements);
                }

                // find `visit` method
                J.MethodDeclaration visitMethod = findVisitMethod(classDecl);
                if (visitMethod != null) {
                    return SearchResult.found(classDecl, "This recipe overrides the `visit(List<SourceFile> before, *)` method, which need to be manually migrated.");
                }

                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                              ExecutionContext ctx) {

                if (GET_SINGLE_SOURCE_APPLICABLE_TEST_METHOD_MATCHER.matches(method.getMethodType())) {
                    return null;
                }

                if (GET_VISITOR_METHOD_MATCHER.matches(method.getMethodType())) {
                    if (MigratedTo8.hasMarker(method)) {
                        return method;
                    }

                    // make the `Recipe#getVisitor()` methods public
                    if (J.Modifier.hasModifier(method.getModifiers(), Protected)) {
                        method = method.withModifiers(ListUtils.map(method.getModifiers(), mod ->
                            mod.getType() == Protected ? mod.withType(Public) : mod));
                    }

                    if (hasSingleSourceApplicableTest && !singleSourceApplicableTestMethodStatements.isEmpty()) {

                        // merge statements
                        List<Statement> statements = method.getBody().getStatements();

                        List<Statement> mergedStatements = new ArrayList<>();

                        maybeAddImport("org.openrewrite.Preconditions", false);
                        //


                        JavaTemplate preconditionsCheckTemplate = JavaTemplate.builder(this::getCursor,
                                "return Preconditions.check(#{any()}, #{any()});")
                            .build();

                        Statement lastStatement = statements.get(statements.size() - 1);
                        Statement theSingleSourceApplicableTest = singleSourceApplicableTestMethodStatements.get(singleSourceApplicableTestMethodStatements.size() - 1);

                        lastStatement = lastStatement.withTemplate(
                            preconditionsCheckTemplate, lastStatement.getCoordinates().replace(),
                            ((J.Return) theSingleSourceApplicableTest).getExpression(),
                            ((J.Return) lastStatement).getExpression()

                        );

                        mergedStatements.add(lastStatement);
                        return MigratedTo8.withMarker( autoFormat(method.withBody(method.getBody().withStatements(mergedStatements)), ctx));
                    }


                    return method;
                }


                return super.visitMethodDeclaration(method, ctx);
            }
        };
    }

    private static J.MethodDeclaration findSingleSourceApplicableTest(J.ClassDeclaration classDecl) {
        return classDecl.getBody()
            .getStatements()
            .stream()
            .filter(statement -> statement instanceof J.MethodDeclaration)
            .map(J.MethodDeclaration.class::cast)
            // .filter(m -> m.getName().getSimpleName().equals("getSingleSourceApplicableTest"))
            .filter(m -> GET_SINGLE_SOURCE_APPLICABLE_TEST_METHOD_MATCHER.matches(m.getMethodType()))
            .findFirst()
            .orElse(null);
    }

    private static J.MethodDeclaration findVisitMethod(J.ClassDeclaration classDecl) {
        return classDecl.getBody()
            .getStatements()
            .stream()
            .filter(statement -> statement instanceof J.MethodDeclaration)
            .map(J.MethodDeclaration.class::cast)
            .filter(m -> VISIT_METHOD_MATCHER.matches(m.getMethodType()))
            .filter(m -> m.getLeadingAnnotations().stream().anyMatch(OVERRIDE_ANNOTATION_MATCHER::matches))
            .findFirst()
            .orElse(null);
    }

    @Value
    @With
    private static class MigratedTo8 implements Marker {
        UUID id;
        static <J2 extends J> J2 withMarker(J2 j) {
            return j.withMarkers(j.getMarkers().addIfAbsent(new MigratedTo8(randomId())));
        }
        static <J2 extends J> J2 removeMarker(J2 j) {
            return j.withMarkers(j.getMarkers().removeByType(MigratedTo8.class));
        }
        static boolean hasMarker(J j) {
            return j.getMarkers().findFirst(MigratedTo8.class).isPresent();
        }
    }


}
