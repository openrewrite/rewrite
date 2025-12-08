/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.rpc;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.JavaScriptParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to reproduce thread-safety issues when multiple threads each have their own
 * RewriteRpc instance and call print() in parallel.
 */
class ParallelPrintTest {
    @TempDir
    Path tempDir;

    @Test
    void parallelPrintWithSeparateRpcInstances() throws Exception {
        // JavaScript code to parse
        @Language("js")
        String jsCode = """
            class Calculator {
                constructor(name) {
                    this.name = name;
                    this.history = [];
                }

                add(a, b) {
                    const result = a + b;
                    this.history.push({ op: 'add', a, b, result });
                    return result;
                }

                subtract(a, b) {
                    const result = a - b;
                    this.history.push({ op: 'subtract', a, b, result });
                    return result;
                }

                multiply(a, b) {
                    const result = a * b;
                    this.history.push({ op: 'multiply', a, b, result });
                    return result;
                }
            }

            const calc = new Calculator("MyCalc");
            console.log(calc.add(5, 3));
            """;

        int numThreads = 6;  // Increased from 4 to stress test more
        int printsPerThread = 20;  // Increased from 20 to make concurrency issues more likely
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Future<String>> futures = new ArrayList<>();

        try {
            // Create threads that will each get their own RPC instance
            for (int i = 0; i < numThreads; i++) {
                final int threadNum = i;
                futures.add(executor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();

                        // Each thread sets up its own RPC instance via ThreadLocal
                        JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder()
                            .recipeInstallDir(tempDir.resolve("thread-" + threadNum))
                            .log(tempDir.resolve("thread-" + threadNum + ".log"))
                        );

                        JavaScriptRewriteRpc rpc;

                        // Parse the code
                        JavaScriptParser parser = JavaScriptParser.builder().build();
                        ExecutionContext ctx = new InMemoryExecutionContext();

                        Parser.Input input = Parser.Input.fromString(
                            Path.of("calculator-" + threadNum + ".js"),
                            jsCode
                        );
                        List<SourceFile> sourceFiles = parser.parseInputs(
                            List.of(input),
                            null,
                            ctx
                        ).toList();

                        assertThat(sourceFiles).hasSize(1);
                        SourceFile sourceFile = sourceFiles.getFirst();

                        // Shutdown RPC after parsing to clear remote cache
                        // This forces GetObject callbacks during print()
                        JavaScriptRewriteRpc.shutdownCurrent();

                        // Restart RPC for print operations
                        rpc = JavaScriptRewriteRpc.getOrStart();

                        // Call print() multiple times in rapid succession
                        for (int j = 0; j < printsPerThread; j++) {
                            String printed = rpc.print(sourceFile);

                            assertThat(printed).isNotEmpty();
                            assertThat(printed).contains("Calculator");
                            successCount.incrementAndGet();
                        }

                        // Cleanup
                        JavaScriptRewriteRpc.shutdownCurrent();

                        return "";
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        throw new RuntimeException("Thread " + threadNum + " failed", e);
                    }
                }));
            }

            // Start all threads simultaneously
            startLatch.countDown();

            // Wait for all threads to complete and collect results
            for (Future<String> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Error getting future result: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            // Verify results
            assertThat(errorCount.get())
                .withFailMessage("Expected no errors but got %d errors. Check output above for details.", errorCount.get())
                .isEqualTo(0);
            assertThat(successCount.get()).isEqualTo(numThreads * printsPerThread);

        } finally {
            executor.shutdown();
            boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
            if (!terminated) {
                System.err.println("Warning: Executor did not terminate in time");
            }
        }
    }
}
