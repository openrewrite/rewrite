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

import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceSetTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1636")
    @Issue("https://github.com/openrewrite/rewrite/issues/1712")
    @Test
    void buildJavaSourceSet() {
        var jss = JavaSourceSet.build("main", emptyList());
        var typesBySignature = jss.getClasspath().stream().collect(Collectors.toMap(JavaType.FullyQualified::toString, Function.identity()));
        assertThat(typesBySignature.get("java.lang.Object")).isInstanceOf(JavaType.FullyQualified.class);
        assertThat(typesBySignature.get("java.util.List")).isInstanceOf(JavaType.FullyQualified.class);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1677")
    @Test
    void shadedJar() {
        var shaded = JavaSourceSet.build("test", JavaParser.dependenciesFromClasspath("hbase-shaded-client"))
          .getClasspath().stream().filter(o -> o.getFullyQualifiedName().startsWith("org.apache.hadoop.hbase.shaded")).collect(Collectors.toList());
        assertThat(shaded).isNotEmpty();
        assertThat(shaded.get(0)).isInstanceOf(JavaType.FullyQualified.class);
    }

    @Test
    void runtimeClasspath() {
        var jss = JavaSourceSet.build("main", JavaParser.runtimeClasspath()).getClasspath()
          .stream().filter(it -> it.getFullyQualifiedName().contains("org.openrewrite"))
          .toList();
        assertThat(jss).isNotEmpty();
    }
}
