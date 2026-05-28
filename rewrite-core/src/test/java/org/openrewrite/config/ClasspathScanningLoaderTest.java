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
import org.openrewrite.AbstractRecipe;
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

    @Test
    void scanYamlInSubdirectory(@TempDir Path tempDir) throws Exception {
        Path resourcesDir = tempDir.resolve("resources");
        Path metaInfSubdir = resourcesDir.resolve("META-INF/rewrite/subdir");
        Files.createDirectories(metaInfSubdir);

        Files.writeString(metaInfSubdir.resolve("recipe.yml"),
            """
            type: specs.openrewrite.org/v1beta/recipe
            name: com.example.SubdirRecipe
            displayName: Subdir Recipe
            description: A test recipe in a subdir.
            recipeList:
              - org.openrewrite.text.ChangeText:
                  toText: subdir
            """);

        List<Path> classpath = List.of(resourcesDir);
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

        Environment env = Environment.builder()
            .scanJar(resourcesDir, classpath, cl)
            .build();

        Recipe recipe = env.activateRecipes("com.example.SubdirRecipe");
        assertThat(recipe).isNotNull();
        assertThat(recipe.getName()).isEqualTo("com.example.SubdirRecipe");
    }

    @Test
    void abstractRecipeAnnotatedClassesAreNotEnumerated() {
        ClasspathScanningLoader loader = new ClasspathScanningLoader(
                new Properties(),
                new String[]{AbstractRecipeFixtures.class.getPackageName()});

        assertThat(loader.listRecipeDescriptors())
                .extracting(RecipeDescriptor::getName)
                .contains(AbstractRecipeFixtures.ConcreteRecipe.class.getName())
                .doesNotContain(AbstractRecipeFixtures.AbstractlyMarkedRecipe.class.getName());
    }

    @Test
    void singleImperativeRecipeActivationDoesNotScan() {
        ClasspathScanningLoader loader = new ClasspathScanningLoader(
                new Properties(),
                new String[]{AbstractRecipeFixtures.class.getPackageName()});
        Environment env = new Environment(List.of(loader));

        Recipe recipe = env.activateRecipes(AbstractRecipeFixtures.ConcreteRecipe.class.getName());

        assertThat(recipe.getName()).isEqualTo(AbstractRecipeFixtures.ConcreteRecipe.class.getName());
        // Neither the class hierarchy walk nor the YAML enumeration should have run.
        assertThat(loader.classScanTriggered()).isFalse();
        assertThat(loader.yamlListingTriggered()).isFalse();
    }

    @Test
    void singleDeclarativeRecipeActivationDoesNotScanClasses(@TempDir Path tempDir) throws Exception {
        Path resourcesDir = tempDir.resolve("resources");
        Path metaInf = resourcesDir.resolve("META-INF/rewrite");
        Files.createDirectories(metaInf);
        Files.writeString(metaInf.resolve("recipe.yml"),
                """
                type: specs.openrewrite.org/v1beta/recipe
                name: com.example.DeclarativeRecipe
                displayName: Declarative
                description: A declarative recipe.
                recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: hello
                """);
        ClassLoader cl = new URLClassLoader(new URL[]{resourcesDir.toUri().toURL()}, Recipe.class.getClassLoader());
        ClasspathScanningLoader loader = new ClasspathScanningLoader(
                resourcesDir, new Properties(), List.of(), cl);
        Environment env = new Environment(List.of(loader));

        Recipe recipe = env.activateRecipes("com.example.DeclarativeRecipe");

        assertThat(recipe.getName()).isEqualTo("com.example.DeclarativeRecipe");
        // YAML listing necessarily happened (the recipe is declarative), but the
        // class hierarchy walk should still be deferred.
        assertThat(loader.classScanTriggered()).isFalse();
        assertThat(loader.yamlListingTriggered()).isTrue();
    }

    @Test
    void singleDeclarativeRecipeActivationDrainsOnlyUntilFirstMatch(@TempDir Path tempDir) throws Exception {
        Path resourcesDir = tempDir.resolve("resources");
        Path metaInf = resourcesDir.resolve("META-INF/rewrite");
        Files.createDirectories(metaInf);
        // Three YAML files, each defining the same recipe (with the same body). The
        // progressive scan only needs to parse one of them to satisfy the lookup —
        // it should not drain the remaining loaders regardless of file walk order.
        String yaml = """
                type: specs.openrewrite.org/v1beta/recipe
                name: com.example.SharedRecipe
                displayName: Shared
                description: Shared recipe.
                recipeList:
                  - org.openrewrite.text.ChangeText:
                      toText: shared
                """;
        Files.writeString(metaInf.resolve("aaa.yml"), yaml);
        Files.writeString(metaInf.resolve("mmm.yml"), yaml);
        Files.writeString(metaInf.resolve("zzz.yml"), yaml);
        ClassLoader cl = new URLClassLoader(new URL[]{resourcesDir.toUri().toURL()}, Recipe.class.getClassLoader());
        ClasspathScanningLoader loader = new ClasspathScanningLoader(
                resourcesDir, new Properties(), List.of(), cl);
        Environment env = new Environment(List.of(loader));

        Recipe recipe = env.activateRecipes("com.example.SharedRecipe");

        assertThat(recipe.getName()).isEqualTo("com.example.SharedRecipe");
        assertThat(loader.yamlLoadersListed()).isEqualTo(3);
        // Exactly one loader was drained — the short-circuit stopped iterating once
        // the recipe turned up in the shared map.
        assertThat(loader.yamlLoadersDrained()).isEqualTo(1);
    }
}
