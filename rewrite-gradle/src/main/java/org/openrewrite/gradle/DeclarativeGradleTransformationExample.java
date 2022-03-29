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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.marker.OmitParentheses;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeclarativeGradleTransformationExample extends Recipe {

    @Option(displayName = "Old groupId",
            description = "The groupId to be changed away from.",
            example = "org.springframework.boot")
    String oldGroupId;

    @Option(displayName = "ArtifactId",
            description = "The second part of a dependency coordinate 'org.apache.logging.log4j:log4j-bom:VERSION'.",
            example = "guava")
    String artifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use.",
            example = "corp.internal.openrewrite.recipe")
    String newGroupId;

    @Option(displayName = "New version",
            description = "An exact version number.",
            example = "3.2.0")
    String newVersion;

    @Override
    public String getDisplayName() {
        return "Dependency transformation example";
    }

    @Override
    public String getDescription() {
        return "Transforms Gradle dependencies with either `bundle(..)` or `group: <groupId>` to map notation.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    protected GroovyVisitor<ExecutionContext> getVisitor() {
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, context);
                if (enclosedInDependencyBlock(getCursor()) && !m.getArguments().isEmpty()) {
                    AtomicBoolean changeArguments = new AtomicBoolean(false);
                    if (m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.MethodInvocation) {
                        J.MethodInvocation methodInvocation = (J.MethodInvocation) m.getArguments().get(0);
                        // May be simplified with a `MethodMatcher`s if more type information is provided.
                        if ("bundle".equals(methodInvocation.getSimpleName()) &&
                                methodInvocation.getArguments().size() == 3 &&
                                methodInvocation.getArguments().get(0) instanceof J.Literal &&
                                ((J.Literal) methodInvocation.getArguments().get(0)).getValue() != null &&
                                oldGroupId.equals(((J.Literal) methodInvocation.getArguments().get(0)).getValue()) &&
                                ((J.Literal) methodInvocation.getArguments().get(1)).getValue() != null &&
                                artifactId.equals(((J.Literal) methodInvocation.getArguments().get(1)).getValue())) {
                            changeArguments.set(true);
                        }
                    } else if (m.getArguments().get(0) instanceof G.MapEntry && m.getArguments().size() == 4) {
                        if (entryMatches(m.getArguments().get(0), oldGroupId) && entryMatches(m.getArguments().get(1), artifactId)) {
                            changeArguments.set(true);
                        }
                    }

                    if (changeArguments.get()) {
                        List<Expression> arguments = generateArguments();
                        m = m.withArguments(ListUtils.map(arguments,
                                        o -> o.withMarkers(o.getMarkers().addIfAbsent(new OmitParentheses(Tree.randomId())))))
                                .withMarkers(m.getMarkers().addIfAbsent(new OmitParentheses(Tree.randomId())));
                    }
                }
                return m;
            }

            private boolean enclosedInDependencyBlock(Cursor cursor) {
                if (cursor.getParent() == null) {
                    return false;
                }

                Cursor parent = cursor.dropParentUntil(is -> is instanceof J.MethodInvocation || is instanceof SourceFile);
                if (parent.getValue() instanceof SourceFile) {
                    return false;
                }

                if ("dependencies".equals(((J.MethodInvocation) parent.getValue()).getSimpleName())) {
                    return true;
                }

                return enclosedInDependencyBlock(parent);
            }

            private boolean entryMatches(Expression entry, String value) {
                return  entry instanceof G.MapEntry &&
                        ((G.MapEntry) entry).getValue() instanceof J.Literal &&
                        value.equals(((J.Literal) ((G.MapEntry) entry).getValue()).getValue());
            }

            private List<Expression> generateArguments() {
                G.MapEntry newGroupId = newMapEntry("group", getNewGroupId());
                G.MapEntry newArtifactId = newMapEntry("name", getArtifactId());
                G.MapEntry newVersion = newMapEntry("version", getNewVersion());
                return Arrays.asList(newGroupId, newArtifactId, newVersion);
            }

            private G.MapEntry newMapEntry(String key, String value) {
                return new G.MapEntry(
                        Tree.randomId(),
                        Space.build(" ", emptyList()),
                        Markers.EMPTY,
                        JRightPadded.build(new J.Literal(
                                Tree.randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                key,
                                key,
                                null,
                                JavaType.Primitive.String)),
                        new J.Literal(Tree.randomId(),
                                Space.build(" ", emptyList()),
                                Markers.EMPTY,
                                value,
                                "'" + value + "'",
                                null,
                                JavaType.Primitive.String),
                        null
                );
            }
        };
    }
}
