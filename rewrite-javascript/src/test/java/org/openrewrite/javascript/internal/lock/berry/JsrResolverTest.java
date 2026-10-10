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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * plugin-jsr helpers.ts: a jsr package is the npm package of its name wrapped into the {@code jsr} scope.
 */
class JsrResolverTest {

    @Test
    void wrapsAJsrPackageIntoItsNpmScope() {
        Descriptor scoped = StructUtils.parseDescriptor("@std/path@jsr:^1.0.0");
        assertThat(StructUtils.stringifyDescriptor(JsrResolver.convertDescriptorFromJsrToNpm(scoped)))
                .isEqualTo("@std/path@npm:@jsr/std__path@^1.0.0");
        Locator npm = StructUtils.parseLocator("@jsr/std__path@npm:1.0.8");
        assertThat(StructUtils.stringifyLocator(JsrResolver.convertLocatorFromNpmToJsr(npm)))
                .isEqualTo("@std/path@jsr:1.0.8");
        assertThat(StructUtils.stringifyLocator(JsrResolver.convertLocatorFromJsrToNpm(
                StructUtils.parseLocator("@std/path@jsr:1.0.8")))).isEqualTo("@jsr/std__path@npm:1.0.8");
    }

    /**
     * Yarn 4.10.3 runs out of heap in the resolution step (open-opt-default-protocol-jsr-edit): the npm alias the
     * resolver wraps into is normalized back to {@code jsr:}, one scope deeper each time.
     */
    @Test
    void aDefaultProtocolOfJsrWrapsWithoutEnd() {
        YarnBerryLockRegenerator regenerator = new YarnBerryLockRegenerator(YarnBerryLockRegenerator.Inputs.builder()
                .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"ms@npm:2.1.2\":\n  version: 2.1.2\n  " +
                      "resolution: \"ms@npm:2.1.2\"\n  checksum: " +
                      "10c0/a437714e2f90dbf881b5191d35a6db792efbca5badf112f87b9e1c712aace4b4b9b742dd6537f3edf90fd6f68" +
                      "4de897cec230abde57e87883766712ddda297cc\n  languageName: node\n  linkType: " +
                      "hard\n\n\"p@workspace:.\":\n  version: 0.0.0-use.local\n  resolution: \"p@workspace:.\"\n  " +
                      "dependencies:\n    ms: \"npm:2.1.2\"\n  languageName: unknown\n  linkType: soft\n")
                .manifest("", "{\"name\": \"p\", \"packageManager\": \"yarn@4.10.3\", " +
                              "\"dependencies\": {\"ms\": \"2.1.3\"}}")
                .configFile(".yarnrc.yml", "defaultProtocol: \"jsr:\"\n")
                .now(Instant.EPOCH)
                .build(), BerryScenarios.NO_REGISTRY);
        Result result = assertTimeoutPreemptively(Duration.ofSeconds(2), regenerator::regenerate);

        Failure failure = result.getFailure();
        assertThat(failure).isNotNull();
        assertThat(failure.getKind()).isEqualTo(Kind.PM_FAILS);
        assertThat(failure.getDetail()).isEqualTo("yarn fails on this edit: crash: JavaScript heap out of memory in " +
                                                  "the resolution step (the resolution chain ms@jsr:2.1.3 -> " +
                                                  "ms@npm:@jsr/ms@2.1.3 -> @jsr/ms@jsr:2.1.3 -> " +
                                                  "@jsr/ms@npm:@jsr/jsr__ms@2.1.3 -> @jsr/jsr__ms@jsr:2.1.3 " +
                                                  "revisits jsr:2.1.3 under a new name each time)");
    }
}
