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
package org.openrewrite.internal;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class TreeVisitorAdapterClassLoaderTest {

    @Test
    void resolvesMixinClassOncePerKey() {
        TreeVisitorAdapterClassLoader cl = new TreeVisitorAdapterClassLoader(getClass().getClassLoader());
        AtomicInteger resolverCalls = new AtomicInteger();
        Function<String, Optional<Class<?>>> resolver = key -> {
            resolverCalls.incrementAndGet();
            return Optional.empty();
        };

        assertThat(cl.mixinClass("delegate\nadaptTo", resolver)).isEmpty();
        assertThat(cl.mixinClass("delegate\nadaptTo", resolver)).isEmpty();

        assertThat(resolverCalls.get()).isEqualTo(1);
    }
}
