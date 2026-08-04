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
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

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
        // The Map overloads only exist in the Groovy DSL, so GroovyVisitor's inability to visit Kotlin scripts is the
        // scoping this recipe wants.
        return Preconditions.check(new IsBuildGradle<>(), new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!"flatDir".equals(m.getSimpleName()) && !"mavenCentral".equals(m.getSimpleName())) {
                    return m;
                }
                if (!inRepositoriesBlock()) {
                    return m;
                }
                List<G.MapEntry> entries = mapNotation(m.getArguments());
                if (entries == null) {
                    return m;
                }
                List<String> configuration = new ArrayList<>(entries.size());
                for (G.MapEntry entry : entries) {
                    String statement = toConfigurationStatement(m.getSimpleName(), entry);
                    if (statement == null) {
                        return m;
                    }
                    configuration.add(statement);
                }
                return buildActionInvocation(m, configuration, ctx);
            }

            private boolean inRepositoriesBlock() {
                for (Iterator<Object> path = getCursor().getPath(J.MethodInvocation.class::isInstance); path.hasNext(); ) {
                    if ("repositories".equals(((J.MethodInvocation) path.next()).getSimpleName())) {
                        return true;
                    }
                }
                return false;
            }

            private @Nullable List<G.MapEntry> mapNotation(List<Expression> arguments) {
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

            private @Nullable String toConfigurationStatement(String repository, G.MapEntry entry) {
                String key = keyName(entry);
                if ("name".equals(key)) {
                    return "name = " + print(entry.getValue());
                }
                if ("dirs".equals(key) && "flatDir".equals(repository)) {
                    return "dirs " + dirsArguments(entry.getValue());
                }
                return null;
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

            /**
             * {@code dirs} takes varargs on the repository, so a list of directories becomes an argument list rather
             * than a single list argument.
             */
            private String dirsArguments(Expression value) {
                if (value instanceof G.ListLiteral) {
                    StringJoiner arguments = new StringJoiner(", ");
                    for (Expression element : ((G.ListLiteral) value).getElements()) {
                        arguments.add(print(element));
                    }
                    return arguments.toString();
                }
                return print(value);
            }

            private String print(Expression expression) {
                return expression.printTrimmed(getCursor());
            }

            private J buildActionInvocation(J.MethodInvocation original, List<String> configuration, ExecutionContext ctx) {
                String indent = indentOf();
                StringBuilder snippet = new StringBuilder(original.getSimpleName()).append(" {\n");
                for (String statement : configuration) {
                    snippet.append(indent).append("    ").append(statement).append('\n');
                }
                snippet.append(indent).append("}\n");

                G.CompilationUnit parsed = (G.CompilationUnit) GradleParser.builder().build()
                        .parse(ctx, snippet.toString())
                        .findFirst()
                        .orElse(null);
                if (parsed == null || parsed.getStatements().isEmpty()) {
                    return original;
                }
                Statement replacement = parsed.getStatements().get(0);
                return replacement.withPrefix(original.getPrefix());
            }

            /**
             * The indentation the replacement's body and closing brace have to line up with. The whitespace is not
             * necessarily on the invocation itself: Groovy wraps the last statement of a closure in an implicit
             * return, which is then what carries the line break and indentation.
             */
            private String indentOf() {
                for (Iterator<Object> path = getCursor().getPath(); path.hasNext(); ) {
                    Object value = path.next();
                    if (value instanceof J.Block || value instanceof G.CompilationUnit) {
                        break;
                    }
                    if (value instanceof J) {
                        String whitespace = ((J) value).getPrefix().getWhitespace();
                        int lastNewline = whitespace.lastIndexOf('\n');
                        if (lastNewline != -1) {
                            return whitespace.substring(lastNewline + 1);
                        }
                    }
                }
                return "";
            }
        });
    }
}
