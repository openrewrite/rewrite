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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
        String gradleVersion;
        if (Files.exists(projectDir.toPath().resolve("gradle/wrapper/gradle-wrapper.properties"))) {
            gradleVersion = wrapperGradleVersion(projectDir.toPath().resolve("gradle/wrapper/gradle-wrapper.properties"));
            connector.useBuildDistribution();
        } else {
            gradleVersion = System.getProperty("org.openrewrite.test.gradleVersion", "8.14.4");
            connector.useGradleVersion(gradleVersion);
        }
        connector
                // Uncomment to hit breakpoints inside OpenRewriteModelBuilder in unit tests
                // Leaving commented out because the exact consequences of this internal API are unclear
                // .embedded(true)
                .forProjectDirectory(projectDir);
        List<String> arguments = new ArrayList<>();
        arguments.add("--init-script");
        Path init = projectDir.toPath().resolve("openrewrite-tooling.gradle").toAbsolutePath();
        arguments.add(init.toString());
        Path settings = null;
        boolean settingsWritten = false;
        if (buildFile != null && buildFile.exists()) {
            if (isGradle9OrLater(gradleVersion)) {
                // Gradle 9 dropped -b; for non-conventional build files write a temporary settings.gradle with rootProject.buildFileName.
                File abs = buildFile.getAbsoluteFile();
                boolean atConventionalLocation = abs.equals(new File(projectDir, "build.gradle").getAbsoluteFile()) ||
                        abs.equals(new File(projectDir, "build.gradle.kts").getAbsoluteFile());
                if (!atConventionalLocation) {
                    Path projectPath = projectDir.toPath();
                    if (!Files.exists(projectPath.resolve("settings.gradle")) &&
                            !Files.exists(projectPath.resolve("settings.gradle.kts"))) {
                        settings = projectPath.resolve("settings.gradle");
                        Files.write(settings, ("rootProject.buildFileName = '" + buildFile.getName() + "'\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
                        settingsWritten = true;
                    }
                }
            } else {
                arguments.add("-b");
                arguments.add(buildFile.getAbsolutePath());
            }
        }
        try (ProjectConnection connection = connector.connect()) {
            ModelBuilder<OpenRewriteModelProxy> customModelBuilder = connection.model(OpenRewriteModelProxy.class);
            if (initScript == null) {
                if (System.getProperty("org.openrewrite.gradle.local.use-embedded-classpath") != null) {
                    // code path only expected to be taken from within openrewrite/rewrite
                    String generatedInitScript = generateInitScriptFromManifest();
                    Files.write(init, generatedInitScript.getBytes(StandardCharsets.UTF_8));
                } else {
                    // Use default init.gradle from resources
                    try (InputStream is = OpenRewriteModel.class.getResourceAsStream("/init.gradle")) {
                        if (is == null) {
                            throw new IllegalStateException("Expected to find init.gradle on the classpath");
                        }
                        Files.copy(is, init);
                    }
                }
            } else {
                Files.write(init, initScript.getBytes());
            }
            customModelBuilder.withArguments(arguments);
            return OpenRewriteModel.from(customModelBuilder.get());
        } finally {
            try {
                if (Files.exists(init)) {
                    Files.delete(init);
                }
                if (settingsWritten) {
                    Files.deleteIfExists(settings);
                }
            } catch (IOException e) {
                //noinspection ThrowFromFinallyBlock
                throw new UncheckedIOException(e);
            }
        }
    }

    private static boolean isGradle9OrLater(@Nullable String version) {
        if (version == null) {
            return false;
        }
        int dot = version.indexOf('.');
        try {
            return Integer.parseInt(dot < 0 ? version : version.substring(0, dot)) >= 9;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static @Nullable String wrapperGradleVersion(Path wrapperProperties) {
        try {
            for (String line : Files.readAllLines(wrapperProperties, StandardCharsets.UTF_8)) {
                if (line.startsWith("distributionUrl=")) {
                    int idx = line.lastIndexOf("gradle-");
                    if (idx < 0) {
                        return null;
                    }
                    int start = idx + "gradle-".length();
                    int end = start;
                    while (end < line.length() && (Character.isDigit(line.charAt(end)) || line.charAt(end) == '.')) {
                        end++;
                    }
                    return end > start ? line.substring(start, end) : null;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * Everywhere except within openrewrite/rewrite itself the plugin will be resolved from some artifact repository
     * But within openrewrite/rewrite it comes from a build directory.
     * So for our build only use an init script which understands that.
     */
    private static String generateInitScriptFromManifest() throws IOException {
        String manifestPath = System.getProperty("org.openrewrite.gradle.local.use-embedded-classpath");
        if (manifestPath == null || manifestPath.isEmpty()) {
            throw new IllegalStateException("System property org.openrewrite.gradle.local.use-embedded-classpath must be set to the path of test-manifest.txt");
        }
        Path manifestFile = Paths.get(manifestPath);
        if (!Files.exists(manifestFile)) {
            throw new IllegalStateException("Manifest file not found at: " + manifestPath);
        }

        StringBuilder initScript = new StringBuilder();
        initScript.append("initscript {\n");
        initScript.append("    dependencies {\n");

        List<String> lines = Files.readAllLines(manifestFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                // Escape backslashes for Groovy string
                String escapedPath = line.replace("\\", "\\\\");
                initScript.append("        classpath files('").append(escapedPath).append("')\n");
            }
        }

        initScript.append("    }\n");
        initScript.append("}\n\n");
        initScript.append("allprojects {\n");
        initScript.append("    apply plugin: org.openrewrite.gradle.toolingapi.ToolingApiOpenRewriteModelPlugin\n");
        initScript.append("}\n");

        return initScript.toString();
    }
}
