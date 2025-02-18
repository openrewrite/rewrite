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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This recipe will remove any imports for types that are not referenced within the compilation unit. This recipe
 * is aware of the import layout style and will correctly handle unfolding of wildcard imports if the import counts
 * drop below the configured values.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRedundantImports extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove redundant imports";
    }

    @Override
    public String getDescription() {
        return "Remove imports that are redundant due to multiple imports of the same class or unnecessary wildcard " +
                "imports. Counterpart fix for: https://checkstyle.sourceforge.io/checks/imports/redundantimport.html.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new NoMissingTypes(), new RemoveRedundantImportsVisitor());
    }

    private static class RemoveRedundantImportsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            Set<J.Import> uniqueImports = new LinkedHashSet<>(cu.getImports());
            if (cu.getImports().size() != uniqueImports.size()) {
                return cu.withImports(new ArrayList<>(uniqueImports));
            }
            return cu;
        }
    }

}
