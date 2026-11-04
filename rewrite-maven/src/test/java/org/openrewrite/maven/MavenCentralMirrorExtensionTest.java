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

import java.util.Map;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_CENTRAL;

class MavenCentralMirrorExtensionTest {

    @Test
    void mainArtifactRegistersExtensionForAutodetection() {
        assertThat(ServiceLoader.load(Extension.class)).hasAtLeastOneElementOfType(MavenCentralMirrorExtension.class);
    }

    @Test
    void centralResolvesToMirrorWithItsCredentials() {
        Map<String, String> env = Map.of(
          "MAVEN_CENTRAL_MIRROR_URL", "https://mirror.example.com/maven2/",
          "MAVEN_CENTRAL_MIRROR_USERNAME", "user",
          "MAVEN_CENTRAL_MIRROR_PASSWORD", "secret");
        MavenExecutionContextView ctx = MavenCentralMirrorExtension.withMavenCentralMirror(
          MavenExecutionContextView.view(new InMemoryExecutionContext()), env::get);

        // The two steps MavenPomDownloader takes for each repository
        MavenRepository central = MavenRepositoryCredentials.apply(ctx.getCredentials(ctx.getSettings()),
          MavenRepositoryMirror.apply(ctx.getMirrors(ctx.getSettings()), MAVEN_CENTRAL));
        assertThat(central.getUri()).isEqualTo("https://mirror.example.com/maven2/");
        assertThat(central.getUsername()).isEqualTo("user");
        assertThat(central.getPassword()).isEqualTo("secret");
    }
}
