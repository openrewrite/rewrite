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

import groovy.lang.Closure;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.marketplace.RecipeClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class GradleApiTypeTableTest {

    @Test
    void everyReferencedClassResolvesToTheTableOrTheParsersGroovy() throws Exception {
        Map<String, Set<String>> classesByVersion = new TreeMap<>();
        Map<String, Set<String>> referencesByVersion = new TreeMap<>();
        Pattern classReference = Pattern.compile("(?<=^|[(;\\[<>)*+\\-^:])L([^;<]+)[;<]");
        for (URL table : Collections.list(getClass().getClassLoader().getResources("META-INF/rewrite/classpath.tsv.gz"))) {
            byte[] rows;
            try (InputStream in = new GZIPInputStream(table.openStream())) {
                rows = in.readAllBytes();
            }
            for (String row : new String(rows, StandardCharsets.ISO_8859_1).split("\n")) {
                String[] columns = row.split("\t", -1);
                if (columns.length < 12 || !"gradle-public-api".equals(columns[1])) {
                    continue;
                }
                classesByVersion.computeIfAbsent(columns[2], v -> new HashSet<>()).add(columns[4]);
                // superclass, superinterfaces, member descriptor and member signature
                for (int column : new int[]{6, 7, 10, 11}) {
                    Matcher matcher = classReference.matcher(columns[column]);
                    while (matcher.find()) {
                        referencesByVersion.computeIfAbsent(columns[2], v -> new TreeSet<>()).add(matcher.group(1));
                    }
                }
            }
        }
        Set<String> groovyCore = new HashSet<>();
        try (JarFile groovy = new JarFile(Path.of(Closure.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile())) {
            groovy.stream().forEach(entry -> groovyCore.add(entry.getName().replaceFirst("\\.class$", "")));
        }

        assertThat(classesByVersion.keySet())
          .extracting(version -> Integer.parseInt(version.split("\\.")[0]))
          .containsExactlyInAnyOrder(Arrays.stream(GradleParser.GRADLE_API_MAJORS).boxed().toArray(Integer[]::new));
        classesByVersion.forEach((version, classes) -> assertThat(classes)
          .as("Groovy classes in the Gradle %s API", version)
          .doesNotContainAnyElementsOf(groovyCore)
          .as("implementation classes in the Gradle %s API", version)
          .doesNotContain("org/gradle/initialization/DefaultSettings")
          .as("Kotlin DSL in the Gradle %s API", version)
          .contains("org/gradle/kotlin/dsl/DependencyHandlerScope"));
        // writePublicApi in build.gradle.kts says why
        ClassLoader jdk = ClassLoader.getPlatformClassLoader();
        referencesByVersion.forEach((version, references) -> assertThat(references)
          .as("classes the Gradle %s API refers to", version)
          // XmlProvider.asElement() returns it, from a JDK module outside java.*
          .contains("org/w3c/dom/Element")
          .allMatch(name -> jdk.getResource(name + ".class") != null || groovyCore.contains(name) ||
                            classesByVersion.get(version).contains(name)));
    }

    @Test
    void recipeClassLoaderParsesAgainstTheTypeTableBesideGradleParser() throws Exception {
        Path javaClasses = Path.of(GradleParser.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path build = javaClasses.resolve("../../..").normalize();
        Path typeTableDir = build.resolve("resources/main");
        Path rewriteGroovy = Path.of(GroovyParser.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        // A host with rewrite-groovy but without rewrite-gradle, which has none of the recipe's Gradle API
        List<URL> host = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            Path path = Path.of(entry);
            if (!path.startsWith(build) && !hasGradleApiClasses(path)) {
                host.add(path.toUri().toURL());
            }
        }
        URL[] recipeJar = {
          javaClasses.toUri().toURL(),
          build.resolve("classes/groovy/main").toUri().toURL(),
          typeTableDir.toUri().toURL(),
          rewriteGroovy.toUri().toURL(),
          DependsOnIsTypeAttributed.class.getProtectionDomain().getCodeSource().getLocation()
        };
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        try (URLClassLoader hostLoader = new URLClassLoader(host.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
             RecipeClassLoader recipeLoader = new RecipeClassLoader(recipeJar, hostLoader)) {
            // TypeTable reads the context class loader's type tables too, and this test's own loader has them all
            thread.setContextClassLoader(hostLoader);
            @SuppressWarnings("unchecked")
            Predicate<String> dependsOnIsTypeAttributed = (Predicate<String>) recipeLoader
              .loadClass(DependsOnIsTypeAttributed.class.getName()).getConstructor().newInstance();
            assertThat(dependsOnIsTypeAttributed.test("tasks.register('report') { dependsOn 'check' }")).isTrue();
        } finally {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * The Groovy compiler attributes types from the classes on its class loader as well as from the type table, so the
     * host leaves out the Gradle API this module's tests run with.
     */
    private static boolean hasGradleApiClasses(Path classpathEntry) throws IOException {
        String task = "org/gradle/api/Task.class";
        if (Files.isDirectory(classpathEntry)) {
            return Files.exists(classpathEntry.resolve(task));
        }
        if (!Files.isRegularFile(classpathEntry)) {
            return false;
        }
        try (JarFile jar = new JarFile(classpathEntry.toFile())) {
            return jar.getEntry(task) != null;
        }
    }

    public static class DependsOnIsTypeAttributed implements Predicate<String> {
        @Override
        public boolean test(String script) {
            // Through Parser.parse, as AddDependencyVisitor parses its template
            G.CompilationUnit cu = GradleParser.builder().build().parse(new InMemoryExecutionContext(), script)
              .map(GradleParser.requireParsed(G.CompilationUnit.class))
              .findFirst()
              .orElseThrow();
            return !FindMethods.find(cu, "org.gradle.api.Task dependsOn(..)", true).isEmpty();
        }
    }
}
