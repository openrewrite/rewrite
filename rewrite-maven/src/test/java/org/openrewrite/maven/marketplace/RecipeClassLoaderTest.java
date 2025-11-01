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

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class RecipeClassLoaderTest {

    @Test
    void loadRecipeWithIsolatedClassLoader() {
        // Find rewrite-java JAR in the classpath
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);

        Path recipeJar = null;
        List<Path> dependencies = new ArrayList<>();

        for (String entry : classpathEntries) {
            Path path = Paths.get(entry);
            String fileName = path.getFileName().toString();

            if (fileName.startsWith("rewrite-java-") && fileName.endsWith(".jar") && !fileName.contains("test")) {
                recipeJar = path;
            }
            if (fileName.startsWith("rewrite-") && fileName.endsWith(".jar")) {
                dependencies.add(path);
            }
        }

        if (recipeJar == null || dependencies.isEmpty()) {
            // Skip test if we can't find the JAR (e.g., running in IDE without built JARs)
            return;
        }

        // Create a ResolvedMavenRecipeBundle with the isolated classloader
        @SuppressWarnings("DataFlowIssue") ResolvedGroupArtifactVersion gav = new ResolvedGroupArtifactVersion(
                null,
                "org.openrewrite",
                "rewrite-java",
                null,
                null
        );

        ResolvedMavenRecipeBundle bundle = new ResolvedMavenRecipeBundle(
                gav,
                recipeJar,
                dependencies,
                RecipeClassLoader::new,
                null
        );

        // Get any recipe descriptor from the environment
        RecipeDescriptor descriptor = bundle.getEnvironment().listRecipeDescriptors().stream()
                .findFirst()
                .orElse(null);

        assertThat(descriptor).isNotNull();

        Recipe recipe = bundle.prepare(descriptor, emptyMap());
        assertThat(recipe).isNotNull();
        assertThat(recipe.getName()).isEqualTo(descriptor.getName());

        assertThat(recipe.getClass().getClassLoader())
                .as("Recipe should be loaded through RecipeClassLoader for isolation")
                .isInstanceOf(RecipeClassLoader.class);
    }
}
