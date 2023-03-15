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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @param <E> Failing type
 * @param <V> Success type
 */
@Immutable
public interface Validation<E, V> {
    /**
     * @return true if the validation is invalid, false otherwise
     */
    default boolean isFail() {
        return !isSuccess();
    }

    /**
     * @return true if the validation is successful, false otherwise
     */
    boolean isSuccess();

    /**
     * @return the failure if the validation is invalid, or an exception if the validation is valid
     */
    E fail();

    /**
     * @return the value if the validation is valid, or an exception if the validation is invalid
     */
    V success();

    /**
     * The catamorphism for validation. Folds over this validation breaking into left or right.
     *
     * @param fail    The function to call if this failed.
     * @param success The function to call if this succeeded.
     * @return The reduced value.
     */
    <X> X validation(final Function<E, X> fail, final Function<V, X> success);

    /**
     * Returns a failing projection of this validation.
     *
     * @return a failing projection of this validation.
     */
    default FailProjection<E, V> f() {
        return new FailProjection<>(this);
    }

    default V orSuccess(V defaultValue) {
        return isSuccess() ? success() : defaultValue;
    }

    default V orSuccess(Function<E, V> f) {
        Objects.requireNonNull(f, "f cannot be null");
        return isSuccess() ? success() : f.apply(fail());
    }

    default <A> Validation<E, A> map(Function<V, A> f) {
        Objects.requireNonNull(f, "f cannot be null");
        return isFail() ? fail(fail()) : success(f.apply(success()));
    }

    default <A> Validation<E, A> bind(Function<V, Validation<E, A>> f) {
        Objects.requireNonNull(f, "f cannot be null");
        return isSuccess() ? f.apply(success()) : fail(fail());
    }

    default <A> Validation<E, A> apply(final Validation<E, Function<V, A>> v) {
        Objects.requireNonNull(v, "v cannot be null");
        return v.bind(this::map);
    }

    default Optional<V> toOptional() {
        return isSuccess() ? Optional.of(success()) : Optional.empty();
    }

    /**
     * Function application on the successful side of this validation, or accumulating the errors on the failing side
     * using the given semigroup should one or more be encountered.
     *
     * @param s The semigroup to accumulate errors with if
     * @param v The validating function to apply.
     * @return A failing validation if this or the given validation failed (with errors accumulated if both) or a
     *         succeeding validation if both succeeded.
     */
    default <A> Validation<E, A> accumapply(final Semigroup<E> s, final Validation<E, Function<V, A>> v) {
        return isFail() ?
                Validation.fail(v.isFail() ?
                        s.sum(v.fail(), fail()) :
                        fail()) :
                v.isFail() ?
                        Validation.fail(v.fail()) :
                        Validation.success(v.success().apply(success()));
    }

    /**
     * Accumulates errors on the failing side of this or any given validation if one or more are encountered, or applies
     * the given function if all succeeded and returns that value on the successful side.
     *
     * @param s  The semigroup to accumulate errors with if one or more validations fail.
     * @param va The second validation to accumulate errors with if it failed.
     * @param f  The function to apply if all validations have succeeded.
     * @return A succeeding validation if all validations succeeded, or a failing validation with errors accumulated if
     *         one or more failed.
     */
    default <A, B> Validation<E, B> accumulate(final Semigroup<E> s, final Validation<E, A> va, final Function<V, Function<A, B>> f) {
        return va.accumapply(s, map(f));
    }

    static <E, V> Validation<E, V> success(V value) {
        return new ValidationSuccess<>(value);
    }

    static <E, V> Validation<E, V> fail(E error) {
        return new ValidationFail<>(error);
    }

    /**
     * A failing projection of a validation.
     */
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FailProjection<E, T> {
        private final Validation<E, T> v;
        /**
         * Returns the underlying validation.
         *
         * @return The underlying validation.
         */
        public Validation<E, T> validation() {
            return v;
        }

        /**
         * Returns the failing value or the given value.
         *
         * @param e The value to return if this is success.
         * @return The failing value or the given value.
         */
        public E orFail(final Supplier<E> e) {
            return v.isFail() ? v.fail() : e.get();
        }

        /**
         * Returns the failing value or the given value.
         *
         * @param e The value to return if this is success.
         * @return The failing value or the given value.
         */
        public E orFail(final E e) {
            return orFail(() ->e);
        }

        /**
         * The failing value or the application of the given function to the success value.
         *
         * @param f The function to execute on the success value.
         * @return The failing value or the application of the given function to the success value.
         */
        public E on(final Function<T, E> f) {
            return v.isFail() ? v.fail() : f.apply(v.success());
        }

        /**
         * Maps the given function across the failing side of this validation.
         *
         * @param f The function to map.
         * @return A new validation with the function mapped.
         */
        public <A> Validation<A, T> map(final Function<E, A> f) {
            Objects.requireNonNull(f, "f cannot be null");
            return v.isFail() ? Validation.fail(f.apply(v.fail())) : Validation.success(v.success());
        }

        /**
         * Binds the given function across this validation's failing value if it has one.
         *
         * @param f The function to bind across this validation.
         * @return A new validation value after binding.
         */
        public <A> Validation<A, T> bind(final Function<E, Validation<A, T>> f) {
            return v.isFail() ? f.apply(v.fail()) : Validation.success(v.success());
        }

        /**
         * Performs a bind across the validation, but ignores the element value in the function.
         *
         * @param v The validation value to apply in the final join.
         * @return A new validation value after the final join.
         */
        public <A> Validation<A, T> sequence(final Validation<A, T> v) {
            return bind(e1 -> v);
        }


        /**
         * Function application on the failing value.
         *
         * @param v The validation of the function to apply on the failing value.
         * @return The result of function application in validation.
         */
        public <A> Validation<A, T> apply(final Validation<Function<E, A>, T> v) {
            return v.f().bind(this::map);
        }

    }

}

@ToString
@EqualsAndHashCode
final class ValidationSuccess<E, V> implements Validation<E, V> {
    private final V value;

    ValidationSuccess(V value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public E fail() {
        throw new IllegalStateException("Cannot fail a successful validation");
    }

    @Override
    public V success() {
        return value;
    }

    @Override
    public <X> X validation(Function<E, X> fail, Function<V, X> success) {
        Objects.requireNonNull(fail, "fail cannot be null");
        Objects.requireNonNull(success, "success cannot be null");
        return success.apply(value);
    }
}

@ToString
@EqualsAndHashCode
final class ValidationFail<E, V> implements Validation<E, V> {
    private final E error;

    ValidationFail(E error) {
        this.error = Objects.requireNonNull(error, "error cannot be null");
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public E fail() {
        return error;
    }

    @Override
    public V success() {
        throw new IllegalStateException("Cannot get the success value of a failed validation");
    }

    @Override
    public <X> X validation(Function<E, X> fail, Function<V, X> success) {
        Objects.requireNonNull(fail, "fail cannot be null");
        Objects.requireNonNull(success, "success cannot be null");
        return fail.apply(error);
    }
}
