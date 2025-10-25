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
package org.openrewrite.maven;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.xml.tree.Xml;
import org.opentest4j.AssertionFailedError;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_LOCAL_DEFAULT;

public class Assertions {
    private Assertions() {
    }

    static void customizeExecutionContext(ExecutionContext ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        boolean nothingConfigured = mctx.getSettings() == null &&
                                    mctx.getLocalRepository().equals(MAVEN_LOCAL_DEFAULT) &&
                                    mctx.getRepositories().isEmpty() &&
                                    mctx.getActiveProfiles().isEmpty() &&
                                    mctx.getMirrors().isEmpty();
        if (nothingConfigured) {
            // Load Maven settings to pick up any mirrors, proxies etc. that might be required in corporate environments
            mctx.setMavenSettings(MavenSettings.readMavenSettingsFromDisk(mctx));
        }
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before) {
        return pomXml(before, s -> {
        });
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", MavenParser.builder(), before,
                Assertions::pomResolvedSuccessfully, Assertions::customizeExecutionContext);
        maven.path("pom.xml");
        spec.accept(maven);
        return maven;
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before, @Language("xml") @Nullable String after) {
        return pomXml(before, after, s -> {
        });
    }

    public static SourceSpecs pomXml(@Language("xml") @Nullable String before, @Language("xml") @Nullable String after,
                                     Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", MavenParser.builder(), before,
                Assertions::pomResolvedSuccessfully, Assertions::customizeExecutionContext).after(s -> after);
        maven.path("pom.xml");
        spec.accept(maven);
        return maven;
    }

    private static SourceFile pomResolvedSuccessfully(SourceFile sourceFile, TypeValidation typeValidation) {
        if (typeValidation.dependencyModel()) {
            sourceFile.getMarkers()
                    .findFirst(ParseExceptionResult.class)
                    .ifPresent(parseExceptionResult -> fail("Problem parsing " + sourceFile.getSourcePath() + ":\n" + parseExceptionResult.getMessage()));
            sourceFile.getMarkers()
                    .findFirst(MavenResolutionResult.class)
                    .orElseThrow(() -> new AssertionFailedError("No MavenResolutionResult found on " + sourceFile.getSourcePath()));
        }
        return sourceFile;
    }

    /**
     * Publishes test POMs to the local Maven repository for use as parents or dependencies in tests,
     * executes the provided test code, then cleans up the published POMs.
     * This ensures the POMs are resolved as external dependencies rather than being parsed as part of the project.
     * WARNING: To avoid overwriting real POMs in your local repository, only use this method with
     * invented/test GAV coordinates. Avoid real coordinates like org.springframework.boot:spring-boot-starter-parent.
     *
     * @param pomXmls The POM XML content strings, each containing groupId, artifactId, and version elements
     * @param testCode The test code to execute after publishing the POMs
     * @throws IllegalArgumentException if any POM XML doesn't contain required GAV coordinates or uses a well-known groupId
     */
    @SuppressWarnings("ConstantValue")
    public static void withLocalRepository(@Language("xml") String[] pomXmls, Runnable testCode) {
        if (pomXmls.length == 0) {
            throw new IllegalArgumentException("At least one POM must be provided");
        }

        // List of well-known groupIds that should not be overwritten
        java.util.Set<String> protectedGroupIds = new java.util.HashSet<>(java.util.Arrays.asList(
                "org.springframework",
                "org.springframework.boot",
                "org.apache",
                "org.apache.maven",
                "org.junit",
                "com.google",
                "com.fasterxml",
                "jakarta",
                "javax",
                "io.quarkus",
                "org.eclipse",
                "org.jetbrains"
        ));

        java.util.List<Path> publishedFiles = new java.util.ArrayList<>();
        try {
            Path localRepo = Paths.get(System.getProperty("user.home"), ".m2", "repository");

            // Publish all POMs
            for (String pomXml : pomXmls) {
                RawPom rawPom = RawPom.parse(
                        new ByteArrayInputStream(pomXml.getBytes(UTF_8)),
                        null
                );
                Pom pom = rawPom.toPom(Paths.get("pom.xml"), null);

                String groupId = pom.getGroupId();
                String artifactId = pom.getArtifactId();
                String version = pom.getVersion();

                if (groupId == null || groupId.trim().isEmpty()) {
                    throw new IllegalArgumentException("POM XML must contain a groupId");
                }
                if (artifactId == null || artifactId.trim().isEmpty()) {
                    throw new IllegalArgumentException("POM XML must contain an artifactId");
                }
                if (version == null || version.trim().isEmpty()) {
                    throw new IllegalArgumentException("POM XML must contain a version");
                }

                // Check if the groupId is protected
                for (String protectedPrefix : protectedGroupIds) {
                    if (groupId.startsWith(protectedPrefix)) {
                        throw new IllegalArgumentException(
                                "Cannot use withLocalRepository with well-known groupId: " + groupId +
                                ". Please use test-specific coordinates like 'com.example.test' to avoid overwriting real POMs.");
                    }
                }

                String[] groupParts = groupId.split("\\.");
                Path pomDir = localRepo;
                for (String part : groupParts) {
                    pomDir = pomDir.resolve(part);
                }
                pomDir = pomDir.resolve(artifactId).resolve(version);

                Files.createDirectories(pomDir);

                Path pomFile = pomDir.resolve(artifactId + "-" + version + ".pom");
                Files.write(pomFile, pomXml.getBytes(UTF_8));
                publishedFiles.add(pomFile);
            }

            testCode.run();

        } catch (IOException e) {
            throw new RuntimeException("Failed to publish POM to local repository", e);
        } finally {
            for (Path file : publishedFiles) {
                try {
                    Files.deleteIfExists(file);
                    Path parent = file.getParent();
                    while (parent != null && !parent.equals(Paths.get(System.getProperty("user.home"), ".m2", "repository"))) {
                        try {
                            Files.deleteIfExists(parent);
                            parent = parent.getParent();
                        } catch (IOException ignored) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Warning: Failed to clean up test POM: " + file + " - " + e.getMessage());
                }
            }
        }
    }

    /**
     * Convenience overload that accepts a single POM.
     * @see #withLocalRepository(String[], Runnable)
     */
    public static void withLocalRepository(@Language("xml") String pomXml, Runnable testCode) {
        withLocalRepository(new String[]{pomXml}, testCode);
    }

    /**
     * Convenience overload that accepts two POMs.
     * @see #withLocalRepository(String[], Runnable)
     */
    public static void withLocalRepository(@Language("xml") String pomXml1, @Language("xml") String pomXml2, Runnable testCode) {
        withLocalRepository(new String[]{pomXml1, pomXml2}, testCode);
    }
}
