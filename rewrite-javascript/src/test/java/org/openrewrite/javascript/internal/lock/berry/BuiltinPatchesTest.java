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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import static org.assertj.core.api.Assertions.assertThat;

class BuiltinPatchesTest {
    @Test
    void resolvesAndFetchesANewBuiltinLocator() {
        BerryScenarios.Scenario scenario = BerryScenarios.discover().stream()
                .filter(s -> s.getName().equals("compat-patch-typescript-bump")).findFirst()
                .orElseThrow(IllegalStateException::new);
        Result result = new YarnBerryLockRegenerator(scenario.inputs(Phase.AFTER, scenario.file("before")),
                scenario.registry()).regenerate();
        assertThat(result.getFailure()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(scenario.file("after"));
    }
    @Test
    void releasePatchTextsMatchTheIndependentHashInventory() {
        java.util.Map<String, String> hashes = new java.util.HashMap<>();
        for (String version : YarnRelease.all()) {
            YarnRelease release = YarnRelease.of(version);
            for (String name : release.getPatches()) {
                String source = CompatPatches.source(release, name);
                assertThat(source).as(version + " " + name).isNotNull();
                for (String packageVersion : new String[]{"1.22.8", "2.3.2", "5.6.3", "7.0.2"}) {
                    String key = HashUtils.sha512Hex(source.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + packageVersion;
                    String hash = hashes.computeIfAbsent(key, k -> PatchUtils.makePatchHash(
                            java.util.Collections.singletonList(source), packageVersion));
                    assertThat(hash).as(version + " " + name + "@" + packageVersion)
                            .isEqualTo(release.compatPatchHash(name, packageVersion));
                }
            }
        }
    }
}
