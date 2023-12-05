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
package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.time.Duration;
import java.util.Comparator;

public class SetDefaultEstimatedEffortPerOccurrence extends Recipe {
    @Override
    public String getDisplayName() {
        return "Set default estimated effort";
    }

    @Override
    public String getDescription() {
        return "Retrofit recipes with a default estimated effort per occurrence.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.openrewrite.Recipe", false), new JavaIsoVisitor<ExecutionContext>() {
            final JavaTemplate addMethod = JavaTemplate.builder(
                            "@Override public Duration getEstimatedEffortPerOccurrence() {\n" +
                            "return Duration.ofMinutes(5);\n" +
                            "}")
                    .imports("java.time.Duration")
                    .build();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(classDecl.getType());
                if (TypeUtils.isAssignableTo("org.openrewrite.Recipe", type)) {
                    assert type != null;
                    for (Statement statement : classDecl.getBody().getStatements()) {
                        if (statement instanceof J.MethodDeclaration) {
                            J.MethodDeclaration method = (J.MethodDeclaration) statement;
                            if ("getEstimatedEffortPerOccurrence".equals(method.getSimpleName())) {
                                return classDecl;
                            }
                        }
                    }

                    maybeAddImport("java.time.Duration");

                    try {
                        return addMethod.apply(
                                getCursor(),
                                classDecl.getBody().getCoordinates().addMethodDeclaration(Comparator.comparing(
                                        J.MethodDeclaration::getSimpleName,
                                        new RuleBasedCollator("< getDisplayName < getDescription < getEstimatedEffortPerOccurrence < getVisitor")
                                ))
                        );
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        });
    }
}
