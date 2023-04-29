/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class NestedEnumsAreNotStatic extends Recipe {
    @Override
    public String getDisplayName() {
        return "Nested enums are not static";
    }

    @Override
    public String getDescription() {
        return "Remove static modifier from nested enum types since they are implicitly static.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2786");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                if (cd.getKind() == J.ClassDeclaration.Kind.Type.Enum && cd.getType() != null && cd.getType().getOwningClass() != null) {
                    cd = SearchResult.found(cd);
                }
                return cd;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (cd.getKind() == J.ClassDeclaration.Kind.Type.Enum && cd.getType() != null && cd.getType().getOwningClass() != null) {
                    if (J.Modifier.hasModifier(cd.getModifiers(), J.Modifier.Type.Static)) {
                        J.Block enumBody = cd.getBody();
                        cd = cd.withBody(null);
                        cd = maybeAutoFormat(cd,
                                cd.withModifiers(ListUtils.map(cd.getModifiers(), mod ->
                                        mod.getType() == J.Modifier.Type.Static ? null : mod)),
                                ctx);
                        cd = cd.withBody(enumBody);
                    }
                }
                return cd;
            }
        });
    }
}
