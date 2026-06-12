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
package org.openrewrite.gradle.gradle9;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseMatchingInsteadOfFindAll extends Recipe {

    /**
     * Container properties exposed by Gradle that are {@code DomainObjectCollection}s, for which the
     * Groovy {@code findAll(Closure)} overload is deprecated in favor of {@code matching(Spec)}.
     * Deliberately conservative: {@code subprojects}/{@code allprojects} expose a plain {@code Set<Project>}
     * (not a {@code DomainObjectCollection}), so {@code findAll} on those is ordinary Groovy and must be left alone.
     */
    private static final Set<String> DOMAIN_OBJECT_COLLECTIONS =
            new HashSet<>(Arrays.asList("tasks", "configurations", "sourceSets"));

    /**
     * Methods that return another {@code DomainObjectCollection} when chained from a container, so a
     * {@code findAll} after one of them is still operating on a {@code DomainObjectCollection}.
     */
    private static final Set<String> COLLECTION_PRESERVING_METHODS =
            new HashSet<>(Arrays.asList("withType", "matching", "named"));

    @Override
    public String getDisplayName() {
        return "Use `matching(Spec)` instead of `findAll(Closure)` on Gradle container collections";
    }

    @Override
    public String getDescription() {
        return "Gradle 9.4 deprecates the Groovy `DomainObjectCollection.findAll(Closure)` overload on " +
                "containers such as `tasks`, `configurations`, and `sourceSets`. It is replaced by the lazy " +
                "`DomainObjectCollection.matching(Spec)`, which returns a live collection that only filters " +
                "elements as they are needed by the build. This recipe rewrites `findAll { ... }` to " +
                "`matching { ... }` when the receiver is a known Gradle container collection.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                // The deprecated Groovy `findAll(Closure)` overload only exists in the Groovy DSL;
                // the Kotlin DSL uses the unrelated `Iterable.filter` extension.
                if (tree instanceof JavaSourceFile && !(tree instanceof G.CompilationUnit)) {
                    return (J) tree;
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!"findAll".equals(m.getSimpleName())) {
                    return m;
                }
                // The deprecated overload takes a single closure argument
                if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
                    return m;
                }
                if (!rootsAtDomainObjectCollection(m.getSelect())) {
                    return m;
                }
                return m.withName(m.getName().withSimpleName("matching"));
            }
        });
    }

    private static boolean rootsAtDomainObjectCollection(@Nullable Expression select) {
        if (select instanceof J.Identifier) {
            return DOMAIN_OBJECT_COLLECTIONS.contains(((J.Identifier) select).getSimpleName());
        }
        if (select instanceof J.MethodInvocation) {
            J.MethodInvocation mi = (J.MethodInvocation) select;
            if (!COLLECTION_PRESERVING_METHODS.contains(mi.getSimpleName())) {
                return false;
            }
            return rootsAtDomainObjectCollection(mi.getSelect());
        }
        if (select instanceof J.FieldAccess) {
            // e.g. `project.tasks`
            return DOMAIN_OBJECT_COLLECTIONS.contains(((J.FieldAccess) select).getSimpleName());
        }
        return false;
    }
}
