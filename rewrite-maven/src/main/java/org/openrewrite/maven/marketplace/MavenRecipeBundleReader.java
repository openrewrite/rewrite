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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.Environment;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.*;
import org.openrewrite.maven.tree.*;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class MavenRecipeBundleReader implements RecipeBundleReader {
    private static final Map<ResolvedGroupArtifactVersion, Lock> DEPENDENCY_LOCKS = new ConcurrentHashMap<>();

    private final @Getter RecipeBundle bundle;
    private final MavenResolutionResult mrr;
    private final MavenArtifactDownloader downloader;
    private final RecipeClassLoaderFactory classLoaderFactory;

    private transient @Nullable Environment environment;
    private transient @Nullable Path recipeJar;
    private transient @Nullable List<Path> classpath;
    private transient @Nullable ClassLoader classLoader;

    @Override
    public RecipeMarketplace read() {
        if (recipeJar == null) {
            for (ResolvedDependency resolvedDependency : mrr.getDependencies().get(Scope.Runtime)) {
                if (resolvedDependency.isDirect() && recipeJar != null) {
                    recipeJar = downloader.downloadArtifact(resolvedDependency);
                }
            }
            if (recipeJar != null) {
                try (JarFile jarFile = new JarFile(recipeJar.toFile())) {
                    JarEntry entry = jarFile.getJarEntry("META-INF/rewrite/recipes.csv");
                    if (entry != null) {
                        try (InputStream recipesCsv = jarFile.getInputStream(entry)) {
                            return new RecipeMarketplaceReader().fromCsv(recipesCsv);
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
    private RecipeMarketplace marketplaceFromClasspathScan() {
        String[] ga = bundle.getPackageName().split(":");
        GroupArtifactVersion gav = new GroupArtifactVersion(ga[0], ga[1], bundle.getVersion());

        RecipeMarketplace marketplace = new RecipeMarketplace();
        Environment env = environment();
        for (RecipeDescriptor descriptor : env.listRecipeDescriptors()) {
            marketplace.install(
                    RecipeListing.fromDescriptor(descriptor, new RecipeBundle(
                            "maven", gav.getGroupId() + ":" + gav.getArtifactId(),
                            requireNonNull(gav.getVersion()), null)),
                    descriptor.inferCategoriesFromName(env)
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
        return applyOptions(r, options);
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
                Lock lock = DEPENDENCY_LOCKS.computeIfAbsent(resolvedDependency.getGav(), g -> new ReentrantLock());
                lock.lock();
                try {
                    if (resolvedDependency.isDirect() && recipeJar != null) {
                        // recipeJar may be non-null if the listRecipes() method was previously
                        // used and the recipe JAR contains a recipes.csv that didn't necessitate
                        // the whole classpath to be scanned.
                        classpath.add(recipeJar);
                        continue;
                    }
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

    private <R extends Recipe> R applyOptions(R recipe, @Nullable Map<String, Object> options) {
        Map<String, Object> m = new HashMap<>();
        m.put("@c", recipe.getName());
        ObjectMapper objectMapper = JsonMapper.builder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
                .build()
                .registerModule(new ParameterNamesModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // This is necessary to allow setting options like `FindTags#xPath`, as Jackson otherwise only sees a `xpath`
        // property, which it derives from the `getXPath()` method generated by Lombok
        objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        try {
            //noinspection unchecked
            R clone = (R) recipe.clone();
            if (options != null) {
                m.putAll(options);
                for (OptionDescriptor optionDescriptor : clone.getDescriptor().getOptions()) {
                    Object value = options.get(optionDescriptor.getName());
                    if (value instanceof String) {
                        Map<String, Object> option = new HashMap<>();
                        option.put("value", value);
                        objectMapper.updateValue(optionDescriptor, option);
                    }
                }
            }
            return objectMapper.updateValue(clone, m);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
