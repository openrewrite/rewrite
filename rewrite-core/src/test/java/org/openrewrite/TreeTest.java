/*
 * Copyright 2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.quark.Quark;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.openrewrite.test.SourceSpecs.text;

class TreeTest implements RewriteTest {

    @Test
    void randomIdReturnsUuidV4WithIetfVariant() {
        for (int i = 0; i < 1000; i++) {
            UUID id = Tree.randomId();
            assertThat(id.version()).isEqualTo(4);
            assertThat(id.variant()).isEqualTo(2); // IETF (RFC 4122)
        }
    }

    @Test
    void randomIdProducesNoDuplicatesAcrossManyCalls() {
        int n = 1_000_000;
        Set<UUID> seen = new HashSet<>(n);
        for (int i = 0; i < n; i++) {
            seen.add(Tree.randomId());
        }
        assertThat(seen).hasSize(n);
    }

    @Test
    void randomIdIsUniqueAcrossThreads() throws Exception {
        int threads = 16;
        int perThread = 100_000;
        Set<UUID> ids = ConcurrentHashMap.newKeySet(threads * perThread);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            ids.add(Tree.randomId());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(ids).hasSize(threads * perThread);
    }

    @Test
    void customMarkerPrinting() {
        rewriteRun(
          text("hello",
            spec -> spec.afterRecipe(pt -> {
                String printed = Markup.info(pt, "jon").printAll(new PrintOutputCapture<>(0,
                  new PrintOutputCapture.MarkerPrinter() {
                      @Override
                      public String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
                          if (marker instanceof Markup.Info markup) {
                              return " " + requireNonNull(markup.getMessage());
                          }
                          return "";
                      }
                  })
                );
                assertThat(printed).isEqualTo("hello jon");
            })
          )
        );
    }

    @CsvSource({
        "true, '\uFEFFHello World'",
        "false, 'Hello World'"
    })
    @ParameterizedTest
    void printBomHandling(boolean charsetBomMarked, String expected) {
        var sourceFile = new PlainText(
            Tree.randomId(),
            Paths.get("test.txt"),
            Markers.EMPTY,
            "UTF-8",
            charsetBomMarked,
            null,
            null,
            "Hello World",
            null
        );

        String printed = sourceFile.printAll();

        assertThat(printed).isEqualTo(expected);
    }

    @Test
    void printQuarkDoesNotThrowOnBomRestoration() {
        var quark = new Quark(
            Tree.randomId(),
            Paths.get("unknown.file"),
            Markers.EMPTY,
            null,
            null
        );

        assertThatCode(quark::printAll).doesNotThrowAnyException();
    }
}
