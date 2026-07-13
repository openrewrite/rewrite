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

import lombok.Value;
import lombok.With;
import org.openrewrite.ipc.http.HttpSender;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-session inputs the host tunnels to the transport. {@link MavenEngine} stamps these onto the session's
 * config properties so {@link HttpSenderTransporterFactory} can resolve them on every {@code newInstance} — the same
 * bootstrapped {@code RepositorySystem} can serve many sessions, each with its own sender, dead-endpoint memory,
 * and accounting hook.
 */
@Value
@With
public class SessionConfig {

    /** The host-injected sender every byte of remote traffic flows through. */
    HttpSender httpSender;

    /**
     * Run-scoped set of {@code host:port} authorities found unreachable this run. Consulted to short-circuit and
     * augmented by the transport on hard connection failures; supplied by the host so it can outlive a single session.
     */
    Set<String> unreachableHosts;

    /** Invoked with the wall-clock time of each exchange (default no-op). */
    ResolutionTimeRecorder resolutionTimeRecorder;

    public static SessionConfig forSender(HttpSender httpSender) {
        return new SessionConfig(httpSender, Collections.newSetFromMap(new ConcurrentHashMap<>()),
                ResolutionTimeRecorder.NOOP);
    }
}
