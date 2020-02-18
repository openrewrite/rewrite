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
package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.tree.*;
import com.netflix.rewrite.tree.visitor.MethodMatcher;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.netflix.rewrite.tree.Formatting.format;
import static com.netflix.rewrite.tree.Tr.randomId;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RemoveImport extends RefactorVisitor {
    @EqualsAndHashCode.Include
    private final String clazz;

    private final MethodMatcher methodMatcher;

    private final Type.Class classType;

    private Tr.Import namedImport;
    private Tr.Import starImport;
    private Tr.Import staticStarImport;

    private final Set<String> referencedTypes = new HashSet<>();
    private final Set<Tr.Ident> referencedMethods = new HashSet<>();
    private final List<Tr.Import> staticNamedImports = new ArrayList<>();

    public RemoveImport(String clazz) {
        this.clazz = clazz;
        this.methodMatcher = new MethodMatcher(clazz + " *(..)");
        this.classType = Type.Class.build(clazz);
    }

    @Override
    public String getRuleName() {
        return "core.RemoveImport{classType=" + clazz + "}";
    }

    @Override
    public List<AstTransform> visitImport(Tr.Import impoort) {
        if (impoort.isStatic()) {
            if (impoort.getQualid().getTarget().printTrimmed().equals(clazz)) {
                if ("*".equals(impoort.getQualid().getSimpleName())) {
                    staticStarImport = impoort;
                } else {
                    staticNamedImports.add(impoort);
                }
            }
        } else {
            if (impoort.getQualid().printTrimmed().equals(clazz)) {
                namedImport = impoort;
            } else if ("*".equals(impoort.getQualid().getSimpleName()) && clazz.startsWith(impoort.getQualid().getTarget().printTrimmed())) {
                starImport = impoort;
            }
        }

        return emptyList();
    }

    @Override
    public List<AstTransform> visitTypeName(NameTree name) {
        Type.Class asClass = TypeUtils.asClass(name.getType());
        if (asClass != null && asClass.getPackageName().equals(classType.getPackageName())) {
            referencedTypes.add(asClass.getFullyQualifiedName());
        }
        return super.visitTypeName(name);
    }

    @Override
    public List<AstTransform> visitMethodInvocation(Tr.MethodInvocation method) {
        if (methodMatcher.matches(method) && method.getType() != null &&
                method.getType().getDeclaringType().getFullyQualifiedName().equals(clazz)) {
            referencedMethods.add(method.getName());
        }
        return super.visitMethodInvocation(method);
    }

    @Override
    public List<AstTransform> visitEnd() {
        List<AstTransform> deletes = new ArrayList<>();
        classImportDeletions(deletes);
        staticImportDeletions(deletes);
        return deletes;
    }

    private void classImportDeletions(List<AstTransform> deletes) {
        if (namedImport != null && referencedTypes.stream().noneMatch(t -> t.equals(clazz))) {
            deletes.addAll(delete(namedImport));
        } else if (starImport != null && referencedTypes.isEmpty()) {
            deletes.addAll(delete(starImport));
        } else if (starImport != null && referencedTypes.size() == 1) {
            deletes.addAll(transform(getCursor().enclosingCompilationUnit(), cu -> cu
                    .withImports(cu.getImports().stream()
                            .map(i -> i == starImport ?
                                    new Tr.Import(randomId(), TreeBuilder.buildName(referencedTypes.iterator().next(), format(" ")), false, i.getFormatting()) :
                                    i
                            )
                            .collect(toList())
                    )
            ));
        }
    }

    private void staticImportDeletions(List<AstTransform> deletes) {
        if(staticStarImport != null && referencedMethods.isEmpty()) {
            deletes.addAll(delete(staticStarImport));
        }
        for (Tr.Import staticImport : staticNamedImports) {
            var method = staticImport.getQualid().getSimpleName();
            if(referencedMethods.stream().noneMatch(m -> m.getSimpleName().equals(method))) {
                deletes.addAll(delete(staticImport));
            }
        }
    }

    private List<AstTransform> delete(Tr.Import impoort) {
        return transform(getCursor().enclosingCompilationUnit(), cu -> cu.withImports(cu.getImports().stream()
                .filter(i -> i != impoort)
                .collect(toList())));
    }
}