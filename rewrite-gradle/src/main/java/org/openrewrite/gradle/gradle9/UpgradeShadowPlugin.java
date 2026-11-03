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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.gradle.trait.GradlePlugin;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class UpgradeShadowPlugin extends Recipe {
    @Override
    public String getDisplayName() {
        return "Upgrade Shadow for Gradle 9";
    }

    @Override
    public String getDescription() {
        return "Migrate literal Shadow plugin declarations to the maintained GradleUp plugin and a Gradle 9 compatible version, preserving newer versions.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()),
                new JavaIsoVisitor<ExecutionContext>() {
                    final VersionComparator versions = requireNonNull(Semver.validate("latest.release", null).getValue());

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        if (!"version".equals(m.getSimpleName()) ||
                            !(m.getSelect() instanceof J.MethodInvocation) ||
                            m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Literal) ||
                            !(((J.Literal) m.getArguments().get(0)).getValue() instanceof String)) {
                            return m;
                        }
                        J.MethodInvocation id = (J.MethodInvocation) m.getSelect();
                        // An explicit receiver can be a user-defined DSL, even inside plugins {}.
                        // Exclude it before the trait interprets its arguments as plugin strings.
                        if (!"id".equals(id.getSimpleName()) || id.getSelect() != null || id.getArguments().size() != 1 ||
                            !(id.getArguments().get(0) instanceof J.Literal) ||
                            !(((J.Literal) id.getArguments().get(0)).getValue() instanceof String)) {
                            return m;
                        }
                        GradlePlugin plugin = new GradlePlugin.Matcher().get(getCursor()).orElse(null);
                        if (plugin == null || plugin.getVersion() == null ||
                            !("com.github.johnrengelman.shadow".equals(plugin.getPluginId()) ||
                              "com.gradleup.shadow".equals(plugin.getPluginId()))) {
                            return m;
                        }
                        // Versionless or computed declarations can be managed elsewhere. Only rewrite
                        // the literal declaration that owns both the plugin id and its version.
                        if (!plugin.getVersion().matches("\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?")) {
                            return m;
                        }
                        J.Literal pluginId = (J.Literal) id.getArguments().get(0);
                        m = m.withSelect(id.withArguments(singletonList(
                                ChangeStringLiteral.withStringValue(pluginId, "com.gradleup.shadow"))));
                        // Shadow 9.0.0 supports Gradle 9 and Java 11, the least restrictive
                        // compatible baseline. Keep later releases rather than pinning them back.
                        if (versions.compare(null, plugin.getVersion(), "9.0.0") < 0) {
                            m = m.withArguments(singletonList(ChangeStringLiteral.withStringValue(
                                    (J.Literal) m.getArguments().get(0), "9.0.0")));
                        }
                        return m;
                    }
                });
    }
}
