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

import java.util.function.Function;

/**
 * The <code>Either</code> type represents a value of one of two possible types (a disjoint union).
 * The data constructors; <code>Left</code> and <code>Right</code> represent the two possible
 * values. The <code>Either</code> type is often used as an alternative to
 * <code>scala.Option</code> where <code>Left</code> represents failure (by convention) and
 * <code>Right</code> is akin to <code>Some</code>.
 *
 * @version %build.number%
 */
public abstract class Either<A, B> {
    private Either() {

    }

    /**
     * Returns <code>true</code> if this either is a left, <code>false</code> otherwise.
     *
     * @return <code>true</code> if this either is a left, <code>false</code> otherwise.
     */
    public abstract boolean isLeft();

    /**
     * Returns <code>true</code> if this either is a right, <code>false</code> otherwise.
     *
     * @return <code>true</code> if this either is a right, <code>false</code> otherwise.
     */
    public abstract boolean isRight();

    /**
     * The catamorphism for either. Folds over this either breaking into left or right.
     *
     * @param left  The function to call if this is left.
     * @param right The function to call if this is right.
     * @return The reduced value.
     */
    public abstract <X> X either(final Function<A, X> left, final Function<B, X> right);

    /**
     * If this is a left, then return the left value in right, or vice versa.
     *
     * @return The value of this either swapped to the opposing side.
     */
    public final Either<B, A> swap() {
        return either(right_(), left_());
    }

    private static final class Left<A, B> extends Either<A, B> {
        private final A a;

        Left(final A a) {
            this.a = a;
        }

        public boolean isLeft() {
            return true;
        }

        public boolean isRight() {
            return false;
        }

        @Override
        public <X> X either(Function<A, X> left, Function<B, X> right) {
            return left.apply(a);
        }
    }

    private static final class Right<A, B> extends Either<A, B> {
        private final B b;

        Right(final B b) {
            this.b = b;
        }

        public boolean isLeft() {
            return false;
        }

        public boolean isRight() {
            return true;
        }

        @Override
        public <X> X either(Function<A, X> left, Function<B, X> right) {
            return right.apply(b);
        }
    }


    /**
     * Construct a left value of either.
     *
     * @param a The value underlying the either.
     * @return A left value of either.
     */
    public static <A, B> Either<A, B> left(final A a) {
        return new Left<>(a);
    }

    /**
     * A function that constructs a left value of either.
     *
     * @return A function that constructs a left value of either.
     */
    public static <A, B> Function<A, Either<A, B>> left_() {
        return Either::left;
    }

    /**
     * A function that constructs a right value of either.
     *
     * @return A function that constructs a right value of either.
     */
    public static <A, B> Function<B, Either<A, B>> right_() {
        return Either::right;
    }

    /**
     * Construct a right value of either.
     *
     * @param b The value underlying the either.
     * @return A right value of either.
     */
    public static <A, B> Either<A, B> right(final B b) {
        return new Right<>(b);
    }

}

