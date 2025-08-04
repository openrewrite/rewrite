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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagedThreadLocalTest {

    private static class TestResource implements AutoCloseable {
        private final String value;
        private boolean closed = false;

        TestResource(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    void requireThrowsWhenNoResourceExists() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();
        assertThatThrownBy(threadLocal::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No managed resource found in current thread context");
    }

    @Test
    void createThenRequireWorksImmediately() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();

        try (var ignore = threadLocal.create(() -> new TestResource("test"))) {
            // Should be able to call require() immediately after create()
            TestResource resource = threadLocal.require();
            assertThat(resource.getValue()).isEqualTo("test");
            assertThat(resource.isClosed()).isFalse();
        }
    }

    @Test
    void createThenScopeMapWorksAsExpected() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();

        try (var scope = threadLocal.create(() -> new TestResource("test"))) {
            String result = scope.map(resource -> {
                assertThat(resource.getValue()).isEqualTo("test");
                return "mapped: " + resource.getValue();
            });
            assertThat(result).isEqualTo("mapped: test");
        }
    }

    @Test
    void resourceIsClosedAfterScopeCloses() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();
        TestResource resource;

        try (var ignore = threadLocal.create(() -> new TestResource("test"))) {
            resource = threadLocal.require();
            assertThat(resource.isClosed()).isFalse();
        }

        assertThat(resource.isClosed()).isTrue();
        assertThatThrownBy(threadLocal::require)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createRestoresPreviousResource() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();

        try (var ignore1 = threadLocal.create(() -> new TestResource("outer"))) {
            TestResource outerResource = threadLocal.require();
            assertThat(outerResource.getValue()).isEqualTo("outer");

            try (var ignore2 = threadLocal.create(() -> new TestResource("inner"))) {
                TestResource innerResource = threadLocal.require();
                assertThat(innerResource.getValue()).isEqualTo("inner");
                assertThat(innerResource).isNotSameAs(outerResource);
            }

            // After inner scope closes, outer resource should be restored
            TestResource restoredResource = threadLocal.require();
            assertThat(restoredResource).isSameAs(outerResource);
            assertThat(restoredResource.getValue()).isEqualTo("outer");
            assertThat(restoredResource.isClosed()).isFalse();
        }
    }

    @Test
    void requireOrCreateReusesExistingResource() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();

        try (var ignore1 = threadLocal.create(() -> new TestResource("existing"))) {
            TestResource existing = threadLocal.require();

            try (var ignore2 = threadLocal.requireOrCreate(() -> new TestResource("new"))) {
                TestResource resource = threadLocal.require();
                assertThat(resource).isSameAs(existing);
                assertThat(resource.getValue()).isEqualTo("existing");
            }
        }
    }

    @Test
    void requireOrCreateCreatesWhenNoneExists() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();

        try (var ignore = threadLocal.requireOrCreate(() -> new TestResource("created"))) {
            //noinspection resource
            TestResource resource = threadLocal.require();
            assertThat(resource.getValue()).isEqualTo("created");
        }
    }

    @Test
    void usingTemporarilyReplacesResource() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();
        TestResource tempResource = new TestResource("temp");

        try (var ignore1 = threadLocal.create(() -> new TestResource("original"))) {
            TestResource original = threadLocal.require();

            try (var ignore2 = threadLocal.using(tempResource)) {
                TestResource current = threadLocal.require();
                assertThat(current).isSameAs(tempResource);
                assertThat(current.getValue()).isEqualTo("temp");
            }

            // Original should be restored
            TestResource restored = threadLocal.require();
            assertThat(restored).isSameAs(original);
            assertThat(restored.getValue()).isEqualTo("original");
        }

        // Temp resource should not be closed by using()
        assertThat(tempResource.isClosed()).isFalse();
    }

    @Test
    void isPresentWorksCorrectly() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();

        assertThat(threadLocal.isPresent()).isFalse();

        try (var ignore = threadLocal.create(() -> new TestResource("test"))) {
            assertThat(threadLocal.isPresent()).isTrue();
        }

        assertThat(threadLocal.isPresent()).isFalse();
    }

    @Test
    void deferredCreationOnlyCallsFactoryWhenNeeded() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();
        boolean[] factoryCalled = {false};

        try (var ignore = threadLocal.create(() -> {
            factoryCalled[0] = true;
            return new TestResource("deferred");
        })) {
            // Factory should not have been called yet
            assertThat(factoryCalled[0]).isFalse();

            // Now trigger creation via require()
            TestResource resource = threadLocal.require();
            assertThat(factoryCalled[0]).isTrue();
            assertThat(resource.getValue()).isEqualTo("deferred");

            // Subsequent calls should not call factory again
            factoryCalled[0] = false;
            TestResource sameResource = threadLocal.require();
            assertThat(factoryCalled[0]).isFalse();
            assertThat(sameResource).isSameAs(resource);
        }
    }

    @Test
    void deferredCreationWorksWithScopeMap() {
        ManagedThreadLocal<TestResource> threadLocal = new ManagedThreadLocal<>();
        boolean[] factoryCalled = {false};

        try (var scope = threadLocal.requireOrCreate(() -> {
            factoryCalled[0] = true;
            return new TestResource("deferred");
        })) {
            // Factory should not have been called yet
            assertThat(factoryCalled[0]).isFalse();

            // Now trigger creation via scope.map()
            String result = scope.map(resource -> {
                assertThat(resource.getValue()).isEqualTo("deferred");
                return "result: " + resource.getValue();
            });

            assertThat(factoryCalled[0]).isTrue();
            assertThat(result).isEqualTo("result: deferred");

            // Subsequent require() should work without calling factory again
            factoryCalled[0] = false;
            //noinspection resource
            TestResource resource = threadLocal.require();
            assertThat(factoryCalled[0]).isFalse();
            assertThat(resource.getValue()).isEqualTo("deferred");
        }
    }
}
