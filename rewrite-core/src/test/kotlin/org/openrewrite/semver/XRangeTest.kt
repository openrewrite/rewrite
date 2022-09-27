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
package org.openrewrite.semver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XRangeTest {

    @Test
    fun isValidWhenCurrentIsNull() {
        val xRange: XRange = XRange.build("*", null).getValue()!!
        assertThat(xRange.isValid(null, "1.0.0")).isTrue
    }

    @Test
    fun pattern() {
        assertThat(XRange.build("*", null).isValid).isTrue
        assertThat(XRange.build("*.0.0", null).isValid).isFalse
        assertThat(XRange.build("1.x", null).isValid).isTrue
        assertThat(XRange.build("1.x.0", null).isValid).isFalse
        assertThat(XRange.build("1.1.X", null).isValid).isTrue
        assertThat(XRange.build("1.1.1.X", null).isValid).isTrue
        assertThat(XRange.build("1.1.1.1.X", null).isValid).isFalse
        assertThat(XRange.build("1.1.x.1", null).isValid).isFalse
        assertThat(XRange.build("a", null).isValid).isFalse
    }

    @Test
    fun doesNotMatchFixedVersion() {
        assertThat(XRange.build("5.3.0", null).isValid).isFalse
    }

    /**
     * X := >=0.0.0
     */
    @Test
    fun anyVersion() {
        val xRange: XRange = XRange.build("X", null).getValue()!!

        assertThat(xRange.isValid("1.0", "0.0.0.0")).isTrue
        assertThat(xRange.isValid("1.0", "0.0.0")).isTrue
    }

    /**
     * 1.* := >=1.0.0 <2.0.0-0
     */
    @Test
    fun matchingMajorVersion() {
        val xRange: XRange = XRange.build("1.*", null).getValue()!!

        assertThat(xRange.isValid("1.0", "1.0.0")).isTrue
        assertThat(xRange.isValid("1.0", "1.0.0.1")).isTrue
        assertThat(xRange.isValid("1.0", "1.2.3.RELEASE")).isTrue
        assertThat(xRange.isValid("1.0", "1.9.9")).isTrue
        assertThat(xRange.isValid("1.0", "2.0.0")).isFalse
    }

    /**
     * 1.2.X := >=1.2.0 <1.3.1
     */
    @Test
    fun matchingMajorAndMinorVersions() {
        val xRange: XRange = XRange.build("1.2.X", null).getValue()!!

        assertThat(xRange.isValid("1.0", "1.2.0")).isTrue
        assertThat(xRange.isValid("1.0", "1.3.0")).isFalse
    }

    @Test
    fun matchingMicroVersions() {
        val xRange: XRange = XRange.build("1.2.3.X", null).getValue()!!

        assertThat(xRange.isValid("1.0", "1.2.3.0")).isTrue
        assertThat(xRange.isValid("1.0", "1.2.3")).isTrue
        assertThat(xRange.isValid("1.0", "1.2.4.0")).isFalse
        assertThat(xRange.isValid("1.0", "1.2.4")).isFalse
    }

    @Test
    fun matchingJavaxValidation() {
        val xRange: XRange = XRange.build("2.X", null).getValue()!!

        // The version pattern of javax.validation:validation-api
        assertThat(xRange.isValid("1.0", "2.0.1.Final")).isTrue
    }
}
