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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * {@link TypeUtils}' searches walk a type's supertype and interfaces. Java source cannot declare a
 * cyclic hierarchy, but a type model assembled from an incompletely attributed source, or from a
 * language whose tree is built on the Java one, can hold one. The walk also reaches the same type
 * once per path through the hierarchy, which is exponential in a diamond lattice.
 */
class TypeUtilsRecursiveHierarchyTest {

    private static JavaType.Class type(String fqn, JavaType.FullyQualified.Kind kind,
                                       JavaType.@Nullable FullyQualified supertype,
                                       @Nullable List<JavaType.FullyQualified> interfaces) {
        return new JavaType.Class(null, 0L, fqn, kind, null, supertype, null, null, interfaces, null, null);
    }

    @Test
    void cyclicSupertype() {
        JavaType.Class parent = type("acme.Parent", JavaType.FullyQualified.Kind.Class, null, null);
        JavaType.Class child = type("acme.Child", JavaType.FullyQualified.Kind.Class, parent, null);
        // Parent's supertype is Child, closing the cycle
        parent.unsafeSet((List<JavaType>) null, child, null, null, null, null, null);

        assertThat(TypeUtils.findDeclaredMethod(child, "noSuchMethod", emptyList())).isEmpty();
    }

    @Test
    void cyclicInterface() {
        JavaType.Class i1 = type("acme.I1", JavaType.FullyQualified.Kind.Interface, null, null);
        JavaType.Class i2 = type("acme.I2", JavaType.FullyQualified.Kind.Interface, null, singletonList(i1));
        // I1 extends I2, which extends I1
        i1.unsafeSet((List<JavaType>) null, null, null, null, singletonList((JavaType.FullyQualified) i2), null, null);

        assertThat(TypeUtils.findDeclaredMethod(i2, "noSuchMethod", emptyList())).isEmpty();
    }

    @Test
    void diamondLatticeIsNotSearchedOncePerPath() {
        // 30 levels of two interfaces each extending both of the level below: 62 distinct interfaces,
        // but 2^30 distinct paths from the bottom. Searching every path takes minutes.
        JavaType.FullyQualified a = type("acme.A0", JavaType.FullyQualified.Kind.Interface, null, null);
        JavaType.FullyQualified b = type("acme.B0", JavaType.FullyQualified.Kind.Interface, null, null);
        for (int i = 1; i <= 30; i++) {
            List<JavaType.FullyQualified> below = Arrays.asList(a, b);
            JavaType.FullyQualified nextA = type("acme.A" + i, JavaType.FullyQualified.Kind.Interface, null, below);
            b = type("acme.B" + i, JavaType.FullyQualified.Kind.Interface, null, below);
            a = nextA;
        }
        JavaType.Class bottom = type("acme.Bottom", JavaType.FullyQualified.Kind.Class,
          JavaType.ShallowClass.build("java.lang.Object"), Arrays.asList(a, b));

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
          assertThat(TypeUtils.findDeclaredMethod(bottom, "noSuchMethod", emptyList())).isEmpty());
    }

    private static JavaType.Method method(JavaType.FullyQualified declaringType, String name) {
        return new JavaType.Method(null, 1L, declaringType, name, JavaType.Primitive.Void,
                (List<String>) null, (List<JavaType>) null, null, null, null, null);
    }

    /**
     * The shape in oracle/coherence-operator, where the Go interfaces {@code ServerRunner} and
     * {@code ClientSimulator} each declare {@code Run(context.Context) error} and neither embeds the
     * other, so each is mapped as extending the other.
     */
    private static JavaType.Class mutuallyExtendingInterfaces() {
        JavaType.Class clientSimulator = type("acme.ClientSimulator", JavaType.FullyQualified.Kind.Interface, null, null);
        JavaType.Class serverRunner = type("acme.ServerRunner", JavaType.FullyQualified.Kind.Interface, null, null);
        clientSimulator.unsafeSet((List<JavaType>) null, null, null, null,
                singletonList((JavaType.FullyQualified) serverRunner), null, singletonList(method(clientSimulator, "run")));
        serverRunner.unsafeSet((List<JavaType>) null, null, null, null,
                singletonList((JavaType.FullyQualified) clientSimulator), null, singletonList(method(serverRunner, "run")));

        JavaType.Class simulator = type("acme.clusterMemberSimulator", JavaType.FullyQualified.Kind.Class, null,
                Arrays.asList(clientSimulator, serverRunner));
        simulator.unsafeSet((List<JavaType>) null, null, null, null,
                Arrays.asList(clientSimulator, serverRunner), null, singletonList(method(simulator, "run")));
        return simulator;
    }

    @Test
    void mutuallyExtendingInterfacesDoNotMakeAnEndlessOverrideChain() {
        JavaType.Method run = mutuallyExtendingInterfaces().getMethods().get(0);

        // the walk RemoveBuiltInModuleBeans and three recipes in rewrite-testing-frameworks do; every
        // call answers with a method genuinely declared in a parent, so none of them misbehaves alone
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            int hops = 0;
            for (JavaType.Method overridden = TypeUtils.findOverriddenMethod(run).orElse(null);
                 overridden != null;
                 overridden = TypeUtils.findOverriddenMethod(overridden).orElse(null)) {
                assertThat(++hops)
                        .withFailMessage("the override chain did not end")
                        .isLessThan(100);
            }
        });
    }

    @Test
    void mutuallyExtendingInterfacesDoNotOverflowIsAssignableTo() {
        JavaType.Class simulator = mutuallyExtendingInterfaces();

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThat(TypeUtils.isAssignableTo("acme.Unrelated", simulator)).isFalse());
    }

    @Test
    void mutuallyExtendingInterfacesDoNotOverflowIsAssignableToType() {
        JavaType.Class simulator = mutuallyExtendingInterfaces();
        JavaType.Class unrelated = type("acme.Unrelated", JavaType.FullyQualified.Kind.Class, null, null);

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThat(TypeUtils.isAssignableTo(unrelated, simulator)).isFalse());
    }

    @Test
    void mutuallyExtendingInterfacesDoNotOverflowIsAssignableToPredicate() {
        JavaType.Class simulator = mutuallyExtendingInterfaces();

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThat(TypeUtils.isAssignableTo(t -> false, simulator)).isFalse());
    }

    @Test
    void mutuallyExtendingInterfacesDoNotOverflowIsOfTypeWithName() {
        JavaType.Class simulator = mutuallyExtendingInterfaces();

        // what MethodMatcher and TypeMatcher reach the hierarchy through
        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThat(TypeUtils.isOfTypeWithName(simulator, true, fqn -> false)).isFalse());
    }
}
