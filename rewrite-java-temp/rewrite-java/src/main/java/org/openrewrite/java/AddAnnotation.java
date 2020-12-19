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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.openrewrite.Formatting.firstPrefix;

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

                String prefix;
                if(c.getAnnotations().isEmpty()) {
                    if(c.getModifiers().isEmpty()) {
                        prefix = c.getKind().getPrefix();
                    } else {
                        prefix = firstPrefix(c.getModifiers());
                    }
                } else {
                    prefix = firstPrefix(c.getAnnotations());
                }

                J.Annotation newAnnot = buildAnnotation(Formatting.format(prefix));

                List<J.Annotation> annots = new ArrayList<>(c.getAnnotations());

                if (annots.stream().noneMatch(ann -> new SemanticallyEqual(newAnnot).visit(ann))) {
                    annots.add(newAnnot);
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
                Formatting formatting = !v.getAnnotations().isEmpty() ?
                        v.getAnnotations().get(0).getFormatting() :
                        !v.getModifiers().isEmpty() ?
                                v.getModifiers().get(0).getFormatting() :
                                Formatting.EMPTY;

                J.Annotation newAnnot = buildAnnotation(formatting);

                List<J.Annotation> annots = new ArrayList<>(v.getAnnotations());

                if (annots.stream().noneMatch(ann -> new SemanticallyEqual(newAnnot).visit(ann))) {
                    annots.add(newAnnot);
                    v = v.withAnnotations(annots);
                }

                List<J.Modifier> modifiers = v.getModifiers();

                if(!modifiers.isEmpty() && modifiers.get(0).getPrefix().isEmpty()) {
                    modifiers.set(0, modifiers.get(0).withPrefix(" "));
                    v = v.withModifiers(modifiers);
                } else if(v.getTypeExpr().getPrefix().isEmpty()) {
                    v = v.withTypeExpr(v.getTypeExpr().withPrefix(" "));
                }

                maybeAddImport(annotationType.getFullyQualifiedName());
                andThen(new AutoFormat(v));
            }

            return v;
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl method) {
            J.MethodDecl m = super.visitMethod(method);

            if (scope.isScope(method)) {

                String prefix;
                if(m.getAnnotations().isEmpty()) {
                    if(m.getTypeParameters() == null) {
                        if(m.getModifiers().isEmpty()) {
                            prefix = (m.getReturnTypeExpr() == null ? m.getName() : m.getReturnTypeExpr()).getPrefix();
                        } else {
                            prefix = firstPrefix(m.getModifiers());
                        }
                    }
                    else {
                        prefix = m.getTypeParameters().getPrefix();
                    }
                } else {
                    prefix = firstPrefix(m.getAnnotations());
                }

                J.Annotation newAnnot = buildAnnotation(Formatting.format(prefix));

                List<J.Annotation> annots = new ArrayList<>(m.getAnnotations());

                if (annots.stream().noneMatch(ann -> new SemanticallyEqual(newAnnot).visit(ann))) {
                    annots.add(newAnnot);
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
