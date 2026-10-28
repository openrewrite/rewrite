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
package org.openrewrite.gradle.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every {@link AddDependencyVisitor} parses its dependency templates with one shared {@link GradleParser}, whose type
 * cache isn't thread-safe, while schedulers such as the Moderne CLI ({@code mod run --parallel}) visit the source
 * files of a run on several threads. Two threads parsing with it at once lose or mix up cached types, which surfaces
 * as parse errors or wrong types on the parsed template.
 * <p>
 * This test holds one visit inside its template parse and checks that a visit on another thread waits for it instead
 * of parsing at the same time.
 */
class AddDependencyVisitorConcurrencyTest {

    private static final String BUILD_FILE = """
      dependencies {
          implementation "commons-lang:commons-lang:2.6"
      }
      """;

    @Timeout(60)
    @Test
    void concurrentVisitsTakeTurnsParsingTemplates() throws Exception {
        SourceFile buildA = parseBuildGradle("a/build.gradle");
        SourceFile buildB = parseBuildGradle("b/build.gradle");

        AtomicBoolean aParsing = new AtomicBoolean();
        AtomicBoolean overlapped = new AtomicBoolean();
        CountDownLatch aStartedParsing = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutionContext ctxA = withParsingListener(new ParsingEventListener() {
            @Override
            public void startedParsing(Parser.Input input) {
                aParsing.set(true);
                aStartedParsing.countDown();
                try {
                    releaseA.await(30, SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public void parsed(Parser.Input input, SourceFile sourceFile) {
                aParsing.set(false);
            }
        });
        ExecutionContext ctxB = withParsingListener(new ParsingEventListener() {
            @Override
            public void startedParsing(Parser.Input input) {
                if (aParsing.get()) {
                    overlapped.set(true);
                }
            }
        });

        AddDependencyVisitor addGuava = addImplementation("com.google.guava", "guava", "29.0-jre");
        AddDependencyVisitor addSlf4j = addImplementation("org.slf4j", "slf4j-api", "2.0.17");
        FutureTask<J> visitA = new FutureTask<>(() -> addGuava.visit(buildA, ctxA));
        FutureTask<J> visitB = new FutureTask<>(() -> addSlf4j.visit(buildB, ctxB));
        Thread threadA = start(visitA);
        try {
            assertThat(aStartedParsing.await(30, SECONDS))
              .as("visit A should start parsing its dependency template")
              .isTrue();
            Thread threadB = start(visitB);
            // Visit B either starts parsing while A still is, or waits for a lock that A holds
            while (!overlapped.get() && !visitB.isDone() && !waitsForLockHeldBy(threadB, threadA)) {
                //noinspection BusyWait
                Thread.sleep(10);
            }
        } finally {
            releaseA.countDown();
        }

        assertThat(((SourceFile) visitA.get()).printAll())
          .contains("implementation \"com.google.guava:guava:29.0-jre\"");
        assertThat(((SourceFile) visitB.get()).printAll())
          .contains("implementation \"org.slf4j:slf4j-api:2.0.17\"");
        assertThat(overlapped)
          .as("visit B parsed a template while visit A was parsing one with the same parser")
          .isFalse();
    }

    private static AddDependencyVisitor addImplementation(String groupId, String artifactId, String version) {
        return new AddDependencyVisitor("implementation", groupId, artifactId, version, null, null, null, null, false);
    }

    private static Thread start(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static boolean waitsForLockHeldBy(Thread waiting, Thread holder) {
        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(waiting.threadId());
        return info != null && info.getLockOwnerId() == holder.threadId();
    }

    private static SourceFile parseBuildGradle(String path) {
        return GradleParser.builder().build()
          .parse(new InMemoryExecutionContext(), BUILD_FILE)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Failed to parse build.gradle"))
          .withSourcePath(Paths.get(path));
    }

    private static ExecutionContext withParsingListener(ParsingEventListener listener) {
        return ParsingExecutionContextView.view(new InMemoryExecutionContext()).setParsingListener(listener);
    }
}
