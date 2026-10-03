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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddJavaToolchain extends Recipe {
    private static final Set<String> JAVA_PLUGIN_IDS = new HashSet<>(Arrays.asList(
            "java", "java-library", "groovy", "scala", "java-gradle-plugin"));

    @Option(displayName = "Java version",
            description = "The Java language version the toolchain requests.",
            example = "17")
    Integer version;

    String displayName = "Add Gradle Java toolchain";

    String description = "Adds a `java { toolchain { languageVersion = JavaLanguageVersion.of(..) } }` block " +
                         "where a Java plugin is applied and no toolchain is declared. A toolchain is the only way " +
                         "to raise the Java version for projects whose convention plugins reset " +
                         "`sourceCompatibility` after the build script is evaluated. Existing toolchains are left " +
                         "for `UpdateJavaCompatibility` to update.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("version", "Version must be > 0.", version, v -> v > 0));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                if (declaresToolchain(cu)) {
                    return cu;
                }

                G.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
                int topLevelIndex = insertionIndex(c.getStatements(), true);
                if (topLevelIndex < 0) {
                    return c;
                }
                Statement anchor = c.getStatements().get(topLevelIndex);
                c = GroovyTemplate.builder(toolchainBlock(""))
                        .build()
                        .apply(new Cursor(getCursor().getParentOrThrow(), c), anchor.getCoordinates().after());
                // Gradle scripts set their top-level blocks apart with a blank line, which a coordinate places
                // but does not style
                return c.withStatements(ListUtils.map(c.getStatements(), (i, s) ->
                        i == topLevelIndex + 1 ? s.withPrefix(Space.format("\n\n")) : s));
            }

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                if (!(getCursor().getParentTreeCursor().getValue() instanceof J.Lambda)) {
                    return b;
                }
                int index = insertionIndex(b.getStatements(), false);
                if (index < 0) {
                    return b;
                }
                Statement anchor = b.getStatements().get(index);
                String prefix = anchor.getPrefix().getWhitespace();
                String indent = prefix.substring(prefix.lastIndexOf('\n') + 1);
                J.Block updated = GroovyTemplate.builder(toolchainBlock(indent))
                        .build()
                        .apply(new Cursor(getCursor().getParentOrThrow(), b), anchor.getCoordinates().after());
                return updated.withStatements(ListUtils.map(updated.getStatements(), (i, s) ->
                        i == index + 1 ? s.withPrefix(Space.format("\n\n" + indent)) : s));
            }
        });
    }

    private String toolchainBlock(String indent) {
        return "java {\n" +
               indent + "    toolchain {\n" +
               indent + "        languageVersion = JavaLanguageVersion.of(" + version + ")\n" +
               indent + "    }\n" +
               indent + "}";
    }

    /**
     * @return The index of the last statement applying a plugin, after which the toolchain belongs, or -1 when none
     * of these statements apply a Java plugin.
     */
    private static int insertionIndex(List<Statement> statements, boolean topLevel) {
        boolean javaPlugin = false;
        int anchor = -1;
        for (int i = 0; i < statements.size(); i++) {
            Statement s = statements.get(i);
            if (s instanceof J.Return && ((J.Return) s).getExpression() instanceof Statement) {
                s = (Statement) ((J.Return) s).getExpression();
            }
            if (!(s instanceof J.MethodInvocation)) {
                continue;
            }
            J.MethodInvocation m = (J.MethodInvocation) s;
            if ("apply".equals(m.getSimpleName())) {
                String pluginId = appliedPluginId(m);
                if (pluginId != null) {
                    javaPlugin |= JAVA_PLUGIN_IDS.contains(pluginId);
                    anchor = i;
                }
            } else if (topLevel && "plugins".equals(m.getSimpleName())) {
                javaPlugin |= pluginsBlockAppliesJavaPlugin(m);
                anchor = i;
            }
        }
        return javaPlugin ? anchor : -1;
    }

    private static @Nullable String appliedPluginId(J.MethodInvocation apply) {
        for (Expression arg : apply.getArguments()) {
            if (arg instanceof G.MapEntry) {
                G.MapEntry entry = (G.MapEntry) arg;
                Expression key = entry.getKey();
                boolean pluginKey =
                        key instanceof J.Literal && "plugin".equals(((J.Literal) key).getValue()) ||
                        key instanceof J.Identifier && "plugin".equals(((J.Identifier) key).getSimpleName());
                if (pluginKey && entry.getValue() instanceof J.Literal &&
                    ((J.Literal) entry.getValue()).getValue() instanceof String) {
                    return (String) ((J.Literal) entry.getValue()).getValue();
                }
            }
        }
        return null;
    }

    /**
     * Whether a {@code plugins} block applies a Java plugin to this project, ignoring those declared
     * {@code apply false}.
     */
    private static boolean pluginsBlockAppliesJavaPlugin(J.MethodInvocation plugins) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                if ("apply".equals(method.getSimpleName()) && method.getArguments().size() == 1 &&
                    method.getArguments().get(0) instanceof J.Literal &&
                    Boolean.FALSE.equals(((J.Literal) method.getArguments().get(0)).getValue())) {
                    return method;
                }
                if ("id".equals(method.getSimpleName()) && method.getArguments().size() == 1 &&
                    method.getArguments().get(0) instanceof J.Literal &&
                    JAVA_PLUGIN_IDS.contains(((J.Literal) method.getArguments().get(0)).getValue())) {
                    found.set(true);
                }
                return super.visitMethodInvocation(method, found);
            }
        }.visit(plugins, found);
        return found.get();
    }

    private static boolean declaresToolchain(Tree tree) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                if ("toolchain".equals(method.getSimpleName()) || "jvmToolchain".equals(method.getSimpleName())) {
                    found.set(true);
                    return method;
                }
                return super.visitMethodInvocation(method, found);
            }
        }.visit(tree, found);
        return found.get();
    }
}
