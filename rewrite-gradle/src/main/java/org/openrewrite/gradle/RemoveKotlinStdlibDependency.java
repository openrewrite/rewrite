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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradlePlugin;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.LatestRelease;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class RemoveKotlinStdlibDependency extends ScanningRecipe<Set<String>> {

    private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";
    private static final String STDLIB = "kotlin-stdlib";
    private static final String DEFAULT_DEPENDENCY_PROPERTY = "kotlin.stdlib.default.dependency";
    private static final String FIRST_KGP_ADDING_STDLIB = "1.4.0";

    private static final Set<String> KOTLIN_PLUGINS = new HashSet<>(asList(
            "org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.android", "org.jetbrains.kotlin.multiplatform",
            "org.jetbrains.kotlin.js", "kotlin", "kotlin-android", "kotlin-multiplatform"));

    @Getter
    final String displayName = "Remove the explicit Kotlin standard library dependency";

    @Getter
    final String description = "The Kotlin Gradle plugin adds a `kotlin-stdlib` dependency matching its own version " +
            "to every Kotlin source set, so declaring it again is redundant. This recipe removes `kotlin(\"stdlib\")` and " +
            "`org.jetbrains.kotlin:kotlin-stdlib` declarations from projects that apply a Kotlin plugin, unless they pin " +
            "a version newer than the plugin's, or `kotlin.stdlib.default.dependency=false` turns the default " +
            "dependency off. " +
            "See [don't explicitly depend on the Kotlin standard library](https://docs.gradle.org/current/userguide/best_practices_dependencies.html#dont_depend_on_kotlin_stdlib).";

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> projectsWithoutDefaultStdlib) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof Properties.File && ((Properties.File) tree).getSourcePath().endsWith("gradle.properties")) {
                    Properties.File properties = (Properties.File) tree;
                    for (Properties.Entry entry : FindProperties.find(properties, DEFAULT_DEPENDENCY_PROPERTY, false)) {
                        if ("false".equals(entry.getValue().getText().trim())) {
                            projectsWithoutDefaultStdlib.add(directoryOf(properties.getSourcePath()));
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> projectsWithoutDefaultStdlib) {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Nullable
            String kotlinPluginVersion;

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    if (!appliesKotlinPlugin(cu) || hasDefaultStdlibDisabled(cu.getSourcePath()) ||
                        kotlinPluginVersion != null && isOlder(kotlinPluginVersion, FIRST_KGP_ADDING_STDLIB)) {
                        return cu;
                    }
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.@Nullable Return visitReturn(J.Return return_, ExecutionContext ctx) {
                J.Return r = super.visitReturn(return_, ctx);
                if (return_.getExpression() != null && r.getExpression() == null) {
                    return null;
                }
                return r;
            }

            @Override
            public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (isRedundantStdlibDeclaration(m)) {
                    return null;
                }
                return m;
            }

            private boolean isRedundantStdlibDeclaration(J.MethodInvocation m) {
                if (m.getArguments().size() != 1 || !isInDependenciesBlock()) {
                    return false;
                }
                Expression argument = m.getArguments().get(0);
                if (argument instanceof J.MethodInvocation && "kotlin".equals(((J.MethodInvocation) argument).getSimpleName())) {
                    J.MethodInvocation kotlin = (J.MethodInvocation) argument;
                    if (kotlin.getArguments().isEmpty() || !isLiteral(kotlin.getArguments().get(0), "stdlib")) {
                        return false;
                    }
                    if (kotlin.getArguments().size() == 1) {
                        return true;
                    }
                    Expression version = kotlin.getArguments().get(1);
                    return kotlin.getArguments().size() == 2 && version instanceof J.Literal &&
                           ((J.Literal) version).getValue() instanceof String &&
                           isNotNewerThanPlugin((String) ((J.Literal) version).getValue());
                }
                if (argument instanceof J.Literal && ((J.Literal) argument).getValue() instanceof String) {
                    Dependency dependency = DependencyNotation.parse((String) ((J.Literal) argument).getValue());
                    return dependency != null && KOTLIN_GROUP.equals(dependency.getGroupId()) &&
                           STDLIB.equals(dependency.getArtifactId()) && dependency.getClassifier() == null &&
                           (dependency.getVersion() == null || isNotNewerThanPlugin(dependency.getVersion()));
                }
                return false;
            }

            private boolean isNotNewerThanPlugin(String version) {
                return kotlinPluginVersion != null && !isOlder(kotlinPluginVersion, version);
            }

            private boolean isInDependenciesBlock() {
                J.MethodInvocation enclosing = getCursor().getParentTreeCursor().firstEnclosing(J.MethodInvocation.class);
                if (enclosing == null || !"dependencies".equals(enclosing.getSimpleName())) {
                    return false;
                }
                return getCursor().getPathAsStream().noneMatch(p -> p instanceof J.MethodInvocation &&
                        ("buildscript".equals(((J.MethodInvocation) p).getSimpleName()) ||
                         "constraints".equals(((J.MethodInvocation) p).getSimpleName())));
            }

            private boolean appliesKotlinPlugin(JavaSourceFile cu) {
                kotlinPluginVersion = null;
                Set<String> declared = new HashSet<>();
                new GradlePlugin.Matcher().asVisitor(plugin -> {
                    if (plugin.getPluginId() != null && KOTLIN_PLUGINS.contains(plugin.getPluginId())) {
                        declared.add(plugin.getPluginId());
                        if (plugin.getVersion() != null) {
                            kotlinPluginVersion = plugin.getVersion();
                        }
                    }
                    return plugin.getTree();
                }).visit(cu, 0);

                GradleProject gradleProject = cu.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gradleProject != null) {
                    if (kotlinPluginVersion == null) {
                        GradleDependencyConfiguration classpath = gradleProject.getBuildscript().getConfiguration("classpath");
                        ResolvedDependency kgp = classpath == null ? null :
                                classpath.findResolvedDependency(KOTLIN_GROUP, "kotlin-gradle-plugin");
                        if (kgp != null) {
                            kotlinPluginVersion = kgp.getVersion();
                        }
                    }
                    if (gradleProject.getPlugins().stream().anyMatch(p -> KOTLIN_PLUGINS.contains(p.getId()))) {
                        return true;
                    }
                }
                return !declared.isEmpty();
            }

            private boolean hasDefaultStdlibDisabled(Path buildFile) {
                String project = directoryOf(buildFile);
                for (String disabled : projectsWithoutDefaultStdlib) {
                    if (disabled.isEmpty() || project.equals(disabled) || project.startsWith(disabled + "/")) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private static boolean isLiteral(Expression expression, String value) {
        return expression instanceof J.Literal && value.equals(((J.Literal) expression).getValue());
    }

    private static boolean isOlder(String version, String than) {
        return new LatestRelease(null).compare(null, version, than) < 0;
    }

    private static String directoryOf(Path path) {
        Path parent = path.getParent();
        return parent == null ? "" : parent.toString().replace('\\', '/');
    }
}
