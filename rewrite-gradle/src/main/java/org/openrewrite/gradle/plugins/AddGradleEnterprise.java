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
package org.openrewrite.gradle.plugins;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.IsSettingsGradle;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

@Value
@EqualsAndHashCode(callSuper = true)
@Incubating(since = "7.33.0")
public class AddGradleEnterprise extends Recipe {

    @Option(displayName = "Plugin version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "3.x")
    String version;

    @Override
    public String getDisplayName() {
        return "Add the Gradle Enterprise plugin";
    }

    @Override
    public String getDescription() {
        return "Add the Gradle Enterprise plugin to `settings.gradle(.kts)`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(new IsBuildGradle<>(), new IsSettingsGradle<>());
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                cu.getMarkers().findFirst(BuildTool.class)
                        .ifPresent(buildTool -> {
                            if (buildTool.getType() == BuildTool.Type.Gradle) {
                                VersionComparator versionComparator = Semver.validate("(,6)", null).getValue();
                                if (versionComparator != null && versionComparator.isValid(null, buildTool.getVersion())) {
                                    doNext(new AddBuildPlugin("com.gradle.build-scan", version, null));
                                    doNext(new UpgradePluginVersion("com.gradle.build-scan", version, null));
                                } else {
                                    doNext(new AddSettingsPlugin("com.gradle.enterprise", version, null));
                                    doNext(new UpgradePluginVersion("com.gradle.enterprise", version, null));
                                }
                            }
                        });
                return cu;
            }
        };
    }
}
