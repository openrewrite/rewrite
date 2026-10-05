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
package org.openrewrite.python.internal.poetrylock;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.LockFileRegeneration.Result;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.internal.poetrylock.PoetryLockFixtures.resource;

/**
 * Live-network validation of the poetry engine against pypi.org, and recorder for the offline
 * fixtures the committed engine test replays. Disabled by default; run explicitly.
 */
@Disabled("hits pypi.org; run manually to re-record /poetrylock/http fixtures")
class PoetryLockEngineLiveTest {

    @Test
    void upgradeSixByteIdentical() {
        Recorder http = new Recorder();
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
                new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false)));

        Result result = PoetryLockEngine.regenerate(
                resource("i-upgrade/pyproject.toml.after"),
                resource("i-upgrade/poetry.lock.before"),
                ctx);

        http.dump(Paths.get(System.getProperty("java.io.tmpdir"), "poetry-http-recording"));
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("i-upgrade/poetry.lock.after"));
    }

    static final class Recorder implements HttpSender {
        private final HttpSender delegate = new HttpUrlConnectionSender();
        private final Map<String, byte[]> recorded = new ConcurrentHashMap<>();

        @Override
        public Response send(Request request) {
            Response response = delegate.send(request);
            byte[] body = response.getBodyAsBytes();
            recorded.put(request.getUrl().toString(), body);
            return new Response(response.getCode(), new java.io.ByteArrayInputStream(body), () -> {
            });
        }

        @Override
        public Request.Builder get(String url) {
            return delegate.get(url);
        }

        @Override
        public Request.Builder post(String url) {
            return delegate.post(url);
        }

        void dump(Path dir) {
            try {
                Files.createDirectories(dir);
                StringBuilder index = new StringBuilder();
                int i = 0;
                for (Map.Entry<String, byte[]> e : recorded.entrySet()) {
                    String file = "resp-" + (i++) + ".bin";
                    Files.write(dir.resolve(file), e.getValue());
                    index.append(file).append('\t').append(e.getKey()).append('\n');
                }
                Files.write(dir.resolve("index.tsv"), index.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }
}
