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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.internal.LockFileRegeneration;
import org.openrewrite.python.internal.LockFileRegeneration.Result;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replays the corpus edits under {@code lock/repository/uv/}: a public repository's manifest before and after an edit,
 * its lock before, and the lock uv wrote at {@code recorded-at}, with the index responses the engine asks for in
 * {@code http/}.
 * A scenario named in {@code uv-repository-gaps.txt} is one the engine does not reproduce yet, and must still do what
 * the file says instead: refuse for the reason it names, or write another lock.
 */
class UvRepositoryScenarioTest {

    private static final Path ROOT = scenarioRoot();
    private static final Map<String, String> GAPS = gaps();

    static Stream<String> scenarios() throws IOException {
        try (Stream<Path> dirs = Files.list(ROOT)) {
            return dirs.filter(d -> Files.isDirectory(d.resolve("tree-before")))
              .map(d -> d.getFileName().toString())
              .sorted()
              .collect(Collectors.toList())
              .stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void regeneratesWhatUvWrote(String name) throws IOException {
        Path dir = ROOT.resolve(name);
        RecordedIndex http = new RecordedIndex(dir.resolve("http"));
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        PythonExecutionContextView.view(ctx).setUvVersion(read(dir, "recorded-with").trim().replaceFirst("^uv ", ""));
        if (Files.exists(dir.resolve("recorded-at"))) {
            ctx.putMessage(UvLockEngine.CURRENT_TIME, Instant.parse(read(dir, "recorded-at").trim()));
        }

        Result result = LockFileRegeneration.UV.regenerate(read(dir, "tree/pyproject.toml"),
          read(dir, "tree-before/pyproject.toml"), read(dir, "before"), ctx);

        assertThat(http.missing).as("index responses %s asked for that the scenario does not record", name).isEmpty();
        boolean reproduced = result.isSuccess() && read(dir, "after").equals(result.getLockFileContent());
        String gap = GAPS.get(name);
        if (gap != null) {
            assertThat(reproduced).as("%s now matches uv, so remove it from uv-repository-gaps.txt", name).isFalse();
            String outcome = result.isSuccess() ? "writes another lock" : "refuses: " + result.getFailure().getReason();
            assertThat(outcome).as("what the engine does with %s", name).isEqualTo(gap);
        } else {
            assertThat(result.getErrorMessage()).as(name).isNull();
            assertThat(result.getLockFileContent()).as(name).isEqualTo(read(dir, "after"));
        }
    }

    /** Serves http/ under the recorder's names, answering 404 for the URLs it recorded as not found. */
    private static final class RecordedIndex implements HttpSender {
        private final Path dir;
        private final Set<String> notFound;
        final List<String> missing = new ArrayList<>();

        RecordedIndex(Path dir) throws IOException {
            this.dir = dir;
            Path list = dir.resolve("404");
            this.notFound = Files.exists(list) ? new HashSet<>(Files.readAllLines(list)) : Collections.emptySet();
        }

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            Path file = recorded(url.replaceFirst("[?#].*$", ""));
            try {
                if (file != null) {
                    return new Response(200, new ByteArrayInputStream(Files.readAllBytes(file)), () -> {
                    });
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (!notFound.contains(url)) {
                missing.add(url);
            }
            return new Response(404, new ByteArrayInputStream(new byte[0]), () -> {
            });
        }

        private @Nullable Path recorded(String path) {
            if (path.endsWith("/")) {
                String trimmed = path.substring(0, path.length() - 1);
                String name = trimmed.substring(trimmed.lastIndexOf('/') + 1);
                for (String suffix : new String[]{"-listing-json", "-listing-html"}) {
                    Path listing = dir.resolve(name + suffix);
                    if (Files.exists(listing)) {
                        return listing;
                    }
                }
                return null;
            }
            Path file = dir.resolve(path.substring(path.lastIndexOf('/') + 1));
            return Files.exists(file) ? file : null;
        }
    }

    private static String read(Path dir, String file) throws IOException {
        return new String(Files.readAllBytes(dir.resolve(file)), StandardCharsets.UTF_8);
    }

    private static Path scenarioRoot() {
        URL url = UvRepositoryScenarioTest.class.getResource("/lock/repository/uv");
        assertThat(url).as("lock/repository/uv on the test classpath").isNotNull();
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> gaps() {
        try {
            Map<String, String> outcomes = new HashMap<>();
            for (String line : Files.readAllLines(Paths.get(UvRepositoryScenarioTest.class.getResource("/uv-repository-gaps.txt").toURI()))) {
                if (!line.trim().isEmpty() && !line.startsWith("#")) {
                    String[] columns = line.split("\t");
                    outcomes.put(columns[0].trim(), columns[1].trim());
                }
            }
            return outcomes;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
