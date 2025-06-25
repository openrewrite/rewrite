/*
 * Copyright 2025 the original author or authors.
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ThreadLocal wrapper for managed resources that need lifecycle management.
 * <p>
 * Designed for AutoCloseable resources like database connections, file handles,
 * network connections, etc. that require proper cleanup.
 * <p>
 * Typical usage:
 * <pre>
 * // At entry point (e.g., request handler)
 * try (var managed = DatabaseConnection.current().requireOrCreate(() -> new DatabaseConnection(...))) {
 *     // Application code can use require() throughout call chain
 *     processRequest();
 * } // Automatic cleanup of both ThreadLocal and resource
 *
 * // In application code
 * public void processRequest() {
 *     DatabaseConnection conn = DatabaseConnection.current().require();
 *     conn.executeQuery("SELECT ...");
 * }
 * </pre>
 */
public class ManagedThreadLocal<T extends AutoCloseable> {
    private final ThreadLocal<@Nullable T> threadLocal = new ThreadLocal<>();

    /**
     * Gets the current resource, throwing if not present.
     * <p>
     * This is the primary method for accessing the resource throughout your call chain.
     * It fails fast with a clear error if no resource has been established.
     *
     * @return the current resource
     * @throws IllegalStateException if no resource is present in the current thread
     */
    public T require() {
        T value = threadLocal.get();
        if (value == null) {
            throw new IllegalStateException("No managed resource found in current thread context");
        }
        return value;
    }

    /**
     * Gets or creates a managed resource with automatic cleanup.
     * <p>
     * If a resource already exists, returns it in a scope with no-op cleanup.
     * If no resource exists, creates one using the factory and sets up automatic cleanup
     * of both the ThreadLocal state and the resource itself when the returned
     * Scope is closed.
     * <p>
     * Use this at entry points where you want to establish the resource context.
     *
     * @param factory supplier to create the resource if none exists
     * @return a Scope that provides access to the resource and handles cleanup
     */
    public Scope<T> requireOrCreate(Supplier<T> factory) {
        T existing = threadLocal.get();
        if (existing != null) {
            // Return existing value with no-op cleanup
            return new Scope<>(existing, () -> {
            });
        }

        T newValue = factory.get();
        threadLocal.set(newValue);

        return new Scope<>(newValue, () -> {
            try {
                threadLocal.remove();
            } finally {
                newValue.close(); // Close the resource
            }
        });
    }

    /**
     * Temporarily replace the resource, returning an AutoCloseable that restores the original.
     * <p>
     * Use this when you need to temporarily use a different resource within a scope.
     * The original resource is automatically restored when the returned AutoCloseable is closed.
     * <p>
     * Note: This does NOT close the temporary resource - you're responsible for its lifecycle.
     *
     * @param value the temporary resource to use
     * @return a Scope that restores the previous resource when closed
     */
    public Scope<T> using(T value) {
        T previousValue = threadLocal.get();
        threadLocal.set(value);

        return new Scope<>(value, () -> {
            if (previousValue != null) {
                threadLocal.set(previousValue);
            } else {
                threadLocal.remove();
            }
        });
    }

    /**
     * Provides access to the underlying ThreadLocal for advanced use cases.
     * <p>
     * Use with caution - direct ThreadLocal manipulation bypasses the managed
     * resource lifecycle. Prefer the managed methods (require, requireOrCreate, using)
     * for typical usage.
     *
     * @return the underlying ThreadLocal instance
     */
    public ThreadLocal<@Nullable T> asThreadLocal() {
        return threadLocal;
    }

    /**
     * Checks if a resource is present in the current thread.
     *
     * @return true if a resource is present, false otherwise
     */
    public boolean isPresent() {
        return threadLocal.get() != null;
    }

    /**
     * A scoped resource that provides both the resource value and automatic cleanup.
     * <p>
     * This is returned by requireOrCreate() and using(). It handles both ThreadLocal
     * restoration and resource cleanup when the scope is closed.
     */
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Scope<T extends AutoCloseable> implements AutoCloseable {
        final T resource;
        final AutoCloseable cleanup;

        /**
         * Applies a function to the scoped resource and returns the result.
         * <p>
         * This provides functional access to the resource without exposing it directly.
         * The function is called with the current resource as its argument.
         * <p>
         * Example usage:
         * <pre>
         * try (var scope = DatabaseConnection.current().requireOrCreate(...)) {
         *     String result = scope.map(conn -> {
         *         conn.executeQuery("SELECT ...");
         *         return conn.getLastResult();
         *     });
         * }
         * </pre>
         *
         * @param <R> the type of the result
         * @param mapper function to apply to the scoped resource
         * @return the result of applying the function to the resource
         */
        public <R> R map(Function<T, R> mapper) {
            return mapper.apply(resource);
        }

        /**
         * Performs cleanup of both ThreadLocal state and the resource.
         * <p>
         * This is called automatically when used with try-with-resources.
         */
        @Override
        public void close() {
            try {
                cleanup.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}