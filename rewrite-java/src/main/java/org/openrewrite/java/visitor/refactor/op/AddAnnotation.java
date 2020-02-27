/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor.refactor.op;

import org.openrewrite.java.tree.*;
import org.openrewrite.java.visitor.refactor.AstTransform;
import org.openrewrite.java.visitor.refactor.ScopedRefactorVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.openrewrite.java.tree.Formatting.*;
import static org.openrewrite.java.tree.J.randomId;
import static java.util.Arrays.asList;

public class AddAnnotation extends ScopedRefactorVisitor {
    private final Type.Class annotationType;
    private final List<Expression> arguments;

    public AddAnnotation(UUID scope, String annotationTypeName, Expression... arguments) {
        super(scope);
        this.annotationType = Type.Class.build(annotationTypeName);
        this.arguments = asList(arguments);
    }

    @Override
    public List<AstTransform> visitClassDecl(J.ClassDecl classDecl) {
        return maybeTransform(classDecl,
                isScope(classDecl),
                super::visitClassDecl,
                (cd, cursor) -> {
                    J.ClassDecl fixedCd = cd;

                    maybeAddImport(annotationType.getFullyQualifiedName());

                    if (fixedCd.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                        List<J.Annotation> fixedAnnotations = new ArrayList<>(fixedCd.getAnnotations());

                        Formatting annotationFormatting = cd.getModifiers().isEmpty() ?
                                (cd.getTypeParameters() == null ?
                                        cd.getKind().getFormatting() :
                                        cd.getTypeParameters().getFormatting()) :
                                format(firstPrefix(cd.getModifiers()));

                        fixedAnnotations.add(buildAnnotation(annotationFormatting));

                        fixedCd = fixedCd.withAnnotations(fixedAnnotations);
                        if (cd.getAnnotations().isEmpty()) {
                            String prefix = formatter().findIndent(0, cd).getPrefix();

                            // special case, where a top-level class is often un-indented completely
                            String cdPrefix = cd.getFormatting().getPrefix();
                            if (cursor.getParentOrThrow().getTree() instanceof J.CompilationUnit &&
                                    cdPrefix.substring(cdPrefix.lastIndexOf('\n')).chars().noneMatch(c -> c == ' ' || c == '\t')) {
                                prefix = "\n";
                            }

                            if (!fixedCd.getModifiers().isEmpty()) {
                                fixedCd = fixedCd.withModifiers(formatFirstPrefix(fixedCd.getModifiers(), prefix));
                            } else if (fixedCd.getTypeParameters() != null) {
                                fixedCd = fixedCd.withTypeParameters(fixedCd.getTypeParameters().withPrefix(prefix));
                            } else {
                                fixedCd = fixedCd.withKind(fixedCd.getKind().withPrefix(prefix));
                            }
                        }
                    }

                    return fixedCd;
                });
    }

    @Override
    public List<AstTransform> visitMultiVariable(J.VariableDecls multiVariable) {
        return maybeTransform(multiVariable,
                isScope(multiVariable),
                super::visitMultiVariable,
                (mv, cursor) -> {
                    J.VariableDecls fixedMv = mv;

                    Tree parent = cursor.getParentOrThrow().getTree();
                    boolean isMethodOrLambdaParameter = parent instanceof J.MethodDecl || parent instanceof J.Lambda;

                    maybeAddImport(annotationType.getFullyQualifiedName());

                    if (fixedMv.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                        List<J.Annotation> fixedAnnotations = new ArrayList<>(fixedMv.getAnnotations());

                        if(!isMethodOrLambdaParameter && mv.getFormatting().getPrefix().chars().filter(c -> c == '\n').count() < 2) {
                            List<?> statements = cursor.enclosingBlock().getStatements();
                            for (int i = 1; i < statements.size(); i++) {
                                if(statements.get(i) == mv) {
                                    fixedMv = fixedMv.withPrefix("\n" + fixedMv.getFormatting().getPrefix());
                                    break;
                                }
                            }
                        }

                        fixedAnnotations.add(buildAnnotation(EMPTY));

                        fixedMv = fixedMv.withAnnotations(fixedAnnotations);
                        if (mv.getAnnotations().isEmpty()) {
                            String prefix = isMethodOrLambdaParameter ? " " :
                                    formatter().format(cursor.enclosingBlock()).getPrefix();

                            if (!fixedMv.getModifiers().isEmpty()) {
                                fixedMv = fixedMv.withModifiers(formatFirstPrefix(fixedMv.getModifiers(), prefix));
                            } else {
                                //noinspection ConstantConditions
                                fixedMv = fixedMv.withTypeExpr(fixedMv.getTypeExpr().withPrefix(prefix));
                            }
                        }
                    }

                    return fixedMv;
                });
    }

    @Override
    public List<AstTransform> visitMethod(J.MethodDecl method) {
        return maybeTransform(method,
                isScope(method),
                super::visitMethod,
                (md, cursor) -> {
                    J.MethodDecl fixedMethod = md;

                    maybeAddImport(annotationType.getFullyQualifiedName());

                    if (fixedMethod.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                        List<J.Annotation> fixedAnnotations = new ArrayList<>(fixedMethod.getAnnotations());

                        fixedAnnotations.add(buildAnnotation(EMPTY));

                        fixedMethod = fixedMethod.withAnnotations(fixedAnnotations);
                        if (md.getAnnotations().isEmpty()) {
                            String prefix = formatter().findIndent(0, md).getPrefix();

                            if (!fixedMethod.getModifiers().isEmpty()) {
                                fixedMethod = fixedMethod.withModifiers(formatFirstPrefix(fixedMethod.getModifiers(), prefix));
                            } else if (fixedMethod.getTypeParameters() != null) {
                                fixedMethod = fixedMethod.withTypeParameters(fixedMethod.getTypeParameters().withPrefix(prefix));
                            } else if(fixedMethod.getReturnTypeExpr() != null) {
                                fixedMethod = fixedMethod.withReturnTypeExpr(fixedMethod.getReturnTypeExpr().withPrefix(prefix));
                            } else {
                                fixedMethod = fixedMethod.withName(fixedMethod.getName().withPrefix(prefix));
                            }
                        }
                    }

                    return fixedMethod;
                });
    }

    private J.Annotation buildAnnotation(Formatting formatting) {
        return new J.Annotation(randomId(),
                J.Ident.build(randomId(), annotationType.getClassName(), annotationType, EMPTY),
                arguments.isEmpty() ? null : new J.Annotation.Arguments(randomId(), arguments, EMPTY),
                formatting);
    }
}
