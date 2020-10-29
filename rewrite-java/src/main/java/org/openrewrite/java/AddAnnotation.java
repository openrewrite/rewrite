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
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.openrewrite.Formatting.*;

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

                Formatting formatting = !c.getAnnotations().isEmpty() ?
                        c.getAnnotations().get(0).getFormatting() :
                        !c.getModifiers().isEmpty() ?
                                c.getModifiers().get(0).getFormatting() :
                                c.getKind().getFormatting();

                J.Annotation newAnnot = new J.Annotation(
                        randomUUID(),
                        J.Ident.build(
                                randomUUID(),
                                annotationType.getClassName(),
                                JavaType.buildType(annotationType.getFullyQualifiedName()),
                                Formatting.EMPTY
                        ),
                        !arguments.isEmpty() ?
                            new J.Annotation.Arguments(
                                randomUUID(),
                                arguments,
                                Formatting.EMPTY
                            ) :
                            null,
                        formatting
                );

                List<J.Annotation> annots = new ArrayList<>(c.getAnnotations());

                if (annots.stream().noneMatch(ann -> new SemanticallyEqual(newAnnot).visit(ann))) {
                    annots.add(0, newAnnot);
                    c = c.withAnnotations(annots);
                }

                maybeAddImport(annotationType.getFullyQualifiedName());
                andThen(new AutoFormat(c));
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

                Formatting formatting = !m.getAnnotations().isEmpty() ?
                        m.getAnnotations().get(0).getFormatting() :
                        !m.getModifiers().isEmpty() ?
                                m.getModifiers().get(0).getFormatting() :
                                m.getReturnTypeExpr() != null ?
                                    m.getReturnTypeExpr().getFormatting() :
                                    m.getName().getFormatting();

                J.Annotation newAnnot = new J.Annotation(
                        randomUUID(),
                        J.Ident.build(
                                randomUUID(),
                                annotationType.getClassName(),
                                JavaType.buildType(annotationType.getFullyQualifiedName()),
                                Formatting.EMPTY
                        ),
                        !arguments.isEmpty() ?
                                new J.Annotation.Arguments(
                                        randomUUID(),
                                        arguments,
                                        Formatting.EMPTY
                                ) :
                                null,
                        formatting
                );

                List<J.Annotation> annots = new ArrayList<>(m.getAnnotations());

                if (annots.stream().noneMatch(ann -> new SemanticallyEqual(newAnnot).visit(ann))) {
                    annots.add(0, newAnnot);
                    m = m.withAnnotations(annots);
                }

                maybeAddImport(annotationType.getFullyQualifiedName());
                andThen(new AutoFormat(m));
            }

            return m;
        }

        private J.Annotation buildAnnotation(Formatting formatting) {
            return J.Annotation.buildAnnotation(formatting, annotationType, arguments);
        }
    }
}
