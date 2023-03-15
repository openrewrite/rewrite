/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.trait.util;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementations must satisfy the law of associativity:
 * <ul>
 * <li><em>Associativity</em>; forall x. forall y. forall z. sum(sum(x, y), z) == sum(x, sum(y, z))</li>
 * </ul>
 */
public final class Semigroup<A> {

    /**
     * Primitives functions of Semigroup: minimal definition and overridable methods.
     */
    public interface Definition<A> {

        A append(A a1, A a2);

        default Function<A, A> prepend(A a) {
            return a2 -> append(a, a2);
        }

        default A multiply1p(int n, A a) {
            if (n <= 0) {
                return a;
            }

            A xTmp = a;
            int yTmp = n;
            A zTmp = a;
            while (true) {
                if ((yTmp & 1) == 1) {
                    zTmp = append(xTmp, zTmp);
                    if (yTmp == 1) {
                        return zTmp;
                    }
                }
                xTmp = append(xTmp, xTmp);
                yTmp = (yTmp) >>> 1;
            }
        }

        default Definition<A> dual() {
            return new Definition<A>() {

                @Override
                public A append(A a1, A a2) {
                    return Definition.this.append(a2, a1);
                }

                @Override
                public A multiply1p(int n, A a) {
                    return Definition.this.multiply1p(n, a);
                }
            };
        }
    }

    /**
     * Primitives functions of Semigroup: alternative minimal definition and overridable methods.
     */
    public interface AltDefinition<A> extends Definition<A> {
        @Override
        Function<A, A> prepend(A a);

        @Override
        default A append(A a1, A a2) {
            return prepend(a1).apply(a2);
        }
    }

    private final Definition<A> def;

    private Semigroup(final Definition<A> def) {
        this.def = def;
    }

    /**
     * Sums the two given arguments.
     *
     * @param a1 A value to sum with another.
     * @param a2 A value to sum with another.
     * @return The of the two given arguments.
     */
    public A sum(final A a1, final A a2) {
        return def.append(a1, a2);
    }

    /**
     * Returns a function that sums the given value according to this semigroup.
     *
     * @param a1 The value to sum.
     * @return A function that sums the given value according to this semigroup.
     */
    public Function<A, A> sum(final A a1) {
        return def.prepend(a1);
    }

    /**
     * Returns a function that sums according to this semigroup.
     *
     * @return A function that sums according to this semigroup.
     */
    public Function<A, Function<A, A>> sum() {
        return def::prepend;
    }

    /**
     * Returns a value summed <code>n + 1</code> times (
     * <code>a + a + ... + a</code>) The default definition uses peasant
     * multiplication, exploiting associativity to only require {@code O(log n)} uses of
     * {@link #sum(Object, Object)}.
     *
     * @param n multiplier
     * @param a the value to be reapeatly summed n + 1 times
     * @return {@code a} summed {@code n} times. If {@code n <= 0}, returns
     * {@code zero()}
     */
    public A multiply1p(int n, A a) {
        return def.multiply1p(n, a);
    }

    public <B, C> Semigroup<C> compose(Semigroup<B> sb, final Function<C, B> b, final Function<C, A> a, final BiFunction<A, B, C> c) {
        Definition<A> saDef = this.def;
        Definition<B> sbDef = sb.def;
        return semigroupDef(new Definition<C>() {

            @Override
            public C append(C c1, C c2) {
                return c.apply(saDef.append(a.apply(c1), a.apply(c2)), sbDef.append(b.apply(c1), b.apply(c2)));
            }

            @Override
            public Function<C, C> prepend(C c1) {
                Function<A, A> prependA = saDef.prepend(a.apply(c1));
                Function<B, B> prependB = sbDef.prepend(b.apply(c1));
                return c2 -> c.apply(prependA.apply(a.apply(c2)), prependB.apply(b.apply(c2)));
            }

            @Override
            public C multiply1p(int n, C c1) {
                return c.apply(saDef.multiply1p(n, a.apply(c1)), sbDef.multiply1p(n, b.apply(c1)));
            }
        });
    }

    /**
     * Constructs a semigroup from the given definition.
     *
     * @param def The definition to construct this semigroup with.
     * @return A semigroup from the given definition.
     */
    public static <A> Semigroup<A> semigroupDef(final Definition<A> def) {
        return new Semigroup<>(def);
    }

    /**
     * Constructs a semigroup from the given definition.
     *
     * @param def The definition to construct this semigroup with.
     * @return A semigroup from the given definition.
     */
    public static <A> Semigroup<A> semigroupDef(final AltDefinition<A> def) {
        return new Semigroup<>(def);
    }

}
