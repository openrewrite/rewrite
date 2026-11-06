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
package org.openrewrite.javascript.internal.lock.npm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An install that changes nothing, up to placing, for every lockfileVersion 2 and 3 lock {@link NpmScenarios} finds:
 * the lock's own bytes, npm's own rewrite of it ({@link NpmScenarios.Lock#rewritten()}), or npm's load error.
 */
class NpmLockRoundTripTest {

    static Stream<Arguments> locks() {
        List<Arguments> out = new ArrayList<>();
        for (NpmScenarios.Scenario scenario : NpmScenarios.discover()) {
            for (NpmScenarios.Lock lock : scenario.locks()) {
                if (!lock.isInput() && ScenarioReplay.lockfileVersion(lock.getText()) >= 2) {
                    out.add(Arguments.of(lock.toString(), lock));
                }
            }
        }
        return out.stream();
    }

    static String roundTrip(NpmScenarios.Lock lock, NpmOptions options) {
        return ScenarioReplay.load(lock, options, ScenarioReplay.projectPath(lock.getText())).getMeta().toString();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("locks")
    void writesBackWhatNpmWrites(String name, NpmScenarios.Lock lock) {
        String expect = lock.getScenario().expect();
        // a lock is loaded and written as the release that recorded the scenario
        NpmOptions options = ScenarioReplay.options(lock.getScenario().release(), lock.getNpmrc());
        if (expect != null && expect.startsWith("malformed override key ") && !lock.isBefore()) {
            String key = expect.substring("malformed override key ".length());
            assertThatThrownBy(() -> roundTrip(lock, options)).as(expect)
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining(key);
            return;
        }
        String loadError = lock.loadError();
        if (loadError != null) {
            assertThatThrownBy(() -> roundTrip(lock, options)).as("npm-again.log")
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage(loadError);
            return;
        }
        String rewritten = lock.rewritePlacesNode() ? null : lock.rewritten();
        String expected = rewritten != null ? rewritten : lock.getText();
        assertThat(ScenarioReplay.load(lock, options, ScenarioReplay.projectPath(expected)).getMeta().toString())
          .isEqualTo(expected);
    }
}
