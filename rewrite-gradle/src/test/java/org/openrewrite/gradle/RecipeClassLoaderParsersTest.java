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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.marketplace.RecipeClassLoader;
import org.openrewrite.maven.MavenParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class RecipeClassLoaderParsersTest {

    @Test
    void everyParserOnTheClasspathLoadsFromTheHost() throws Exception {
        List<Class<?>> parsers = new ArrayList<>();
        Set<Path> entriesWithParsers = new LinkedHashSet<>();
        // Only the languages rewrite-gradle depends on are on this classpath
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            Path path = Path.of(entry);
            for (String name : classFiles(path)) {
                // JavaUnrestrictedClassLoader defines the isolated package, so a recipe's loader never loads it
                if (name.startsWith("org/openrewrite/") && name.endsWith("Parser.class") && !name.contains("$") &&
                    !name.startsWith("org/openrewrite/java/isolated/")) {
                    String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                    Class<?> type = Class.forName(className, false, getClass().getClassLoader());
                    if (Parser.class.isAssignableFrom(type)) {
                        parsers.add(type);
                        entriesWithParsers.add(path);
                    }
                }
            }
        }
        assertThat(parsers).contains(GradleParser.class, GroovyParser.class, KotlinParser.class, MavenParser.class);

        List<String> loadedFromTheRecipe = new ArrayList<>();
        try (RecipeClassLoader recipeLoader = new RecipeClassLoader(
          RecipeClassLoader.getUrls(null, new ArrayList<>(entriesWithParsers)), getClass().getClassLoader())) {
            for (Class<?> parser : parsers) {
                if (recipeLoader.loadClass(parser.getName()) != parser) {
                    loadedFromTheRecipe.add(parser.getName());
                }
            }
        }
        assertThat(loadedFromTheRecipe).isEmpty();
    }

    private static List<String> classFiles(Path classpathEntry) throws IOException {
        if (Files.isDirectory(classpathEntry)) {
            try (Stream<Path> files = Files.walk(classpathEntry)) {
                return files
                  .map(file -> classpathEntry.relativize(file).toString().replace(File.separatorChar, '/'))
                  .filter(name -> name.endsWith(".class"))
                  .collect(toList());
            }
        }
        if (!Files.isRegularFile(classpathEntry) || !classpathEntry.toString().endsWith(".jar")) {
            return new ArrayList<>();
        }
        try (JarFile jar = new JarFile(classpathEntry.toFile())) {
            return jar.stream().map(entry -> entry.getName()).filter(name -> name.endsWith(".class")).collect(toList());
        }
    }
}
