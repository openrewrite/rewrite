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
import org.openrewrite.Validated;

import static org.assertj.core.api.Assertions.assertThat;

class SemverGlobMetadataTest {

    @Test
    void globMetadataPatternAcceptedAndMatches() {
        Validated<VersionComparator> validated = Semver.validate("latest.patch", "+backpatch*");
        assertThat(validated.isValid()).isTrue();
        VersionComparator vc = validated.getValue();
        assertThat(vc).isNotNull();
        assertThat(vc.isValid("1.2.3", "1.2.3+backpatch.001")).isTrue();
        assertThat(vc.isValid("1.2.3", "1.2.3+backpatch.999")).isTrue();
        assertThat(vc.isValid("1.2.3", "1.2.3")).isFalse();
        assertThat(vc.isValid("1.2.3", "1.2.3+something-else")).isFalse();
    }

    @Test
    void globQuestionMarkMatchesSingleCharacter() {
        Validated<VersionComparator> validated = Semver.validate("latest.patch", "+backpatch.00?");
        assertThat(validated.isValid()).isTrue();
        VersionComparator vc = validated.getValue();
        assertThat(vc).isNotNull();
        assertThat(vc.isValid("1.2.3", "1.2.3+backpatch.001")).isTrue();
        assertThat(vc.isValid("1.2.3", "1.2.3+backpatch.0010")).isFalse();
    }

    @Test
    void literalMetadataPatternStillWorks() {
        Validated<VersionComparator> validated = Semver.validate("25-29", "-jre");
        assertThat(validated.isValid()).isTrue();
        VersionComparator vc = validated.getValue();
        assertThat(vc).isNotNull();
        assertThat(vc.isValid("25", "29.0-jre")).isTrue();
        assertThat(vc.isValid("25", "29.0-android")).isFalse();
    }

    @Test
    void regexMetadataPatternStillWorks() {
        Validated<VersionComparator> validated = Semver.validate("latest.patch", "\\+backpatch\\..*");
        assertThat(validated.isValid()).isTrue();
        VersionComparator vc = validated.getValue();
        assertThat(vc).isNotNull();
        assertThat(vc.isValid("1.2.3", "1.2.3+backpatch.001")).isTrue();
        assertThat(vc.isValid("1.2.3", "1.2.3")).isFalse();
    }

    @Test
    void unbalancedRegexFallsBackToGlobThatHappensToBeValid() {
        // "(" is invalid regex on its own, and the glob form Pattern.quotes it -> valid regex.
        Validated<VersionComparator> validated = Semver.validate("latest.patch", "+(suffix");
        assertThat(validated.isValid()).isTrue();
        VersionComparator vc = validated.getValue();
        assertThat(vc).isNotNull();
        assertThat(vc.isValid("1.2.3", "1.2.3+(suffix")).isTrue();
    }
}
