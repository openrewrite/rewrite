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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.test.MockHttpSender;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.openrewrite.Tree.randomId;

/**
 * A recipe instance and its accumulator are shared across all source files of a run, and schedulers
 * such as the Moderne CLI ({@code mod run --parallel}) visit source files of the same repository
 * concurrently. The {@code updatedRequested}/{@code updatedResolved} memo caches of
 * {@link ChangeDependency} are therefore accessed by multiple threads at once; with a plain
 * {@link java.util.HashMap}, a {@code put} performed by one thread while another thread is still inside
 * {@code computeIfAbsent}'s mapping function (which performs a Maven metadata download, so it is open
 * for a long time) fails the map's modCount check and throws
 * {@link java.util.ConcurrentModificationException} from {@code updateGradleModel}.
 * <p>
 * This test mirrors how {@code RecipeRunCycle} drives a run: a fresh visitor per source file, all
 * sharing one recipe instance and one root cursor (through which {@code ScanningRecipe} resolves the
 * shared accumulator).
 */
class ChangeDependencyConcurrencyTest {

    private static final String BUILD_FILE = """
      dependencies {
          implementation "commons-lang:commons-lang"
      }
      """;

    private static final String COMMONS_LANG3_METADATA = """
      <metadata>
          <groupId>org.apache.commons</groupId>
          <artifactId>commons-lang3</artifactId>
          <versioning>
              <latest>3.14.0</latest>
              <release>3.14.0</release>
              <versions>
                  <version>3.14.0</version>
              </versions>
              <lastUpdated>20240101000000</lastUpdated>
          </versioning>
      </metadata>
      """;

    @Timeout(60)
    @Test
    void concurrentVisitsSharingOneRecipeInstanceMustNotThrowConcurrentModificationException() throws Exception {
        // One recipe instance for the whole run, exactly as RecipeRunCycle uses it
        ChangeDependency recipe = new ChangeDependency(
          "commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.x", null, null, false);

        GroovyParser.Builder groovyParser = GroovyParser.builder();
        // The two files resolve different versions of the old dependency so that their map keys differ:
        // with a properly concurrent cache both threads may compute their entries independently.
        SourceFile fileA = parseBuildGradle(groovyParser, "a/build.gradle", "2.5");
        SourceFile fileB = parseBuildGradle(groovyParser, "b/build.gradle", "2.6");

        Cursor rootCursor = new Cursor(null, Cursor.ROOT_VALUE);
        CountDownLatch aInsideMappingFunction = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);

        // Thread A's metadata download parks inside updateGradleModel's computeIfAbsent mapping function,
        // simulating a slow download while other files of the same run are being visited.
        ExecutionContext ctxA = withMetadataOverHttp(request -> {
            aInsideMappingFunction.countDown();
            try {
                if (!releaseA.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Thread A was never released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return metadataResponse(request);
        });
        ExecutionContext ctxB = withMetadataOverHttp(ChangeDependencyConcurrencyTest::metadataResponse);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Object> visitA = executor.submit(() -> visit(recipe, fileA, ctxA, rootCursor));
            assertThat(aInsideMappingFunction.await(30, TimeUnit.SECONDS))
              .as("thread A should reach the metadata download inside updateGradleModel")
              .isTrue();

            // While thread A is still inside the mapping function, visit another file of the same
            // run to completion; it inserts into the shared updatedRequested/updatedResolved caches.
            Object resultB;
            try {
                resultB = visit(recipe, fileB, ctxB, rootCursor);
            } finally {
                releaseA.countDown();
            }

            assertThatCode(visitA::get)
              .as("concurrent per-file visits sharing one ChangeDependency instance")
              .doesNotThrowAnyException();

            assertThat(((SourceFile) visitA.get()).printAll()).contains("org.apache.commons:commons-lang3");
            assertThat(((SourceFile) resultB).printAll()).contains("org.apache.commons:commons-lang3");
        } finally {
            releaseA.countDown();
            executor.shutdownNow();
        }
    }

    private static Object visit(ChangeDependency recipe, SourceFile sourceFile, ExecutionContext ctx, Cursor rootCursor) {
        // RecipeRunCycle obtains a fresh visitor per source file; the recipe instance and the root
        // cursor (through which the ScanningRecipe accumulator is resolved) are what is shared
        TreeVisitor<?, ExecutionContext> visitor = recipe.getVisitor();
        visitor.setCursor(rootCursor);
        return visitor.visit(sourceFile, ctx, rootCursor);
    }

    private static SourceFile parseBuildGradle(GroovyParser.Builder groovyParser, String path, String resolvedVersion) {
        Dependency requested = Dependency.builder()
          .gav(new GroupArtifactVersion("commons-lang", "commons-lang", null))
          .scope("implementation")
          .build();
        ResolvedDependency resolved = ResolvedDependency.builder()
          .repository(MavenRepository.MAVEN_CENTRAL)
          .gav(new ResolvedGroupArtifactVersion(null, "commons-lang", "commons-lang", resolvedVersion, null))
          .requested(requested)
          .depth(0)
          .build();
        GradleDependencyConfiguration implementation = GradleDependencyConfiguration.builder()
          .name("implementation")
          .isTransitive(true)
          .isCanBeResolved(true)
          .extendsFrom(emptyList())
          .requested(singletonList(requested))
          .directResolved(singletonList(resolved))
          .build();
        GradleProject gradleProject = GradleProject.builder()
          .group("com.example")
          .name("example")
          .version("1.0")
          .path(":")
          .mavenRepositories(singletonList(MavenRepository.MAVEN_CENTRAL))
          .nameToConfiguration(singletonMap("implementation", implementation))
          .build();
        return GradleParser.builder().groovyParser(groovyParser).build()
          .parse(new InMemoryExecutionContext(), BUILD_FILE)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Failed to parse build.gradle"))
          .withSourcePath(Paths.get(path))
          .withMarkers(new Markers(randomId(), singletonList(gradleProject)));
    }

    private static ExecutionContext withMetadataOverHttp(HttpSender sender) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        HttpSenderExecutionContextView.view(ctx).setHttpSender(sender);
        return ctx;
    }

    private static HttpSender.Response metadataResponse(HttpSender.Request request) {
        if (!request.getUrl().toString().endsWith("maven-metadata.xml")) {
            throw new IllegalStateException("Unexpected request: " + request.getUrl());
        }
        return new MockHttpSender(() -> new ByteArrayInputStream(COMMONS_LANG3_METADATA.getBytes(UTF_8))).send(request);
    }
}
