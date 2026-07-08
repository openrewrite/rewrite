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

import org.eclipse.aether.spi.connector.transport.Transporter;
import org.junit.jupiter.api.Test;
import org.openrewrite.ipc.http.HttpSender;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code classify()} drives all of Maven's negative caching, so it is tested exhaustively. A deterministic 4xx
 * (everything except 408/425/429) means the resource is authoritatively absent — {@code ERROR_NOT_FOUND}; anything
 * else is a transfer error — {@code ERROR_OTHER}.
 */
class HttpSenderTransporterClassifyTest {

    private static final HttpSender NOOP_SENDER = request -> {
        throw new UnsupportedOperationException("classify does not send");
    };

    private final HttpSenderTransporter transporter = new HttpSenderTransporter(
            NOOP_SENDER, "https://example.com/", null, null, Collections.emptyMap(),
            Duration.ofSeconds(10), Duration.ofSeconds(30), Collections.emptySet(), ResolutionTimeRecorder.NOOP);

    @Test
    void deterministicClientErrorPredicateIsExhaustive() {
        for (int code = 0; code <= 599; code++) {
            boolean expected = code >= 400 && code <= 499 && code != 408 && code != 425 && code != 429;
            assertEquals(expected, HttpSenderTransporter.isDeterministicClientError(code),
                    "isDeterministicClientError(" + code + ")");
        }
    }

    @Test
    void classifyMapsEveryHttpStatus() {
        for (int code = 400; code <= 599; code++) {
            int expected = (code <= 499 && code != 408 && code != 425 && code != 429)
                    ? Transporter.ERROR_NOT_FOUND : Transporter.ERROR_OTHER;
            assertEquals(expected, transporter.classify(new HttpSenderTransporter.HttpResponseException(code)),
                    "classify(HTTP " + code + ")");
        }
    }

    @Test
    void transientClientErrorsAreNotNotFound() {
        // 408/425/429 are likely to change on retry — never negatively cached.
        for (int code : new int[]{408, 425, 429}) {
            assertEquals(Transporter.ERROR_OTHER,
                    transporter.classify(new HttpSenderTransporter.HttpResponseException(code)), "HTTP " + code);
        }
    }

    @Test
    void classifyUnwrapsNestedCause() {
        Throwable nested = new RuntimeException("wrapper", new HttpSenderTransporter.HttpResponseException(404));
        assertEquals(Transporter.ERROR_NOT_FOUND, transporter.classify(nested));
    }

    @Test
    void nonHttpErrorsAreOther() {
        assertEquals(Transporter.ERROR_OTHER, transporter.classify(new IOException("connection reset")));
        assertEquals(Transporter.ERROR_OTHER, transporter.classify(new RuntimeException("boom")));
    }
}
