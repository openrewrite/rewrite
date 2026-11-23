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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LockFileRegenerationStabilityTest {
    @Test
    void retriesNpmNormalizationWithTheSameProjectInputs() {
        Request request = Request.builder().packageJson("edited").lock("before")
          .packageJsonPath(Paths.get("package.json"))
          .manifest(Paths.get("apps/a/package.json"), "member")
          .packageManagerFile(Paths.get(".npmrc"), "config")
          .editedFile(Paths.get("extra.json"), "created").build();
        List<String> inputs = new ArrayList<>();
        Result result = LockFileRegeneration.stabilize(request, next -> {
            assertThat(next.getPackageJson()).isEqualTo(request.getPackageJson());
            assertThat(next.getManifests()).isEqualTo(request.getManifests());
            assertThat(next.getPackageManagerFiles()).isEqualTo(request.getPackageManagerFiles());
            assertThat(next.getPackageJsonPath()).isEqualTo(request.getPackageJsonPath());
            inputs.add(next.getLock());
            return "before".equals(next.getLock()) ? rewrites("first") : Result.success("stable");
        });
        assertThat(inputs).containsExactly("before", "first");
        assertThat(result.getLockFileContent()).isEqualTo("stable");
    }

    @Test
    void neverRetriesPeerRejections() {
        Failure rejection = new Failure(Kind.SECOND_INSTALL, Reason.PEER_CONFLICT, "typescript", "ERESOLVE", "first");
        List<String> inputs = new ArrayList<>();
        Result result = LockFileRegeneration.stabilize(request(), next -> {
            inputs.add(next.getLock());
            return Result.failure(rejection);
        });
        assertThat(inputs).hasSize(1);
        assertThat(result.getFailure()).isSameAs(rejection);
    }

    @Test
    void stopsOnCyclesAndRetainsTheFailure() {
        List<String> inputs = new ArrayList<>();
        Result result = LockFileRegeneration.stabilize(request(), next -> {
            inputs.add(next.getLock());
            return rewrites("before".equals(next.getLock()) ? "other" : "before");
        });
        assertThat(inputs).containsExactly("before", "other");
        assertThat(result.getFailure()).isNotNull();
    }

    @Test
    void boundsNonConvergentInstalls() {
        List<String> inputs = new ArrayList<>();
        Result result = LockFileRegeneration.stabilize(request(), next -> {
            inputs.add(next.getLock());
            return rewrites("lock" + inputs.size());
        });
        assertThat(inputs).hasSize(4);
        assertThat(result.getFailure()).isNotNull();
    }

    private static Request request() {
        return Request.builder().packageJson("edited").lock("before").build();
    }

    private static Result rewrites(String written) {
        return Result.failure(new Failure(Kind.SECOND_INSTALL, Reason.RESOLUTION_REQUIRED, null,
          "a second npm install would rewrite it", written));
    }
}
