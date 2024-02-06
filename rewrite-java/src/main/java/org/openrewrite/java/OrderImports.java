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
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.FormatFirstClassPrefix;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

/**
 * This recipe will group and order the imports for a compilation unit using the rules defined by an {@link ImportLayoutStyle}.
 * If a style has not been defined, this recipe will use the default import layout style that is modelled after
 * IntelliJ's default import settings.
 * <p>
 * The @{link {@link OrderImports#removeUnused}} flag (which is defaulted to true) can be used to also remove any
 * imports that are not referenced within the compilation unit.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class OrderImports extends Recipe {

    @Option(displayName = "Remove unused",
            description = "Remove unnecessary imports.",
            required = false)
    @Nullable
    Boolean removeUnused;

    @Override
    public String getDisplayName() {
        return "Order imports";
    }

    @Override
    public String getDescription() {
        return "Groups and orders import statements. If a [style has been defined](https://docs.openrewrite.org/concepts-explanations/styles), this recipe will order the imports " +
                "according to that style. If no style is detected, this recipe will default to ordering imports in " +
                "the same way that IntelliJ IDEA does.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                ImportLayoutStyle layoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                        .orElse(IntelliJ.importLayout());

                Optional<JavaSourceSet> sourceSet = cu.getMarkers().findFirst(JavaSourceSet.class);
                List<JavaType.FullyQualified> classpath = emptyList();
                if (sourceSet.isPresent()) {
                    classpath = sourceSet.get().getClasspath();
                }

                List<JRightPadded<J.Import>> orderedImports = layoutStyle.orderImports(cu.getPadding().getImports(), classpath);

                boolean changed = false;
                if (orderedImports.size() != cu.getImports().size()) {
                    cu = cu.getPadding().withImports(orderedImports);
                    changed = true;
                } else {
                    for (int i = 0; i < orderedImports.size(); i++) {
                        if (orderedImports.get(i) != cu.getPadding().getImports().get(i)) {
                            cu = cu.getPadding().withImports(orderedImports);
                            changed = true;
                            break;
                        }
                    }
                }

                if (Boolean.TRUE.equals(removeUnused)) {
                    doAfterVisit(new RemoveUnusedImports().getVisitor());
                } else if (changed) {
                    doAfterVisit(new FormatFirstClassPrefix<>());
                }

                return cu;
            }
        };
    }
}
