/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.marker;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Paths;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.marker.JavaSourceSet.gavFromPath;

class JavaSourceSetTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1636")
    @Issue("https://github.com/openrewrite/rewrite/issues/1712")
    @Test
    void buildJavaSourceSet() {
        var jss = JavaSourceSet.build("main", emptyList());
        var typesBySignature = jss.getClasspath().stream().collect(toMap(JavaType.FullyQualified::toString, Function.identity()));
        assertThat(typesBySignature.get("java.lang.Object")).isInstanceOf(JavaType.FullyQualified.class);
        assertThat(typesBySignature.get("java.util.List")).isInstanceOf(JavaType.FullyQualified.class);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1677")
    @Test
    void shadedJar() {
        JavaSourceSet jss = JavaSourceSet.build("test", JavaParser.dependenciesFromClasspath("hbase-shaded-client"));
        var shaded = jss.getClasspath().stream()
          .filter(o -> o.getFullyQualifiedName().startsWith("org.apache.hadoop.hbase.CacheEvictionStats"))
          .findAny();
        assertThat(shaded).isPresent();
        assertThat(jss.getGavToTypes().get("org.apache.hbase:hbase-shaded-client:2.4.11")).contains(shaded.get());
    }

    @Test
    void runtimeClasspath() {
        var jss = JavaSourceSet.build("main", JavaParser.runtimeClasspath()).getClasspath()
          .stream().filter(it -> it.getFullyQualifiedName().contains("org.openrewrite"))
          .toList();
        assertThat(jss).isNotEmpty();
        assertThat(jss).allSatisfy(c -> assertThat(c.getFullyQualifiedName()).startsWith("org.openrewrite"));
    }

    @Test
    void gavCoordinateFromGradle() {
        assertThat(gavFromPath(Paths.get("C:/Users/Sam/.gradle/caches/modules-2/files-2.1/org.openrewrite/rewrite-core/8.32.0/64ddcc371f1bf29593b4b27e907757d5554d1a83/rewrite-core-8.32.0.jar")))
          .isEqualTo("org.openrewrite:rewrite-core:8.32.0");
    }

    @Test
    void gavCoordinateFromMaven() {
        assertThat(gavFromPath(Paths.get("C:/Users/Sam/.m2/repository/org/openrewrite/rewrite-core/8.32.0/rewrite-core-8.32.0.jar")))
          .isEqualTo("org.openrewrite:rewrite-core:8.32.0");
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/pull/4401")
    void tolerateWeirdClassNames(){
        assertThat(JavaSourceSet.isDeclarable("fj.data.$")).isFalse();
    }
}
