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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
class DefaultJavaTypeFactoryTest {

    private static final String FQN = "com.example.Foo";

    private static JavaType.Class object(DefaultJavaTypeFactory factory) {
        return factory.computeClass("java.lang.Object", 0, JavaType.FullyQualified.Kind.Class, s -> {
        });
    }

    @Test
    void richAttributionUpgradesAThinStubInPlace() {
        DefaultJavaTypeFactory factory = new DefaultJavaTypeFactory(new JavaTypeCache());

        // A placeholder/facade path wins the race first: empty initializer => thin stub.
        JavaType.Class handedOut = factory.computeClass(FQN, 0, JavaType.FullyQualified.Kind.Class, stub -> {
        });
        assertThat(handedOut.getSupertype()).isNull();
        assertThat(handedOut.getMembers()).isEmpty();

        // A later, richer attribution arrives for the same FQN.
        JavaType.Class object = object(factory);
        JavaType.Variable field = new JavaType.Variable(null, 0, "bar", null, object, null);
        JavaType.Class upgraded = factory.computeClass(FQN, 1L, JavaType.FullyQualified.Kind.Interface, stub ->
                stub.unsafeSet(null, object, null, null, null, singletonList(field), null));

        // Same canonical instance is returned...
        assertThat(upgraded).isSameAs(handedOut);
        // ...and the reference handed out earlier now sees the richer attribution.
        assertThat(handedOut.getSupertype()).isSameAs(object);
        assertThat(handedOut.getMembers()).hasSize(1);
        assertThat(handedOut.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Interface);
    }

    @Test
    void placeholderDoesNotClobberAlreadyRichType() {
        DefaultJavaTypeFactory factory = new DefaultJavaTypeFactory(new JavaTypeCache());

        JavaType.Class object = object(factory);
        JavaType.Variable field = new JavaType.Variable(null, 0, "bar", null, object, null);
        JavaType.Class rich = factory.computeClass(FQN, 1L, JavaType.FullyQualified.Kind.Interface, stub ->
                stub.unsafeSet(null, object, null, null, null, singletonList(field), null));

        // A later placeholder/empty initializer must NOT wipe the rich attribution.
        JavaType.Class second = factory.computeClass(FQN, 0, JavaType.FullyQualified.Kind.Class, stub -> {
        });

        assertThat(second).isSameAs(rich);
        assertThat(second.getMembers()).hasSize(1);
        assertThat(second.getSupertype()).isSameAs(object);
        assertThat(second.getKind()).isEqualTo(JavaType.FullyQualified.Kind.Interface);
    }

    @Test
    void selfReferentialTypeDoesNotRecurse() {
        DefaultJavaTypeFactory factory = new DefaultJavaTypeFactory(new JavaTypeCache());
        JavaType.Class object = object(factory);

        // The initializer references its own FQN (e.g. a self-typed field); the
        // re-entrant computeClass must return the in-progress stub, not recurse.
        JavaType.Class self = factory.computeClass(FQN, 0, JavaType.FullyQualified.Kind.Class, stub -> {
            JavaType.Class reentrant = factory.computeClass(FQN, 0, JavaType.FullyQualified.Kind.Class, s -> {
            });
            assertThat(reentrant).isSameAs(stub);
            JavaType.Variable field = new JavaType.Variable(null, 0, "self", stub, stub, null);
            stub.unsafeSet(null, object, null, null, null, singletonList(field), null);
        });

        assertThat(self.getMembers()).hasSize(1);
        assertThat(self.getMembers().getFirst().getType()).isSameAs(self);
    }

    @Test
    void selfReferentialMethodDoesNotRecurse() {
        DefaultJavaTypeFactory factory = new DefaultJavaTypeFactory(new JavaTypeCache());

        // Mirrors Spring's @AliasFor: attributing a method re-enters the build for the same
        // signature (the method's annotations reference an annotation whose element is that
        // same method). Without an in-progress placeholder this overflows the stack.
        String signature = "com.example.AliasFor{name=value}";
        AtomicReference<Supplier<JavaType.Method>> builder = new AtomicReference<>();
        AtomicReference<JavaType.Method> reentrant = new AtomicReference<>();
        builder.set(() -> {
            reentrant.set(factory.methodFor(signature, builder.get()));
            return new JavaType.Method(null, 0, null, "value", null,
                    (List<String>) null, null, null, null, null, null);
        });

        JavaType.Method method = factory.methodFor(signature, builder.get());

        // The re-entrant lookup received the in-progress placeholder, which the build then
        // populated in place, so every reference resolves to one canonical instance.
        assertThat(method).isNotNull();
        assertThat(reentrant.get()).isSameAs(method);
        assertThat(method.getName()).isEqualTo("value");
    }
}
