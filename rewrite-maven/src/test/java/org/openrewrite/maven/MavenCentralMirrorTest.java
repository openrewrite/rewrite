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
package org.openrewrite.maven;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.trait.MavenDependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the guarantee that a {@code <mirrorOf>central</mirrorOf>} (or {@code *}) mirror carried in an LST's
 * {@link MavenResolutionResult#getMavenSettings()} redirects all resolution traffic to the mirror,
 * including the implicitly added Maven Central, and that {@code repo.maven.apache.org} is never contacted.
 * <p>
 * In particular this guards the behavior fixed in <a href="https://github.com/openrewrite/rewrite/pull/4956">#4956</a>:
 * Maven settings supplied on the {@link ExecutionContext} at recipe run time must be merged with, not
 * replace, the settings captured in the LST at parse time.
 */
class MavenCentralMirrorTest {

    private static final String MIRROR_URL = "https://artifacts.example.com/maven2";

    @Language("xml")
    private static final String POM = """
      <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
      </project>
      """;

    @Language("xml")
    private static final String METADATA = """
      <metadata>
          <groupId>org.example.hermetic</groupId>
          <artifactId>example-lib</artifactId>
          <versioning>
              <latest>1.1</latest>
              <release>1.1</release>
              <versions>
                  <version>1.0</version>
                  <version>1.1</version>
              </versions>
          </versioning>
      </metadata>
      """;

    /**
     * Serves canned metadata for any {@code maven-metadata.xml} request and records every requested URL,
     * so tests can assert on which hosts resolution actually contacted without any real network access.
     */
    static class RecordingHttpSender implements HttpSender {
        final List<String> requestedUrls = new CopyOnWriteArrayList<>();

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            requestedUrls.add(url);
            byte[] body = url.endsWith("maven-metadata.xml") ? METADATA.getBytes(StandardCharsets.UTF_8) : new byte[0];
            return new Response(200, new ByteArrayInputStream(body), () -> {
            });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"central", "*"})
    void lstMirrorHonoredWhenContextSettingsHaveNoMirror(String mirrorOf) throws MavenDownloadingException {
        // At recipe run time settings are often supplied on the execution context, e.g. to carry
        // credentials. Their presence must not discard the mirror captured in the LST (#4956).
        MavenResolutionResult mrr = parsePomWith(settingsWithMirror(mirrorOf));
        ExecutionContext ctx = runContext();
        MavenExecutionContextView.view(ctx).setMavenSettings(settingsWithServerCredentialsOnly());

        String newerVersion = findNewerVersion(mrr, ctx);

        assertThat(newerVersion).isEqualTo("1.1");
        assertOnlyMirrorContacted(ctx);
    }

    @Test
    void lstMirrorHonoredWhenContextHasNoSettings() throws MavenDownloadingException {
        MavenResolutionResult mrr = parsePomWith(settingsWithMirror("central"));
        ExecutionContext ctx = runContext();

        String newerVersion = findNewerVersion(mrr, ctx);

        assertThat(newerVersion).isEqualTo("1.1");
        assertOnlyMirrorContacted(ctx);
    }

    @Test
    void implicitlyAddedCentralRedirectedToMirror() throws MavenDownloadingException {
        // Even when no repository is supplied at all, the downloader implicitly adds Maven Central;
        // that injected repository must also come out redirected to the mirror.
        ExecutionContext ctx = runContext();
        MavenPomDownloader downloader = new MavenPomDownloader(emptyMap(), ctx, settingsWithMirror("central"), null);

        MavenRepository normalizedCentral = downloader.normalizeRepository(
          MavenRepository.MAVEN_CENTRAL, MavenExecutionContextView.view(ctx), null);
        assertThat(normalizedCentral).isNotNull();
        assertThat(normalizedCentral.getUri()).startsWith(MIRROR_URL);
        assertThat(normalizedCentral.getId()).isEqualTo("internal-mirror");

        MavenMetadata metadata = downloader.downloadMetadata(
          new GroupArtifact("org.example.hermetic", "example-lib"), null, emptyList());

        assertThat(metadata.getVersioning().getVersions()).contains("1.0", "1.1");
        assertOnlyMirrorContacted(ctx);
    }

    private static MavenSettings settingsWithMirror(String mirrorOf) {
        //language=xml
        return requireNonNull(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"), """
          <settings>
              <mirrors>
                  <mirror>
                      <id>internal-mirror</id>
                      <url>%s</url>
                      <mirrorOf>%s</mirrorOf>
                  </mirror>
              </mirrors>
          </settings>
          """.formatted(MIRROR_URL, mirrorOf)), throwingContext()));
    }

    private static MavenSettings settingsWithServerCredentialsOnly() {
        //language=xml
        return requireNonNull(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"), """
          <settings>
              <servers>
                  <server>
                      <id>internal-mirror</id>
                      <username>ci</username>
                      <password>secret</password>
                  </server>
              </servers>
          </settings>
          """), throwingContext()));
    }

    /**
     * Parses a pom with the given settings on the parsing context, as an LST producer would, and returns
     * the resolution result marker carrying those settings.
     */
    private static MavenResolutionResult parsePomWith(MavenSettings settings) {
        ExecutionContext parseCtx = parseContext(settings);
        Xml.Document pom = (Xml.Document) MavenParser.builder().build()
          .parse(parseCtx, POM)
          .findFirst()
          .orElseThrow();
        RecordingHttpSender parseSender = (RecordingHttpSender)
          HttpSenderExecutionContextView.view(parseCtx).getHttpSender();
        assertThat(parseSender.requestedUrls).isEmpty();

        MavenResolutionResult mrr = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
        assertThat(requireNonNull(mrr.getMavenSettings()).getMirrors()).isNotNull();
        return mrr;
    }

    private static ExecutionContext parseContext(MavenSettings settings) {
        ExecutionContext parseCtx = runContext();
        MavenExecutionContextView.view(parseCtx).setMavenSettings(settings);
        return parseCtx;
    }

    private static ExecutionContext runContext() {
        ExecutionContext ctx = throwingContext();
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new RecordingHttpSender());
        // The local repository is consulted through the filesystem, invisible to the recording
        // sender; disable it so the developer's ~/.m2 cannot influence results.
        MavenExecutionContextView.view(ctx).setAddLocalRepository(false);
        return ctx;
    }

    private static InMemoryExecutionContext throwingContext() {
        return new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
    }

    private static @Nullable String findNewerVersion(MavenResolutionResult mrr, ExecutionContext ctx)
      throws MavenDownloadingException {
        VersionComparator latestRelease = requireNonNull(Semver.validate("latest.release", null).getValue());
        return MavenDependency.findNewerVersion(
          "org.example.hermetic", "example-lib", "1.0", mrr,
          new MavenMetadataFailures(Recipe.noop()), latestRelease, ctx);
    }

    private static void assertOnlyMirrorContacted(ExecutionContext ctx) {
        RecordingHttpSender sender = (RecordingHttpSender)
          HttpSenderExecutionContextView.view(ctx).getHttpSender();
        assertThat(sender.requestedUrls)
          .as("all resolution traffic must be redirected to the mirror")
          .allSatisfy(url -> assertThat(url).startsWith(MIRROR_URL))
          .as("at least one metadata request must have reached the mirror")
          .anySatisfy(url -> assertThat(url).endsWith("maven-metadata.xml"));
    }
}
