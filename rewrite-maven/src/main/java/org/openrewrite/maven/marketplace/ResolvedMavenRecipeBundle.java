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

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ResolvedMavenRecipeBundle implements RecipeBundle {
    private final ResolvedGroupArtifactVersion gav;
    private final Path recipeJar;
    private final List<Path> classpath;

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
    public String getVersion() {
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
            environment = Environment.builder().scanJar(
                    recipeJar,
                    classpath,
                    classLoader()
            ).build();
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
            classLoader = new URLClassLoader(
                    Stream.concat(classpath.stream(), Stream.of(recipeJar))
                            .map(Path::toUri)
                            .map(uri -> {
                                try {
                                    return uri.toURL();
                                } catch (MalformedURLException e) {
                                    throw new UncheckedIOException(e);
                                }
                            })
                            .toArray(URL[]::new)
            );
        }
        return classLoader;
    }
}
