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
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ThreadLocal wrapper for managed resources that need lifecycle management.
 * <p>
 * Designed for AutoCloseable resources like database connections, file handles,
 * network connections, etc. that require proper cleanup.
 * <p>
 * <strong>Resource Creation:</strong>
 * Resources are created lazily - the factory function is not called until the
 * resource is first accessed via the returned scope's {@code map()} method. This
 * avoids creating expensive resources that may never be used.
 * <p>
 * Typical usage:
 * <pre>
 * // At entry point (e.g., request handler) - resource NOT created yet
 * try (var scope = DatabaseConnection.current().requireOrCreate(() -> new DatabaseConnection(...))) {
 *     // Application code can conditionally use the resource
 *     if (needsDatabase()) {
 *         // Resource is created HERE on first access
 *         scope.map(conn -> {
 *             conn.executeQuery("SELECT ...");
 *             return conn.getResults();
 *         });
 *     }
 * } // Automatic cleanup of both ThreadLocal and resource (if created)
 *
 * // In application code that assumes resource context exists
 * public void processRequest() {
 *     DatabaseConnection conn = DatabaseConnection.current().require();
 *     conn.executeQuery("SELECT ...");
 * }
 * </pre>
 * <p>
 * <strong>Thread Safety:</strong>
 * This class uses ThreadLocal storage, so each thread maintains its own resource
 * instance. Resources are not shared between threads.
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
     * Requires a resource to exist, creating one if necessary with automatic cleanup.
     * <p>
     * If a resource already exists, returns it in a scope with no-op cleanup.
     * If no resource exists, defers creation until the resource is first accessed via
     * the returned scope's {@code map()} method. The resource is created using the
     * provided factory and automatic cleanup of both the ThreadLocal state and the
     * resource itself is handled when the scope is closed.
     * <p>
     * <strong>Deferred creation behavior:</strong>
     * <ul>
     * <li>The factory is <em>not</em> called when this method returns</li>
     * <li>Resource creation happens on first access via {@code scope.map()}</li>
     * <li>Once created, the same resource instance is used for subsequent accesses</li>
     * <li>If the scope is never accessed, no resource is created and no cleanup occurs</li>
     * </ul>
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

        // No existing resource - set up lazy creation
        AtomicReference<@Nullable T> resourceRef = new AtomicReference<>();

        return new Scope<>(
                (Supplier<T>) () -> {
                    T resource = resourceRef.get();
                    if (resource == null) {
                        resource = factory.get();
                        threadLocal.set(resource);
                        resourceRef.set(resource);
                    }
                    return resource;
                },
                () -> {
                    T resource = resourceRef.get();
                    if (resource != null) {
                        try {
                            threadLocal.remove();
                        } finally {
                            try {
                                resource.close();
                            } catch (Exception e) {
                                // Log appropriately in real implementation
                                System.err.println("Error closing managed resource: " + e.getMessage());
                            }
                        }
                    }
                }
        );
    }

    /**
     * Creates a new managed resource with automatic cleanup.
     * <p>
     * Always creates a new resource using the factory, regardless of whether a resource
     * already exists in the current thread. The new resource is created when first accessed
     * via the returned scope's {@code map()} method and will be automatically closed when
     * the scope is closed. Any existing resource is temporarily replaced and restored when
     * the scope closes.
     * <p>
     * <strong>Deferred creation behavior:</strong>
     * <ul>
     * <li>The factory is <em>not</em> called when this method returns</li>
     * <li>Resource creation happens on first access via {@code scope.map()}</li>
     * <li>Once created, the same resource instance is used for subsequent accesses</li>
     * <li>If the scope is never accessed, no resource is created and no cleanup occurs</li>
     * </ul>
     * <p>
     * This is useful when you need a guaranteed new resource instance, such as for
     * temporary operations that require isolation from the existing resource context.
     * <p>
     * Example usage:
     * <pre>
     * // Create a temporary database connection for audit logging
     * try (var scope = DatabaseConnection.current().create(() ->
     *         new DatabaseConnection("jdbc:audit://localhost"))) {
     *     scope.map(auditConn -> {
     *         auditConn.logEvent("User action performed");
     *         return null;
     *     });
     * } // Audit connection is closed, original connection restored
     * </pre>
     * <p>
     * Compare with {@link #using(AutoCloseable)} which does not close the temporary resource,
     * and {@link #requireOrCreate(Supplier)} which reuses existing resources.
     *
     * @param factory supplier to create the new resource
     * @return a Scope that provides access to the new resource and handles cleanup
     */
    public Scope<T> create(Supplier<T> factory) {
        T previousValue = threadLocal.get();
        AtomicReference<@Nullable T> resourceRef = new AtomicReference<>();

        return new Scope<>(
                (Supplier<T>) () -> {
                    T resource = resourceRef.get();
                    if (resource == null) {
                        resource = factory.get();
                        threadLocal.set(resource);
                        resourceRef.set(resource);
                    }
                    return resource;
                },
                () -> {
                    T resource = resourceRef.get();
                    try {
                        // Restore the previous ThreadLocal state
                        if (previousValue != null) {
                            threadLocal.set(previousValue);
                        } else {
                            threadLocal.remove();
                        }
                    } finally {
                        if (resource != null) {
                            try {
                                resource.close();
                            } catch (Exception e) {
                                // Log appropriately in real implementation
                                System.err.println("Error closing created resource: " + e.getMessage());
                            }
                        }
                    }
                }
        );
    }

    /**
     * Temporarily replace the resource, returning a Scope that restores the original.
     * <p>
     * Use this when you need to temporarily use a different resource within a scope.
     * The original resource is automatically restored when the returned Scope is closed.
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
     * resource lifecycle. Prefer the managed methods (require, requireOrCreate, create, using)
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
     * This is returned by requireOrCreate(), create(), and using(). It handles both ThreadLocal
     * restoration and resource cleanup when the scope is closed.
     */
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Scope<T extends AutoCloseable> implements AutoCloseable {
        Supplier<T> resourceSupplier;
        AutoCloseable cleanup;

        // Constructor for eager resources (using)
        Scope(T resource, AutoCloseable cleanup) {
            this.resourceSupplier = () -> resource;
            this.cleanup = cleanup;
        }

        // Constructor for lazy resources (requireOrCreate, create)
        Scope(Supplier<T> resourceSupplier, AutoCloseable cleanup) {
            this.resourceSupplier = resourceSupplier;
            this.cleanup = cleanup;
        }

        /**
         * Applies a function to the scoped resource and returns the result.
         * <p>
         * This provides functional access to the resource without exposing it directly.
         * For deferred resources, the function call triggers resource creation if it
         * hasn't occurred yet.
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
            return mapper.apply(resourceSupplier.get());
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
