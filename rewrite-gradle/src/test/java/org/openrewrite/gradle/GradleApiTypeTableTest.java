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

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
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
          .doesNotContainAnyElementsOf(groovyCore));
        // writePublicApi in build.gradle.kts says why
        referencesByVersion.forEach((version, references) -> assertThat(references)
          .as("classes the Gradle %s API refers to", version)
          .allMatch(name -> name.startsWith("java/") || groovyCore.contains(name) ||
                            classesByVersion.get(version).contains(name)));
    }
}
