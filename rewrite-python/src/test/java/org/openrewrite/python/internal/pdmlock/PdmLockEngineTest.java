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
package org.openrewrite.python.internal.pdmlock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.LockFileRegeneration.Result;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.internal.pdmlock.PdmLockFixtures.resource;

/**
 * Golden scenarios replay PDM 2.28.0 runs against pypi.org (see the fixture README); the recorded
 * index/metadata responses under {@code /pdmlock/http/} keep the tests offline.
 */
class PdmLockEngineTest {

    RoutedHttp http;
    ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        http = new RoutedHttp();
        ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
                new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false)));
    }

    private void stubPypiSix() {
        http.route("https://pypi.org/simple/six/", resource("http/six-simple-json"));
        http.route("https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl.metadata",
                resource("http/six-1.17.0-py2.py3-none-any.whl.metadata"));
    }

    @Test
    void pinBumpMatchesRealPdmByteForByte() {
        stubPypiSix();
        Result result = PdmLockEngine.regenerate(
                resource("g-upgrade/pyproject.toml.after"),
                resource("g-upgrade/pdm.lock.before"),
                ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("g-upgrade/pdm.lock.after"));
    }

    @Test
    void noOpRelockIsByteIdenticalWithoutNetwork() {
        Result result = PdmLockEngine.regenerate(
                resource("g-upgrade/pyproject.toml.after"),
                resource("g-upgrade/pdm.lock.after"),
                ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("g-upgrade/pdm.lock.after"));
        assertThat(http.requests).isEmpty();
    }

    @Test
    void addingDependencyDefersToResolution() {
        String pyproject = resource("a-minimal/pyproject.toml")
                .replace("dependencies = [\"six==1.16.0\"]", "dependencies = [\"six==1.16.0\", \"attrs==25.1.0\"]");
        Result result = PdmLockEngine.regenerate(pyproject, resource("a-minimal/pdm.lock"), ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    static final class RoutedHttp implements HttpSender {
        final Map<String, byte[]> routes = new LinkedHashMap<>();
        final List<String> requests = new ArrayList<>();

        void route(String url, String body) {
            routes.put(url, body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            requests.add(url);
            byte[] body = routes.get(url);
            if (body == null) {
                return new Response(404, new ByteArrayInputStream(new byte[0]), () -> {
                });
            }
            return new Response(200, new ByteArrayInputStream(body), () -> {
            });
        }
    }
}
