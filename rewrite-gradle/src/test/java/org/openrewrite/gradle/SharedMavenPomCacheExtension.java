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
package org.openrewrite.gradle;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.CompositeMavenPomCache;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.tree.*;
import org.openrewrite.test.RewriteTest;

import java.net.URI;
import java.util.Optional;

/**
 * Shares the POMs, metadata and repository probes that recipes download through the default {@link ExecutionContext}
 * among all the tests in a test JVM, so each is fetched once per JVM rather than once per test. Auto-registered like
 * {@link MavenSettingsAutoLoadingExtension}.
 */
public class SharedMavenPomCacheExtension implements BeforeAllCallback {

    private static final MavenPomCache DOWNLOADS = new SuccessfulDownloads();

    @Override
    public void beforeAll(ExtensionContext context) {
        RewriteTest.defaultExecutionContextCustomizers.putIfAbsent(
                SharedMavenPomCacheExtension.class,
                SharedMavenPomCacheExtension::shareDownloads);
    }

    private static void shareDownloads(ExecutionContext ctx) {
        MavenExecutionContextView.view(ctx)
                .setPomCache(new CompositeMavenPomCache(new InMemoryMavenPomCache(), DOWNLOADS));
    }

    /**
     * Only what was found in a remote repository, as first downloaded, so whatever that download swallowed, such as
     * Gradle module metadata answered with a 429, stays missing for the rest of the JVM. Failed downloads, resolved POMs
     * (which depend on a test's repositories and profiles) and anything from a file: repository (where tests publish
     * POMs of their own) stay with the test that fetched them.
     */
    private static class SuccessfulDownloads implements MavenPomCache {
        private final InMemoryMavenPomCache cache = new InMemoryMavenPomCache();

        @Override
        public @Nullable ResolvedPom getResolvedDependencyPom(ResolvedGroupArtifactVersion dependency) {
            return null;
        }

        @Override
        public void putResolvedDependencyPom(ResolvedGroupArtifactVersion dependency, ResolvedPom resolved) {
        }

        @Override
        public @Nullable Optional<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav) {
            return cache.getMavenMetadata(repo, gav);
        }

        @Override
        public void putMavenMetadata(URI repo, GroupArtifactVersion gav, @Nullable MavenMetadata metadata) {
            if (metadata != null && isRemote(repo.toString())) {
                cache.putMavenMetadata(repo, gav, metadata);
            }
        }

        @Override
        public @Nullable Optional<Pom> getPom(ResolvedGroupArtifactVersion gav) {
            return cache.getPom(gav);
        }

        @Override
        public void putPom(ResolvedGroupArtifactVersion gav, @Nullable Pom pom) {
            if (pom != null && isRemote(gav.getRepository())) {
                cache.putPom(gav, pom);
            }
        }

        @Override
        public @Nullable Optional<MavenRepository> getNormalizedRepository(MavenRepository repository) {
            return cache.getNormalizedRepository(repository);
        }

        @Override
        public void putNormalizedRepository(MavenRepository repository, @Nullable MavenRepository normalized) {
            if (normalized != null && isRemote(repository.getUri())) {
                cache.putNormalizedRepository(repository, normalized);
            }
        }

        private static boolean isRemote(@Nullable String repositoryUri) {
            return repositoryUri != null && !repositoryUri.regionMatches(true, 0, "file:", 0, 5);
        }
    }
}
