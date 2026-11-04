/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Moderne CLI's {@code goBinaryPathSupplier} takes a JVM-wide exclusive
 * {@link FileLock} on the Go workspace while it rebuilds the RPC binary. Because
 * {@link RewriteRpcProcessManager} holds one RPC per thread, several worker threads
 * can start a Go RPC at once, each invoking the supplier concurrently. A {@code FileLock}
 * is held per JVM, not per thread, so overlapping lock attempts from one JVM throw
 * {@link OverlappingFileLockException} rather than blocking (openrewrite/rewrite#8527).
 * {@link GoRewriteRpc.Builder#resolveGoBinaryPath} serializes the supplier so at most one
 * thread holds the workspace lock at a time.
 */
class GoBinaryPathConcurrencyTest {

    private static final int THREADS = 8;

    @Test
    void concurrentSupplierInvocationsAreSerialized(@TempDir Path tempDir) throws Exception {
        // given
        Path workspaceLock = tempDir.resolve("workspace.lock");
        Path binary = tempDir.resolve("rewrite-go-rpc");

        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<Path> resolved = new CopyOnWriteArrayList<>();

        // A supplier that mimics the CLI: it takes the JVM-wide workspace file lock,
        // holds it briefly (as a rebuild would), then releases and returns the binary path.
        java.util.function.Supplier<Path> supplier = () -> {
            try (FileChannel channel = FileChannel.open(workspaceLock, CREATE, WRITE);
                 FileLock ignored = channel.lock()) {
                Thread.sleep(25);
                return binary;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // when
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                    resolved.add(GoRewriteRpc.Builder.resolveGoBinaryPath(supplier));
                } catch (Throwable t) {
                    failures.add(t);
                }
            });
            threads[i].start();
        }
        assertThat(ready.await(30, SECONDS)).as("threads failed to start").isTrue();
        go.countDown();
        for (Thread t : threads) {
            t.join(SECONDS.toMillis(30));
        }

        // then
        assertThat(failures)
                .as("no thread may see an OverlappingFileLockException")
                .noneMatch(OverlappingFileLockException.class::isInstance);
        assertThat(failures).isEmpty();
        assertThat(resolved).hasSize(THREADS).containsOnly(binary);
    }
}
