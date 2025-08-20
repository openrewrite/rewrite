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
package org.openrewrite.gradle.toolingapi;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("JavadocLinkAsPlainText")
public class OpenRewriteModelBuilder {

    /**
     * Build an OpenRewriteModel for a project directory, using the default Gradle init script bundled within this jar.
     * The included init script accesses public artifact repositories (Maven Central, Nexus Snapshots) to be able to
     * download rewrite dependencies, so public repositories must be accessible for this to work.
     */
    public static OpenRewriteModel forProjectDirectory(File projectDir, @Nullable File buildFile) throws IOException {
        return forProjectDirectory(projectDir, buildFile, null);
    }

    /**
     * Build an OpenRewriteModel for a project directory, using the init script contents passed to this function.
     * When Maven Central / Nexus Snapshots are inaccessible this overload can be used with an alternate Groovy init script
     * which applies the ToolingApiOpenRewriteModelPlugin to all projects.
     * Example init script:
     * <pre>
     * initscript {
     *     repositories {
     *         mavenLocal()
     *         maven{ url = uri("https://central.sonatype.com/repository/maven-snapshots") }
     *         mavenCentral()
     *     }
     *
     *     configurations.all {
     *         resolutionStrategy{
     *             cacheChangingModulesFor 0, 'seconds'
     *             cacheDynamicVersionsFor 0, 'seconds'
     *         }
     *     }
     *
     *     dependencies {
     *         classpath 'org.openrewrite.gradle.tooling:plugin:latest.integration'
     *         classpath 'org.openrewrite:rewrite-maven:latest.integration'
     *     }
     * }
     *
     * allprojects {
     *     apply plugin: org.openrewrite.gradle.toolingapi.ToolingApiOpenRewriteModelPlugin
     * }
     * </pre>
     */
    public static OpenRewriteModel forProjectDirectory(File projectDir, @Nullable File buildFile, @Nullable String initScript) throws IOException {
        DefaultGradleConnector connector = (DefaultGradleConnector) GradleConnector.newConnector();
        if (System.getProperty("org.openrewrite.test.gradleVersion") != null) {
            connector.useGradleVersion(System.getProperty("org.openrewrite.test.gradleVersion"));
        } else if (Files.exists(projectDir.toPath().resolve("gradle/wrapper/gradle-wrapper.properties"))) {
            connector.useBuildDistribution();
        } else {
            connector.useGradleVersion("8.12");
        }
        connector
                // Uncomment to hit breakpoints inside OpenRewriteModelBuilder in unit tests
                // Leaving commented out because the exact consequences of this internal API are unclear
                // .embedded(true)
                .forProjectDirectory(projectDir);
        List<String> arguments = new ArrayList<>();
        if (buildFile != null && buildFile.exists()) {
            arguments.add("-b");
            arguments.add(buildFile.getAbsolutePath());
        }
        arguments.add("--init-script");
        Path init = projectDir.toPath().resolve("openrewrite-tooling.gradle").toAbsolutePath();
        arguments.add(init.toString());
        try (ProjectConnection connection = connector.connect()) {
            ModelBuilder<OpenRewriteModelProxy> customModelBuilder = connection.model(OpenRewriteModelProxy.class);
            try {
                if (initScript == null) {
                    try (InputStream is = OpenRewriteModel.class.getResourceAsStream("/init.gradle")) {
                        if (is == null) {
                            throw new IllegalStateException("Expected to find init.gradle on the classpath");
                        }
                        Files.copy(is, init);
                    }
                } else {
                    Files.write(init, initScript.getBytes());
                }
                customModelBuilder.withArguments(arguments);
                return OpenRewriteModel.from(customModelBuilder.get());
            } finally {
                try {
                    Files.delete(init);
                } catch (IOException e) {
                    //noinspection ThrowFromFinallyBlock
                    throw new UncheckedIOException(e);
                }
            }
        }
    }
}
