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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Host;

import static org.assertj.core.api.Assertions.assertThat;

class BunLockRegeneratorTest {

    /**
     * {@code open-patched-backslash}, recorded on macOS, where bun keeps the backslash; Windows is not recorded.
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "win32")
    void aBackslashInAPatchPathIsCannotKnowOffPosix(String os) {
        BunScenarios.Scenario scenario = BunScenarios.discover().stream()
          .filter(s -> "open-patched-backslash".equals(s.getLabel()))
          .findFirst()
          .orElseThrow(IllegalStateException::new);
        Result result = new BunLockRegenerator(BunScenarioTest.inputs(scenario).toBuilder()
          .host(Host.builder().os(os).build())
          .build(), new BunScenarioTest.ScenarioRegistry(scenario)).regenerate();
        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).isEqualTo("the engine cannot know patch file " +
          "patches\\ms@2.1.3.patch (bun on Windows reads its backslash as a separator, and the host is not known to " +
          "be another platform)");
    }
}
