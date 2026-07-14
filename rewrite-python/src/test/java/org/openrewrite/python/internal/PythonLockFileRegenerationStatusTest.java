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
package org.openrewrite.python.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.python.table.PythonLockFileRegenerationResults.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for the status derivation that feeds the
 * {@code PythonLockFileRegenerationResults} data table. Exercising the pure
 * classification directly keeps all five outcomes deterministic, independent of
 * whether {@code uv}/{@code pipenv} are installed in the test environment.
 */
class PythonLockFileRegenerationStatusTest {

    @Test
    void noRegenerationMeansNoLockPresent() {
        assertThat(PyProjectHelper.regenerationStatus("old lock", null))
                .isEqualTo(Status.NO_LOCK_PRESENT);
    }

    @Test
    void toolNotInstalled() {
        assertThat(PyProjectHelper.regenerationStatus("old lock",
                LockFileRegeneration.Result.toolNotInstalled("pipenv is not installed")))
                .isEqualTo(Status.TOOL_NOT_INSTALLED);
    }

    @Test
    void failed() {
        assertThat(PyProjectHelper.regenerationStatus("old lock",
                LockFileRegeneration.Result.failure("pipenv lock failed (exit code 1): boom")))
                .isEqualTo(Status.FAILED);
    }

    @Test
    void unchangedWhenRegeneratedLockMatchesOriginal() {
        assertThat(PyProjectHelper.regenerationStatus("same lock",
                LockFileRegeneration.Result.success("same lock")))
                .isEqualTo(Status.UNCHANGED);
    }

    @Test
    void regeneratedWhenContentDiffers() {
        assertThat(PyProjectHelper.regenerationStatus("old lock",
                LockFileRegeneration.Result.success("new lock")))
                .isEqualTo(Status.REGENERATED);
    }

    @Test
    void regeneratedWhenNoOriginalLockToCompareAgainst() {
        assertThat(PyProjectHelper.regenerationStatus(null,
                LockFileRegeneration.Result.success("new lock")))
                .isEqualTo(Status.REGENERATED);
    }
}
