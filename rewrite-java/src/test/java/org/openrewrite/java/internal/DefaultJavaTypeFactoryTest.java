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
package org.openrewrite.java.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultJavaTypeFactoryTest {

    /**
     * A method's attribution can recurse back to its own signature: an
     * annotation-element method whose own annotations reference an annotation
     * whose element is that same method — the Spring {@code @AliasFor} shape,
     * where {@code value()} is annotated {@code @AliasFor} and {@code @AliasFor}'s
     * element is {@code value()}.
     * <p>
     * {@link DefaultJavaTypeFactory#methodFor} must cache the in-progress stub
     * before running the initializer so the re-entrant lookup on the same
     * signature resolves to that stub instead of recursing until the stack
     * overflows — the same signature+cache cycle-breaking {@code computeClass}
     * relies on.
     */
    @Test
    void selfReferentialMethodResolvesToInProgressStub() {
        DefaultJavaTypeFactory factory = new DefaultJavaTypeFactory(new JavaTypeCache());
        String signature = "com.Example{name=value,return=int,parameters=[]}";

        JavaType.Method method = factory.methodFor(signature,
                () -> new JavaType.Method(null, 0, null, "value", null,
                        (List<String>) null, null, null, null, null, null),
                m -> {
                    // Re-enter for the same signature mid-attribution. The cache must
                    // hand back the in-progress stub; neither the stub supplier nor the
                    // initializer may run again (that re-entry is the stack overflow).
                    JavaType.Method reentrant = factory.methodFor(signature,
                            () -> {
                                throw new AssertionError("stub supplier must not run on re-entry");
                            },
                            again -> {
                                throw new AssertionError("initializer must not run on re-entry");
                            });
                    assertThat(reentrant).isSameAs(m);
                    m.unsafeSet(null, JavaType.Primitive.Int, (List<JavaType>) null, null, null);
                });

        // The same canonical, fully-attributed instance is served from cache afterward.
        JavaType.Method cached = factory.methodFor(signature,
                () -> {
                    throw new AssertionError("should be served from cache");
                },
                m -> {
                    throw new AssertionError("should be served from cache");
                });
        assertThat(cached).isSameAs(method);
        assertThat(method.getReturnType()).isEqualTo(JavaType.Primitive.Int);
    }
}
