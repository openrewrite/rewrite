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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.LockFileRegeneration.Result;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.internal.uvlock.UvLockFixtures.resource;

/**
 * Live-network validation that the greedy-forward delta resolver (ADR 0010 T1 cascades,
 * T2 adds, Increment 3 markered adds) reproduces real {@code uv lock} byte-for-byte:
 * {@code x1-cascade-boto3} bumps boto3 and forces botocore to move; {@code x2-add-leaf} adds a
 * leaf; {@code x3-add-closure} adds a package with a transitive closure; {@code off4-add-markered}
 * adds a marker-gated leaf. Fork induction and conditional-transitive intersection must fail loud.
 * Disabled by default because the packages' listings are too large to record as offline fixtures.
 */
@Disabled("hits pypi.org; run manually to validate T1 cascade + T2 adds + fork detection against real uv")
class UvLockCascadeLiveTest {

    /** boto3 bump forces botocore (1-level cascade, T1). */
    @Test
    void boto3CascadeByteIdentical() {
        assertScenarioMatchesRealUv("x1-cascade-boto3");
    }

    /** Adding a leaf package (six) inserts one [[package]] entry and a root edge (T2). */
    @Test
    void addLeafByteIdentical() {
        assertScenarioMatchesRealUv("x2-add-leaf");
    }

    /** Adding a package with a transitive closure (requests -> certifi/charset-normalizer/idna/urllib3) (T2). */
    @Test
    void addClosureByteIdentical() {
        assertScenarioMatchesRealUv("x3-add-closure");
    }

    /** Adding a leaf gated on a non-version marker records the marker on the root edge (Increment 3). */
    @Test
    void addMarkeredByteIdentical() {
        assertScenarioMatchesRealUv("off4-add-markered");
    }

    /**
     * Adding portalocker resolves its {@code platform_system == 'Windows'} transitive edge to
     * uv's normalized {@code sys_platform == 'win32'} form and pulls in pywin32 (Increment 3
     * marker normalization; unmarkered add exercising a fresh platform_system-gated closure).
     */
    @Test
    void addPlatformSystemTransitiveByteIdentical() {
        assertScenarioMatchesRealUv("x8-add-portalocker");
    }

    /**
     * Adding a package gated on a marker whose closure has a marker-gated transitive edge
     * (portalocker on linux needs pywin32 on win32; uv intersects to empty and drops it) must
     * fail loud rather than emit the transitive edge the intersection removes (Increment 3).
     */
    @Test
    void conditionalTransitiveMarkeredAddFailsLoud() {
        Result result = UvLockEngine.regenerate(
                resource("x7-add-markered-conditional/pyproject.toml"),
                resource("x7-add-markered-conditional/uv.lock.before"),
                liveCtx());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("marker-gated edge");
    }

    /**
     * Adding a package that uv would fork across the requires-python range (click on a >=3.8
     * lock: 8.1.8 for &lt;3.10, 8.4.2 for &gt;=3.10) must fail loud, not silently lock one version.
     */
    @Test
    void forkInducingAddFailsLoud() {
        ExecutionContext ctx = liveCtx();
        Result result = UvLockEngine.regenerate(
                resource("x5-add-forks/pyproject.toml"),
                resource("x5-add-forks/uv.lock.before"),
                ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("fork");
    }

    private static ExecutionContext liveCtx() {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender());
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
                new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false)));
        return ctx;
    }

    private static void assertScenarioMatchesRealUv(String scenario) {
        Result result = UvLockEngine.regenerate(
                resource(scenario + "/pyproject.toml"),
                resource(scenario + "/uv.lock.before"),
                liveCtx());

        assertThat(result.getFailure())
                .withFailMessage("engine failed: %s", result.getErrorMessage())
                .isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "/uv.lock.after"));
    }
}
