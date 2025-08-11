/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.internal;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.maven.tree.MavenMetadata;

import java.time.ZonedDateTime;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class MavenMetadataTest {

    @Test
    void deserializeMetadata() throws Exception {
        @Language("xml") String metadata = """
          <metadata>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot</artifactId>
              <versioning>
                  <latest>2.4.2</latest>
                  <release>2.4.2</release>
                  <versions>
                      <version>2.4.1</version>
                      <version>2.4.2</version>
                  </versions>
                  <lastUpdated>20210115042754</lastUpdated>
              </versioning>
          </metadata>
          """;

        MavenMetadata parsed = MavenMetadata.parse(metadata.getBytes());
        MavenMetadata.Versioning versioning = parsed.getVersioning();
        assertThat(versioning.getVersions()).hasSize(2);
        assertThat(versioning.getLastUpdated()).isEqualTo(ZonedDateTime.of(2021, 1, 15, 4, 27, 54, 0, UTC));
        assertThat(versioning.getLatest()).isEqualTo("2.4.2");
        assertThat(versioning.getRelease()).isEqualTo("2.4.2");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void deserializeSnapshotMetadata() throws Exception {
        @Language("xml") String metadata = """
          <metadata modelVersion="1.1.0">
              <groupId>org.openrewrite.recipe</groupId>
              <artifactId>rewrite-recommendations</artifactId>
              <version>0.1.0-SNAPSHOT</version>
              <versioning>
                  <snapshot>
                      <timestamp>20220927.033510</timestamp>
                      <buildNumber>223</buildNumber>
                  </snapshot>
                  <snapshotVersions>
                      <snapshotVersion>
                          <extension>pom.asc</extension>
                          <value>0.1.0-20220927.033510-223</value>
                          <updated>20220927033510</updated>
                      </snapshotVersion>
                      <snapshotVersion>
                          <extension>pom</extension>
                          <value>0.1.0-20220927.033510-223</value>
                          <updated>20220927033510</updated>
                      </snapshotVersion>
                  </snapshotVersions>
              </versioning>
          </metadata>
          """;

        MavenMetadata parsed = MavenMetadata.parse(metadata.getBytes());
        MavenMetadata.Versioning versioning = parsed.getVersioning();

        assertThat(versioning.getLastUpdated()).isNull();
        assertThat(versioning.getSnapshot().getTimestamp()).isEqualTo("20220927.033510");
        assertThat(versioning.getSnapshot().getBuildNumber()).isEqualTo("223");
        assertThat(versioning.getVersions()).isNotNull();
        assertThat(versioning.getSnapshotVersions()).hasSize(2);
        assertThat(versioning.getSnapshotVersions().getFirst().getExtension()).isNotNull();
        assertThat(versioning.getSnapshotVersions().getFirst().getValue()).isNotNull();
        assertThat(versioning.getSnapshotVersions().getFirst().getUpdated()).isNotNull();
    }


    @Test
    @Issue("https://github.com/openrewrite/rewrite/pull/4285")
    void deserializeMetadataWithEmptyVersions() throws Exception {
        assertThat(MavenMetadata.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata/>\n".getBytes())).isNull();
    }
}
