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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.*;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public class MavenRecipeBundle implements RecipeBundle {
    private static final Map<ResolvedGroupArtifactVersion, Lock> DEPENDENCY_LOCKS = new ConcurrentHashMap<>();

    private final ResolvedGroupArtifactVersion gav;
    private final ExecutionContext ctx;
    private final @Nullable MavenArtifactDownloader downloader;

    @Getter
    private final @Nullable String team;

    private transient @Nullable ResolvedMavenRecipeBundle resolvedBundle;
    private transient @Nullable List<Path> classpath;

    @Override
    public String getPackageEcosystem() {
        return "maven";
    }

    @Override
    public String getPackageName() {
        return gav.getGroupId() + ":" + gav.getArtifactId();
    }

    @Override
    public String getVersion() {
        return gav.getDatedSnapshotVersion() == null ? gav.getVersion() : gav.getDatedSnapshotVersion();
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        return resolvedBundle().describe(listing);
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        return resolvedBundle().prepare(listing, options);
    }

    private ResolvedMavenRecipeBundle resolvedBundle() {
        if (resolvedBundle == null) {
            Path recipeJar = classpath().stream()
                    .filter(a -> a.toAbsolutePath().toString().contains(gav.getGroupId().replaceAll("\\.", File.separator)) &&
                                 a.toAbsolutePath().toString().contains(File.separator + gav.getArtifactId() + File.separator)
                    )
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Failed to install recipe. No download error occurred."));

            resolvedBundle = new ResolvedMavenRecipeBundle(
                    gav,
                    recipeJar,
                    classpath(),
                    team
            );
        }
        return resolvedBundle;
    }

    List<Path> classpath() {
        if (classpath == null) {
            if (downloader == null) {
                throw new IllegalStateException("No downloader configured");
            }
            classpath = new ArrayList<>();
            for (ResolvedDependency resolvedDependency : resolve().getDependencies().get(Scope.Runtime)) {
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
        }
        return classpath;
    }

    MavenResolutionResult resolve() {
        //language=xml
        return MavenParser.builder().build().parse(ctx,
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
    }

    public static void main(String[] args) {
        RecipeMarketplaceReader reader = new RecipeMarketplaceReader(
                new MavenRecipeBundleLoader(
                        new InMemoryExecutionContext(),
                        new MavenArtifactDownloader(
                                new LocalMavenArtifactCache(Paths.get(".rewrite")),
                                null,
                                Throwable::printStackTrace
                        )
                ),
                new YamlRecipeBundleLoader(new Properties())
        );

        RecipeMarketplace recipeMarketplace = reader.fromCsv(Paths.get("recipes.csv"));
        System.out.println(new RecipeMarketplacePrinter(opt -> opt.nameStyle(
                RecipeMarketplacePrinter.NameStyle.DISPLAY_NAME)).print(recipeMarketplace));
    }
}
