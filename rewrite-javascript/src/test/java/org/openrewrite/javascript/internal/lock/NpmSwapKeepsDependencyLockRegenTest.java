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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Swapping {@code ms} for {@code debug}, which itself depends on {@code ms}: npm keeps {@code ms}, now as
 * {@code debug}'s dependency rather than the root's. Each test replays the fixture OFFLINE and asserts the emitted
 * lock is BYTE-IDENTICAL to a golden {@code after} recorded from a real {@code npm install --package-lock-only}
 * (npm 11.16.0).
 */
class NpmSwapKeepsDependencyLockRegenTest extends LockRegenTestSupport {

    private static final String FIXTURE = "lock/npm/swap-keeps-dependency/";

    @BeforeEach
    void routes() {
        routes.put(REG + "debug", resource(FIXTURE + "http/debug"));
        routes.put(REG + "debug/4.4.3", resource(FIXTURE + "http/debug-4.4.3"));
        routes.put(REG + "ms", resource(FIXTURE + "http/ms"));
        routes.put(REG + "ms/2.1.3", resource(FIXTURE + "http/ms-2.1.3"));
    }

    @Test
    void swapKeepsDependencyOfAddedPackageV3() {
        Result result = NativeLockEngine.regenerate(PackageManager.Npm, resource(FIXTURE + "pkg-after"),
                resource(FIXTURE + "pkg-before"), resource(FIXTURE + "before-v3"), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(FIXTURE + "after-v3"));
    }

    @Test
    void swapKeepsDependencyOfAddedPackageV2() {
        Result result = NativeLockEngine.regenerate(PackageManager.Npm, resource(FIXTURE + "pkg-after"),
                resource(FIXTURE + "pkg-before"), resource(FIXTURE + "before-v2"), null, Paths.get("package.json"), ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(FIXTURE + "after-v2"));
    }

    @Test
    void devDependencySwapKeepsDependencyAsProduction() {
        // ms was a devDependency; the production debug now needs it, so npm drops its dev flag.
        Result result = NativeLockEngine.regenerate(PackageManager.Npm, resource(FIXTURE + "pkg-after"),
                resource(FIXTURE + "pkg-before-dev"), resource(FIXTURE + "before-dev"), null, Paths.get("package.json"),
                ctx);

        assertThat(result.isSuccess()).as(String.valueOf(result.getErrorMessage())).isTrue();
        assertThat(result.getLockFileContent()).isEqualTo(resource(FIXTURE + "after-dev"));
    }
}
