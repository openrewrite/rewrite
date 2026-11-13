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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryMirror;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedPom;

import java.nio.file.Path;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

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

    /**
     * Recipes hand the downloader {@link MavenExecutionContextView#effectiveSettings}, so a mirror
     * configured by whoever is running the recipe has to beat the one recorded when the source was
     * parsed. {@link MavenRepositoryMirror#apply} takes the first match and {@code MavenSettings.merge}
     * puts the receiver's mirrors first.
     */
    @Test
    void contextMirrorsWinOverThoseRecordedWhenTheSourceWasParsed() {
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(mirroring("configured", "external:*", "https://configured.example.com", ctx));

        MavenRepository mirrored = MavenRepositoryMirror.apply(
          ctx.getMirrors(ctx.effectiveSettings(
            parsedWith(mirroring("from-build", "central", "https://from-build.example.com", ctx)))),
          MavenRepository.MAVEN_CENTRAL);

        assertThat(mirrored.getUri()).isEqualTo("https://configured.example.com");
    }

    @Test
    void mirrorsRecordedWhenTheSourceWasParsedStillApplyWithoutContextSettings() {
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());

        MavenRepository mirrored = MavenRepositoryMirror.apply(
          ctx.getMirrors(ctx.effectiveSettings(
            parsedWith(mirroring("from-build", "central", "https://from-build.example.com", ctx)))),
          MavenRepository.MAVEN_CENTRAL);

        assertThat(mirrored.getUri()).isEqualTo("https://from-build.example.com");
    }

    /** Merged, not replaced: each source still mirrors the repositories the other says nothing about. */
    @Test
    void mirrorsFromBothSourcesApply() {
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(mirroring("configured", "central", "https://configured.example.com", ctx));

        MavenSettings effective = ctx.effectiveSettings(
          parsedWith(mirroring("from-build", "jboss", "https://from-build.example.com", ctx)));
        MavenRepository jboss = MavenRepository.builder()
          .id("jboss")
          .uri("https://repository.jboss.org/nexus/content/groups/public-jboss/")
          .build();

        assertThat(MavenRepositoryMirror.apply(ctx.getMirrors(effective), MavenRepository.MAVEN_CENTRAL).getUri())
          .isEqualTo("https://configured.example.com");
        assertThat(MavenRepositoryMirror.apply(ctx.getMirrors(effective), jboss).getUri())
          .isEqualTo("https://from-build.example.com");
    }

    private static MavenResolutionResult parsedWith(MavenSettings settings) {
        return new MavenResolutionResult(randomId(), null, ResolvedPom.builder().build(),
          emptyList(), null, emptyMap(), settings, emptyList(), emptyMap());
    }

    private static MavenSettings mirroring(String id, String mirrorOf, String url, ExecutionContext ctx) {
        return requireNonNull(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
          //language=xml
          """
            <settings>
                <mirrors>
                    <mirror>
                        <id>%s</id>
                        <mirrorOf>%s</mirrorOf>
                        <url>%s</url>
                    </mirror>
                </mirrors>
            </settings>
            """.formatted(id, mirrorOf, url)), ctx));
    }
}
