/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.StreamSupport.stream;

/**
 * A container object which may or may not contain a valid value.
 * If a value is valid, {@link #isValid()} returns {@code true}.
 * If the value is invalid, the object is considered <i>invalid</i> and
 * {@link #isValid()} returns {@code false}.
 *
 * @param <T> the type of value being validated
 */
public interface Validated<T> extends Iterable<Validated<T>> {
    boolean isValid();

    default boolean isInvalid() {
        return !isValid();
    }

    default List<Invalid<T>> failures() {
        List<Invalid<T>> list = new ArrayList<>();
        for (Validated<T> v : this) {
            if (v.isInvalid()) {
                list.add((Invalid<T>) v);
            }
        }
        return list;
    }

    @SuppressWarnings("unused")
    static Secret validSecret(String property, String value) {
        return new Secret(property, value);
    }

    static <T> None<T> none() {
        return new None<>();
    }

    static <T> Valid<T> valid(String property, @Nullable T value) {
        return new Valid<>(property, value);
    }


    /**
     * Validate that the Predicate will evaluate to 'true' on the supplied value.
     * When the Predicate evaluates to 'false' the error message will be of the form:
     * <p>
     * "[property] was '[value]' but it [message]"
     *
     * @param property The property name to test
     * @param message  The failure message if the test doesn't pass
     * @param value    The value of the property
     * @param test     The test predicate
     * @param <T>      The property value type.
     * @return A validation result
     */
    static <T> Validated<T> test(String property, String message, @Nullable T value, Predicate<T> test) {
        return test.test(value) ?
                valid(property, value) :
                invalid(property, value, message.replace("{}", value == null ? "null" : value.toString()));
    }

    /**
     * Validate that the Predicate will evaluate to 'true' on the supplied value.
     * Will return a {@link None} if the value is valid.
     * <p>
     * This allows validation of a value that is not the intended return value.
     * <p>
     * When the Predicate evaluates to 'false' the error message will be of the form:
     * <p>
     * "[property] was '[value]' but it [message]"
     *
     * @param property The property name to test
     * @param message  The failure message if the test doesn't pass
     * @param value    The value of the property
     * @param test     The test predicate
     * @param <T>      The property value type.
     * @return A validation result
     */
    static <T, V> Validated<T> testNone(String property, String message, @Nullable V value, Predicate<V> test) {
        return test.test(value) ?
                new None<>() :
                invalid(property, value, message.replace("{}", value == null ? "null" : value.toString()));
    }

    static <T> Validated<T> required(String property, @Nullable T value) {
        return value != null ?
                valid(property, value) :
                missing(property, null, "is required");
    }

    static Validated<String> notBlank(String property, @Nullable String value) {
        return test(property, "must not be blank", value, s -> value != null && !StringUtils.isBlank(value));
    }

    static <T> Missing<T> missing(String property, @Nullable T value, String message) {
        return new Missing<>(property, value, message);
    }

    static <T> Invalid<T> invalid(String property, @Nullable Object value, String message) {
        return invalid(property, value, message, null);
    }

    static <T> Invalid<T> invalid(String property, @Nullable Object value, String message,
                                  @Nullable Throwable exception) {
        return new Invalid<>(property, value, message, exception);
    }

    @SuppressWarnings("unchecked")
    default Validated<T> and(Validated<? extends T> validated) {
        if (validated instanceof None) {
            return this;
        }
        return new Both<>(this, (Validated<T>) validated);
    }

    @SuppressWarnings("unchecked")
    default Validated<T> or(Validated<? extends T> validated) {
        if (validated instanceof None) {
            return this;
        }
        return new Either<>(this, (Validated<T>) validated);
    }

    @Nullable T getValue();

    /**
     * Indicates that no validation has occurred. None is considered "valid", effectively a no-op validation.
     */
    class None<T> implements Validated<T> {
        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Iterator<Validated<T>> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public T getValue() {
            throw new IllegalStateException("Value does not exist");
        }

        @Override
        public Validated<T> or(Validated<? extends T> validated) {
            if (validated instanceof None) {
                return this;
            }
            //noinspection unchecked
            return (Validated<T>) validated;
        }

        @Override
        public Validated<T> and(Validated<? extends T> validated) {
            if (validated instanceof None) {
                return this;
            }
            //noinspection unchecked
            return (Validated<T>) validated;
        }
    }

    /**
     * A specialization {@link Valid} that won't print the secret in plain text if the validation is serialized.
     */
    class Secret extends Valid<String> {
        public Secret(String property, String value) {
            super(property, value);
        }

        @Override
        public String toString() {
            return "Secret{" +
                    "property='" + property + '\'' +
                    '}';
        }
    }

    /**
     * A valid property value.
     */
    class Valid<T> implements Validated<T> {
        protected final String property;
        @Nullable
        private final T value;

        public Valid(String property, @Nullable T value) {
            this.property = property;
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Iterator<Validated<T>> iterator() {
            return Stream.of((Validated<T>) this).iterator();
        }

        public String getProperty() {
            return property;
        }

        @Override
        @Nullable
        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Valid{" +
                    "property='" + property + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    class Invalid<T> implements Validated<T> {
        private final String property;

        @Nullable
        private final Object value;

        private final String message;

        @Nullable
        private final Throwable exception;

        public Invalid(String property, @Nullable Object value, String message, @Nullable Throwable exception) {
            this.property = property;
            this.value = value;
            this.message = message;
            this.exception = exception;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public @Nullable T getValue() {
            throw new ValidationException(this);
        }

        @Override
        public Iterator<Validated<T>> iterator() {
            return Stream.of((Validated<T>) this).iterator();
        }

        public String getMessage() {
            return message;
        }

        public String getProperty() {
            return property;
        }

        @Nullable
        public Object getInvalidValue() {
            return value;
        }

        @Nullable
        public Throwable getException() {
            return exception;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "property='" + property + '\'' +
                    ", value='" + value + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    class Missing<T> extends Invalid<T> {
        public Missing(String property, @Nullable T value, String message) {
            super(property, value, message, null);
        }
    }

    class Either<T> implements Validated<T> {
        private final Validated<T> left;
        private final Validated<T> right;

        public Either(Validated<T> left, Validated<T> right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isValid() {
            return left.isValid() || right.isValid();
        }

        public Optional<Validated<T>> findAny() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.CONCURRENT), false)
                    .filter(Validated::isValid)
                    .findAny();
        }

        @Override
        public T getValue() {
            return findAny()
                    .map(Validated::getValue)
                    .orElseThrow(() -> new IllegalStateException("Value does not exist"));
        }

        @Override
        public Iterator<Validated<T>> iterator() {
            //If only one side is valid, this short circuits the invalid path.
            if (left.isValid() && right.isInvalid()) {
                return stream(left.spliterator(), false).iterator();
            } else if (left.isInvalid() && right.isValid()) {
                return stream(right.spliterator(), false).iterator();
            } else {
                //If both are valid/invalid, concat all validations.
                return Stream.concat(
                        stream(left.spliterator(), false),
                        stream(right.spliterator(), false)
                ).iterator();
            }
        }
    }

    class Both<T> implements Validated<T> {
        protected final Validated<T> left;
        protected final Validated<T> right;

        public Both(Validated<T> left, Validated<T> right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isValid() {
            return left.isValid() && right.isValid();
        }

        @Override
        public T getValue() {
            return right.getValue();
        }

        @Override
        public Iterator<Validated<T>> iterator() {
            return Stream.concat(
                    stream(left.spliterator(), false),
                    stream(right.spliterator(), false)
            ).iterator();
        }
    }
}
