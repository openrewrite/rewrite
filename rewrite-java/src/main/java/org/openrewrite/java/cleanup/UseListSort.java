/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class UseListSort extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace invocations of `Collections#sort(List, Comparator)` with `List#sort(Comparator)`";
    }

    @Override
    public String getDescription() {
        return "The `java.util.Collections#sort(..)` implementation defers to the `java.util.List#sort(Comparator)`, replaced it with the `java.util.List#sort(Comparator)` implementation for better readability.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final MethodMatcher collectionsSort = new MethodMatcher("java.util.Collections sort(..)");
        return Preconditions.check(new UsesMethod<>(collectionsSort), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                if (collectionsSort.matches(mi)) {
                    maybeRemoveImport("java.util.Collections");
                    if (mi.getArguments().size() == 1) {
                        return mi.withTemplate(JavaTemplate.builder(() -> getCursor().getParent(), "#{any(java.util.List)}.sort(null)")
                                        .imports("java.util.List").build(),
                                mi.getCoordinates().replace(), mi.getArguments().get(0));
                    } else {
                        return mi.withTemplate(JavaTemplate.builder(() -> getCursor().getParent(), "#{any(java.util.List)}.sort(#{any(java.util.Comparator)})")
                                        .imports("java.util.List", "java.util.Comparator").build(),
                                mi.getCoordinates().replace(), mi.getArguments().get(0), mi.getArguments().get(1));
                    }
                }
                return mi;
            }
        });
    }
}
