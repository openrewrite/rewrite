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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class PackageFetcherTest {

    private static final String RACE = "the engine cannot know which of hasown@^2.0.3 (locked at 2.0.4, under " +
                                       "is-accessor-descriptor@^1.0.1) and hasown@^2.0.0 (resolved anew to 2.0.4, " +
                                       "under is-data-descriptor@^1.0.1) yarn resolves first: the fetch step " +
                                       "downloads the entry they share from the first one's remote, and ";
    private static final String HASOWN_INTEGRITY = "sha512-T2UbfbBEF32wiepXIsMlTW9+dDYC6wMh/t/vYA4tuOMKqWz/n3vr1NFSx" +
                                                   "QiyP+zk2mXsoMA/i/7qV6LKut1t1A==";
    private static final String FUNCTION_BIND_INTEGRITY = "sha512-7XHNxH7qX9xG5mIwxkhumTox/MIRNcOgDrxWsMt2pAr23WHp6" +
                                                          "MrRlN7FBSFpCpr+oVO0F744iUgR82nJMfG2SA==";

    /**
     * Under --pure-lockfile yarn writes no lock, so which of two raced entries it resolves first decides only the
     * tarball it downloads (open-order-race-small-pure-lockfile: exit 0 either way).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "both remotes name one tarball|false|false|null",
            "the fresh entry's tarball answers 404|true|false|the engine cannot know whether yarn's cache holds " +
            "hasown@2.0.4, whose tarball https://registry.yarnpkg.com/hasown/-/hasown-2.0.4.tgz answers 404 (yarn " +
            "fails on a cold cache and installs from a warm one)",
            "the lock entry names another tarball|false|true|the two remotes do not name one tarball",
    })
    void fetchesARacedEntryTheSameFromEitherRemote(String label, boolean freshTarballMissing, boolean otherIntegrity,
                                                   @Nullable String cannotKnow) {
        YarnClassicScenarios.Scenario scenario = YarnClassicScenarios.discover().stream()
                .filter(s -> s.getName().equals("open-order-race-small-pure-lockfile")).findFirst()
                .orElseThrow(IllegalStateException::new);
        String before = requireNonNull(scenario.file("before"));
        if (otherIntegrity) {
            before = before.replace(HASOWN_INTEGRITY, FUNCTION_BIND_INTEGRITY);
        }
        YarnClassicScenarioTest.ScenarioRegistry recorded = new YarnClassicScenarioTest.ScenarioRegistry(scenario);
        RegistrySource registry = new RegistrySource() {
            @Override
            public @Nullable String packument(String url, String name, boolean full, @Nullable String authorization) {
                return recorded.packument(url, name, full, authorization);
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
                return freshTarballMissing && url.startsWith("https://registry.yarnpkg.com/hasown/") ? null :
                        recorded.tarball(url, name, version, authorization);
            }
        };
        Result result = new YarnClassicLockRegenerator(scenario.inputs(Phase.AFTER, before),
                registry).regenerate();
        if (cannotKnow == null) {
            assertThat(result.getLockFileContent()).isEqualTo(before);
        } else {
            assertThat(requireNonNull(result.getFailure()).getDetail()).isEqualTo(RACE + cannotKnow);
        }
        assertThat(recorded.missing).isEmpty();
    }
}
