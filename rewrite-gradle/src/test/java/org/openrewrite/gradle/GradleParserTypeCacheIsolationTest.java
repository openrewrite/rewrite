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
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.kotlin.KotlinParser;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code org.openrewrite.gradle.Assertions} exposes a single {@code static final} {@link GradleParser.Builder}
 * that every {@code gradle(...)} source spec reuses. {@code RewriteTest} clones that builder per test via
 * {@code sourceSpec.getParser().clone()} "to ensure that no state leaks between tests", and JUnit Jupiter runs
 * a class's test methods concurrently when {@code junit.jupiter.execution.parallel} is enabled (as
 * rewrite-java-security does).
 * <p>
 * {@link GroovyParser.Builder#clone()} and {@link KotlinParser.Builder#clone()} each clone their
 * {@link JavaTypeCache}, but {@link GradleParser.Builder} inherited a shallow {@link Object#clone()} that left
 * both sub-builders — and therefore their single shared {@link JavaTypeCache} — aliased across every clone.
 * Concurrent test methods then wrote the one shared cache's {@link org.openrewrite.internal.AdaptiveRadixTree}
 * from multiple threads at once, corrupting it and (with assertions enabled) tripping
 * {@code AdaptiveRadixTree$InternalNode.split}'s {@code assert commonPrefix < keyLength}.
 * <p>
 * Regression test for the 2026-07-20 rewrite-java-security CI failure (run 29768303424); the crash was
 * surfaced by openrewrite/rewrite#7878, which reworked {@code AdaptiveRadixTree} insert/split so that
 * concurrent-write corruption trips an assertion instead of silently returning wrong types.
 */
class GradleParserTypeCacheIsolationTest {

    @Test
    void cloneIsolatesGroovyAndKotlinTypeCaches() throws Exception {
        // Mirror org.openrewrite.gradle.Assertions#gradleParser: one shared builder that every gradle() spec
        // reuses and RewriteTest clones per test method.
        GradleParser.Builder shared = GradleParser.builder()
          .groovyParser(GroovyParser.builder())
          .kotlinParser(KotlinParser.builder());

        GradleParser.Builder cloneA = (GradleParser.Builder) shared.clone();
        GradleParser.Builder cloneB = (GradleParser.Builder) shared.clone();

        // The sub-builders — and their type caches — must be distinct across clones and from the original, so
        // that parsers built on different threads never write one another's AdaptiveRadixTree concurrently.
        assertThat(cloneA.groovyParser)
          .isNotSameAs(shared.groovyParser)
          .isNotSameAs(cloneB.groovyParser);
        assertThat(cloneA.kotlinParser)
          .isNotSameAs(shared.kotlinParser)
          .isNotSameAs(cloneB.kotlinParser);

        assertThat(typeCacheOf(cloneA.groovyParser))
          .isNotSameAs(typeCacheOf(shared.groovyParser))
          .isNotSameAs(typeCacheOf(cloneB.groovyParser));
        assertThat(typeCacheOf(cloneA.kotlinParser))
          .isNotSameAs(typeCacheOf(shared.kotlinParser))
          .isNotSameAs(typeCacheOf(cloneB.kotlinParser));
    }

    private static JavaTypeCache typeCacheOf(Object parserBuilder) throws Exception {
        Field field = parserBuilder.getClass().getDeclaredField("typeCache");
        field.setAccessible(true);
        return (JavaTypeCache) field.get(parserBuilder);
    }
}
