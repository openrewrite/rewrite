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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * validate-npm-package-name 4.0.0's and 7.0.2's own tests and the inputs where their sources differ
 * ({@code upstream/validate-npm-package-name-*}): each answer's errors and {@code validForOldPackages}.
 */
class ValidateNpmPackageNameTest {

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (ValidateNpmPackageName.Release release : ValidateNpmPackageName.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                if (call.arg(0) instanceof String) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String resource(ValidateNpmPackageName.Release release) {
        return "validate-npm-package-name-" + release.name().substring(1).replace('_', '.') + ".jsonl";
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(ValidateNpmPackageName.Release release, Call call) {
        String name = (String) call.arg(0);
        Map<?, ?> out = (Map<?, ?>) call.getOut();
        Object errors = out.containsKey("errors") ? out.get("errors") : Collections.emptyList();
        assertThat(ValidateNpmPackageName.errors(name, release)).isEqualTo(errors);
        assertThat(ValidateNpmPackageName.validForOldPackages(name, release)).isEqualTo(out.get("validForOldPackages"));
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (ValidateNpmPackageName.Release release : ValidateNpmPackageName.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                if (!(call.arg(0) instanceof String)) {
                    notPorted.merge(release + " a value that is not a string: every caller passes one", 1,
                            Integer::sum);
                }
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("V7_0_2 a value that is not a string: every caller passes one", 3);
        assertThat(notPorted).isEqualTo(expected);
    }
}
