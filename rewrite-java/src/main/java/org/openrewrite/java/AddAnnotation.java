/*
 * Copyright 2020 the original author or authors.
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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.openrewrite.Formatting.*;

public final class AddAnnotation {
    private AddAnnotation() {
    }

    public static class Scoped extends JavaRefactorVisitor {
        private final Tree scope;
        private final JavaType.Class annotationType;
        private final List<Expression> arguments;

        public Scoped(Tree scope, String annotationTypeName, Expression... arguments) {
            this.scope = scope;
            this.annotationType = JavaType.Class.build(annotationTypeName);
            this.arguments = asList(arguments);
            setCursoringOn();
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("annotation.type", annotationType.getFullyQualifiedName());
        }

        @Override
        public J visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = refactor(classDecl, super::visitClassDecl);

            if (scope.isScope(classDecl)) {
                maybeAddImport(annotationType.getFullyQualifiedName());

                if (c.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                    boolean isTopLevelClass = getCursor().getParentOrThrow().getTree() instanceof J.CompilationUnit;
                    c = c.addAnnotation(c, isTopLevelClass, annotationType, arguments, formatter);
                }
            }

            return c;
        }

        @Override
        public J visitMultiVariable(J.VariableDecls multiVariable) {
            J.VariableDecls v = refactor(multiVariable, super::visitMultiVariable);

            if (scope.isScope(multiVariable)) {
                Tree parent = getCursor().getParentOrThrow().getTree();
                boolean isMethodOrLambdaParameter = parent instanceof J.MethodDecl || parent instanceof J.Lambda;

                maybeAddImport(annotationType.getFullyQualifiedName());

                if (v.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                    List<J.Annotation> fixedAnnotations = new ArrayList<>(v.getAnnotations());

                    if (!isMethodOrLambdaParameter && multiVariable.getPrefix().chars().filter(c -> c == '\n').count() < 2) {
                        List<?> statements = enclosingBlock().getStatements();
                        for (int i = 1; i < statements.size(); i++) {
                            if (statements.get(i) == multiVariable) {
                                v = v.withPrefix("\n" + v.getPrefix());
                                break;
                            }
                        }
                    }

                    fixedAnnotations.add(buildAnnotation(EMPTY));

                    v = v.withAnnotations(fixedAnnotations);
                    if (multiVariable.getAnnotations().isEmpty()) {
                        String prefix = isMethodOrLambdaParameter ? " " : formatter.format(enclosingBlock()).getPrefix();

                        if (!v.getModifiers().isEmpty()) {
                            v = v.withModifiers(formatFirstPrefix(v.getModifiers(), prefix));
                        } else {
                            //noinspection ConstantConditions
                            v = v.withTypeExpr(v.getTypeExpr().withPrefix(prefix));
                        }
                    }
                }
            }

            return v;
        }

        @Override
        public J visitMethod(J.MethodDecl method) {
            J.MethodDecl m = refactor(method, super::visitMethod);

            if (scope.isScope(method)) {
                maybeAddImport(annotationType.getFullyQualifiedName());

                if (m.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                    m = m.addAnnotation(m, annotationType, arguments, formatter);
                }
            }

            return m;
        }

        private J.Annotation buildAnnotation(Formatting formatting) {
            return J.Annotation.buildAnnotation(formatting, annotationType, arguments);
        }
    }
}
