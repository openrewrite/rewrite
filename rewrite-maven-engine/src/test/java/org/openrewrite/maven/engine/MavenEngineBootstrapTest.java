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
package org.openrewrite.maven.engine;

import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The engine bootstraps a working RepositorySystem via plain {@code new} through the shaded stack, collects a graph,
 * and guarantees every byte flows through the injected {@link org.openrewrite.ipc.http.HttpSender} (no bypass), with
 * per-session sender isolation.
 */
class MavenEngineBootstrapTest {

    private static HttpUrlConnectionSender sender() {
        return new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10));
    }

    @Test
    void bootstrapAndCollectLinearGraph(@TempDir Path scratch) throws Exception {
        try (MavenEngine engine = new MavenEngine();
             TinyMavenRepo repo = new TinyMavenRepo()) {
            RecordingHttpSender sender = new RecordingHttpSender(sender());
            try (CloseableSession session = engine.newSession(scratch, SessionConfig.forSender(sender))) {
                EngineTests.assertLinearGraph(
                        EngineTests.collect(engine.getRepositorySystem(), session, repo.remoteRepository(), "com.example:app:1"));
            }
            // Collection reads POMs only, never JARs — exactly app, lib-a, lib-b.
            assertEquals(3, sender.countPomGets(), "POM GETs: " + sender.requests());
            assertFalse(sender.anyJarRequested(), "collect must not request any .jar: " + sender.requests());
            assertFalse(repo.anyJarRequested());
        }
    }

    @Test
    void everyByteThroughTheSender_noBypass(@TempDir Path scratchA, @TempDir Path scratchB) throws Exception {
        // One engine (one bootstrap), two sessions, two distinct senders. The sender is resolved per session from
        // config, never baked in at bootstrap — so B's traffic must never touch A's recorder.
        try (MavenEngine engine = new MavenEngine();
             TinyMavenRepo repo = new TinyMavenRepo()) {
            RecordingHttpSender senderA = new RecordingHttpSender(sender());
            RecordingHttpSender senderB = new RecordingHttpSender(sender());

            try (CloseableSession sessionA = engine.newSession(scratchA, SessionConfig.forSender(senderA))) {
                EngineTests.collect(engine.getRepositorySystem(), sessionA, repo.remoteRepository(), "com.example:app:1");
            }
            // Cold run through A's sender: all three POMs.
            assertEquals(3, senderA.countPomGets(), "session A (cold): " + senderA.requests());
            long senderACountAfterA = senderA.count();

            try (CloseableSession sessionB = engine.newSession(scratchB, SessionConfig.forSender(senderB))) {
                EngineTests.collect(engine.getRepositorySystem(), sessionB, repo.remoteRepository(), "com.example:app:1");
            }

            // Per-session isolation: session B routed entirely through its own sender and never A's.
            assertEquals(senderACountAfterA, senderA.count(), "session B leaked onto session A's sender");
            assertTrue(senderB.count() > 0, "session B's sender received nothing: " + senderB.requests());

            // No bypass: every request the server saw came through one of the two injected senders.
            assertEquals(senderA.count() + senderB.count(), repo.serverRequestCount(),
                    "server count must equal the sum of both recorders (nothing bypassed the sender)"
                            + "\n  A=" + senderA.requests() + "\n  B=" + senderB.requests()
                            + "\n  server=" + repo.requested);
        }
    }

    @Test
    void sharedRepositoryCacheServesSecondSessionWithFewerFetches(@TempDir Path scratchA, @TempDir Path scratchB)
            throws Exception {
        // The engine holds one RepositoryCache across sessions: a warm session re-fetches strictly fewer POMs than a
        // cold one, even against a different (empty) scratch local repository.
        try (MavenEngine engine = new MavenEngine();
             TinyMavenRepo repo = new TinyMavenRepo()) {
            RecordingHttpSender cold = new RecordingHttpSender(sender());
            try (CloseableSession a = engine.newSession(scratchA, SessionConfig.forSender(cold))) {
                EngineTests.collect(engine.getRepositorySystem(), a, repo.remoteRepository(), "com.example:app:1");
            }
            RecordingHttpSender warm = new RecordingHttpSender(sender());
            try (CloseableSession b = engine.newSession(scratchB, SessionConfig.forSender(warm))) {
                EngineTests.collect(engine.getRepositorySystem(), b, repo.remoteRepository(), "com.example:app:1");
            }
            assertEquals(3, cold.countPomGets());
            assertTrue(warm.countPomGets() < cold.countPomGets(),
                    "warm session should reuse the shared cache: " + warm.requests());
        }
    }

    /**
     * FD-leak pin (Phase 4 slice D). The resolver's default named-lock factory is file-based: it holds a FileChannel on
     * a {@code .locks/*.lock} file per artifact for the duration of the concurrent collect, which a BOM-heavy reactor
     * accumulates to the OS file-descriptor cap (locks are only released on session close — the fresh-ctx parses that
     * exhausted the cap never closed theirs). The session template pins the in-JVM {@code rwlock-local} factory, which
     * synchronizes the collector's worker threads with zero files. Guarding the config knob directly is stable across
     * platforms (an FD-count assertion is not).
     */
    @Test
    void sessionUsesInJvmNamedLocks(@TempDir Path scratch) {
        try (MavenEngine engine = new MavenEngine();
             CloseableSession session = engine.newSession(scratch, SessionConfig.forSender(new RecordingHttpSender(sender())))) {
            assertEquals("rwlock-local", session.getConfigProperties().get("aether.syncContext.named.factory"),
                    "session must select the file-free in-JVM named-lock factory (file locks leak FDs on BOM-heavy reactors)");
        }
    }
}
