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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Override scenarios recorded from the real package managers against one shared, recorded registry
 * ({@code lock/overrides-registry}), replayed OFFLINE and compared BYTE-IDENTICAL with the tool's own lock.
 */
abstract class OverrideLockRegenTestSupport extends LockRegenTestSupport {

    private static final String REGISTRY = "lock/overrides-registry/";
    private static final String[] MANIFESTS = {"is-buffer@1.1.6", "is-buffer@2.0.0", "is-buffer@2.0.5",
            "is-number@1.1.2", "is-number@3.0.0", "is-number@7.0.0", "is-odd@0.1.1", "is-odd@0.1.2", "kind-of@3.2.2",
            "left-pad@1.3.0"};

    @BeforeEach
    void routes() {
        for (String nameVersion : MANIFESTS) {
            int at = nameVersion.lastIndexOf('@');
            String name = nameVersion.substring(0, at);
            String version = nameVersion.substring(at + 1);
            routes.put(REG + name, resource(REGISTRY + name));
            routes.put(REG + name + "/" + version, resource(REGISTRY + name + "-" + version));
            // yarn berry checksums a moved entry from its tarball.
            binaryRoutes.put(REG + name + "/-/" + name + "-" + version + ".tgz",
                    bytesResource(REGISTRY + name + "-" + version + ".tgz"));
        }
    }

    /** Replay {@code dir}'s {@code pkg-before -> pkg-after} edit over {@code before} and expect exactly {@code after}. */
    void assertMatchesTool(PackageManager pm, String dir) {
        Result result = NativeLockEngine.regenerate(pm, resource(dir + "/pkg-after"), resource(dir + "/pkg-before"),
                resource(dir + "/before"), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(dir + "/after"));
    }
}
