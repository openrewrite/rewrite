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
import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.*;
import org.openrewrite.maven.tree.*;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class MavenRecipeBundleReader implements RecipeBundleReader {
    private static final Map<ResolvedGroupArtifactVersion, Lock> DEPENDENCY_LOCKS = new ConcurrentHashMap<>();

    private final @Getter RecipeBundle bundle;
    private final MavenResolutionResult mrr;
    private final MavenArtifactDownloader downloader;
    private final RecipeClassLoaderFactory classLoaderFactory;

    private transient @Nullable Environment environment;
    transient @Nullable Path recipeJar;
    transient @Nullable List<Path> classpath;
    private transient @Nullable ClassLoader classLoader;

    @Override
    public RecipeMarketplace read() {
        if (recipeJar == null) {
            for (ResolvedDependency resolvedDependency : mrr.getDependencies().get(Scope.Runtime)) {
                if (isResolvedBundle(resolvedDependency)) {
                    recipeJar = downloader.downloadArtifact(resolvedDependency);
                    break;
                }
            }
            if (recipeJar != null) {
                try (JarFile jarFile = new JarFile(recipeJar.toFile())) {
                    JarEntry entry = jarFile.getJarEntry("META-INF/rewrite/recipes.csv");
                    if (entry != null) {
                        try (InputStream recipesCsv = jarFile.getInputStream(entry)) {
                            RecipeMarketplace marketplace = new RecipeMarketplaceReader().fromCsv(recipesCsv);
                            for (RecipeListing recipe : marketplace.getAllRecipes()) {
                                // The recipes.csv inside a JAR may be generated without a version,
                                // since the version of a published Maven artifact is determined at
                                // publish time if the artifact is a snapshot. Having resolved the
                                // JAR containing the recipes.csv, we now know the version.
                                recipe.getBundle().setVersion(bundle.getVersion());
                            }
                            return marketplace;
                        }
                    }
                } catch (IOException e) {
                    // If we can't read the recipes.csv, fall back to full classpath scanning
                }
            }
        }

        return marketplaceFromClasspathScan();
    }

    /**
     * @return Build a marketplace that consists of just the recipes found via classpath scanning
     * in the resolved recipe JAR (not including its dependencies)
     */
    RecipeMarketplace marketplaceFromClasspathScan() {
        String[] ga = bundle.getPackageName().split(":");
        RecipeMarketplace marketplace = new RecipeMarketplace();
        List<Path> classpath = classpath();
        RecipeClassLoader classLoader = new RecipeClassLoader(requireNonNull(recipeJar), classpath);

        // First pass: Scan only the recipe jar for recipes and don't list recipes from dependencies
        Environment env = Environment.builder().scanJar(
                requireNonNull(recipeJar).toAbsolutePath(),
                classpath.stream().map(Path::toAbsolutePath).collect(toList()),
                classLoader
        ).build();

        // Second pass: Scan all jars in classpath for recipes and categories
        // This gives us proper root categories from category YAMLs.
        Environment envWithCategories = environment();

        // Bundle version may be set in the environment() call above (as the JARs making up
        // the classpath are resolved)
        GroupArtifactVersion gav = new GroupArtifactVersion(ga[0], ga[1], bundle.getVersion());

        for (RecipeDescriptor descriptor : env.listRecipeDescriptors()) {
            marketplace.install(
                    RecipeListing.fromDescriptor(descriptor, new RecipeBundle(
                            "maven", gav.getGroupId() + ":" + gav.getArtifactId(),
                            bundle.getRequestedVersion() == null ? gav.getVersion() : bundle.getRequestedVersion(),
                            gav.getVersion(), null)),
                    descriptor.inferCategoriesFromName(envWithCategories)
            );
        }
        return marketplace;
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        return environment().activateRecipes(listing.getName()).getDescriptor();
    }

    @Override
    public Recipe prepare(RecipeListing listing, @Nullable Map<String, Object> options) {
        Recipe r = environment().activateRecipes(listing.getName());
        return r.withOptions(options);
    }

    private Environment environment() {
        if (environment == null) {
            environment = Environment.builder()
                    .load(new ClasspathScanningLoader(new Properties(), classLoader()))
                    .build();
        }
        return environment;
    }

    private ClassLoader classLoader() {
        if (classLoader == null) {
            // Create an isolated classloader with controlled parent delegation
            // This ensures maximum isolation while still allowing necessary shared types
            List<Path> classpath = classpath();
            classLoader = classLoaderFactory.create(requireNonNull(recipeJar), classpath);
        }
        return classLoader;
    }

    List<Path> classpath() {
        if (classpath == null) {
            classpath = new ArrayList<>();
            for (ResolvedDependency resolvedDependency : mrr.getDependencies().get(Scope.Runtime)) {
                if (recipeJar != null && isResolvedBundle(resolvedDependency)) {
                    // recipeJar may be non-null if the listRecipes() method was previously
                    // used and the recipe JAR contains a recipes.csv that didn't necessitate
                    // the whole classpath to be scanned.
                    classpath.add(recipeJar);
                    continue;
                }
                Lock lock = DEPENDENCY_LOCKS.computeIfAbsent(resolvedDependency.getGav(), g -> new ReentrantLock());
                lock.lock();
                try {
                    Path path = downloader.downloadArtifact(resolvedDependency);
                    if (path == null) {
                        throw new IllegalStateException("Unable to download dependency " + resolvedDependency.getGav());
                    }
                    if (resolvedDependency.isDirect()) {
                        recipeJar = path;
                    }
                    classpath.add(path);
                } finally {
                    lock.unlock();
                }
            }
        }
        return classpath;
    }

    private boolean isResolvedBundle(ResolvedDependency resolvedDependency) {
        return resolvedDependency.isDirect() && bundle.getPackageName()
                .equals(resolvedDependency.getGroupId() + ":" + resolvedDependency.getArtifactId());
    }
}
