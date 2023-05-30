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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

public class MigrateMarkersSearchResult extends Recipe {
    private static final MethodMatcher WITH_MARKERS_METHOD_MATCHER = new MethodMatcher("org.openrewrite.yaml.tree.Yaml.Mapping.Entry withMarkers(org.openrewrite.marker.Markers)");
    private static final MethodMatcher SEARCH_RESULT_METHOD_MATCHER = new MethodMatcher("org.openrewrite.marker.Markers searchResult(java.lang.String)");
    private static final MethodMatcher GET_MARKERS_METHOD_MATCHER = new MethodMatcher("org.openrewrite.yaml.tree.Yaml.Mapping.Entry getMarkers()");
    private static final String MANUAL_CHANGE_COMMENT = " [Rewrite8 migration] `org.openrewrite.marker.Markers#SearchResult(..)` are deprecated and removed in rewrite 8, use `SearchResult.found()` instead." +
                                                        "please follow the migration guide here: " + MigrateRecipeToRewrite8.MIGRATION_GUIDE_URL;

    @Override
    public String getDisplayName() {
        return "Migrate deprecated `org.openrewrite.marker.Markers#SearchResult(..)`";
    }

    @Override
    public String getDescription() {
        return "Methods of `org.openrewrite.marker.Markers#SearchResult(..)` are deprecated and removed in rewrite 8, use `SearchResult.found()` instead.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("Rewrite8 migration");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                            ExecutionContext executionContext) {
                method = super.visitMethodInvocation(method, executionContext);
                if (WITH_MARKERS_METHOD_MATCHER.matches(method)) {

                    Expression select = method.getSelect();
                    if (method.getArguments().isEmpty()) {
                        return method;
                    }
                    Expression arg = method.getArguments().get(0);

                    if (arg instanceof J.MethodInvocation) {
                        J.MethodInvocation m = (J.MethodInvocation) arg;
                        if (SEARCH_RESULT_METHOD_MATCHER.matches(m.getMethodType())) {
                            Expression text = m.getArguments().get(0);

                            Expression searchResultSelect = m.getSelect();
                            if (searchResultSelect instanceof J.MethodInvocation) {
                                J.MethodInvocation maybeGetMarkers = (J.MethodInvocation) searchResultSelect;
                                if (GET_MARKERS_METHOD_MATCHER.matches(maybeGetMarkers)) {
                                    Expression s2 = maybeGetMarkers.getSelect();
                                    if (select instanceof J.Identifier && s2 instanceof J.Identifier) {
                                        J.Identifier id1 = (J.Identifier) select;
                                        J.Identifier id2 = (J.Identifier) s2;
                                        if (!id1.getSimpleName().equals(id2.getSimpleName())) {
                                            return (J.MethodInvocation) MigrateRecipeToRewrite8.commentOf(method, MANUAL_CHANGE_COMMENT);
                                        }

                                        maybeAddImport("org.openrewrite.marker.SearchResult");
                                        JavaTemplate searchResultTemplate = JavaTemplate
                                                .builder("SearchResult.found(#{any()}, #{any()})")
                                                .javaParser(JavaParser.fromJavaVersion()
                                                        .classpath(JavaParser.runtimeClasspath()))
                                                .imports("org.openrewrite.marker.SearchResult")
                                                .build();

                                        return method.withTemplate(
                                                searchResultTemplate,
                                                getCursor().getParentOrThrow(),
                                                method.getCoordinates().replace(),
                                                select,
                                                text
                                        );
                                    }
                                }
                            }

                            return (J.MethodInvocation) MigrateRecipeToRewrite8.commentOf(method, MANUAL_CHANGE_COMMENT);
                        }

                    }
                    return method;
                }
                return method;
            }
        };
    }
}
