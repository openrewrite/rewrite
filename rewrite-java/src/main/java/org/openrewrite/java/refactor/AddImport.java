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
package org.openrewrite.java.refactor;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Formatting;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.FindType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TreeBuilder;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class AddImport extends JavaRefactorVisitor {
    // TODO make this configurable
    OrderImports orderImports = new IntellijOrderImports();

    @EqualsAndHashCode.Include
    private final String clazz;

    @EqualsAndHashCode.Include
    @Nullable
    private final String staticMethod;

    @EqualsAndHashCode.Include
    private final boolean onlyIfReferenced;

    private final JavaType.Class classType;

    private boolean coveredByExistingImport;

    public AddImport(String clazz, @Nullable String staticMethod, boolean onlyIfReferenced) {
        this.clazz = clazz;
        this.staticMethod = staticMethod;
        this.onlyIfReferenced = onlyIfReferenced;
        this.classType = JavaType.Class.build(clazz);
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("class", clazz, "static.method", staticMethod == null ? "none" : staticMethod);
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu) {
        coveredByExistingImport = false;
        boolean hasReferences = !new FindType(clazz).visit(cu).isEmpty();

        if (onlyIfReferenced && !hasReferences) {
            return cu;
        }

        cu = refactor(cu, super::visitCompilationUnit);

        if (coveredByExistingImport) {
            return cu;
        }

        if (classType.getPackageName().isEmpty()) {
            return cu;
        }

        cu = cu.withImports(orderImports.addImport(cu));

        if (cu.getClasses().size() > 0 && cu.getImports().isEmpty() ||
                cu.getClasses().get(0).getFormatting().getPrefix().chars().takeWhile(c -> c == '\n' || c == '\r').count() < 2) {
            List<J.ClassDecl> classes = new ArrayList<>(cu.getClasses());
            classes.set(0, classes.get(0).withPrefix("\n\n"));
            cu = cu.withClasses(classes);
        }

        return cu;
    }

    @Override
    public J visitImport(J.Import impoort) {
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

    public abstract class OrderImports {
        public abstract List<J.Import> addImport(J.CompilationUnit cu);

        /**
         * @return The list of imports that could, together with the import to add, be replaced by a star import.
         */
        protected List<J.Import> importsThatCouldBeStarReplaced(J.CompilationUnit cu) {
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
        public List<J.Import> addImport(J.CompilationUnit cu) {
            List<J.Import> importsThatCouldBeStarReplaced = importsThatCouldBeStarReplaced(cu);
            boolean starImporting = importsThatCouldBeStarReplaced.size() > (staticMethod == null ? classCountToUseStarImport : namesCountToUseStarImport);

            List<J.Import> importsWithAdded = new ArrayList<>(cu.getImports());
            if (starImporting) {
                importsWithAdded.removeAll(importsThatCouldBeStarReplaced);
            }

            J.FieldAccess classImportField = TreeBuilder.buildName(clazz, Formatting.format(" "));
            J.Import importStatementToAdd;
            if (staticMethod == null) {
                importStatementToAdd = new J.Import(randomId(),
                        starImporting ?
                                (J.FieldAccess) TreeBuilder.buildName(classType.getPackageName() + ".*", Formatting.format(" ")) :
                                classImportField,
                        false,
                        Formatting.format("\n"));

                boolean added = false;
                for (int i = 0; i < importsWithAdded.size(); i++) {
                    J.Import anImport = importsWithAdded.get(i);
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
                    J.Import anImport = importsWithAdded.get(i);
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
                importStatementToAdd = new J.Import(randomId(),
                        new J.FieldAccess(randomId(), classImportField,
                                J.Ident.build(randomId(), starImporting ? "*" : staticMethod, null, Formatting.EMPTY), null, Formatting.EMPTY),
                        true,
                        Formatting.format("\n"));

                boolean added = false;
                for (int i = 0; i < importsWithAdded.size(); i++) {
                    J.Import anImport = importsWithAdded.get(i);
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
                    J.Import anImport = importsWithAdded.get(i);
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
