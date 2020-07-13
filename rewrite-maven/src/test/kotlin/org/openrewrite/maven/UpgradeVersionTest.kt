/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpgradeVersionTest {

    @Nested
    inner class LatestReleaseTest {
        private val latestRelease = UpgradeVersion.LatestRelease()

        @Test
        fun onlyNumericPartsValid() {
            assertThat(latestRelease.isValid("1.1.1")).isTrue()
            assertThat(latestRelease.isValid("1.1")).isTrue()
            assertThat(latestRelease.isValid("1")).isTrue()
            assertThat(latestRelease.isValid("1.1.a")).isFalse()
        }

        @Test
        fun differentPatchVersions() {
            assertThat(latestRelease.compare("1.1.1", "1.1.2")).isLessThan(0)
        }

        @Test
        fun differentMinorVersions() {
            assertThat(latestRelease.compare("1.1.1", "1.2.1")).isLessThan(0)
            assertThat(latestRelease.compare("1.1", "1.2")).isLessThan(0)
        }

        @Test
        fun differentMajorVersions() {
            assertThat(latestRelease.compare("1.1.1", "2.1.1")).isLessThan(0)
            assertThat(latestRelease.compare("1.1", "2.1")).isLessThan(0)
            assertThat(latestRelease.compare("1", "2")).isLessThan(0)
        }

        @Test
        fun differentNumberOfParts() {
            assertThat(latestRelease.compare("1.1", "1.1.1")).isLessThan(0)
            assertThat(latestRelease.compare("1", "1.1")).isLessThan(0)
        }
    }

    @Nested
    inner class HyphenRangeTest {
        /**
         * 1.2.3 - 2.3.4 := >=1.2.3 <=2.3.4
         */
        @Test
        fun inclusiveSet() {
            val hyphenRange: UpgradeVersion.HyphenRange = UpgradeVersion.HyphenRange
                    .build("1.2.3 - 2.3.4")
                    .getValue()

            assertThat(hyphenRange.isValid("1.2.2")).isFalse()
            assertThat(hyphenRange.isValid("1.2.3")).isTrue()
            assertThat(hyphenRange.isValid("2.3.4")).isTrue()
            assertThat(hyphenRange.isValid("2.3.5")).isFalse()
        }

        /**
         * 1.2 - 2 := >=1.2.0 <=2.0.0
         */
        @Test
        fun partialVersion() {
            val hyphenRange: UpgradeVersion.HyphenRange = UpgradeVersion.HyphenRange
                    .build("1.2 - 2")
                    .getValue()

            assertThat(hyphenRange.isValid("1.1.9")).isFalse()
            assertThat(hyphenRange.isValid("1.2.0")).isTrue()
            assertThat(hyphenRange.isValid("2.0.0")).isTrue()
            assertThat(hyphenRange.isValid("2.0.1")).isFalse()
        }
    }

    @Nested
    inner class XRangeTest {
        @Test
        fun pattern() {
            assertThat(UpgradeVersion.XRange.build("*").isValid).isTrue()
            assertThat(UpgradeVersion.XRange.build("*.0.0").isValid).isFalse()
            assertThat(UpgradeVersion.XRange.build("1.x").isValid).isTrue()
            assertThat(UpgradeVersion.XRange.build("1.x.0").isValid).isFalse()
            assertThat(UpgradeVersion.XRange.build("1.1.X").isValid).isTrue()
            assertThat(UpgradeVersion.XRange.build("a").isValid).isFalse()
        }

        /**
         * X := >=0.0.0
         */
        @Test
        fun anyVersion() {
            val xRange: UpgradeVersion.XRange = UpgradeVersion.XRange
                    .build("X")
                    .getValue()

            assertThat(xRange.isValid("0.0.0")).isTrue()
        }

        /**
         * 1.* := >=1.0.0 <2.0.0-0
         */
        @Test
        fun matchingMajorVersion() {
            val xRange: UpgradeVersion.XRange = UpgradeVersion.XRange
                    .build("1.*")
                    .getValue()

            assertThat(xRange.isValid("1.0.0")).isTrue()
            assertThat(xRange.isValid("1.9.9")).isTrue()
            assertThat(xRange.isValid("2.0.0")).isFalse()
        }

        /**
         * 1.2.X := >=1.2.0 <1.3.1
         */
        @Test
        fun matchingMajorAndMinorVersions() {
            val xRange: UpgradeVersion.XRange = UpgradeVersion.XRange
                    .build("1.2.X")
                    .getValue()

            assertThat(xRange.isValid("1.2.0")).isTrue()
            assertThat(xRange.isValid("1.3.0")).isFalse()
        }
    }

    @Nested
    inner class TildeRangeTest {
        /**
         * ~1.2.3 := >=1.2.3 <1.(2+1).0 := >=1.2.3 <1.3.0
         */
        @Test
        fun updatePatch() {
            val tildeRange: UpgradeVersion.TildeRange = UpgradeVersion.TildeRange
                    .build("~1.2.3")
                    .getValue()

            assertThat(tildeRange.isValid("1.2.3")).isTrue()
            assertThat(tildeRange.isValid("1.2.4")).isTrue()
            assertThat(tildeRange.isValid("1.3.0")).isFalse()
        }

        /**
         * ~1.2 := >=1.2.0 <1.(2+1).0 := >=1.2.0 <1.3.0-0 (Same as 1.2.x)
         */
        @Test
        fun updatePatchImplicitZeroPatch() {
            val tildeRange: UpgradeVersion.TildeRange = UpgradeVersion.TildeRange
                    .build("~1.2")
                    .getValue()

            assertThat(tildeRange.isValid("1.2.0")).isTrue()
            assertThat(tildeRange.isValid("1.2.4")).isTrue()
            assertThat(tildeRange.isValid("1.3.0")).isFalse()
        }

        /**
         * ~1 := >=1.0.0 <(1+1).0.0 := >=1.0.0 <2.0.0-0 (Same as 1.x)
         */
        @Test
        fun updateMajor() {
            val tildeRange: UpgradeVersion.TildeRange = UpgradeVersion.TildeRange
                    .build("~1")
                    .getValue()

            assertThat(tildeRange.isValid("1.0.1")).isTrue()
            assertThat(tildeRange.isValid("1.9.9")).isTrue()
            assertThat(tildeRange.isValid("2.0.0")).isFalse()
        }
    }

    @Nested
    inner class CaretRangeTest {
        /**
         * ^1.2.3 := >=1.2.3 <2.0.0
         */
        @Test
        fun updateMinorAndPatch() {
            val caretRange: UpgradeVersion.CaretRange = UpgradeVersion.CaretRange
                    .build("^1.2.3")
                    .getValue()

            assertThat(caretRange.isValid("1.2.3")).isTrue()
            assertThat(caretRange.isValid("1.2.4")).isTrue()
            assertThat(caretRange.isValid("1.9.0")).isTrue()
            assertThat(caretRange.isValid("2.0.0")).isFalse()
        }

        /**
         * ^0.2.3 := >=0.2.3 <0.3.0
         */
        @Test
        fun updatePatch() {
            val caretRange: UpgradeVersion.CaretRange = UpgradeVersion.CaretRange
                    .build("^0.2.3")
                    .getValue()

            assertThat(caretRange.isValid("0.2.3")).isTrue()
            assertThat(caretRange.isValid("0.2.4")).isTrue()
            assertThat(caretRange.isValid("0.3.0")).isFalse()
        }

        @Test
        fun updateNothing() {
            val caretRange: UpgradeVersion.CaretRange = UpgradeVersion.CaretRange
                    .build("^0.0.3")
                    .getValue()

            assertThat(caretRange.isValid("0.0.3")).isFalse()
            assertThat(caretRange.isValid("0.0.4")).isFalse()
        }

        /**
         * ^1.x := >=1.0.0 <2.0.0
         */
        @Test
        fun desugarMinorWildcard() {
            val caretRange: UpgradeVersion.CaretRange = UpgradeVersion.CaretRange
                    .build("^1.x")
                    .getValue()

            assertThat(caretRange.isValid("1.0.0")).isTrue()
            assertThat(caretRange.isValid("1.0.1")).isTrue()
            assertThat(caretRange.isValid("1.1.0")).isTrue()
            assertThat(caretRange.isValid("2.0.0")).isFalse()
        }

        /**
         * ^0.0.x := >=0.0.0 <0.1.0
         */
        @Test
        fun desugarPatchWildcard() {
            val caretRange: UpgradeVersion.CaretRange = UpgradeVersion.CaretRange
                    .build("^0.0.x")
                    .getValue()

            assertThat(caretRange.isValid("0.0.0")).isTrue()
            assertThat(caretRange.isValid("0.0.1")).isTrue()
            assertThat(caretRange.isValid("0.1.0")).isFalse()
        }
    }
}
