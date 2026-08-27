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
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline replay of the greedy-forward delta resolver (ADR 0010 T2 adds and T1 cascades)
 * against tiny packages so CI has a network-free regression test. Each scenario's index and
 * metadata responses under its {@code /uvlock/offN-.../http/} directory were recorded from
 * pypi.org while the engine's output was verified byte-identical to real {@code uv lock} 0.10.11; the
 * network-hitting counterparts live in {@link UvLockCascadeLiveTest}.
 */
class UvLockDeltaOfflineTest {

    RoutedHttp http;
    ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        http = new RoutedHttp();
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false)));
    }

    /** T2: adding a leaf package (iniconfig, no dependencies) inserts one entry and a root edge. */
    @Test
    void addLeafMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/iniconfig/", "off1-add-leaf/http/iniconfig-listing-json");
        http.route("https://files.pythonhosted.org/packages/cb/b1/3846dd7f199d53cb17f49cba7e651e9ce294d8497c8c150530ed11865bb8/iniconfig-2.3.0-py3-none-any.whl.metadata",
          "off1-add-leaf/http/iniconfig-2.3.0-py3-none-any.whl.metadata");
        assertScenario("off1-add-leaf");
    }

    /** T2: adding python-dateutil resolves it and its transitive closure (six). */
    @Test
    void addClosureMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/python-dateutil/", "off2-add-closure/http/python-dateutil-listing-json");
        http.route("https://pypi.org/simple/six/", "off2-add-closure/http/six-listing-json");
        http.route("https://files.pythonhosted.org/packages/ec/57/56b9bcc3c9c6a792fcbaf139543cee77261f3651ca9da0c93f5c1221264b/python_dateutil-2.9.0.post0-py2.py3-none-any.whl.metadata",
          "off2-add-closure/http/python_dateutil-2.9.0.post0-py2.py3-none-any.whl.metadata");
        http.route("https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl.metadata",
          "off2-add-closure/http/six-1.17.0-py2.py3-none-any.whl.metadata");
        assertScenario("off2-add-closure");
    }

    /** T1: bumping flake8 6.0.0 -> 6.1.0 forces its lockstep pins pycodestyle and pyflakes to move. */
    @Test
    void cascadeMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/flake8/", "off3-cascade-flake8/http/flake8-listing-json");
        http.route("https://pypi.org/simple/pycodestyle/", "off3-cascade-flake8/http/pycodestyle-listing-json");
        http.route("https://pypi.org/simple/pyflakes/", "off3-cascade-flake8/http/pyflakes-listing-json");
        http.route("https://files.pythonhosted.org/packages/b0/24/bbf7175ffc47cb3d3e1eb523ddb23272968359dfcf2e1294707a2bf12fc4/flake8-6.1.0-py2.py3-none-any.whl.metadata",
          "off3-cascade-flake8/http/flake8-6.1.0-py2.py3-none-any.whl.metadata");
        http.route("https://files.pythonhosted.org/packages/b1/90/a998c550d0ddd07e38605bb5c455d00fcc177a800ff9cc3dafdcb3dd7b56/pycodestyle-2.11.1-py2.py3-none-any.whl.metadata",
          "off3-cascade-flake8/http/pycodestyle-2.11.1-py2.py3-none-any.whl.metadata");
        http.route("https://files.pythonhosted.org/packages/00/e9/1e1fd7fae559bfd07704991e9a59dd1349b72423c904256c073ce88a9940/pyflakes-3.1.0-py2.py3-none-any.whl.metadata",
          "off3-cascade-flake8/http/pyflakes-3.1.0-py2.py3-none-any.whl.metadata");
        assertScenario("off3-cascade-flake8");
    }

    /**
     * Increment 3: adding a leaf gated on a non-version marker records the marker on the root
     * edge and requires-dist only; the [[package]] entry is identical to an unmarkered add, so
     * off1's iniconfig fixtures serve both.
     */
    @Test
    void addMarkeredLeafMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/iniconfig/", "off1-add-leaf/http/iniconfig-listing-json");
        http.route("https://files.pythonhosted.org/packages/cb/b1/3846dd7f199d53cb17f49cba7e651e9ce294d8497c8c150530ed11865bb8/iniconfig-2.3.0-py3-none-any.whl.metadata",
          "off1-add-leaf/http/iniconfig-2.3.0-py3-none-any.whl.metadata");
        assertScenario("off4-add-markered");
    }

    /**
     * Increment 3: the marker on a markered add stays on the root edge only; the pulled-in
     * closure (python-dateutil -> six, all unconditional) is byte-identical to the unmarkered
     * closure add, so off2's fixtures serve both.
     */
    @Test
    void addMarkeredClosureMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/python-dateutil/", "off2-add-closure/http/python-dateutil-listing-json");
        http.route("https://pypi.org/simple/six/", "off2-add-closure/http/six-listing-json");
        http.route("https://files.pythonhosted.org/packages/ec/57/56b9bcc3c9c6a792fcbaf139543cee77261f3651ca9da0c93f5c1221264b/python_dateutil-2.9.0.post0-py2.py3-none-any.whl.metadata",
          "off2-add-closure/http/python_dateutil-2.9.0.post0-py2.py3-none-any.whl.metadata");
        http.route("https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl.metadata",
          "off2-add-closure/http/six-1.17.0-py2.py3-none-any.whl.metadata");
        assertScenario("off5-add-markered-closure");
    }

    /**
     * uv rewrites {@code platform_system == 'Windows'} to {@code sys_platform == 'win32'} on both
     * the root edge and requires-dist; the engine reproduces that normalization (off1's iniconfig
     * fixtures serve the unchanged [[package]] entry).
     */
    @Test
    void addMarkeredPlatformSystemMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/iniconfig/", "off1-add-leaf/http/iniconfig-listing-json");
        http.route("https://files.pythonhosted.org/packages/cb/b1/3846dd7f199d53cb17f49cba7e651e9ce294d8497c8c150530ed11865bb8/iniconfig-2.3.0-py3-none-any.whl.metadata",
          "off1-add-leaf/http/iniconfig-2.3.0-py3-none-any.whl.metadata");
        assertScenario("off7-add-markered-platform");
    }

    /**
     * A python-version-gated add makes real uv record lock-level resolution-markers (a fork
     * boundary at 3.11) even though iniconfig resolves to one version; that is marker-space
     * resolution, so the engine fails loud before any network fetch.
     */
    @Test
    void markeredVersionAddFailsLoud() {
        Result result = UvLockEngine.regenerate(
          resource("off6-add-markered-version/pyproject.toml"),
          resource("off6-add-markered-version/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    /**
     * A lock restricted to a subset of environments ({@code [tool.uv] environments}, recorded as
     * {@code supported-markers}) is resolved per environment: uv drops edges gated on an unsupported
     * platform (this base lock's linux-only supported-markers already dropped click's win32-only
     * {@code colorama}). Resolving a new dependency into it needs marker-space resolution, so the
     * engine fails loud before any network fetch -- even when the manifest itself does not repeat the
     * {@code [tool.uv]} block (environments can come from {@code uv.toml} or the CLI).
     */
    @Test
    void restrictedEnvironmentsLockFailsLoud() {
        Result result = UvLockEngine.regenerate(
          resource("off8-restricted-environments/pyproject.toml"),
          resource("off8-restricted-environments/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    /**
     * A manifest declaring {@code [tool.uv] environments} restricts resolution to those environments;
     * the engine rejects any resolution under it (here an unforked base lock, so the manifest is the
     * only signal) rather than resolve universally and emit a lock uv would not.
     */
    @Test
    void manifestEnvironmentsFailsLoud() {
        Result result = UvLockEngine.regenerate(
          resource("off9-manifest-environments/pyproject.toml"),
          resource("off1-add-leaf/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    private void assertScenario(String scenario) {
        Result result = UvLockEngine.regenerate(
          resource(scenario + "/pyproject.toml"),
          resource(scenario + "/uv.lock.before"),
          ctx);
        assertThat(result.getFailure())
          .withFailMessage("engine failed: %s", result.getErrorMessage())
          .isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "/uv.lock.after"));
        // every URL the engine hit was recorded (returned 200); nothing fell through to the network
        assertThat(http.requests).isNotEmpty();
        assertThat(http.routes.keySet()).containsAll(http.requests);
    }

    static String resource(String name) {
        return new String(resourceBytes(name), StandardCharsets.UTF_8);
    }

    static byte[] resourceBytes(String name) {
        try (InputStream is = UvLockDeltaOfflineTest.class.getResourceAsStream("/uvlock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Serves recorded responses by exact URL; unrouted URLs return 404 and are recorded. */
    static final class RoutedHttp implements HttpSender {
        final Map<String, byte[]> routes = new LinkedHashMap<>();
        final List<String> requests = new ArrayList<>();

        void route(String url, String fixtureName) {
            routes.put(url, resourceBytes(fixtureName));
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
