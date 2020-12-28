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

import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;

import java.util.*;


/**
 * This visitor will group and order the imports for a compilation unit using the rules defined by a {@link ImportLayoutStyle}.
 * If a style has not been defined, this visitor will use the default import layout style that is modelled after
 * IntelliJ's default import settings.
 * <P><P>
 * The @{link {@link OrderImports#setRemoveUnused}} flag (which is defaulted to true) can be used to also remove any
 * imports that are not referenced within the compilation unit.
 */
public class OrderImports extends JavaIsoRefactorVisitor {

    private boolean removeUnused = true;

    public void setRemoveUnused(boolean removeUnused) {
        this.removeUnused = removeUnused;
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {

        ImportLayoutStyle layoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                .orElse(ImportLayoutStyle.getDefaultImportLayoutStyle());

        List<J.Import> orderedImports = layoutStyle.orderImports(cu.getImports());

        if (orderedImports.size() != cu.getImports().size()) {
            return cu.withImports(orderedImports);
        }

        for (int i = 0; i < orderedImports.size(); i++) {
            if (orderedImports.get(i) != cu.getImports().get(i)) {
                return cu.withImports(orderedImports);
            }
        }

        if (removeUnused) {
            andThen(new RemoveUnusedImports());
        }

        return cu;
    }
}
