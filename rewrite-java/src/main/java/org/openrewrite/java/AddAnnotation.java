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
import static org.openrewrite.Tree.randomId;

public final class AddAnnotation {
    private AddAnnotation() {
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
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
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = super.visitClassDecl(classDecl);

            if (scope.isScope(classDecl)) {
                maybeAddImport(annotationType.getFullyQualifiedName());

                if (c.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                    List<J.Annotation> fixedAnnotations = new ArrayList<>(c.getAnnotations());

                    Formatting annotationFormatting = classDecl.getModifiers().isEmpty() ?
                            (classDecl.getTypeParameters() == null ?
                                    classDecl.getKind().getFormatting() :
                                    classDecl.getTypeParameters().getFormatting()) :
                            format(firstPrefix(classDecl.getModifiers()));

                    fixedAnnotations.add(buildAnnotation(annotationFormatting));

                    c = c.withAnnotations(fixedAnnotations);
                    if (classDecl.getAnnotations().isEmpty()) {
                        String prefix = formatter.findIndent(0, c).getPrefix();

                        // special case, where a top-level class is often un-indented completely
                        String cdPrefix = c.getPrefix();
                        if (getCursor().getParentOrThrow().getTree() instanceof J.CompilationUnit &&
                                cdPrefix.substring(Math.max(cdPrefix.lastIndexOf('\n'), 0)).chars().noneMatch(p -> p == ' ' || p == '\t')) {
                            prefix = "\n";
                        }

                        if (!c.getModifiers().isEmpty()) {
                            c = c.withModifiers(formatFirstPrefix(c.getModifiers(), prefix));
                        } else if (c.getTypeParameters() != null) {
                            c = c.withTypeParameters(c.getTypeParameters().withPrefix(prefix));
                        } else {
                            c = c.withKind(c.getKind().withPrefix(prefix));
                        }
                    }
                }
            }

            return c;
        }

        @Override
        public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable) {
            J.VariableDecls v = super.visitMultiVariable(multiVariable);

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
        public J.MethodDecl visitMethod(J.MethodDecl method) {
            J.MethodDecl m = super.visitMethod(method);

            if (scope.isScope(method)) {
                maybeAddImport(annotationType.getFullyQualifiedName());

                if (m.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                    List<J.Annotation> fixedAnnotations = new ArrayList<>(m.getAnnotations());

                    fixedAnnotations.add(buildAnnotation(EMPTY));

                    m = m.withAnnotations(fixedAnnotations);
                    if (method.getAnnotations().isEmpty()) {
                        String prefix = formatter.findIndent(0, method).getPrefix();

                        if (!m.getModifiers().isEmpty()) {
                            m = m.withModifiers(formatFirstPrefix(m.getModifiers(), prefix));
                        } else if (m.getTypeParameters() != null) {
                            m = m.withTypeParameters(m.getTypeParameters().withPrefix(prefix));
                        } else if (m.getReturnTypeExpr() != null) {
                            m = m.withReturnTypeExpr(m.getReturnTypeExpr().withPrefix(prefix));
                        } else {
                            m = m.withName(m.getName().withPrefix(prefix));
                        }
                    }
                }
            }

            return m;
        }

        private J.Annotation buildAnnotation(Formatting formatting) {
            return new J.Annotation(randomId(),
                    J.Ident.build(randomId(), annotationType.getClassName(), annotationType, EMPTY),
                    arguments.isEmpty() ? null : new J.Annotation.Arguments(randomId(), arguments, EMPTY),
                    formatting);
        }
    }
}
