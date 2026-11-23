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
package org.openrewrite.maven.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.tree.MavenRepository;

import java.net.URI;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MavenRepositoryRemoteAuthenticatorTest {

    private static final MavenRepository MIRROR = MavenRepository.builder()
      .id("corporate")
      .uri("https://repo.example.com/maven2")
      .username("user")
      .password("secret")
      .build();

    @ParameterizedTest
    @ValueSource(strings = {
      "https://repo.example.com/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip",
      "https://REPO.example.com/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip",
      "https://repo.example.com:443/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
    })
    void appliesToUrisUnderTheRepository(String uri) {
        // given
        MavenRepositoryRemoteAuthenticator authenticator = MavenRepositoryRemoteAuthenticator.forRepository(MIRROR, null);

        // when
        boolean applies = authenticator.appliesTo(URI.create(uri));

        // then
        assertThat(applies).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip",
      "http://repo.example.com/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip",
      "https://repo.example.com:8443/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip",
      "https://repo.example.com/maven2-snapshots/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip",
      "https://repo.example.com/other/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip",
      "https://repo.example.com.evil.example/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
    })
    void doesNotApplyToOtherUris(String uri) {
        // given
        MavenRepositoryRemoteAuthenticator authenticator = MavenRepositoryRemoteAuthenticator.forRepository(MIRROR, null);

        // when
        boolean applies = authenticator.appliesTo(URI.create(uri));

        // then
        assertThat(applies).isFalse();
    }

    @Test
    void addsHttpHeadersAndBasicAuthentication() {
        // given
        MavenRepositoryRemoteAuthenticator authenticator = MavenRepositoryRemoteAuthenticator.forRepository(MIRROR, settings(
          //language=xml
          """
            <settings>
              <servers>
                <server>
                  <id>corporate</id>
                  <configuration>
                    <httpHeaders>
                      <property>
                        <name>Private-Token</name>
                        <value>abc</value>
                      </property>
                      <property>
                        <name>Unresolved</name>
                        <value>${env.UNSET_TOKEN}</value>
                      </property>
                    </httpHeaders>
                  </configuration>
                </server>
              </servers>
            </settings>
            """));
        HttpSender httpSender = new HttpUrlConnectionSender();
        URI uri = URI.create("https://repo.example.com/maven2/file.zip");

        // when
        HttpSender.Request request = authenticator.authenticate(uri, httpSender.get(uri.toString())).build();

        // then
        assertThat(request.getRequestHeaders())
          .containsEntry("Private-Token", "abc")
          .containsEntry("Authorization", "Basic dXNlcjpzZWNyZXQ=")
          .doesNotContainKey("Unresolved");
    }

    @Test
    void notCreatedWithoutAnythingToSend() {
        // given
        MavenRepository unresolvedCredentials = MIRROR.withUsername("${env.UNSET_USER}");

        // when
        MavenRepositoryRemoteAuthenticator authenticator = MavenRepositoryRemoteAuthenticator.forRepository(unresolvedCredentials, null);

        // then
        assertThat(authenticator).isNull();
    }

    private static MavenSettings settings(String xml) {
        return MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"), xml), new InMemoryExecutionContext());
    }
}
