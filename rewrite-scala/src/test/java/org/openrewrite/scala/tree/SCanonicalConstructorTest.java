/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enforces the universal OpenRewrite LST convention that every concrete {@code S} node exposes a
 * <b>public canonical all-args constructor</b> over its non-transient instance fields, in declaration
 * order. The Moderne CLI's V3 tree-serialization codegen reflects over every concrete {@code Tree}
 * subclass and only includes a type if it can find a constructor whose parameter count equals the
 * number of non-transient instance fields; types without one are silently dropped (lossy V3 output).
 * <p>
 * This guards against regressions like the six types fixed in
 * <a href="https://github.com/openrewrite/rewrite/pull/7869">#7869</a>, regardless of whether the
 * constructor is provided by {@code @RequiredArgsConstructor}, {@code @Data}, or a hand-written ctor.
 */
class SCanonicalConstructorTest {

    @TestFactory
    Stream<DynamicTest> everyConcreteSTypeHasPublicCanonicalConstructor() {
        return Arrays.stream(S.class.getDeclaredClasses())
                .filter(c -> S.class.isAssignableFrom(c))
                .filter(c -> !c.isInterface() && !Modifier.isAbstract(c.getModifiers()))
                .map(c -> DynamicTest.dynamicTest(c.getSimpleName(), () -> {
                    long fieldCount = Arrays.stream(c.getDeclaredFields())
                            .filter(f -> !Modifier.isStatic(f.getModifiers()))
                            .filter(f -> !Modifier.isTransient(f.getModifiers()))
                            .count();
                    boolean hasPublicCanonical = Arrays.stream(c.getDeclaredConstructors())
                            .anyMatch(ctor -> Modifier.isPublic(ctor.getModifiers()) &&
                                    ctor.getParameterCount() == fieldCount);
                    assertThat(hasPublicCanonical)
                            .as("%s must expose a public canonical all-args constructor " +
                                    "(arity == %d non-transient fields) for V3 serialization",
                                    c.getSimpleName(), fieldCount)
                            .isTrue();
                }));
    }
}
