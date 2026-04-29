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
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MavenExecutionContextViewTest {

    @Test
    void artifactCacheIsMemoizedAcrossCalls() {
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());

        MavenArtifactCache first = ctx.getArtifactCache();
        MavenArtifactCache second = ctx.getArtifactCache();

        assertThat(first).isSameAs(second);
    }

    @Test
    void setArtifactCacheOverridesDefault(@TempDir Path tempDir) {
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        MavenArtifactCache override = new LocalMavenArtifactCache(tempDir);

        ctx.setArtifactCache(override);

        assertThat(ctx.getArtifactCache()).isSameAs(override);
    }

    @Test
    void artifactCacheIsSharedBetweenViewsOverSameDelegate() {
        InMemoryExecutionContext delegate = new InMemoryExecutionContext();

        MavenArtifactCache fromFirstView = MavenExecutionContextView.view(delegate).getArtifactCache();
        MavenArtifactCache fromSecondView = MavenExecutionContextView.view(delegate).getArtifactCache();

        assertThat(fromFirstView).isSameAs(fromSecondView);
    }
}
