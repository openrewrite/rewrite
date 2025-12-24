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
package org.openrewrite.marketplace;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Factory for creating recipe classloaders.
 */
@FunctionalInterface
public interface RecipeClassLoaderFactory {
    /**
     * Create a new recipe classloader for the given recipe JAR and classpath.
     *
     * @param recipeJar Recipe JAR to load. If nullable, there is no primary recipe JAR and
     *                  the resultant classloader is being used to load sub-recipes, such as
     *                  in a non-Java recipe which re-uses Java-written recipes in its
     *                  implementation.
     * @param classpath Runtime dependencies of the recipe JAR
     * @return A new recipe classloader instance.
     */
    RecipeClassLoader create(@Nullable Path recipeJar, List<Path> classpath);
}
