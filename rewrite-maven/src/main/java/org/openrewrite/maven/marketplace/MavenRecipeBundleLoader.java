/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven.marketplace;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleLoader;
import org.openrewrite.marketplace.YamlRecipeBundleLoader;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class MavenRecipeBundleLoader implements RecipeBundleLoader, URLStreamHandlerFactory {
    private final ExecutionContext ctx;
    private final MavenArtifactDownloader downloader;
    private final RecipeClassLoaderFactory classLoaderFactory;

    public MavenRecipeBundleLoader(ExecutionContext ctx, MavenArtifactDownloader downloader,
                                   RecipeClassLoaderFactory classLoaderFactory) {
        this.ctx = ctx;
        this.downloader = downloader;
        this.classLoaderFactory = classLoaderFactory;
        URL.setURLStreamHandlerFactory(this);
    }

    @Override
    public String getEcosystem() {
        return "maven";
    }

    @Override
    public RecipeBundle createBundle(String packageName, String version, @Nullable String team) {
        ResolvedGroupArtifactVersion gav = parseGav(packageName);

        // Check if version is a dated snapshot (e.g., "2.1.0-20231201.123456-1")
        String maybeSnapshotVersion;
        String datedSnapshotVersion;
        if (version.matches(".*-\\d{8}\\.\\d{6}-\\d+")) {
            // Extract base snapshot version (e.g., "2.1.0-SNAPSHOT" from "2.1.0-20231201.123456-1")
            datedSnapshotVersion = version;
            int dashIndex = version.indexOf('-');
            maybeSnapshotVersion = version.substring(0, dashIndex) + "-SNAPSHOT";
        } else {
            maybeSnapshotVersion = version;
            datedSnapshotVersion = null;
        }

        gav = gav.withVersion(maybeSnapshotVersion).withDatedSnapshotVersion(datedSnapshotVersion);

        return new MavenRecipeBundle(gav, ctx, downloader, classLoaderFactory, team);
    }

    /**
     * Enables {@link YamlRecipeBundleLoader} to load YAML recipes from
     * the META-INF/rewrite directory of Maven recipe artifacts. A recipe marketplace entry of the
     * form "maven-rewrite-yaml:org.openrewrite:rewrite-core:8.0.0!/META-INF/rewrite/rewrite.yml" will
     * load recipes from rewrite-core after downloading the dependency.
     *
     * @param protocol Adds support for the scheme "maven-rewrite-yaml".
     * @return A URLStreamHandler that {@link YamlRecipeBundleLoader} uses.
     */
    @Override
    public @Nullable URLStreamHandler createURLStreamHandler(String protocol) {
        return "maven-rewrite-yaml".equals(protocol) ? new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                String[] gavAndPath = u.getPath().split("!/", 2);
                ResolvedGroupArtifactVersion gav = parseGav(gavAndPath[0]);
                MavenRecipeBundle bundle = new MavenRecipeBundle(gav, ctx, downloader,
                        classLoaderFactory, null);
                MavenResolutionResult mrr = bundle.resolve();
                ResolvedDependency recipeDependency = mrr.getDependencies().get(Scope.Compile).get(0);
                Path recipeJar = requireNonNull(downloader.downloadArtifact(recipeDependency));
                return new URL("jar:file:" + recipeJar.toAbsolutePath() + "!/" + gavAndPath[1])
                        .openConnection();
            }
        } : null;
    }

    private static ResolvedGroupArtifactVersion parseGav(String gav) {
        // Parse packageName as "groupId:artifactId"
        String[] parts = gav.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Maven package name must be in format 'groupId:artifactId', but found " + gav);
        }

        //noinspection DataFlowIssue
        return new ResolvedGroupArtifactVersion(
                null, // repository
                parts[0], // groupId
                parts[1], // artifactId
                null,
                null
        );
    }
}
