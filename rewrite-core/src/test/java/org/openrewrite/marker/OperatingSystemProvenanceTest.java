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
package org.openrewrite.marker;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class OperatingSystemProvenanceTest {

    @Test
    void noDeadlockBetweenLinuxAndMacOs() throws InterruptedException {
        // This test must be first to ensure classes are not already initialized

        // given
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    if (threadNum % 2 == 0) {
                        Class.forName("org.openrewrite.marker.OperatingSystemProvenance$Linux");
                    } else {
                        Class.forName("org.openrewrite.marker.OperatingSystemProvenance$MacOs");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // when
        startLatch.countDown();

        // test
        boolean completed = completeLatch.await(1, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).as("All threads should complete within 1 second (no deadlock)").isTrue();
    }

    @Test
    void hostname() {
        String hostname = OperatingSystemProvenance.hostname();
        assertThat(hostname).isNotNull();
    }
}
