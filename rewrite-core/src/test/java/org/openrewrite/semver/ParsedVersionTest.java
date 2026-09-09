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

import static org.assertj.core.api.Assertions.assertThat;

class ParsedVersionTest {

    @Test
    void capturesNumericGroupsAndQualifier() {
        ParsedVersion parsed = ParsedVersion.parse("3.1.4.2.5-RC1");
        assertThat(parsed.matches()).isTrue();
        assertThat(parsed.group(1)).isEqualTo("3");
        assertThat(parsed.group(2)).isEqualTo("1");
        assertThat(parsed.group(3)).isEqualTo("4");
        assertThat(parsed.group(4)).isEqualTo("2");
        assertThat(parsed.group(5)).isEqualTo("5");
        assertThat(parsed.qualifier()).isEqualTo("-RC1");
    }

    @Test
    void absentGroupsAreNull() {
        ParsedVersion parsed = ParsedVersion.parse("3.0");
        assertThat(parsed.matches()).isTrue();
        assertThat(parsed.group(1)).isEqualTo("3");
        assertThat(parsed.group(2)).isEqualTo("0");
        assertThat(parsed.group(3)).isNull();
        assertThat(parsed.group(4)).isNull();
        assertThat(parsed.group(5)).isNull();
        assertThat(parsed.qualifier()).isNull();
    }

    @Test
    void releaseQualifierIsNotPreRelease() {
        assertThat(ParsedVersion.parse("2.7.18").isPreReleaseEnding()).isFalse();
        assertThat(ParsedVersion.parse("1.5.22.RELEASE").isPreReleaseEnding()).isFalse();
        assertThat(ParsedVersion.parse("3.0.0.Final").isPreReleaseEnding()).isFalse();
    }

    @Test
    void recognizesPreReleaseEndings() {
        assertThat(ParsedVersion.parse("3.0.0-RC1").isPreReleaseEnding()).isTrue();
        assertThat(ParsedVersion.parse("3.0.0-M1").isPreReleaseEnding()).isTrue();
        assertThat(ParsedVersion.parse("3.0.0-SNAPSHOT").isPreReleaseEnding()).isTrue();
        assertThat(ParsedVersion.parse("2.0.0.Alpha2").isPreReleaseEnding()).isTrue();
    }

    @Test
    void nonVersionDoesNotMatch() {
        ParsedVersion parsed = ParsedVersion.parse("not-a-version");
        assertThat(parsed.matches()).isFalse();
        assertThat(parsed.group(1)).isNull();
        assertThat(parsed.qualifier()).isNull();
    }

    @Test
    void repeatedParsesAreMemoized() {
        // The same instance is returned for repeat parses, eliminating the per-candidate
        // Matcher (and regex-group) allocations that dominate large multi-module upgrades.
        ParsedVersion first = ParsedVersion.parse("3.0.0");
        ParsedVersion second = ParsedVersion.parse("3.0.0");
        assertThat(second).isSameAs(first);
    }
}
