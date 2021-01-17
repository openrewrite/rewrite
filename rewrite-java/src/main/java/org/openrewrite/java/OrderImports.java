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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;

import java.util.*;

/**
 * This recipe will group and order the imports for a compilation unit using the rules defined by a {@link ImportLayoutStyle}.
 * If a style has not been defined, this recipe will use the default import layout style that is modelled after
 * IntelliJ's default import settings.
 *
 * The @{link {@link OrderImports#setRemoveUnused}} flag (which is defaulted to true) can be used to also remove any
 * imports that are not referenced within the compilation unit.
 */
public class OrderImports extends Recipe {
    private boolean removeUnused = true;

    public OrderImports() {
        this.processor = () -> new OrderImportsProcessor(removeUnused);
    }

    public void setRemoveUnused(boolean removeUnused) {
        this.removeUnused = removeUnused;
    }

    private static class OrderImportsProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final boolean removeUnused;

        private OrderImportsProcessor(boolean removeUnused) {
            this.removeUnused = removeUnused;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            ImportLayoutStyle layoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                    .orElse(IntelliJ.importLayout());

            List<JRightPadded<J.Import>> orderedImports = layoutStyle.orderImports(cu.getImports());

            if (orderedImports.size() != cu.getImports().size()) {
                cu = cu.withImports(orderedImports);
            }

            for (int i = 0; i < orderedImports.size(); i++) {
                if (orderedImports.get(i) != cu.getImports().get(i)) {
                    cu = cu.withImports(orderedImports);
                }
            }

            if (removeUnused) {
                doAfterVisit(new RemoveUnusedImports());
            }

            return cu;
        }
    }
}
