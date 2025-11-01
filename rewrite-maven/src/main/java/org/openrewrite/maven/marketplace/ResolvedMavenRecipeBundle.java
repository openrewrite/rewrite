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
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.RecipeLoader;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ResolvedMavenRecipeBundle implements RecipeBundle {
    private final ResolvedGroupArtifactVersion gav;
    private final Path recipeJar;
    private final List<Path> classpath;
    private final RecipeClassLoaderFactory classLoaderFactory;

    @Getter
    private final @Nullable String team;

    private transient @Nullable Environment environment;

    /**
     * A loader, initialized with the fully resolved runtime classpath of the dependency
     * represented by {@link #gav}.
     */
    private transient @Nullable RecipeLoader recipeLoader;

    private transient @Nullable ClassLoader classLoader;

    @Override
    public String getPackageEcosystem() {
        return "maven";
    }

    @Override
    public String getPackageName() {
        return gav.getGroupId() + ":" + gav.getArtifactId();
    }

    @Override
    public @Nullable String getVersion() {
        return gav.getDatedSnapshotVersion() == null ? gav.getVersion() : gav.getDatedSnapshotVersion();
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        for (RecipeDescriptor descriptor : getEnvironment().listRecipeDescriptors()) {
            if (descriptor.getName().equals(listing.getName())) {
                return descriptor.withBundle(this);
            }
        }
        throw new IllegalArgumentException("Did not find a matching recipe descriptor in the recipe JAR " + gav);
    }

    public Environment getEnvironment() {
        if (environment == null) {
            // Use standard classloading for scanning since ClassGraph doesn't actually load classes during scanning
            // It only reads bytecode to determine class structure and hierarchy
            ClassLoader scanningClassLoader = RecipeClassLoader.forScanning(recipeJar, classpath);

            // Scan all jars in the classloader for both recipes and categories
            environment = Environment.builder()
                    .load(new org.openrewrite.config.ClasspathScanningLoader(
                            new java.util.Properties(),
                            scanningClassLoader))
                    .build();
        }
        return environment;
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        return recipeLoader().load(listing.getName(), options);
    }

    private RecipeLoader recipeLoader() {
        if (recipeLoader == null) {
            recipeLoader = new RecipeLoader(classLoader());
        }
        return recipeLoader;
    }

    private ClassLoader classLoader() {
        if (classLoader == null) {
            // Create an isolated classloader with controlled parent delegation
            // This ensures maximum isolation while still allowing necessary shared types
            classLoader = classLoaderFactory.create(recipeJar, classpath);
        }
        return classLoader;
    }
}
