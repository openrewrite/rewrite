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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TreeVisitorAdapterClassLoaderTest {

    @Test
    void resolvesAndMemoizesRegisteredMixin() {
        TreeVisitorAdapterClassLoader cl = new TreeVisitorAdapterClassLoader(getClass().getClassLoader());

        Optional<Class<?>> first = cl.mixinClass(MixinDelegate.class, MixinTarget.class);
        Optional<Class<?>> second = cl.mixinClass(MixinDelegate.class, MixinTarget.class);

        assertThat(first).contains(RegisteredMixin.class);
        // ClassValue memoizes per (delegate, adaptTo): the same Optional instance is
        // returned, proving the classpath scan ran only once.
        assertThat(second).isSameAs(first);
    }

    @Test
    void emptyWhenNoMixinRegistered() {
        TreeVisitorAdapterClassLoader cl = new TreeVisitorAdapterClassLoader(getClass().getClassLoader());
        assertThat(cl.mixinClass(UnregisteredDelegate.class, MixinTarget.class)).isEmpty();
    }
}

class MixinTarget extends TreeVisitor<Tree, Integer> {
}

class RegisteredMixin extends MixinTarget {
}

class MixinDelegate extends TreeVisitor<Tree, Integer> {
}

class UnregisteredDelegate extends TreeVisitor<Tree, Integer> {
}
