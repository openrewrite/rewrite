/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Recipe;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ClasspathScanningLoaderTest {

    @Test
    void scanJarWithSeparateClassesAndResourcesDirs(@TempDir Path tempDir) throws Exception {
        // given: classes and resources in separate directories (as in CLI active recipe flow)
        Path classesDir = tempDir.resolve("classes");
        Path resourcesDir = tempDir.resolve("resources");
        Files.createDirectories(classesDir);
        Path metaInf = resourcesDir.resolve("META-INF/rewrite");
        Files.createDirectories(metaInf);

        Files.writeString(metaInf.resolve("recipe.yml"),
            """
            type: specs.openrewrite.org/v1beta/recipe
            name: com.example.TestRecipe
            displayName: Test Recipe
            description: A test recipe.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: hello
            """);

        List<Path> classpath = List.of(classesDir, resourcesDir);
        URL[] urls = classpath.stream()
            .map(p -> {
                try {
                    return p.toUri().toURL();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .toArray(URL[]::new);
        ClassLoader cl = new URLClassLoader(urls, Recipe.class.getClassLoader());

        // when: scanJar with classesDir as "jar" and full classpath as dependencies
        Environment env = Environment.builder()
            .scanJar(classesDir, classpath, cl)
            .build();

        // then: recipe defined in resourcesDir should still be found
        Recipe recipe = env.activateRecipes("com.example.TestRecipe");
        assertThat(recipe).isNotNull();
        assertThat(recipe.getName()).isEqualTo("com.example.TestRecipe");
    }
}
