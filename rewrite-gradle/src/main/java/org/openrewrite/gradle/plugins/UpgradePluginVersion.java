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
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Disabled;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.openrewrite.gradle.plugins.GradlePluginUtils.availablePluginVersions;

@Disabled("Gradle plugin portal is down on August 23, 2022")
@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradePluginVersion extends Recipe {
    @Option(displayName = "Plugin id",
            description = "The `ID` part of `plugin { ID }`, as a glob expression.",
            example = "com.jfrog.bintray")
    String pluginIdPattern;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Update a Gradle plugin by id";
    }

    @Override
    public String getDescription() {
        return "Update a Gradle plugin by id to a later version.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(Semver.validate(newVersion, versionPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;
        MethodMatcher pluginMatcher = new MethodMatcher("PluginSpec id(..)", false);
        MethodMatcher versionMatcher = new MethodMatcher("Plugin version(..)", false);
        return Preconditions.check(Preconditions.or(new IsBuildGradle<>(), new IsSettingsGradle<>()), new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!(versionMatcher.matches(m) &&
                        m.getSelect() instanceof J.MethodInvocation &&
                        pluginMatcher.matches(m.getSelect()))) {
                    return m;
                }
                List<Expression> pluginArgs = ((J.MethodInvocation) m.getSelect()).getArguments();
                if (!(pluginArgs.get(0) instanceof J.Literal)) {
                    return m;
                }
                String pluginId = (String) ((J.Literal) pluginArgs.get(0)).getValue();
                if(pluginId == null || !StringUtils.matchesGlob(pluginId, pluginIdPattern)) {
                    return m;
                }

                List<Expression> versionArgs = m.getArguments();
                if (!(versionArgs.get(0) instanceof J.Literal)) {
                    return m;
                }
                String currentVersion = (String) ((J.Literal) versionArgs.get(0)).getValue();
                if(currentVersion == null) {
                    return m;
                }
                Optional<String> version;
                if(versionComparator instanceof ExactVersion) {
                    version = versionComparator.upgrade(currentVersion, Collections.singletonList(newVersion));
                } else {
                    version = versionComparator.upgrade(currentVersion, availablePluginVersions(pluginId, ctx));
                }
                J.MethodInvocation finalM = m;
                m = version.map(upgradeVersion -> finalM.withArguments(ListUtils.map(versionArgs, v -> {
                            J.Literal versionLiteral = (J.Literal) v;
                            assert versionLiteral.getValueSource() != null;
                            return versionLiteral
                                    .withValue(upgradeVersion)
                                    .withValueSource(versionLiteral.getValueSource().replace(currentVersion, upgradeVersion));
                        })))
                        .orElse(m);

                return m;
            }
        });
    }
}
