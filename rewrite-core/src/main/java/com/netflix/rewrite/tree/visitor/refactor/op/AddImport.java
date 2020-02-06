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

import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.TreeBuilder;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import com.netflix.rewrite.tree.visitor.search.FindType;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.netflix.rewrite.tree.Tr.randomId;
import static java.util.Collections.emptyList;

public class AddImport extends RefactorVisitor<Tr.CompilationUnit> {
    static final Comparator<String> packageComparator = (p1, p2) -> {
        var p1s = p1.split("\\.");
        var p2s = p2.split("\\.");

        for (int i = 0; i < p1s.length; i++) {
            String s = p1s[i];
            if (p2s.length < i + 1) {
                return 1;
            }
            if (!s.equals(p2s[i])) {
                return s.compareTo(p2s[i]);
            }
        }

        return p1s.length < p2.length() ? -1 : 0;
    };

    String clazz;

    @Nullable
    String staticMethod;

    boolean onlyIfReferenced;

    @NonFinal
    boolean coveredByExistingImport;

    @NonFinal
    boolean hasReferences;

    @NonFinal
    Tr.CompilationUnit cu;

    @NonFinal
    final
    Type.Class classType;

    public AddImport(String clazz, @Nullable String staticMethod, boolean onlyIfReferenced) {
        this.clazz = clazz;
        this.staticMethod = staticMethod;
        this.onlyIfReferenced = onlyIfReferenced;
        this.classType = Type.Class.build(clazz);
    }

    @Override
    protected String getRuleName() {
        return "add-import";
    }

    @Override
    public List<AstTransform<Tr.CompilationUnit>> visitCompilationUnit(Tr.CompilationUnit cu) {
        this.cu = cu;
        this.hasReferences = !new FindType(clazz).visit(cu).isEmpty();
        return super.visitCompilationUnit(cu);
    }

    @Override
    public List<AstTransform<Tr.CompilationUnit>> visitImport(Tr.Import impoort) {
        var importedType = impoort.getQualid().getSimpleName();

        if (staticMethod != null) {
            if (impoort.matches(clazz) && impoort.isStatic() && (importedType.equals(staticMethod) || importedType.equals("*"))) {
                coveredByExistingImport = true;
            }
        } else {
            if (impoort.matches(clazz)) {
                coveredByExistingImport = true;
            } else if (importedType.equals("*") && impoort.getQualid().getTarget().printTrimmed().equals(classType.getPackageName())) {
                coveredByExistingImport = true;
            }
        }

        return super.visitImport(impoort);
    }

    @Override
    public List<AstTransform<Tr.CompilationUnit>> visitEnd() {
        if (coveredByExistingImport) {
            return emptyList();
        }

        if (onlyIfReferenced && !hasReferences) {
            return emptyList();
        }

        if (classType.getPackageName().isEmpty()) {
            return emptyList();
        }

        var lastPrior = lastPriorImport();
        Tr.FieldAccess classImportField = (Tr.FieldAccess) TreeBuilder.buildName(clazz, Formatting.format(" "));

        var importStatementToAdd = staticMethod == null ?
                new Tr.Import(randomId(), classImportField, false, Formatting.INFER) :
                new Tr.Import(randomId(), new Tr.FieldAccess(randomId(), classImportField, Tr.Ident.build(randomId(), staticMethod, null, Formatting.EMPTY), null, Formatting.EMPTY), true, Formatting.INFER);

        return lastPrior == null ?
                transform(cu -> {
                    List<Tr.Import> imports = new ArrayList<>(cu.getImports().size() + 1);
                    imports.add(importStatementToAdd);
                    imports.addAll(cu.getImports());
                    return cu.withImports(imports);
                }) :
                transform(cu -> {
                    List<Tr.Import> imports = new ArrayList<>(cu.getImports().size() + 1);
                    for (Tr.Import im : cu.getImports()) {
                        imports.add(im);
                        if (im == lastPrior) {
                            imports.add(importStatementToAdd);
                        }
                    }
                    return cu.withImports(imports);
                });
    }

    @Nullable
    private Tr.Import lastPriorImport() {
        return cu.getImports().stream()
                .filter(im -> {
                    // static imports go after all non-static imports
                    if (staticMethod != null && !im.isStatic()) {
                        return true;
                    }

                    // non-static imports should always go before static imports
                    if (staticMethod == null && im.isStatic()) {
                        return false;
                    }

                    int comp = packageComparator.compare(im.getQualid().getTarget().printTrimmed(),
                            staticMethod != null ? clazz : classType.getPackageName());
                    return comp == 0 ?
                            im.getQualid().getSimpleName().compareTo(staticMethod != null ? staticMethod : classType.getClassName()) < 0 :
                            comp < 0;
                })
                .reduce((import1, import2) -> import2)
                .orElse(null);
    }
}
