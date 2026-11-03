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
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("JavadocLinkAsPlainText")
public class OpenRewriteModelBuilder {

    /**
     * Build an OpenRewriteModel for a project directory, using the default Gradle init script bundled within this jar.
     * The included init script accesses public artifact repositories (Maven Central, Nexus Snapshots) to be able to
     * download rewrite dependencies, so public repositories must be accessible for this to work. If
     * {@code MAVEN_CENTRAL_MIRROR_URL}, {@code MAVEN_CENTRAL_MIRROR_USERNAME} and
     * {@code MAVEN_CENTRAL_MIRROR_PASSWORD} are all set, it downloads them from that mirror instead.
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
        ForkGradleUserHome.configure(connector);
        connector
                // Uncomment to hit breakpoints inside OpenRewriteModelBuilder in unit tests
                // Leaving commented out because the exact consequences of this internal API are unclear
                // .embedded(true)
                .forProjectDirectory(buildRoot);
        try (ProjectConnection connection = connector.connect()) {
            ModelBuilder<OpenRewriteModelProxy> customModelBuilder = connection.model(OpenRewriteModelProxy.class);
            String resolvedInitScript;
            if (initScript == null) {
                if (System.getProperty("org.openrewrite.gradle.local.use-embedded-classpath") != null) {
                    // code path only expected to be taken from within openrewrite/rewrite
                    resolvedInitScript = generateInitScriptFromManifest();
                } else {
                    // Use default init.gradle from resources
                    try (InputStream is = OpenRewriteModel.class.getResourceAsStream("/init.gradle")) {
                        if (is == null) {
                            throw new IllegalStateException("Expected to find init.gradle on the classpath");
                        }
                        byte[] bytes = readAllBytes(is);
                        resolvedInitScript = new String(bytes, StandardCharsets.UTF_8);
                    }
                }
            } else {
                resolvedInitScript = initScript;
            }
            Files.write(init, (resolvedInitScript + mirrorScriptSnippet()).getBytes(StandardCharsets.UTF_8));
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

    /**
     * When the env vars {@code MAVEN_CENTRAL_MIRROR_URL}, {@code MAVEN_CENTRAL_MIRROR_USERNAME}, and
     * {@code MAVEN_CENTRAL_MIRROR_PASSWORD} are all set, returns a Groovy init-script fragment that
     * points the embedded Gradle build's Maven Central, Sonatype snapshot and Gradle plugin portal
     * repositories at that mirror instead, with its credentials, including the portal Gradle adds on
     * its own when a build names no plugin repository. Other repositories stay as the build files
     * declare them. If any var is missing, returns the empty string.
     */
    private static String mirrorScriptSnippet() {
        String url = System.getenv("MAVEN_CENTRAL_MIRROR_URL");
        String user = System.getenv("MAVEN_CENTRAL_MIRROR_USERNAME");
        String pass = System.getenv("MAVEN_CENTRAL_MIRROR_PASSWORD");
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
                "        if (repo.url?.host in ['repo.maven.apache.org', 'repo1.maven.org', 'plugins.gradle.org',\n" +
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
                // When a build names no plugin repository, Gradle adds the portal itself, where the hook above
                // can't see it; naming it here lets the hook point it at the mirror
                "    gradle.settingsEvaluated { settings ->\n" +
                "        if (settings.pluginManagement.repositories.isEmpty()) {\n" +
                "            settings.pluginManagement.repositories.gradlePluginPortal()\n" +
                "        }\n" +
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

    /**
     * A Gradle user home of this test JVM's own, whose script and transform caches, and the locks on them, only this
     * JVM's daemons use. It links to the default home's dependency cache, and the connector keeps distributions in the
     * default home, so all forks share their downloads. It has copies of the default home's settings and init scripts.
     * Only openrewrite/rewrite's own tests set the property, and their test task clears the homes when it starts.
     */
    static final class ForkGradleUserHome {
        private static final Path DEFAULT_HOME = defaultGradleUserHome();
        private static final @Nullable File HOME = forThisJvm();

        static void configure(DefaultGradleConnector connector) {
            if (HOME != null) {
                connector.useGradleUserHomeDir(HOME);
                connector.useDistributionBaseDir(DEFAULT_HOME.toFile());
                // Nothing reuses these daemons once the JVM's tests are done
                connector.daemonMaxIdleTime(3, TimeUnit.MINUTES);
            }
        }

        private static @Nullable File forThisJvm() {
            String homes = System.getProperty("org.openrewrite.gradle.local.gradle-user-homes");
            if (homes == null) {
                return null;
            }
            String name = System.getProperty("org.gradle.test.worker", ManagementFactory.getRuntimeMXBean().getName());
            try {
                Path home = create(Paths.get(homes), name, DEFAULT_HOME);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> delete(home)));
                return home.toFile();
            } catch (IOException | RuntimeException e) {
                // Where the link can't be made, such as on Windows without the privilege, builds use the default home
                return null;
            }
        }

        static Path create(Path homes, String name, Path defaultHome) throws IOException {
            Path home = homes.resolve(name);
            Path dependencyCache = Files.createDirectories(defaultHome.resolve("caches/modules-2"));
            Path link = home.resolve("caches/modules-2");
            Files.createDirectories(link.getParent());
            // A home left behind by an earlier JVM with the same worker name already has its link
            if (!Files.isSymbolicLink(link)) {
                Files.createSymbolicLink(link, dependencyCache);
            }
            // Test builds get the proxy settings, credentials and mirror scripts every build in the default home gets
            copySettings(defaultHome, home);
            return home;
        }

        private static void copySettings(Path defaultHome, Path home) throws IOException {
            Path properties = defaultHome.resolve("gradle.properties");
            if (Files.isRegularFile(properties)) {
                Files.copy(properties, home.resolve("gradle.properties"), StandardCopyOption.REPLACE_EXISTING);
            }
            Path initScripts = defaultHome.resolve("init.d");
            if (Files.isDirectory(initScripts)) {
                Path copies = Files.createDirectories(home.resolve("init.d"));
                try (DirectoryStream<Path> scripts = Files.newDirectoryStream(initScripts)) {
                    for (Path script : scripts) {
                        if (Files.isRegularFile(script)) {
                            Files.copy(script, copies.resolve(script.getFileName().toString()),
                                    StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        }

        /**
         * Deletes as much of the home as it can, as a daemon can still be writing to it. It doesn't follow the home's
         * link, so the default home's dependency cache stays intact.
         */
        static void delete(Path home) {
            try {
                Files.walkFileTree(home, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        deleteIfPossible(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException e) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException e) {
                        deleteIfPossible(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {
                // Only the visitor's own exceptions surface here, and it throws none
            }
        }

        private static void deleteIfPossible(Path path) {
            try {
                Files.delete(path);
            } catch (IOException ignored) {
            }
        }

        private static Path defaultGradleUserHome() {
            String home = System.getProperty("gradle.user.home");
            if (home == null) {
                home = System.getenv("GRADLE_USER_HOME");
            }
            return home == null ? Paths.get(System.getProperty("user.home"), ".gradle") : Paths.get(home);
        }
    }
}
