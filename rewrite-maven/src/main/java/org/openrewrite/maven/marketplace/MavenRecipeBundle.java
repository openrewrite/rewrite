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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.emptyMap;

@RequiredArgsConstructor
public class MavenRecipeBundle implements RecipeBundle {
    private static final Map<ResolvedGroupArtifactVersion, Lock> DEPENDENCY_LOCKS = new ConcurrentHashMap<>();

    private final ResolvedGroupArtifactVersion gav;
    private final MavenExecutionContextView ctx;
    private final MavenArtifactDownloader downloader;

    @Getter
    private final @Nullable String team;

    /**
     * A loader, initialized with the fully resolved runtime classpath of the dependency
     * represented by {@link #gav}.
     */
    private transient @Nullable RecipeLoader recipeLoader;

    @Override
    public String getPackageEcosystem() {
        return "Maven";
    }

    @Override
    public String getPackageName() {
        return gav.getGroupId() + ":" + gav.getArtifactId();
    }

    @Override
    public String getVersion() {
        return gav.getVersion();
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        if (listing instanceof RecipeDescriptor) {
            // Already fully described in the marketplace, so we can return that as-is
            return (RecipeDescriptor) listing;
        }
        return prepare(listing, emptyMap()).getDescriptor();
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        return getRecipeLoader().load(listing.getName(), options);
    }

    protected ClassLoader buildClassLoader(Collection<Path> classpath) {
        return new URLClassLoader(
                classpath.stream()
                        .map(Path::toUri)
                        .map(uri -> {
                            try {
                                return uri.toURL();
                            } catch (MalformedURLException e) {
                                throw new UncheckedIOException(e);
                            }
                        })
                        .toArray(URL[]::new),
                getClass().getClassLoader()
        );
    }

    private RecipeLoader getRecipeLoader() {
        if (recipeLoader == null) {
            MavenResolutionResult mavenModel = MavenParser.builder().build().parse(ctx,
                            //language=xml
                            "<project>" +
                            "    <groupId>io.moderne</groupId>" +
                            "    <artifactId>recipe-downloader</artifactId>" +
                            "    <version>1</version>" +
                            "    <dependencies>" +
                            "        <dependency>" +
                            "            <groupId>" + gav.getGroupId() + "</groupId>" +
                            "            <artifactId>" + gav.getArtifactId() + "</artifactId>" +
                            "            <version>" + gav.getVersion() + "</version>" +
                            "        </dependency>" +
                            "    </dependencies>" +
                            "</project>"
                    ).findFirst()
                    .flatMap(sf -> sf.getMarkers().findFirst(MavenResolutionResult.class))
                    .filter(mrr -> !mrr.getDependencies().isEmpty())
                    .orElseThrow(() -> new IllegalStateException("Unable to download recipe"));

            List<Path> classpath = new ArrayList<>();
            for (ResolvedDependency resolvedDependency : mavenModel.getDependencies().get(Scope.Runtime)) {
                Lock lock = DEPENDENCY_LOCKS.computeIfAbsent(resolvedDependency.getGav(), g -> new ReentrantLock());
                lock.lock();
                try {
                    Path path = downloader.downloadArtifact(resolvedDependency);
                    if (path == null) {
                        throw new IllegalStateException("Unable to download dependency " + resolvedDependency.getGav());
                    }
                    classpath.add(path);
                } finally {
                    lock.unlock();
                }
            }
            recipeLoader = new RecipeLoader(buildClassLoader(classpath));
        }

        return recipeLoader;
    }
}
