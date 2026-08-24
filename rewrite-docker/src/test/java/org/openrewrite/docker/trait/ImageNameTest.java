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
package org.openrewrite.docker.trait;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ImageNameTest {

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
      "ubuntu,                                    null,               null,             ubuntu",
      "library/ubuntu,                            null,               library,          ubuntu",
      "myuser/myimage,                            null,               myuser,           myimage",
      "docker.io/library/ubuntu,                  docker.io,          library,          ubuntu",
      "index.docker.io/library/ubuntu,            index.docker.io,    library,          ubuntu",
      "mcr.microsoft.com/windows/servercore,      mcr.microsoft.com,  windows,          servercore",
      "mcr.microsoft.com/dotnet/framework/sdk,    mcr.microsoft.com,  dotnet/framework, sdk",
      "registry.example.com:5000/app,             registry.example.com:5000, null,      app",
      "localhost/app,                             localhost,          null,             app",
      "localhost:5000/team/app,                   localhost:5000,     team,             app",
    })
    void decompose(String name, @Nullable String registry,
                   @Nullable String namespace, String repository) {
        ImageName parsed = ImageName.parse(name);
        assertThat(parsed.getRegistry()).isEqualTo(registry);
        assertThat(parsed.getNamespace()).isEqualTo(namespace);
        assertThat(parsed.getRepository()).isEqualTo(repository);
        assertThat(parsed).hasToString(name);
    }

    @Test
    void firstComponentWithoutDotIsAnOrganizationNotARegistry() {
        ImageName parsed = ImageName.parse("redhat/ubi9-minimal");
        assertThat(parsed.getRegistry()).isNull();
        assertThat(parsed.getNamespace()).isEqualTo("redhat");
        assertThat(parsed.getResolvedRegistry()).isEqualTo("docker.io");
    }

    @ParameterizedTest
    @ValueSource(strings = {"${REGISTRY}", "$REGISTRY"})
    void aVariableInTheFirstComponentIsARegistry(String variable) {
        ImageName parsed = ImageName.parse(variable + "/app");
        assertThat(parsed.getRegistry()).isEqualTo(variable);
        assertThat(parsed.getRepository()).isEqualTo("app");
        assertThat(parsed.isDockerHub()).isFalse();
    }

    @Test
    void aVariableAsTheWholeNameIsARepository() {
        ImageName parsed = ImageName.parse("${BASE_IMAGE}");
        assertThat(parsed.getRegistry()).isNull();
        assertThat(parsed.getRepository()).isEqualTo("${BASE_IMAGE}");
        assertThat(parsed.getResolvedRegistry()).isEqualTo("docker.io");
    }

    @ParameterizedTest
    @CsvSource({
      "ubuntu,                                ubuntu,                               docker.io/library/ubuntu",
      "library/ubuntu,                        ubuntu,                               docker.io/library/ubuntu",
      "docker.io/library/ubuntu,              ubuntu,                               docker.io/library/ubuntu",
      "index.docker.io/library/ubuntu,        ubuntu,                               docker.io/library/ubuntu",
      "registry.hub.docker.com/library/nginx, nginx,                                docker.io/library/nginx",
      "registry-1.docker.io/library/python,   python,                               docker.io/library/python",
      "myuser/myimage,                        myuser/myimage,                       docker.io/myuser/myimage",
      "docker.io/myuser/myimage,              myuser/myimage,                       docker.io/myuser/myimage",
      "redhat/ubi9-minimal,                   redhat/ubi9-minimal,                  docker.io/redhat/ubi9-minimal",
      "gcr.io/myproject/myimage,              gcr.io/myproject/myimage,             gcr.io/myproject/myimage",
      "mcr.microsoft.com/windows/servercore,  mcr.microsoft.com/windows/servercore, mcr.microsoft.com/windows/servercore",
      "my.private.registry.com/myimage,       my.private.registry.com/myimage,      my.private.registry.com/myimage",
    })
    void familiarAndCanonicalSpellings(String name, String familiar, String canonical) {
        ImageName parsed = ImageName.parse(name);
        assertThat(parsed.getFamiliar()).isEqualTo(familiar);
        assertThat(parsed.getCanonical()).isEqualTo(canonical);
    }

    @Test
    void everySpellingOfAnOfficialImageSharesACanonicalForm() {
        assertThat(ImageName.parse("ubuntu").getCanonical())
          .isEqualTo(ImageName.parse("library/ubuntu").getCanonical())
          .isEqualTo(ImageName.parse("docker.io/library/ubuntu").getCanonical())
          .isEqualTo(ImageName.parse("index.docker.io/library/ubuntu").getCanonical());
    }

    @ParameterizedTest
    @ValueSource(strings = {"docker.io", "index.docker.io", "registry.hub.docker.com", "registry-1.docker.io"})
    void dockerHubAliases(String registry) {
        assertThat(ImageName.parse(registry + "/library/ubuntu").isDockerHub()).isTrue();
    }

    @Test
    void otherRegistriesAreNotDockerHub() {
        assertThat(ImageName.parse("gcr.io/library/ubuntu").isDockerHub()).isFalse();
    }
}
