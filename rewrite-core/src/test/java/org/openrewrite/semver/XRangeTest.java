/*
 * Copyright 2021 the original author or authors.
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XRangeTest {

    @Test
    void isValidWhenCurrentIsNull() {
        XRange xRange = XRange.build("*", null).getValue();

        assertThat(xRange).isNotNull();
        assertThat(xRange.isValid(null, "1.0.0")).isTrue();
    }

    @Test
    void pattern() {
        assertThat(XRange.build("*", null).isValid()).isTrue();
        assertThat(XRange.build("*.0.0", null).isValid()).isFalse();
        assertThat(XRange.build("1.x", null).isValid()).isTrue();
        assertThat(XRange.build("1.x.0", null).isValid()).isFalse();
        assertThat(XRange.build("1.1.X", null).isValid()).isTrue();
        assertThat(XRange.build("1.1.1.X", null).isValid()).isTrue();
        assertThat(XRange.build("1.1.1.1.X", null).isValid()).isFalse();
        assertThat(XRange.build("1.1.x.1", null).isValid()).isFalse();
        assertThat(XRange.build("a", null).isValid()).isFalse();
    }

    @Test
    void doesNotMatchFixedVersion() {
        assertThat(XRange.build("5.3.0", null).isValid()).isFalse();
    }

    /**
     * X := >=0.0.0
     */
    @Test
    void anyVersion() {
        XRange xRange = XRange.build("X", null).getValue();

        assertThat(xRange).isNotNull();
        assertThat(xRange.isValid("1.0", "0.0.0.0")).isTrue();
        assertThat(xRange.isValid("1.0", "0.0.0")).isTrue();
    }

    /**
     * 1.* := >=1.0.0 <2.0.0-0
     */
    @Test
    void matchingMajorVersion() {
        XRange xRange = XRange.build("1.*", null).getValue();

        assertThat(xRange).isNotNull();
        assertThat(xRange.isValid("1.0", "1.0.0")).isTrue();
        assertThat(xRange.isValid("1.0", "1.0.0.1")).isTrue();
        assertThat(xRange.isValid("1.0", "1.2.3.RELEASE")).isTrue();
        assertThat(xRange.isValid("1.0", "1.9.9")).isTrue();
        assertThat(xRange.isValid("1.0", "2.0.0")).isFalse();
    }

    /**
     * 1.2.X := >=1.2.0 <1.3.0
     */
    @Test
    void matchingMajorAndMinorVersions() {
        XRange xRange = XRange.build("1.2.X", null).getValue();

        assertThat(xRange).isNotNull();
        assertThat(xRange.isValid("1.0", "1.2.0")).isTrue();
        assertThat(xRange.isValid("1.0", "1.3.0")).isFalse();
    }

    @Test
    void matchingMicroVersions() {
        XRange xRange = XRange.build("1.2.3.X", null).getValue();

        assertThat(xRange).isNotNull();
        assertThat(xRange.isValid("1.0", "1.2.3.0")).isTrue();
        assertThat(xRange.isValid("1.0", "1.2.3")).isTrue();
        assertThat(xRange.isValid("1.0", "1.2.4.0")).isFalse();
        assertThat(xRange.isValid("1.0", "1.2.4")).isFalse();
    }

    @Test
    void matchingJavaxValidation() {
        XRange xRange = XRange.build("2.X", null).getValue();

        assertThat(xRange).isNotNull();
        // The version pattern of javax.validation:validation-api
        assertThat(xRange.isValid("1.0", "2.0.1.Final")).isTrue();
    }

    @Test
    void matchCustomMetadata() {
        assertThat(new XRange("3", "2", "*", "", ".Final-custom-\\d+").isValid(null, "3.2.9.Final-custom-00003")).isTrue();
        // -beta is a recognized pre-release pattern, which isn't allowed.
        assertThat(XRange.build("3.5.x", "-beta").getValue().isValid(null, "3.5.1-beta")).isFalse();
    }

    @Test
    void compareRCVersion() {
        XRange xRange = XRange.build("3.5.x", null).getValue();
        assertThat(xRange).isNotNull();
        assertThat(xRange.upgrade("3.5.0-RC1", List.of("3.5.0", "3.5.1-RC1")).orElse(null)).isEqualTo("3.5.0");
    }

    @Test
    void shouldNotSwitchToSnapshot() {
        XRange xRange = XRange.build("3.6.x", null).getValue();
        assertThat(xRange).isNotNull();
        assertThat(xRange.isValid(null, "3.6.1-SNAPSHOT")).isFalse();
        assertThat(xRange.isValid(null, "3.6.0")).isTrue();
        assertThat(xRange.upgrade("3.4.0", List.of("3.6.0", "3.6.1-SNAPSHOT")).orElse(null)).isEqualTo("3.6.0");
    }

    @Test
    void compare() {
        XRange xrange = XRange.build("1.0.x", null).getValue();

        assertThat(xrange).isNotNull();
        assertThat(xrange.compare(null, "0.9", "1.0.x")).isNegative();
        assertThat(xrange.compare(null, "1.0.x", "0.9")).isPositive();
        assertThat(xrange.compare(null, "1.0", "1.0.x")).isZero();
        assertThat(xrange.compare(null, "1.0.x", "1.0")).isZero();
        assertThat(xrange.compare(null, "1.1", "1.0.x")).isPositive();
        assertThat(xrange.compare(null, "1.0.x", "1.1")).isNegative();
        assertThat(xrange.compare(null, "1.0.x", "1.0.x")).isZero();
        assertThat(xrange.compare(null, "1.x", "1.0.x")).isPositive();
        assertThat(xrange.compare(null, "1.0.x", "1.x")).isNegative();
    }

    /**
     * Tests Gradle-style dynamic versions using '+' as a wildcard.
     * The '+' wildcard should behave identically to 'x', '*', or 'X'.
     */
    @Test
    void gradleDynamicVersionWithPlus() {
        // Test that '+' is recognized as a valid wildcard
        assertThat(XRange.build("2.+", null).isValid()).isTrue();
        assertThat(XRange.build("1.0.+", null).isValid()).isTrue();
        assertThat(XRange.build("1.2.3.+", null).isValid()).isTrue();

        // Test major version wildcard: 2.+ should match any 2.x.x version
        XRange majorWildcard = XRange.build("2.+", null).getValue();
        assertThat(majorWildcard).isNotNull();
        assertThat(majorWildcard.isValid("2.0", "2.0.0")).isTrue();
        assertThat(majorWildcard.isValid("2.0", "2.5.3")).isTrue();
        assertThat(majorWildcard.isValid("2.0", "2.99.99")).isTrue();
        assertThat(majorWildcard.isValid("2.0", "3.0.0")).isFalse();
        assertThat(majorWildcard.isValid("2.0", "1.9.9")).isFalse();

        // Test minor version wildcard: 1.0.+ should match any 1.0.x version
        XRange minorWildcard = XRange.build("1.0.+", null).getValue();
        assertThat(minorWildcard).isNotNull();
        assertThat(minorWildcard.isValid("1.0", "1.0.0")).isTrue();
        assertThat(minorWildcard.isValid("1.0", "1.0.9")).isTrue();
        assertThat(minorWildcard.isValid("1.0", "1.1.0")).isFalse();

        // Test that 2.+ behaves identically to 2.x
        XRange plusRange = XRange.build("2.+", null).getValue();
        XRange xRange = XRange.build("2.x", null).getValue();
        assertThat(plusRange.isValid("2.0", "2.5.3")).isEqualTo(xRange.isValid("2.0", "2.5.3"));
        assertThat(plusRange.isValid("2.0", "3.0.0")).isEqualTo(xRange.isValid("2.0", "3.0.0"));
    }
}
