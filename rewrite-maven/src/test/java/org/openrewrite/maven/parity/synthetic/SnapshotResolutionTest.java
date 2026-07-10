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
package org.openrewrite.maven.parity.synthetic;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scope;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomPath;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomXml;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.dependencies;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * Dated-snapshot resolution from {@code maven-metadata.xml}: classifier-aware
 * {@code <snapshotVersions>} selection, the {@code <snapshot>} timestamp/buildNumber fallback,
 * context-pinned snapshot versions, and per-repository snapshot policy (a2 §1.4). The
 * dated-projection tests are LEGACY-pinned: the engine's {@code DependencyGraphMapper} threads
 * the base version as {@code datedSnapshotVersion}, so the timestamped form is a legacy
 * observable. Policy skipping holds on the engine and is asserted
 * engine-side; the engine's pinned-snapshot short-circuit is pinned by
 * {@code EngineDependencyCollectorTest}.
 */
class SnapshotResolutionTest {
    private static final String G = "org.parity.synthetic";
    private static final String METADATA_PATH = "/org/parity/synthetic/snap/1.0-SNAPSHOT/maven-metadata.xml";

    //language=xml
    private static final String METADATA_WITH_SNAPSHOT_VERSIONS = """
      <metadata modelVersion="1.1.0">
          <groupId>org.parity.synthetic</groupId>
          <artifactId>snap</artifactId>
          <version>1.0-SNAPSHOT</version>
          <versioning>
              <snapshot>
                  <timestamp>20260101.010101</timestamp>
                  <buildNumber>3</buildNumber>
              </snapshot>
              <lastUpdated>20260101010101</lastUpdated>
              <snapshotVersions>
                  <snapshotVersion>
                      <extension>pom</extension>
                      <value>1.0-20260101.010101-3</value>
                      <updated>20260101010101</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                      <classifier>tests</classifier>
                      <extension>jar</extension>
                      <value>1.0-20251231.235959-2</value>
                      <updated>20251231235959</updated>
                  </snapshotVersion>
              </snapshotVersions>
          </versioning>
      </metadata>
      """;

    private static ResolvedDependency single(SyntheticHarness.Resolution resolution) {
        List<ResolvedDependency> compile = resolution.marker().getDependencies().get(Scope.Compile);
        assertThat(compile).hasSize(1);
        return compile.get(0);
    }

