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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class NativeLockEngineTest {

    private ExecutionContext ctx;
    private final Map<String, String> routes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        HttpSender http = request -> {
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        // Pin the registry so discovery is deterministic (never reads a real ~/.npmrc or env).
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true, false)));
    }

    private Result regen(PackageManager pm, String original, String edited, String lock) {
        return NativeLockEngine.regenerate(pm, edited, original, lock, null, Paths.get("package.json"), ctx);
    }

    private static String npmLock(String lockedVersion) {
        return "{\n" +
                "  \"name\": \"x\",\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"name\": \"x\", \"dependencies\": {\"lodash\": \"^4.17.20\"}},\n" +
                "    \"node_modules/lodash\": {\"version\": \"" + lockedVersion + "\"}\n" +
                "  }\n" +
                "}\n";
    }

    @Test
    void closureChangingUpgradeFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash",
                "{\"versions\":{\"4.17.20\":{},\"4.18.0\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{}}");
        routes.put("https://registry.npmjs.org/lodash/4.18.0",
                "{\"name\":\"lodash\",\"version\":\"4.18.0\",\"dependencies\":{\"tslib\":\"^2.0.0\"}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.18.0\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("lodash");
        assertThat(result.getFailure().getDetail()).contains("dependencies changed");
    }

    @Test
    void addedPeerDependencyFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash",
                "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{}," +
                        "\"peerDependencies\":{\"react\":\">=17\"}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("peerDependencies changed");
    }

    @Test
    void writeThroughEnginesBumpReachesAbsentPatcher() {
        routes.put("https://registry.npmjs.org/lodash",
                "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.20",
                "{\"name\":\"lodash\",\"version\":\"4.17.20\",\"dependencies\":{},\"engines\":{\"node\":\">=12\"}}");
        routes.put("https://registry.npmjs.org/lodash/4.17.21",
                "{\"name\":\"lodash\",\"version\":\"4.17.21\",\"dependencies\":{},\"engines\":{\"node\":\">=14\"}," +
                        "\"dist\":{\"tarball\":\"https://r/lodash-4.17.21.tgz\",\"integrity\":\"sha512-x\",\"shasum\":\"abc\"}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                npmLock("4.17.20"));

        // The engines-only delta passes the closure proof (write-through tier), so the engine reaches the
        // patcher-dispatch seam; Wave 3 has not registered a patcher yet, so it fails loud but cleanly.
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("no native patcher");
    }

    @Test
    void pnpmLegacyLockfileVersionUnsupported() {
        Result result = regen(PackageManager.Pnpm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^4.17.21\"}}",
                "lockfileVersion: 5.4\n");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_LOCKFILE_VERSION);
    }

    @Test
    void addFailsLoud() {
        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{}}",
                "{\"dependencies\":{\"left-pad\":\"^1.3.0\"}}",
                "{\"lockfileVersion\":3,\"packages\":{\"\":{}}}");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("left-pad");
        assertThat(result.getFailure().getDetail()).contains("adding");
    }

    @Test
    void unsupportedEntryTypeFailsLoud() {
        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"git+https://github.com/lodash/lodash.git\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE);
    }

    @Test
    void versionNotFoundFailsLoud() {
        routes.put("https://registry.npmjs.org/lodash", "{\"versions\":{\"4.17.20\":{},\"4.17.21\":{}}}");

        Result result = regen(PackageManager.Npm,
                "{\"dependencies\":{\"lodash\":\"^4.17.20\"}}",
                "{\"dependencies\":{\"lodash\":\"^5.0.0\"}}",
                npmLock("4.17.20"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.VERSION_NOT_FOUND);
    }
}
