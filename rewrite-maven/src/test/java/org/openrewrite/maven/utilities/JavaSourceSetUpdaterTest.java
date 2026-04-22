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
package org.openrewrite.maven.utilities;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceSetUpdaterTest {

    @Test
    void changeDependencyMarksSourceSetDirty() {
        JavaSourceSetUpdater updater = new JavaSourceSetUpdater(new InMemoryExecutionContext());
        JavaSourceSet clean = new JavaSourceSet(Tree.randomId(), "main", Collections.emptyList(), Collections.emptyMap(), false);

        JavaSourceSet updated = updater.changeDependency(clean, dep("org.slf4j", "slf4j-api", "1.7.36"), dep("org.slf4j", "slf4j-api", "2.0.9"));

        assertThat(updated.isDirty()).isTrue();
    }

    @Test
    void addDependencyMarksSourceSetDirty() {
        JavaSourceSetUpdater updater = new JavaSourceSetUpdater(new InMemoryExecutionContext());
        JavaSourceSet clean = new JavaSourceSet(Tree.randomId(), "main", Collections.emptyList(), Collections.emptyMap(), false);

        JavaSourceSet updated = updater.addDependency(clean, "org.slf4j", "slf4j-api", "2.0.9", Collections.singletonList(MavenRepository.MAVEN_CENTRAL));

        assertThat(updated.isDirty()).isTrue();
    }

    @Test
    void alreadyDirtySourceSetIsReturnedUnchanged() {
        JavaSourceSetUpdater updater = new JavaSourceSetUpdater(new InMemoryExecutionContext());
        JavaSourceSet dirty = new JavaSourceSet(Tree.randomId(), "main", Collections.emptyList(), Collections.emptyMap(), true);

        JavaSourceSet updated = updater.changeDependency(dirty, dep("g", "a", "1"), dep("g", "a", "2"));

        assertThat(updated).isSameAs(dirty);
    }

    private static ResolvedDependency dep(String groupId, String artifactId, String version) {
        return ResolvedDependency.builder()
                .gav(new ResolvedGroupArtifactVersion(null, groupId, artifactId, version, null))
                .requested(Dependency.builder().gav(new GroupArtifactVersion(groupId, artifactId, version)).build())
                .build();
    }
}
