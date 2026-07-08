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
package org.openrewrite.maven.parity.synthetic;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.parity.ParityHarness;
import org.openrewrite.maven.parity.RecordingResolutionListener;
import org.openrewrite.maven.parity.ResolutionSnapshot;
import org.openrewrite.maven.parity.SnapshotNormalizer;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * {@link ParityHarness}'s counterpart for synthetic (MockWebServer / file://) repositories:
 * the standard MavenParser path with central and local repositories disabled and all errors and
 * events captured. Unlike the fixture harness, a resolution failure is representable: dependency
 * download failures keep the complete model with resolvable scopes populated (ledger L-P0-004)
 * and surface via {@link Resolution#errored()}; only effective-pom failures discard the marker.
 * A {@link Session} reuses one context so caching behavior across resolutions is observable.
 */
class SyntheticHarness {

    static Resolution resolve(@Language("xml") String pomXml, Consumer<MavenExecutionContextView> customize, Path... normalizerRoots) {
        return new Session(customize).resolve(pomXml, normalizerRoots);
    }

    //language=xml
    static String rootPom(String body) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.parity.synthetic</groupId>
              <artifactId>app</artifactId>
              <version>1.0</version>
              %s
          </project>
          """.formatted(body);
    }

    /**
     * Repositories used together with a {@code MavenSettings} object must be declared in the pom:
     * once settings are present, {@code MavenExecutionContextView.getRepositories(settings, ...)}
     * returns only the settings' active-profile repositories and context-injected ones are
     * dropped (they merely enrich same-id settings repositories).
     */
    static String repositories(String... idUrlPairs) {
        StringBuilder s = new StringBuilder("<repositories>");
        for (String pair : idUrlPairs) {
            int separator = pair.indexOf('=');
            s.append("<repository><id>").append(pair, 0, separator).append("</id>")
                    .append("<url>").append(pair.substring(separator + 1)).append("</url></repository>");
        }
        return s.append("</repositories>").toString();
    }

    static String dependencies(String... gavs) {
        StringBuilder s = new StringBuilder("<dependencies>");
        for (String gav : gavs) {
            String[] parts = gav.split(":");
            s.append("<dependency><groupId>").append(parts[0]).append("</groupId>")
                    .append("<artifactId>").append(parts[1]).append("</artifactId>")
                    .append("<version>").append(parts[2]).append("</version>");
            if (parts.length > 3) {
                s.append("<classifier>").append(parts[3]).append("</classifier>");
            }
            s.append("</dependency>");
        }
        return s.append("</dependencies>").toString();
    }

    static class Session {
        final List<Throwable> errors = new ArrayList<>();
        final RecordingResolutionListener listener = new RecordingResolutionListener();
        final MavenExecutionContextView ctx;

        Session(Consumer<MavenExecutionContextView> customize) {
            ctx = MavenExecutionContextView.view(new InMemoryExecutionContext(errors::add));
            ctx.setPomCache(new InMemoryMavenPomCache());
            ctx.setAddCentralRepository(false);
            ctx.setAddLocalRepository(false);
            ctx.setResolutionListener(listener);
            HttpSenderExecutionContextView.view(ctx).setHttpSender(new FailFastHttpsSender());
            customize.accept(ctx);
        }

        Resolution resolve(@Language("xml") String pomXml, Path... normalizerRoots) {
            SourceFile doc = MavenParser.builder().build()
                    .parseInputs(singletonList(Parser.Input.fromString(Paths.get("pom.xml"), pomXml)), null, ctx)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("pom did not parse"));
            MavenResolutionResult marker = doc.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
            return new Resolution(marker, this, new SnapshotNormalizer(normalizerRoots));
        }
    }

    /**
     * The https-preference probe against a plaintext MockWebServer stalls until read-timeout (the
     * TLS ClientHello never parses as an HTTP request line) and the timeout is then retried; a
     * real plaintext server fails the handshake immediately. This sender stands in for that
     * immediate failure on {@code https://localhost} only; everything else uses the JDK sender.
     */
    static class FailFastHttpsSender extends HttpUrlConnectionSender {
        @Override
        public Response send(Request request) {
            if ("https".equals(request.getUrl().getProtocol()) && "localhost".equals(request.getUrl().getHost())) {
                throw new UncheckedIOException(new ConnectException("plaintext test server; TLS unavailable"));
            }
            return super.send(request);
        }
    }

    static class Resolution {
        private final @Nullable MavenResolutionResult marker;
        private final Session session;
        private final SnapshotNormalizer normalizer;
        private @Nullable ResolutionSnapshot snapshot;

        Resolution(@Nullable MavenResolutionResult marker, Session session, SnapshotNormalizer normalizer) {
            this.marker = marker;
            this.session = session;
            this.normalizer = normalizer;
        }

        boolean failed() {
            return marker == null;
        }

        boolean errored() {
            return !session.errors.isEmpty();
        }

        MavenResolutionResult marker() {
            return requireNonNull(marker, () -> "resolution failed; errors: " + session.errors);
        }

        List<Throwable> errors() {
            return session.errors;
        }

        // Memoized: the resolve() identity probe appends events to the live listener
        ResolutionSnapshot snapshot() {
            if (snapshot == null) {
                snapshot = ResolutionSnapshot.of(marker(), session.errors, session.listener.getEvents(), normalizer, session.ctx);
            }
            return snapshot;
        }
    }
}
