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
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;

import static com.netflix.rewrite.tree.Formatting.format;
import static com.netflix.rewrite.tree.Tr.randomId;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddImport extends RefactorVisitor {
    // TODO make this configurable
    OrderImports orderImports = new IntellijOrderImports();

    @EqualsAndHashCode.Include
    private final String clazz;

    @EqualsAndHashCode.Include
    @Nullable
    private final String staticMethod;

    private final boolean onlyIfReferenced;
    private final Type.Class classType;

    private boolean coveredByExistingImport;
    private boolean hasReferences;
    private Tr.CompilationUnit cu;


    public AddImport(String clazz, @Nullable String staticMethod, boolean onlyIfReferenced) {
        this.clazz = clazz;
        this.staticMethod = staticMethod;
        this.onlyIfReferenced = onlyIfReferenced;
        this.classType = Type.Class.build(clazz);
    }

    @Override
    public String getRuleName() {
        return MessageFormatter.arrayFormat( "core.AddImport{classType={},staticMethod={}}",
                new String[] { classType.getFullyQualifiedName(), staticMethod }).toString();
    }

    @Override
    public List<AstTransform> visitCompilationUnit(Tr.CompilationUnit cu) {
        this.cu = cu;
        this.hasReferences = !new FindType(clazz).visit(cu).isEmpty();
        return super.visitCompilationUnit(cu);
    }

    @Override
    public List<AstTransform> visitImport(Tr.Import impoort) {
        var importedType = impoort.getQualid().getSimpleName();

        if (staticMethod != null) {
            if (impoort.isFromType(clazz) && impoort.isStatic() && (importedType.equals(staticMethod) || importedType.equals("*"))) {
                coveredByExistingImport = true;
            }
        } else {
            if (impoort.isFromType(clazz)) {
                coveredByExistingImport = true;
            } else if (importedType.equals("*") && impoort.getQualid().getTarget().printTrimmed().equals(classType.getPackageName())) {
                coveredByExistingImport = true;
            }
        }

        return super.visitImport(impoort);
    }

    @Override
    public List<AstTransform> visitEnd() {
        if (coveredByExistingImport) {
            return emptyList();
        }

        if (onlyIfReferenced && !hasReferences) {
            return emptyList();
        }

        if (classType.getPackageName().isEmpty()) {
            return emptyList();
        }

        List<AstTransform> changes = transform(getCursor().enclosingCompilationUnit(), cu -> cu.withImports(orderImports.addImport()));

        if (cu.getClasses().size() > 0 && cu.getImports().isEmpty() ||
                cu.getClasses().get(0).getFormatting().getPrefix().chars().takeWhile(c -> c == '\n' || c == '\r').count() < 2) {
            changes.addAll(transform(cu.getClasses().get(0), clazz -> clazz.withPrefix("\n\n")));
        }

        return changes;
    }

    public abstract class OrderImports {
        public abstract List<Tr.Import> addImport();

        /**
         * @return The list of imports that could, together with the import to add, be replaced by a star import.
         */
        protected List<Tr.Import> importsThatCouldBeStarReplaced() {
            return staticMethod == null ?
                    cu.getImports().stream()
                            .filter(i -> !i.isStatic() && i.getPackageName().equals(classType.getPackageName()))
                            .collect(toList()) :
                    cu.getImports().stream()
                            .filter(i -> {
                                String fqn = i.getQualid().getTarget().printTrimmed();
                                return i.isStatic() && fqn.substring(0, Math.max(0, fqn.lastIndexOf('.'))).equals(clazz);
                            })
                            .collect(toList());
        }
    }

    @RequiredArgsConstructor
    public class IntellijOrderImports extends OrderImports {
        private final int classCountToUseStarImport;
        private final int namesCountToUseStarImport;

        public IntellijOrderImports() {
            this(5, 3);
        }

        @Override
        public List<Tr.Import> addImport() {
            List<Tr.Import> importsThatCouldBeStarReplaced = importsThatCouldBeStarReplaced();
            boolean starImporting = importsThatCouldBeStarReplaced.size() > (staticMethod == null ? classCountToUseStarImport : namesCountToUseStarImport);

            List<Tr.Import> importsWithAdded = new ArrayList<>(cu.getImports());
            if (starImporting) {
                importsWithAdded.removeAll(importsThatCouldBeStarReplaced);
            }

            Tr.FieldAccess classImportField = TreeBuilder.buildName(clazz, format(" "));
            Tr.Import importStatementToAdd;
            if (staticMethod == null) {
                importStatementToAdd = new Tr.Import(randomId(),
                        starImporting ?
                                (Tr.FieldAccess) TreeBuilder.buildName(classType.getPackageName() + ".*", format(" ")) :
                                classImportField,
                        false,
                        format("\n"));

                boolean added = false;
                for (int i = 0; i < importsWithAdded.size(); i++) {
                    Tr.Import anImport = importsWithAdded.get(i);
                    if (anImport.isStatic() || importStatementToAdd.compareTo(anImport) < 0) {
                        importsWithAdded.add(i, importStatementToAdd);
                        added = true;
                        break;
                    }
                }

                if (!added) {
                    importsWithAdded.add(importStatementToAdd);
                }

                boolean encounteredJavaImport = false;
                for (int i = 0; i < importsWithAdded.size(); i++) {
                    Tr.Import anImport = importsWithAdded.get(i);
                    if (i == 0) {
                        encounteredJavaImport = anImport.getPackageName().startsWith("java");
                        if (cu.getPackageDecl() == null) {
                            continue;
                        }
                    }
                    if (i == 0 || (!encounteredJavaImport && (encounteredJavaImport = anImport.getPackageName().startsWith("java"))) ||
                            anImport.isStatic()) {
                        importsWithAdded.set(i, anImport.withPrefix("\n\n"));
                        if (anImport.isStatic()) {
                            // don't attempt any other formatting, because we will not have modified this list
                            break;
                        }
                    } else {
                        importsWithAdded.set(i, anImport.withPrefix("\n"));
                    }
                }
            } else {
                importStatementToAdd = new Tr.Import(randomId(),
                        new Tr.FieldAccess(randomId(), classImportField,
                                Tr.Ident.build(randomId(), starImporting ? "*" : staticMethod, null, Formatting.EMPTY), null, Formatting.EMPTY),
                        true,
                        format("\n"));

                boolean added = false;
                for (int i = 0; i < importsWithAdded.size(); i++) {
                    Tr.Import anImport = importsWithAdded.get(i);
                    if (anImport.isStatic() && importStatementToAdd.compareTo(anImport) < 0) {
                        importsWithAdded.add(i, importStatementToAdd);
                        added = true;
                        break;
                    }
                }

                if (!added) {
                    importsWithAdded.add(importStatementToAdd);
                }

                boolean encounteredStatic = false;
                for (int i = 0; i < importsWithAdded.size(); i++) {
                    Tr.Import anImport = importsWithAdded.get(i);
                    if (!encounteredStatic && (encounteredStatic = anImport.isStatic())) {
                        importsWithAdded.set(i, anImport.withPrefix("\n\n"));
                    } else if (anImport.isStatic()) {
                        importsWithAdded.set(i, anImport.withPrefix("\n"));
                    }
                }
            }

            return importsWithAdded;
        }
    }
}