    @Test
    void versionStaysBaseWhileDatedSnapshotVersionCarriesTimestamp() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve(METADATA_PATH, METADATA_WITH_SNAPSHOT_VERSIONS);
            repo.serve(pomPath(G, "snap", "1.0-SNAPSHOT", "1.0-20260101.010101-3"), pomXml(G, "snap", "1.0-SNAPSHOT"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":snap:1.0-SNAPSHOT")),
              SyntheticHarness.legacyPinned(ctx -> ctx.setRepositories(List.of(repo.repo("snapshots")))));

            ResolvedGroupArtifactVersion gav = single(resolution).getGav();
            assertThat(gav.getVersion()).isEqualTo("1.0-SNAPSHOT");
            assertThat(gav.getDatedSnapshotVersion()).isEqualTo("1.0-20260101.010101-3");
            // Metadata and pom fetched once; later scopes hit the cache
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests()).containsExactly(
                  "GET " + METADATA_PATH,
                  "GET " + pomPath(G, "snap", "1.0-SNAPSHOT", "1.0-20260101.010101-3"));
            }
            assertThat(resolution.snapshot().getJson().at("/scopes/Compile/0/dated").asText())
              .isEqualTo("1.0-<ts>");
        }
    }

    @Test
    void snapshotVersionSelectedByClassifier() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve(METADATA_PATH, METADATA_WITH_SNAPSHOT_VERSIONS);
            repo.serve(pomPath(G, "snap", "1.0-SNAPSHOT", "1.0-20251231.235959-2"), pomXml(G, "snap", "1.0-SNAPSHOT"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":snap:1.0-SNAPSHOT:tests")),
              SyntheticHarness.legacyPinned(ctx -> ctx.setRepositories(List.of(repo.repo("snapshots")))));

            ResolvedDependency dependency = single(resolution);
            assertThat(dependency.getClassifier()).isEqualTo("tests");
            // The classifier's own (older) snapshotVersion wins over the newest unclassified one
            assertThat(dependency.getGav().getDatedSnapshotVersion()).isEqualTo("1.0-20251231.235959-2");
        }
    }

    @Test
    void snapshotElementFallbackWhenNoMatchingSnapshotVersion() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            //language=xml
            repo.serve(METADATA_PATH, """
              <metadata modelVersion="1.1.0">
                  <groupId>org.parity.synthetic</groupId>
                  <artifactId>snap</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <versioning>
                      <snapshot>
                          <timestamp>20260101.010101</timestamp>
                          <buildNumber>3</buildNumber>
                      </snapshot>
                      <lastUpdated>20260101010101</lastUpdated>
                  </versioning>
              </metadata>
              """);
            repo.serve(pomPath(G, "snap", "1.0-SNAPSHOT", "1.0-20260101.010101-3"), pomXml(G, "snap", "1.0-SNAPSHOT"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":snap:1.0-SNAPSHOT")),
              SyntheticHarness.legacyPinned(ctx -> ctx.setRepositories(List.of(repo.repo("snapshots")))));

            assertThat(single(resolution).getGav().getDatedSnapshotVersion()).isEqualTo("1.0-20260101.010101-3");
        }
    }

    @Test
    void pinnedSnapshotVersionWinsWithoutMetadataRequest() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve(pomPath(G, "snap", "1.0-SNAPSHOT", "1.0-20260201.000000-9"), pomXml(G, "snap", "1.0-SNAPSHOT"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":snap:1.0-SNAPSHOT")),
              SyntheticHarness.legacyPinned(ctx -> {
                  ctx.setRepositories(List.of(repo.repo("snapshots")));
                  ctx.setPinnedSnapshotVersions(List.of(new ResolvedGroupArtifactVersion(
                    null, G, "snap", "1.0-SNAPSHOT", "1.0-20260201.000000-9")));
              }));

            assertThat(single(resolution).getGav().getDatedSnapshotVersion()).isEqualTo("1.0-20260201.000000-9");
            // The metadata is never requested (pinned) in either mode; the exact single-request log is legacy-scoped.
            assertThat(repo.requests()).noneMatch(r -> r.contains("maven-metadata"));
            if (!SyntheticHarness.shadowMode()) {
                assertThat(repo.requests())
                  .containsExactly("GET " + pomPath(G, "snap", "1.0-SNAPSHOT", "1.0-20260201.000000-9"));
            }
        }
    }

    @Test
    void snapshotsDisabledRepositoryIsSkippedWithoutRequests() {
        try (MockMavenRepo disabled = new MockMavenRepo(); MockMavenRepo enabled = new MockMavenRepo()) {
            enabled.serve(METADATA_PATH, METADATA_WITH_SNAPSHOT_VERSIONS);
            enabled.serve(pomPath(G, "snap", "1.0-SNAPSHOT", "1.0-20260101.010101-3"), pomXml(G, "snap", "1.0-SNAPSHOT"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":snap:1.0-SNAPSHOT")),
              ctx -> ctx.setRepositories(List.of(
                MavenRepository.builder().id("releases-only").uri(disabled.url()).snapshots(false).knownToExist(true).build(),
                enabled.repo("snapshots"))));

            assertThat(resolution.failed()).isFalse();
            assertThat(disabled.requests()).isEmpty();
            // The enabled repository resolved the snapshot; the dated projection itself is pinned
            // legacy-side above (the engine threads the base version)
            assertThat(enabled.requests()).contains("GET " + METADATA_PATH);
            assertThat(single(resolution).getGav().getVersion()).isEqualTo("1.0-SNAPSHOT");
        }
    }
}
