/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.scheduling;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFileTimeoutException;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceFileDeadlineTest {
    PlainText a = PlainText.builder().sourcePath(Path.of("a.txt")).text("a").build();
    PlainText b = PlainText.builder().sourcePath(Path.of("b.txt")).text("b").build();

    @Test
    void noDeadlineByDefault() {
        assertThatCode(SourceFileDeadline::check).doesNotThrowAnyException();
    }

    @Test
    void expiredDeadlineThrowsUntilClosed() throws InterruptedException {
        try (SourceFileDeadline ignored = SourceFileDeadline.start(Recipe.noop(), a, 0)) {
            Thread.sleep(1);
            assertThatThrownBy(SourceFileDeadline::check)
              .isInstanceOfSatisfying(SourceFileTimeoutException.class,
                e -> assertThat(e.getSourcePath()).isEqualTo(Path.of("a.txt")));
        }
        assertThatCode(SourceFileDeadline::check).doesNotThrowAnyException();
    }

    @Test
    void closingRestoresEnclosingDeadline() throws InterruptedException {
        try (SourceFileDeadline ignored = SourceFileDeadline.start(Recipe.noop(), a, 0)) {
            Thread.sleep(1);
            try (SourceFileDeadline ignored2 = SourceFileDeadline.start(Recipe.noop(), b, Long.MAX_VALUE)) {
                assertThatCode(SourceFileDeadline::check).doesNotThrowAnyException();
            }
            assertThatThrownBy(SourceFileDeadline::check).isInstanceOf(SourceFileTimeoutException.class);
        }
    }

    @Test
    void onlyAppliesToTheThreadThatStartedIt() throws InterruptedException {
        try (SourceFileDeadline ignored = SourceFileDeadline.start(Recipe.noop(), a, 0)) {
            Thread.sleep(1);
            // A dedicated thread, as a ForkJoinPool join() may run the task on this thread
            AtomicReference<Throwable> otherThreadError = new AtomicReference<>();
            Thread other = new Thread(() -> {
                try {
                    SourceFileDeadline.check();
                } catch (Throwable t) {
                    otherThreadError.set(t);
                }
            });
            other.start();
            other.join();
            assertThat(otherThreadError).hasNullValue();
            assertThatThrownBy(SourceFileDeadline::check).isInstanceOf(SourceFileTimeoutException.class);
        }
    }

    @Test
    void remembersBeingAbandonedAfterTheTimeoutIsCaught() throws InterruptedException {
        try (SourceFileDeadline deadline = SourceFileDeadline.start(Recipe.noop(), a, 0)) {
            Thread.sleep(1);
            // Passing the deadline without a failed check doesn't abandon the work
            assertThat(deadline.isAbandoned()).isFalse();
            assertThatCode(deadline::throwIfAbandoned).doesNotThrowAnyException();

            try {
                SourceFileDeadline.check();
            } catch (SourceFileTimeoutException ignored) {
            }
            assertThat(deadline.isAbandoned()).isTrue();
            assertThatThrownBy(deadline::throwIfAbandoned).isInstanceOf(SourceFileTimeoutException.class);
        }
    }

    @Test
    void treeVisitorChecksDeadline() throws InterruptedException {
        try (SourceFileDeadline ignored = SourceFileDeadline.start(Recipe.noop(), a, 0)) {
            Thread.sleep(1);
            assertThatThrownBy(() -> new TreeVisitor<Tree, Integer>() {
            }.visit(a, 0))
              .isInstanceOf(RecipeRunException.class)
              .hasCauseInstanceOf(SourceFileTimeoutException.class);
        }
    }
}
