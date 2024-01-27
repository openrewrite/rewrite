/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MavenRepositoryMirrorTest {

    MavenRepositoryMirror one = new MavenRepositoryMirror("one", "https://one.org/m2", "*", true, true);
    MavenRepositoryMirror two = new MavenRepositoryMirror("two", "https://two.org/m2", "*", true, true);
    MavenRepository foo = new MavenRepository("foo", "https://foo.org/m2", "true", "true", null, null);

    @Test
    void useFirstMirror() {
        assertThat(MavenRepositoryMirror.apply(List.of(one, two), foo)).satisfies(repo -> {
            assertThat(repo.getId()).isEqualTo(one.getId());
            assertThat(repo.getUri()).isEqualTo(one.getUrl());
        });
    }

    @Test
    void matchById() {
        MavenRepositoryMirror oneMirror = new MavenRepositoryMirror("mirror", "https://mirror", "one", true, true);

        MavenRepository one = MavenRepository.builder()
          .id("one")
          .uri("https://one")
          .build();

        MavenRepository two = MavenRepository.builder()
          .id("two")
          .uri("https://two")
          .build();

        assertThat(oneMirror.apply(two))
          .isEqualTo(two);

        MavenRepository oneMirrored = oneMirror.apply(one);
        assertThat(oneMirrored).extracting(MavenRepository::getId).isEqualTo("mirror");
        assertThat(oneMirrored).extracting(MavenRepository::getUri).isEqualTo("https://mirror");
    }

    @Test
    void excludeFromWildcard() {
        MavenRepositoryMirror oneMirror = new MavenRepositoryMirror("mirror", "https://mirror", "*,!two", true, true);
        MavenRepository one = MavenRepository.builder()
          .id("one")
          .uri("https://one")
          .build();

        MavenRepository two = MavenRepository.builder()
          .id("two")
          .uri("https://two")
          .build();

        assertThat(oneMirror.apply(two))
          .isEqualTo(two);

        MavenRepository oneMirrored = oneMirror.apply(one);
        assertThat(oneMirrored).extracting(MavenRepository::getId).isEqualTo("mirror");
        assertThat(oneMirrored).extracting(MavenRepository::getUri).isEqualTo("https://mirror");
    }
}
