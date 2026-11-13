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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * glob-parent 2.0.0's and 5.1.2's own tests and the inputs where their sources differ ({@code upstream/glob-parent-*}),
 * whose one option, {@code flipBackslashes}, only Windows reads.
 */
class GlobParentTest {

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (GlobParent.Release release : GlobParent.Release.values()) {
            String resource = "glob-parent-" + release.name().substring(1).replace('_', '.') + ".jsonl";
            for (Call call : UpstreamCalls.read(resource)) {
                out.add(Arguments.of(release, call));
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(GlobParent.Release release, Call call) {
        assertThat(GlobParent.globParent((String) call.arg(0), release)).isEqualTo(call.getOut());
    }
}
