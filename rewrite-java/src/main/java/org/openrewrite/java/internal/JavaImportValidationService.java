/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ImportValidationService;
import org.openrewrite.java.NoMissingTypes;
import org.openrewrite.java.internal.template.UndeclaredTemplateReferences;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.J;

import java.util.*;

import static java.util.Collections.emptyList;

public class JavaImportValidationService implements ImportValidationService {

    @Override
    public List<String> unimported(SourceFile sourceFile) {
        // Kotlin and Groovy import more by default, and alias imports
        return sourceFile instanceof J.CompilationUnit ?
                UnimportedReferences.of((J.CompilationUnit) sourceFile) : emptyList();
    }

    @Override
    public List<String> newlyUnusedImports(@Nullable SourceFile before, SourceFile after) {
        List<J.Import> unused = after instanceof J.CompilationUnit ? unused((J.CompilationUnit) after) : null;
        if (unused == null || unused.isEmpty()) {
            return emptyList();
        }
        J.CompilationUnit beforeCu = before instanceof J.CompilationUnit ? (J.CompilationUnit) before : null;
        List<J.Import> unusedBefore = beforeCu == null ? emptyList() : unused(beforeCu);
        List<String> newlyUnused = new ArrayList<>();
        for (J.Import anImport : unused) {
            boolean existed = beforeCu != null && containsSame(beforeCu.getImports(), anImport);
            if (!existed || unusedBefore != null && !containsSame(unusedBefore, anImport)) {
                newlyUnused.add(name(anImport));
            }
        }
        return newlyUnused;
    }

    @Override
    public Recording recordUndeclared() {
        return UndeclaredTemplateReferences.record();
    }

    /**
     * @return The imports {@link ImportService#removeUnusedImportsVisitor()} drops, other than those it folds into a
     * star import or unfolds from one, or null when the compilation unit's missing types keep it from deciding.
     */
    private static @Nullable List<J.Import> unused(J.CompilationUnit cu) {
        if (cu.getImports().isEmpty()) {
            return emptyList();
        }
        ExecutionContext ctx = new InMemoryExecutionContext();
        if (new NoMissingTypes().visit(cu, ctx) == cu) {
            return null;
        }
        J.CompilationUnit cleaned = (J.CompilationUnit) cu.service(ImportService.class)
                .removeUnusedImportsVisitor().visitNonNull(cu, ctx);
        Set<UUID> keptIds = new HashSet<>();
        Set<String> keptNames = new HashSet<>();
        for (J.Import kept : cleaned.getImports()) {
            keptIds.add(kept.getId());
            keptNames.add(name(kept));
        }
        List<J.Import> unused = new ArrayList<>();
        for (J.Import anImport : cu.getImports()) {
            if (!keptIds.contains(anImport.getId()) && !stillImported(name(anImport), keptNames)) {
                unused.add(anImport);
            }
        }
        return unused;
    }

    private static boolean stillImported(String name, Set<String> keptNames) {
        String owner = name.substring(0, name.lastIndexOf('.'));
        if (name.endsWith(".*")) {
            for (String kept : keptNames) {
                if (kept.startsWith(owner + ".") && kept.lastIndexOf('.') == owner.length()) {
                    return true;
                }
            }
            return false;
        }
        return keptNames.contains(owner + ".*");
    }

    /**
     * A recipe that renames an import keeps its id, and one that rebuilds it keeps what it imports.
     */
    private static boolean containsSame(List<J.Import> imports, J.Import anImport) {
        String name = name(anImport);
        for (J.Import i : imports) {
            if (i.getId().equals(anImport.getId()) || name(i).equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String name(J.Import anImport) {
        return "import " + (anImport.isStatic() ? "static " : "") + anImport.getQualid().printTrimmed();
    }
}
