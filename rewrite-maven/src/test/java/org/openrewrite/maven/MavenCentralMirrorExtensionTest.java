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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryCredentials;
import org.openrewrite.maven.tree.MavenRepositoryMirror;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_CENTRAL;

class MavenCentralMirrorExtensionTest {

    private static final Map<String, String> ENV = Map.of(
      "MAVEN_CENTRAL_MIRROR_URL", "https://mirror.example.com/maven2/",
      "MAVEN_CENTRAL_MIRROR_USERNAME", "user",
      "MAVEN_CENTRAL_MIRROR_PASSWORD", "secret");

    @Test
    void mainArtifactRegistersExtensionForAutodetection() {
        assertThat(ServiceLoader.load(Extension.class)).hasAtLeastOneElementOfType(MavenCentralMirrorExtension.class);
    }

    @Test
    void centralResolvesToMirrorWithItsCredentials() {
        assertCentralResolvesToMirror(MavenExecutionContextView.view(new InMemoryExecutionContext()));

        assertCentralResolvesToMirror(MavenExecutionContextView.view(new InMemoryExecutionContext())
          .setMavenSettings(new MavenSettings(null, null, null, null, null)));
    }

    @Test
    void keepsWhatTheContextSetBesideItsSettings() {
        MavenRepository localRepository = MavenRepository.builder().id("local").uri("file:///tmp/m2/").build();
        MavenRepository repository = MavenRepository.builder().id("company").uri("https://repo.example.com/maven2/").build();
        MavenRepositoryCredentials credentials = new MavenRepositoryCredentials("company", "name", "token");
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext())
          .setMavenSettings(new MavenSettings(null, null, null, null, null), "ci")
          .setLocalRepository(localRepository)
          .setRepositories(List.of(repository))
          .setCredentials(List.of(credentials));

        MavenCentralMirrorExtension.withMavenCentralMirror(ctx, ENV::get);

        assertThat(ctx.getActiveProfiles()).containsExactly("ci");
        assertThat(ctx.getLocalRepository()).isEqualTo(localRepository);
        assertThat(ctx.getRepositories()).containsExactly(repository);
        assertThat(ctx.getCredentials()).contains(credentials);
    }

    private static void assertCentralResolvesToMirror(MavenExecutionContextView ctx) {
        MavenCentralMirrorExtension.withMavenCentralMirror(ctx, ENV::get);
        // The two steps MavenPomDownloader takes for each repository
        MavenRepository central = MavenRepositoryCredentials.apply(ctx.getCredentials(ctx.getSettings()),
          MavenRepositoryMirror.apply(ctx.getMirrors(ctx.getSettings()), MAVEN_CENTRAL));
        assertThat(central.getUri()).isEqualTo("https://mirror.example.com/maven2/");
        assertThat(central.getUsername()).isEqualTo("user");
        assertThat(central.getPassword()).isEqualTo("secret");
    }
}
