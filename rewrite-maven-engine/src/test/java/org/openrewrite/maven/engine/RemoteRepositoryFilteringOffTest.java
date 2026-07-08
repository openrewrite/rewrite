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
 * Remote Repository Filtering must be off: the bare mvn3 supplier enables GroupId/Prefixes filter sources that GET
 * {@code .meta/prefixes.txt} out of band (and reach live Central). {@link EngineRepositorySystemSupplier} disables
 * them, so a collect issues only the three POM GETs — zero {@code prefixes.txt}.
 */
class RemoteRepositoryFilteringOffTest {

    @Test
    void noPrefixesTxtRequests(@TempDir Path scratch) throws Exception {
        try (MavenEngine engine = new MavenEngine();
             TinyMavenRepo repo = new TinyMavenRepo()) {
            RecordingHttpSender sender =
                    new RecordingHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10)));
            try (CloseableSession session = engine.newSession(scratch, SessionConfig.forSender(sender))) {
                EngineTests.collect(engine.getRepositorySystem(), session, repo.remoteRepository(), "com.example:app:1");
            }

            assertFalse(repo.anyPrefixesTxtRequested(), "RRF must be off; server saw: " + repo.requested);
            assertTrue(sender.requests().stream().noneMatch(r -> r.path.contains("prefixes.txt")),
                    "sender saw prefixes.txt traffic: " + sender.requests());
            // Exactly the three POM GETs and nothing else.
            assertEquals(3, sender.count(), "expected only 3 POM GETs: " + sender.requests());
        }
    }
}
