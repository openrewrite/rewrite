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
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UnnecessaryPrimitiveAnnotations extends Recipe {
    private static final AnnotationMatcher CHECK_FOR_NULL_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.annotation.CheckForNull");
    private static final AnnotationMatcher NULLABLE_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.annotation.Nullable");

    @Override
    public String getDisplayName() {
        return "Remove Nullable and CheckForNull annotations from primitives";
    }

    @Override
    public String getDescription() {
        return "Remove `@Nullable` and `@CheckForNull` annotations from primitives since they can't be null.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-4682");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesType<>("javax.annotation.CheckForNull", false), new UsesType<>("javax.annotation.Nullable", false)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
                        if (md.getReturnTypeExpression() != null
                            && !(md.getReturnTypeExpression() instanceof J.ArrayType)
                            && md.getReturnTypeExpression().getType() instanceof JavaType.Primitive) {
                            md = maybeAutoFormat(md, md.withLeadingAnnotations(filterAnnotations(md.getLeadingAnnotations())), executionContext);
                        }
                        return md;
                    }

                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                        J.VariableDeclarations varDecls = super.visitVariableDeclarations(multiVariable, executionContext);
                        if (varDecls.getType() instanceof JavaType.Primitive && varDecls.getVariables().stream().noneMatch(nv -> nv.getType() instanceof JavaType.Array)) {
                            varDecls = varDecls.withLeadingAnnotations(filterAnnotations(varDecls.getLeadingAnnotations()));
                        }
                        return varDecls;
                    }

                    private List<J.Annotation> filterAnnotations(List<J.Annotation> annotations) {
                        return ListUtils.map(annotations, anno -> {
                            if (NULLABLE_ANNOTATION_MATCHER.matches(anno) || CHECK_FOR_NULL_ANNOTATION_MATCHER.matches(anno)) {
                                maybeRemoveImport("javax.annotation.CheckForNull");
                                maybeRemoveImport("javax.annotation.Nullable");
                                return null;
                            }
                            return anno;
                        });
                    }
                }
        );
    }
}
