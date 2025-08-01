/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
public class GradlePlugin implements Trait<J> {
    Cursor cursor;

    @Nullable
    String pluginId;

    @Nullable
    String pluginClass;

    @Nullable
    String version;

    boolean applied;

    public static class Matcher extends GradleTraitMatcher<GradlePlugin> {
        private static final MethodMatcher ALIAS_DSL_MATCHER = new MethodMatcher("* alias(..)", false);
        private static final MethodMatcher APPLY_DSL_MATCHER = new MethodMatcher("* apply(..)", false);
        private static final MethodMatcher PLUGIN_ID_DSL_MATCHER = new MethodMatcher("* id(..)", false);
        private static final MethodMatcher KOTLIN_PLUGIN_DSL_MATCHER = new MethodMatcher("* kotlin(..)", false);
        private static final MethodMatcher PLUGIN_VERSION_DSL_MATCHER = new MethodMatcher("* version(..)", false);
        private static final String[] CORE_PLUGIN_NAMES = new String[]{
                "java", "java-base", "java-library", "java-platform", "groovy", "scala", "antlr", "jvm-test-suite", "test-report-aggregation",
                "application", "war", "ear", "maven-publish", "ivy-publish", "distribution", "java-library-distribution",
                "checkstyle", "pmd", "jacoco", "jacoco-report-aggregation", "codenarc", "eclipse", "eclipse-wtp", "idea",
                "visual-studio", "xcode", "base", "signing", "java-gradle-plugin", "project-report"};

        @Nullable
        protected String pluginIdPattern;

        @Nullable
        protected String pluginClass;

        protected boolean acceptTransitive;

        public GradlePlugin.Matcher pluginIdPattern(@Nullable String pluginIdPattern) {
            this.pluginIdPattern = pluginIdPattern;
            return this;
        }

        public GradlePlugin.Matcher pluginClass(@Nullable String pluginClass) {
            this.pluginClass = pluginClass;
            return this;
        }

