/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.semver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NodeRangeTest {

    @ParameterizedTest
    @MethodSource("rangeParse")
    void desugars(String range, String expected) {
        NodeRange parsed = NodeRange.parse(range);
        assertThat(parsed).as(range).isNotNull();
        assertThat(parsed.toString()).as(range).isEqualTo(expected);
    }

    static Stream<Arguments> rangeParse() {
        return NodeSemverCorpus.rows("/node-semver/range-parse.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @Test
    void upperBoundCarriesPrereleaseTail() {
        // The npm-specific `-0` convention that excludes 2.0.0-alpha from ^1.2.3.
        assertThat(NodeRange.parse("^1.2.3").toString()).isEqualTo(">=1.2.3 <2.0.0-0");
        assertThat(NodeRange.parse("~1.2.3").toString()).isEqualTo(">=1.2.3 <1.3.0-0");
    }

    @Test
    void validRange() {
        assertThat(NodeSemver.validRange("^1.2.3")).isTrue();
        assertThat(NodeSemver.validRange(">=1.0.0 <2.0.0")).isTrue();
        assertThat(NodeSemver.validRange("1.2.x || 2.x")).isTrue();
        assertThat(NodeSemver.validRange("not-a-range")).isFalse();
        assertThat(NodeSemver.validRange("^1.2.3.4")).isFalse();
        assertThat(NodeSemver.validRange(">=1.0.0 <>2.0.0")).isFalse();
    }

    @Test
    void wildcardRanges() {
        // Wildcard collapse forms aren't in range-parse.tsv; assert them via satisfies behavior.
        assertThat(NodeSemver.satisfies("1.2.3", "*")).isTrue();
        assertThat(NodeSemver.satisfies("1.2.3", "")).isTrue();
        assertThat(NodeSemver.satisfies("1.2.3", "x")).isTrue();
        assertThat(NodeSemver.satisfies("1.2.3", ">=*")).isTrue();
    }
}
