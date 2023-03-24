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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
@Incubating(since = "7.33.0")
public class AddGradleEnterprise extends Recipe {

    @Option(displayName = "Plugin version",
            description = "An exact version number or node-style semver selector used to select the plugin version.",
            example = "3.x")
    String version;

//    @Option(displayName = "Server URL",
//            description = "The URL of the Gradle Enterprise server. If omitted the recipe will set no URL and Gradle will direct scans to https://scans.gradle.com/",
//            required = false,
//            example = "https://ge.openrewrite.org/")
//    @Nullable
//    String server;
//
//    @Option(displayName = "Allow untrusted server",
//            description = "When set to `true` the plugin will be configured to allow unencrypted http connections with the server. " +
//                    "If set to `false` or omitted, the plugin will refuse to communicate without transport layer security enabled.",
//            required = false,
//            example = "true")
//    @Nullable
//    Boolean allowUntrustedServer;
//
//    @Option(displayName = "Capture task input files",
//            description = "When set to `true` the plugin will capture additional information about the inputs to Gradle tasks. " +
//                    "This increases the size of build scans, but is useful for diagnosing issues with task caching. ",
//            required = false,
//            example = "true")
//    @Nullable
//    Boolean captureTaskInputFiles;
//
//    @Option(displayName = "Upload in background",
//            description = "When set to `true` the plugin will capture additional information about the outputs of Gradle tasks. " +
//                    "This increases the size of build scans, but is useful for diagnosing issues with task caching. ",
//            required = false,
//            example = "true")
//    @Nullable
//    Boolean uploadInBackground;

    @Override
    public String getDisplayName() {
        return "Add the Gradle Enterprise plugin";
    }

    @Override
    public String getDescription() {
        return "Add the Gradle Enterprise plugin to settings.gradle files.";
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
                Optional<BuildTool> maybeBuildTool = cu.getMarkers().findFirst(BuildTool.class);
                if(!maybeBuildTool.isPresent()) {
                    return cu;
                }
                BuildTool buildTool = maybeBuildTool.get();
                if(buildTool.getType() != BuildTool.Type.Gradle) {
                    return cu;
                }
                VersionComparator versionComparator = Semver.validate("(,6)", null).getValue();
                if(versionComparator == null) {
                    return cu;
                }
                boolean gradleSixOrLater = versionComparator.compare(null, buildTool.getVersion(), "6.0") >= 0;
                // Newer than 6.0 goes in settings
                // Older than 6.0 goes in root build.gradle only, not in build.gradle of subprojects
                if (gradleSixOrLater && cu.getSourcePath().endsWith("settings.gradle")) {
                    cu = (JavaSourceFile) new AddSettingsPlugin("com.gradle.enterprise", version, null).getVisitor()
                            .visit(cu, executionContext);
                    cu = (JavaSourceFile) new UpgradePluginVersion("com.gradle.build-scan", version, null).getVisitor()
                            .visit(cu, executionContext);
                } else if(!gradleSixOrLater && cu.getSourcePath().toString().equals("build.gradle")) {
                    // This heuristic for identifying the root build.gradle is not perfect. Shortcomings include:
                    //   The build root might not be the repository root
                    //   The build file may have been renamed to something other than "build.gradle"
                    cu = (JavaSourceFile) new AddBuildPlugin("com.gradle.build-scan", version, null).getVisitor()
                            .visit(cu, executionContext);
                    cu = (JavaSourceFile) new UpgradePluginVersion("com.gradle.build-scan", version, null).getVisitor()
                            .visit(cu, executionContext);
                }

                return cu;
            }
        };
    }
}