        public GradlePlugin.Matcher acceptTransitive(boolean acceptTransitive) {
            this.acceptTransitive = acceptTransitive;
            return this;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<GradlePlugin, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public @Nullable J visit(@Nullable Tree tree, P p) {
                    J j = super.visit(tree, p);
                    if (j instanceof JavaSourceFile) {
                        GradlePlugin plugin = test(new Cursor(getCursor(), j));
                        return plugin != null ?
                                (J) visitor.visit(plugin, p) :
                                j;
                    }
                    return j;
                }

                @Override
                public J visitIdentifier(J.Identifier ident, P p) {
                    GradlePlugin plugin = test(getCursor());
                    return plugin != null ?
                            (J) visitor.visit(plugin, p) :
                            super.visitIdentifier(ident, p);
                }

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, P p) {
                    GradlePlugin plugin = test(getCursor());
                    return plugin != null ?
                            (J) visitor.visit(plugin, p) :
                            super.visitMethodInvocation(method, p);
                }
            };
        }

        @Override
        protected @Nullable GradlePlugin test(Cursor cursor) {
            Object object = cursor.getValue();
            if (acceptTransitive && object instanceof JavaSourceFile) {
                GradleProject gp = getGradleProject(cursor);
                if (gp == null) {
                    return null;
                }

                return gp.getPlugins()
                        .stream()
                        .map(pluginDescriptor -> maybeGradlePlugin(cursor, pluginDescriptor.getId(), pluginDescriptor.getFullyQualifiedClassName(), null, true))
                        .findFirst()
                        .orElse(null);
            } else if (object instanceof J.MethodInvocation) {
                J.MethodInvocation m = (J.MethodInvocation) object;

                if (withinPlugins(cursor)) {
                    if (ALIAS_DSL_MATCHER.matches(m, true)) {
                        if (!(m.getArguments().get(0) instanceof J.FieldAccess)) {
                            return null;
                        }

                        return maybeGradlePlugin(cursor, null, null, null, true);
                    } else if (APPLY_DSL_MATCHER.matches(m, true)) {
                        if (!(m.getArguments().get(0) instanceof J.Literal) || !(m.getSelect() instanceof J.MethodInvocation)) {
                            return null;
                        }

                        J.MethodInvocation versionSelect = (J.MethodInvocation) m.getSelect();
                        if (!PLUGIN_VERSION_DSL_MATCHER.matches(versionSelect, true) ||
                                !(versionSelect.getArguments().get(0) instanceof J.Literal) ||
                                !(versionSelect.getSelect() instanceof J.MethodInvocation)) {
                            return null;
                        }

                        J.MethodInvocation idSelect = (J.MethodInvocation) versionSelect.getSelect();
                        if (!(PLUGIN_ID_DSL_MATCHER.matches(idSelect, true) || KOTLIN_PLUGIN_DSL_MATCHER.matches(idSelect, true)) ||
                                !(idSelect.getArguments().get(0) instanceof J.Literal)) {
                            return null;
                        }

                        J.Literal idLiteral = (J.Literal) idSelect.getArguments().get(0);
                        J.Literal versionLiteral = (J.Literal) versionSelect.getArguments().get(0);
                        J.Literal applyLiteral = (J.Literal) m.getArguments().get(0);
                        String pluginId = "kotlin".equals(idSelect.getSimpleName()) ? "org.jetbrains.kotlin." + idLiteral.getValue() : (String) idLiteral.getValue();
                        String version = (String) versionLiteral.getValue();
                        boolean applied = Boolean.TRUE.equals(applyLiteral.getValue());
                        return maybeGradlePlugin(cursor, pluginId, null, version, applied);
                    } else if (PLUGIN_VERSION_DSL_MATCHER.matches(m, true)) {
                        String version = null;
                        if (m.getArguments().get(0) instanceof J.Literal) {
                            J.Literal versionLiteral = (J.Literal) m.getArguments().get(0);
                            version = (String) versionLiteral.getValue();
                        }

                        if (!(m.getSelect() instanceof J.MethodInvocation &&
                                (PLUGIN_ID_DSL_MATCHER.matches((J.MethodInvocation) m.getSelect(), true) || KOTLIN_PLUGIN_DSL_MATCHER.matches((J.MethodInvocation) m.getSelect(), true)))) {
                            return null;
                        }

                        J.MethodInvocation select = (J.MethodInvocation) m.getSelect();
                        if (!(select.getArguments().get(0) instanceof J.Literal)) {
                            return null;
                        }

                        J.Literal idLiteral = (J.Literal) select.getArguments().get(0);
                        String pluginId = "kotlin".equals(select.getSimpleName()) ? "org.jetbrains.kotlin." + idLiteral.getValue() : (String) idLiteral.getValue();
                        return maybeGradlePlugin(cursor, pluginId, null, version, !withinBlock(cursor, "pluginManagement"));
                    } else if (PLUGIN_ID_DSL_MATCHER.matches(m, true) || KOTLIN_PLUGIN_DSL_MATCHER.matches(m, true)) {
                        if (!(m.getArguments().get(0) instanceof J.Literal)) {
                            return null;
                        }

                        J.Literal literal = (J.Literal) m.getArguments().get(0);
                        String pluginId = "kotlin".equals(m.getSimpleName()) ? "org.jetbrains.kotlin." + literal.getValue() : (String) literal.getValue();
                        return maybeGradlePlugin(cursor, pluginId, null, null, !withinBlock(cursor, "pluginManagement"));
                    }
                } else if (isProjectReceiver(cursor) && APPLY_DSL_MATCHER.matches(m, true)) {
                    Expression e = m.getArguments().get(0);
                    if (e instanceof G.MapEntry) {
                        G.MapEntry entry = (G.MapEntry) e;
                        if (!(entry.getKey() instanceof J.Literal && "plugin".equals(((J.Literal) entry.getKey()).getValue()))) {
                            return null;
                        }

                        if (entry.getValue() instanceof J.Literal) {
                            String pluginId = (String) ((J.Literal) entry.getValue()).getValue();
                            return maybeGradlePlugin(cursor, pluginId, null, null, true);
                        } else if (entry.getValue() instanceof J.FieldAccess || entry.getValue() instanceof J.Identifier) {
                            return maybeGradlePlugin(cursor, null, null, null, true);
                        }
                    } else if (e instanceof J.Assignment) {
                        J.Assignment assignment = (J.Assignment) e;
                        if (!(assignment.getVariable() instanceof J.Identifier && "plugin".equals(((J.Identifier) assignment.getVariable()).getSimpleName())) ||
                                !(assignment.getAssignment() instanceof J.Literal)) {
                            return null;
                        }

                        J.Literal literal = (J.Literal) assignment.getAssignment();
                        String pluginId = (String) literal.getValue();
                        return maybeGradlePlugin(cursor, pluginId, null, null, true);
                    } else if (m.getTypeParameters() != null && !m.getTypeParameters().isEmpty()) {
                        if (m.getTypeParameters().get(0) instanceof J.FieldAccess || m.getTypeParameters().get(0) instanceof J.Identifier) {
                            return maybeGradlePlugin(cursor, null, null, null, true);
                        }
                    }
                }
            } else if (withinPlugins(cursor) && object instanceof J.Identifier) {
                J.Identifier i = (J.Identifier) object;
                String maybePluginId = i.getSimpleName();

                if (!isCorePlugin(maybePluginId)) {
                    return null;
                }

                return maybeGradlePlugin(cursor, maybePluginId, null, null, true);
            }

            return null;
        }

        private boolean withinPlugins(Cursor cursor) {
            Cursor parent = cursor.dropParentUntil(value -> value instanceof J.MethodInvocation || value == Cursor.ROOT_VALUE);
            if (parent.isRoot() || !"plugins".equals(((J.MethodInvocation) parent.getValue()).getSimpleName())) {
                return false;
            }

            parent = parent.dropParentUntil(value -> value instanceof J.MethodInvocation || value == Cursor.ROOT_VALUE);
            return parent.isRoot() || "pluginManagement".equals(((J.MethodInvocation) parent.getValue()).getSimpleName());
        }

        private boolean isProjectReceiver(Cursor cursor) {
            Cursor parent = cursor.dropParentUntil(value -> value instanceof J.MethodInvocation || value == Cursor.ROOT_VALUE);
            if (parent.isRoot()) {
                return true;
            }

            J.MethodInvocation m = parent.getValue();
            switch (m.getSimpleName()) {
                case "allprojects":
                case "subprojects":
                case "configure":
                    return true;
                default:
                    return false;
            }
        }

        private boolean isCorePlugin(String pluginId) {
            for (String pluginName : CORE_PLUGIN_NAMES) {
                if (pluginName.equals(pluginId)) {
                    return true;
                }
            }
            return false;
        }

        private @Nullable GradlePlugin maybeGradlePlugin(Cursor cursor, @Nullable String pluginId, @Nullable String pluginClass, @Nullable String version, boolean applied) {
            if (!StringUtils.isBlank(pluginIdPattern) && !matchesGlob(pluginId, pluginIdPattern) ||
                    !StringUtils.isBlank(this.pluginClass) && !matchesGlob(pluginClass, this.pluginClass)) {
                return null;
            }

            return new GradlePlugin(cursor, pluginId, pluginClass, version, applied);
        }
    }
}
