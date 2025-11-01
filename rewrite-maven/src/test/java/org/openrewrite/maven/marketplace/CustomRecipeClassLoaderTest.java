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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that demonstrates how to create a custom RecipeClassLoaderFactory
 * that adds additional parent-delegated packages.
 */
class CustomRecipeClassLoaderTest {

    @Test
    void customRecipeClassLoaderFactory() {
        // Create a factory that produces classloaders with custom parent-delegated packages
        RecipeClassLoaderFactory factoryWithCustomDelegation = (recipeJar, classpath) ->
                new RecipeClassLoader(recipeJar, classpath) {
                    @Override
                    protected List<String> getAdditionalParentDelegatedPackages() {
                        return Arrays.asList(
                                "io.moderne.devcenter.",
                                "com.mycompany.shared."
                        );
                    }
                };

        // This demonstrates the extensibility - users can create their own factories
        // that produce classloaders with custom parent delegation rules
        RecipeClassLoader loader = factoryWithCustomDelegation.create(
                Path.of("dummy.jar"),
                Arrays.asList(Path.of("dep1.jar"), Path.of("dep2.jar"))
        );

        assertThat(loader).isNotNull();
    }

    @Test
    void defaultRecipeClassLoaderFactory() {
        // The default factory just creates a standard RecipeClassLoader
        RecipeClassLoaderFactory defaultFactory = RecipeClassLoader::new;

        RecipeClassLoader loader = defaultFactory.create(
                Path.of("dummy.jar"),
                Arrays.asList(Path.of("dep1.jar"), Path.of("dep2.jar"))
        );

        assertThat(loader).isNotNull();
    }
}