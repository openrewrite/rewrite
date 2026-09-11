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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Collections.singletonList;

public class UseRepositoryHandlerActionOverloads extends Recipe {

    @Getter
    final String displayName = "Use the `Action` overloads of `flatDir` and `mavenCentral`";

    @Getter
    final String description = "Gradle 9.6 deprecates `RepositoryHandler.flatDir(Map)` and " +
            "`RepositoryHandler.mavenCentral(Map)` in favor of the `Action` overloads that configure the repository " +
            "through its own API. This recipe rewrites `flatDir dirs: 'libs'` to `flatDir { dirs 'libs' }` and " +
            "`mavenCentral name: 'central2'` to `mavenCentral { name = 'central2' }`. Map notation carrying keys " +
            "with no straightforward equivalent, such as the separately deprecated `artifactUrls`, is left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // The Map overloads only exist in the Groovy DSL, so GroovyVisitor skipping Kotlin scripts is the scoping this wants.
        return Preconditions.check(new IsBuildGradle<>(), new GroovyVisitor<ExecutionContext>() {

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!"flatDir".equals(m.getSimpleName()) && !"mavenCentral".equals(m.getSimpleName())) {
                    return m;
                }
                if (!isInsideRepositoriesBlock()) {
                    return m;
                }
                List<G.MapEntry> entries = mapNotationEntries(m.getArguments());
                if (entries == null) {
                    return m;
                }

                // Only the map entry values come from the source, and they travel as parameters. Everything in the
                // template text is the recipe's own, including the repository name, which the guard above pins to
                // one of two literals.
                StringBuilder action = new StringBuilder(m.getSimpleName()).append(" {\n");
                List<Expression> values = new ArrayList<>();
                for (G.MapEntry entry : entries) {
                    String key = keyName(entry);
                    if ("name".equals(key)) {
                        action.append("    name = #{any()}\n");
                        values.add(unprefixed(entry.getValue()));
                    } else if ("dirs".equals(key) && "flatDir".equals(m.getSimpleName())) {
                        // A list becomes varargs, so the template grows a placeholder per element
                        List<Expression> dirs = elementsOf(entry.getValue());
                        action.append("    dirs ");
                        for (int i = 0; i < dirs.size(); i++) {
                            action.append(i == 0 ? "#{any()}" : ", #{any()}");
                            values.add(unprefixed(dirs.get(i)));
                        }
                        action.append('\n');
                    } else {
                        return m;
                    }
                }
                action.append('}');

                return GroovyTemplate.builder(action.toString())
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), values.toArray());
            }

            private boolean isInsideRepositoriesBlock() {
                for (Iterator<Object> path = getCursor().getPath(J.MethodInvocation.class::isInstance); path.hasNext(); ) {
                    if ("repositories".equals(((J.MethodInvocation) path.next()).getSimpleName())) {
                        return true;
                    }
                }
                return false;
            }

            private @Nullable List<G.MapEntry> mapNotationEntries(List<Expression> arguments) {
                if (arguments.isEmpty()) {
                    return null;
                }
                if (arguments.size() == 1 && arguments.get(0) instanceof G.MapLiteral) {
                    return ((G.MapLiteral) arguments.get(0)).getElements();
                }
                List<G.MapEntry> entries = new ArrayList<>(arguments.size());
                for (Expression argument : arguments) {
                    if (!(argument instanceof G.MapEntry)) {
                        return null;
                    }
                    entries.add((G.MapEntry) argument);
                }
                return entries;
            }

            private @Nullable String keyName(G.MapEntry entry) {
                if (entry.getKey() instanceof J.Literal && ((J.Literal) entry.getKey()).getType() == JavaType.Primitive.String) {
                    return (String) ((J.Literal) entry.getKey()).getValue();
                }
                if (entry.getKey() instanceof J.Identifier) {
                    return ((J.Identifier) entry.getKey()).getSimpleName();
                }
                return null;
            }

            private List<Expression> elementsOf(Expression value) {
                return value instanceof G.ListLiteral ? ((G.ListLiteral) value).getElements() : singletonList(value);
            }

            // The template's own spacing separates the arguments, so a value arrives without the one it had in the map
            private Expression unprefixed(Expression value) {
                return value.withPrefix(Space.EMPTY);
            }
        });
    }
}
