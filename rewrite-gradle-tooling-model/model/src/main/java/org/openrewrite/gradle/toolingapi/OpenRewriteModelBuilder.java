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

import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("JavadocLinkAsPlainText")
public class OpenRewriteModelBuilder {

    /**
     * Build an OpenRewriteModel for a project directory, using the default Gradle init script bundled within this jar.
     * The included init script downloads the model plugin at this jar's own version, with its rewrite dependencies, from
     * Maven Central, or from the mirror at {@code MAVEN_CENTRAL_MIRROR_URL} when it, {@code MAVEN_CENTRAL_MIRROR_USERNAME}
     * and {@code MAVEN_CENTRAL_MIRROR_PASSWORD} are all set. Snapshots come from https://artifacts.codegenomeproject.org/maven,
     * given {@code codegenomeUsername} and {@code codegenomePassword} in the Gradle user home's gradle.properties or as
     * {@code ORG_GRADLE_PROJECT_} environment variables, or {@code CODEGENOME_USERNAME} and {@code CODEGENOME_TOKEN} in
     * the environment.
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
        return forProjectDirectory(projectDir, buildFile, initScript, null);
    }

    /**
     * As {@link #forProjectDirectory(File, File, String)}, but builds in the given Gradle user home, like TestKit's
     * {@code GradleRunner.withTestKitDir}. Builds in separate homes don't wait on each other's cache locks.
     * Distributions stay in the default home.
     * <p>
     * A fresh home has none of the default home's {@code gradle.properties} or {@code init.d} scripts, so settings,
     * credentials and repository mirrors configured there don't apply.
     *
     * @param gradleUserHome The Gradle user home to build in, or null for the default one.
     */
    public static OpenRewriteModel forProjectDirectory(File projectDir, @Nullable File buildFile, @Nullable String initScript, @Nullable File gradleUserHome) throws IOException {
        DefaultGradleConnector connector = (DefaultGradleConnector) GradleConnector.newConnector();
        if (gradleUserHome != null) {
            connector.useGradleUserHomeDir(gradleUserHome);
            connector.useDistributionBaseDir(defaultGradleUserHome());
            // No other JVM reuses these daemons
            connector.daemonMaxIdleTime(2, TimeUnit.MINUTES);
        }
        Path wrapperProperties = projectDir.toPath().resolve("gradle/wrapper/gradle-wrapper.properties");
        String gradleVersion;
        if (Files.exists(wrapperProperties)) {
            gradleVersion = wrapperGradleVersion(wrapperProperties);
            connector.useBuildDistribution();
        } else {
            gradleVersion = System.getProperty("org.openrewrite.test.gradleVersion", "9.8.2");
            connector.useGradleVersion(gradleVersion);
        }
        List<String> arguments = new ArrayList<>();
        arguments.add("--init-script");
        Path init = projectDir.toPath().resolve("openrewrite-tooling.gradle").toAbsolutePath();
        arguments.add(init.toString());
        File buildRoot = projectDir;
        Path settings = null;
        boolean settingsWritten = false;
        if (buildFile != null && buildFile.exists()) {
            if (isGradle9OrLater(gradleVersion)) {
                // Gradle 9 dropped -b, which built from the build file's own directory; do the same, writing a
                // temporary settings.gradle with rootProject.buildFileName for build files elsewhere or named otherwise
                File abs = buildFile.getAbsoluteFile();
                boolean atConventionalLocation = abs.equals(new File(projectDir, "build.gradle").getAbsoluteFile()) ||
                        abs.equals(new File(projectDir, "build.gradle.kts").getAbsoluteFile());
                if (!atConventionalLocation) {
                    buildRoot = abs.getParentFile();
                    URI wrapperDistribution = wrapperDistributionUrl(wrapperProperties);
                    if (wrapperDistribution != null) {
                        // The build distribution is looked up from the build root, which no longer holds the wrapper
                        connector.useDistribution(wrapperDistribution);
                    }
                    Path projectPath = buildRoot.toPath();
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
        connector
                // Uncomment to hit breakpoints inside OpenRewriteModelBuilder in unit tests
                // Leaving commented out because the exact consequences of this internal API are unclear
                // .embedded(true)
                .forProjectDirectory(buildRoot);
        try (ProjectConnection connection = connector.connect()) {
            ModelBuilder<OpenRewriteModelProxy> customModelBuilder = connection.model(OpenRewriteModelProxy.class);
            String resolvedInitScript;
            String pluginVersion = null;
            if (initScript == null) {
                if (System.getProperty("org.openrewrite.gradle.local.use-embedded-classpath") != null) {
                    // code path only expected to be taken from within openrewrite/rewrite
                    resolvedInitScript = generateInitScriptFromManifest();
                } else {
                    Package modelPackage = OpenRewriteModel.class.getPackage();
                    pluginVersion = modelPackage == null ? null : modelPackage.getImplementationVersion();
                    resolvedInitScript = defaultInitScript(pluginVersion);
                }
            } else {
                resolvedInitScript = initScript;
            }
            Files.write(init, (resolvedInitScript + mirrorScriptSnippet()).getBytes(StandardCharsets.UTF_8));
            customModelBuilder.withArguments(arguments);
            try {
                return OpenRewriteModel.from(customModelBuilder.get());
            } catch (GradleConnectionException e) {
                throw pluginVersion == null ? e : explainMissingPlugin(e, pluginVersion);
            }
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

    private static final String PLUGIN = "org.openrewrite.gradle.tooling:plugin:";

    /**
     * The bundled init script, loading the plugin at the given version, so that the markers it builds are the ones this
     * jar reads. Without a version, as for a model loaded from a classes directory, it loads the newest plugin.
     */
    static String defaultInitScript(@Nullable String pluginVersion) throws IOException {
        try (InputStream is = OpenRewriteModel.class.getResourceAsStream("/init.gradle")) {
            if (is == null) {
                throw new IllegalStateException("Expected to find init.gradle on the classpath");
            }
            String script = new String(readAllBytes(is), StandardCharsets.UTF_8);
            return pluginVersion == null ? script : script.replace(PLUGIN + "latest.integration", PLUGIN + pluginVersion);
        }
    }

    static GradleConnectionException explainMissingPlugin(GradleConnectionException failure, String pluginVersion) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(PLUGIN + pluginVersion)) {
                return new GradleConnectionException("Could not load " + PLUGIN + pluginVersion + ", the version of " +
                        "this model. Its snapshots are published to https://artifacts.codegenomeproject.org/maven, which " +
                        "the init script uses when the Gradle user home's gradle.properties or ORG_GRADLE_PROJECT_ " +
                        "environment variables set codegenomeUsername and codegenomePassword, or the environment sets " +
                        "CODEGENOME_USERNAME and CODEGENOME_TOKEN.", failure);
            }
        }
        return failure;
    }

    /**
     * When the env vars {@code MAVEN_CENTRAL_MIRROR_URL}, {@code MAVEN_CENTRAL_MIRROR_USERNAME}, and
     * {@code MAVEN_CENTRAL_MIRROR_PASSWORD} are all set, returns a Groovy init-script fragment that
     * points the embedded Gradle build's Maven Central and Sonatype snapshot repositories at that
     * mirror instead, with its credentials. Other repositories stay as the build files declare them.
     * The Gradle plugin portal contains releases that Maven Central does not, so it must stay separate.
     * If any var is missing, returns the empty string.
     */
    private static String mirrorScriptSnippet() {
        return mirrorScriptSnippet(System.getenv("MAVEN_CENTRAL_MIRROR_URL"),
                System.getenv("MAVEN_CENTRAL_MIRROR_USERNAME"), System.getenv("MAVEN_CENTRAL_MIRROR_PASSWORD"));
    }

    static String mirrorScriptSnippet(@Nullable String url, @Nullable String user, @Nullable String pass) {
        if (url == null || user == null || pass == null || url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            return "";
        }
        return "\n\n" +
                "def __rewriteMirrorUrl = '" + escapeGroovy(url) + "'\n" +
                "def __rewriteMirrorUser = '" + escapeGroovy(user) + "'\n" +
                "def __rewriteMirrorPass = '" + escapeGroovy(pass) + "'\n" +
                // configureEach also reaches the repositories the build files add after these hooks run
                "def __rewriteUseMirror = { repositories ->\n" +
                "    repositories.withType(MavenArtifactRepository).configureEach { repo ->\n" +
                "        if (repo.url?.host in ['repo.maven.apache.org', 'repo1.maven.org',\n" +
                "                'central.sonatype.com', 'oss.sonatype.org', 's01.oss.sonatype.org']) {\n" +
                "            repo.url = __rewriteMirrorUrl\n" +
                "            repo.credentials {\n" +
                "                it.username = __rewriteMirrorUser\n" +
                "                it.password = __rewriteMirrorPass\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "try {\n" +
                "    gradle.beforeSettings { settings ->\n" +
                "        __rewriteUseMirror(settings.pluginManagement.repositories)\n" +
                "        try { __rewriteUseMirror(settings.buildscript.repositories) } catch (Throwable ignored) {}\n" +
                "        try { __rewriteUseMirror(settings.dependencyResolutionManagement.repositories) } catch (Throwable ignored) {}\n" +
                "    }\n" +
                "} catch (Throwable ignored) {}\n" +
                "gradle.beforeProject { project ->\n" +
                "    __rewriteUseMirror(project.buildscript.repositories)\n" +
                "    __rewriteUseMirror(project.repositories)\n" +
                "}\n";
    }

    private static String escapeGroovy(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
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

    private static @Nullable URI wrapperDistributionUrl(Path wrapperProperties) {
        if (!Files.exists(wrapperProperties)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(wrapperProperties)) {
            properties.load(in);
        } catch (IOException e) {
            return null;
        }
        String distributionUrl = properties.getProperty("distributionUrl");
        if (distributionUrl == null) {
            return null;
        }
        URI uri = URI.create(distributionUrl);
        return uri.isAbsolute() ? uri : wrapperProperties.getParent().toUri().resolve(uri);
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

    private static File defaultGradleUserHome() {
        String home = System.getProperty("gradle.user.home");
        if (home == null) {
            home = System.getenv("GRADLE_USER_HOME");
        }
        return home == null ? new File(System.getProperty("user.home"), ".gradle") : new File(home);
    }

    /**
     * Everywhere except within openrewrite/rewrite itself the plugin will be resolved from some artifact repository
     * But within openrewrite/rewrite it comes from a build directory.
     * So for our build only use an init script which understands that.
     */
    static String generateInitScriptFromManifest() throws IOException {
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
