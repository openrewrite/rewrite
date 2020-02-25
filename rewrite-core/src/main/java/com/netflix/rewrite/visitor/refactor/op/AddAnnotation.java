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
package com.netflix.rewrite.visitor.refactor.op;

import com.netflix.rewrite.tree.*;
import com.netflix.rewrite.visitor.refactor.AstTransform;
import com.netflix.rewrite.visitor.refactor.ScopedRefactorVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.netflix.rewrite.tree.Formatting.*;
import static com.netflix.rewrite.tree.Formatting.formatFirstPrefix;
import static com.netflix.rewrite.tree.Tr.randomId;

public class AddAnnotation extends ScopedRefactorVisitor {
    private final Type.Class annotationType;

    public AddAnnotation(UUID scope, String annotationTypeName) {
        super(scope);
        this.annotationType = Type.Class.build(annotationTypeName);
    }

    @Override
    public List<AstTransform> visitClassDecl(Tr.ClassDecl classDecl) {
        return maybeTransform(classDecl,
                isScope(classDecl),
                super::visitClassDecl,
                (cd, cursor) -> {
                    Tr.ClassDecl fixedCd = cd;

                    maybeAddImport(annotationType.getFullyQualifiedName());

                    if (fixedCd.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                        List<Tr.Annotation> fixedAnnotations = new ArrayList<>(fixedCd.getAnnotations());

                        Formatting annotationFormatting = cd.getModifiers().isEmpty() ?
                                (cd.getTypeParams() == null ?
                                        cd.getKind().getFormatting() :
                                        cd.getTypeParams().getFormatting()) :
                                format(firstPrefix(cd.getModifiers()));

                        fixedAnnotations.add(new Tr.Annotation(randomId(),
                                Tr.Ident.build(randomId(), annotationType.getClassName(), annotationType, EMPTY),
                                null,
                                annotationFormatting)
                        );

                        fixedCd = fixedCd.withAnnotations(fixedAnnotations);
                        if (cd.getAnnotations().isEmpty()) {
                            String prefix = formatter().findIndent(0, cd).getPrefix();

                            // special case, where a top-level class is often un-indented completely
                            String cdPrefix = cd.getFormatting().getPrefix();
                            if (cursor.getParentOrThrow().getTree() instanceof Tr.CompilationUnit &&
                                    cdPrefix.substring(cdPrefix.lastIndexOf('\n')).chars().noneMatch(c -> c == ' ' || c == '\t')) {
                                prefix = "\n";
                            }

                            if (!fixedCd.getModifiers().isEmpty()) {
                                fixedCd = fixedCd.withModifiers(formatFirstPrefix(fixedCd.getModifiers(), prefix));
                            } else if (fixedCd.getTypeParams() != null) {
                                fixedCd = fixedCd.withTypeParams(fixedCd.getTypeParams().withPrefix(prefix));
                            } else {
                                fixedCd = fixedCd.withKind(fixedCd.getKind().withPrefix(prefix));
                            }
                        }
                    }

                    return fixedCd;
                });
    }

    @Override
    public List<AstTransform> visitMultiVariable(Tr.VariableDecls multiVariable) {
        return maybeTransform(multiVariable,
                isScope(multiVariable),
                super::visitMultiVariable,
                (mv, cursor) -> {
                    Tr.VariableDecls fixedMv = mv;

                    maybeAddImport(annotationType.getFullyQualifiedName());

                    if (fixedMv.getAnnotations().stream().noneMatch(ann -> TypeUtils.isOfClassType(ann.getType(), annotationType.getFullyQualifiedName()))) {
                        List<Tr.Annotation> fixedAnnotations = new ArrayList<>(fixedMv.getAnnotations());

                        if(mv.getFormatting().getPrefix().chars().filter(c -> c == '\n').count() < 2) {
                            List<?> statements = cursor.enclosingBlock().getStatements();
                            for (int i = 1; i < statements.size(); i++) {
                                if(statements.get(i) == mv) {
                                    fixedMv = fixedMv.withPrefix("\n" + fixedMv.getFormatting().getPrefix());
                                    break;
                                }
                            }
                        }

                        fixedAnnotations.add(new Tr.Annotation(randomId(),
                                Tr.Ident.build(randomId(), annotationType.getClassName(), annotationType, EMPTY),
                                null,
                                EMPTY)
                        );

                        fixedMv = fixedMv.withAnnotations(fixedAnnotations);
                        if (mv.getAnnotations().isEmpty()) {
                            String prefix = formatter().format(cursor.enclosingBlock()).getPrefix();

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
}
