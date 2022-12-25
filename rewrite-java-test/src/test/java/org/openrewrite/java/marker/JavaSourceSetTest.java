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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTypeGoat;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

class JavaSourceSetTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1636")
    @Test
    void buildJavaSourceSet() {
        var typeCache = new JavaTypeCache();
        var jss = JavaSourceSet.build("main", emptyList(), typeCache, false);
        var typesBySignature = jss.getClasspath().stream().collect(Collectors.toMap(JavaType.FullyQualified::toString, Function.identity()));
        assertThat(typesBySignature.get("java.lang.Object")).isInstanceOf(JavaType.Class.class);
        assertThat(typesBySignature.get("java.util.List")).isInstanceOf(JavaType.Class.class);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1712")
    @Test
    void shallowTypes() {
        var typeCache = new JavaTypeCache();
        var jss = JavaSourceSet.build("main", emptyList(), typeCache, false);
        var typesBySignature = jss.getClasspath().stream().collect(Collectors.toMap(JavaType.FullyQualified::toString, Function.identity()));
        assertThat(typesBySignature.get("java.lang.Object")).isInstanceOf(JavaType.ShallowClass.class);
        assertThat(typesBySignature.get("java.util.List")).isInstanceOf(JavaType.ShallowClass.class);
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1677")
    @Test
    void shadedJar() {
        var typeCache = new JavaTypeCache();
        var shaded = JavaSourceSet.build("test", JavaParser.dependenciesFromClasspath("hbase-shaded-client"), typeCache, false)
          .getClasspath().stream().filter(o -> o.getFullyQualifiedName().startsWith("org.apache.hadoop.hbase.shaded")).collect(Collectors.toList());
        assertThat(shaded).isNotEmpty();
        assertThat(shaded.get(0)).isInstanceOf(JavaType.ShallowClass.class);
    }

    // This test uses a lot of memory and examines the "fullTypeInformation" path that we don't actually take anywhere right now
    @Disabled
    @Test
    void doesNotDuplicateTypesInCache() {
        var typeCache = new JavaTypeCache();
        Set<JavaType> uniqueTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        var reflectiveGoat = new JavaReflectionTypeMapping(typeCache).type(JavaTypeGoat.class);
        newUniqueTypes(uniqueTypes, reflectiveGoat, false);

        var classpathGoat = JavaSourceSet.build("main", JavaParser.runtimeClasspath(), typeCache, true)
          .getClasspath()
          .stream()
          .filter(t -> t.getClassName().equals("JavaTypeGoat"))
          .findAny()
          .orElseThrow(() -> new IllegalStateException("Could not find JavaTypeGoat in classpath"));

        newUniqueTypes(uniqueTypes, classpathGoat, true);
    }

    private void newUniqueTypes(Set<JavaType> uniqueTypes, JavaType root, boolean report) {
        var newUnique = new AtomicBoolean(false);

        new JavaTypeVisitor<Integer>() {
            @Override
            public JavaType visit(@Nullable JavaType javaType, Integer p) {
                if (javaType != null) {
                    if (uniqueTypes.add(javaType)) {
                        if (report) {
                            newUnique.set(true);
                            System.out.println(javaType);
                        }
                        return super.visit(javaType, p);
                    }
                }
                //noinspection ConstantConditions
                return null;
            }
        }.visit(root, 0);

        if (report && newUnique.get()) {
            fail("Found new unique types there should have been none.");
        }
    }
}
