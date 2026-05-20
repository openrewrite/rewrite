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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.KotlinParser;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for <a href="https://github.com/openrewrite/rewrite/issues/7730">#7730</a>:
 * Kotlin's FIR reports a Java method's {@code String} parameter as {@code kotlin.String}, but
 * {@code KotlinTypeMapping} remaps it to {@code java.lang.String} for Java-origin classes so
 * {@code MethodMatcher} patterns written against Java FQNs match. Constructors went through
 * that remap already; the static-method path did not, leaving {@code kotlin.String} in
 * {@code JavaType.Method.parameterTypes} for calls like {@code Foo.bar("...")}.
 */
class JavaStringParameterMappingTest {

    @Test
    void javaStringParametersNormalisedForStaticMethodCalls() {
        AtomicReference<JavaType.Method> staticCall = new AtomicReference<>();
        AtomicReference<JavaType.Method> ctorCall = new AtomicReference<>();

        KotlinParser.builder()
          .classpathFromResources(new InMemoryExecutionContext(), "jakarta.persistence-api")
          .build()
          .parse(
            "import jakarta.persistence.Persistence\n" +
            "import jakarta.persistence.PersistenceException\n" +
            "class X {\n" +
            "    fun a() = Persistence.createEntityManagerFactory(\"unit\")\n" +
            "    fun b() = PersistenceException(\"msg\")\n" +
            "}\n"
          )
          .forEach(sf -> new KotlinIsoVisitor<Integer>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, Integer p) {
                  if ("createEntityManagerFactory".equals(mi.getSimpleName())) {
                      staticCall.set(mi.getMethodType());
                  }
                  return super.visitMethodInvocation(mi, p);
              }

              @Override
              public J.NewClass visitNewClass(J.NewClass nc, Integer p) {
                  ctorCall.set(nc.getConstructorType());
                  return super.visitNewClass(nc, p);
              }
          }.visit(sf, 0));

        assertThat(ctorCall.get()).isNotNull();
        assertThat(ctorCall.get().getParameterTypes().get(0).toString())
          .isEqualTo("java.lang.String");

        assertThat(staticCall.get()).isNotNull();
        assertThat(staticCall.get().getParameterTypes().get(0).toString())
          .isEqualTo("java.lang.String");
    }
}
