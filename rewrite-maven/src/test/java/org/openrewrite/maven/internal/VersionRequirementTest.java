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
package org.openrewrite.maven.internal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.ResolutionStrategy;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VersionRequirementTest {
    static Iterable<String> available() {
        return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
    }

    @Nested
    class NearestWins {

        @Test
        void rangeSet() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("[1,11)", ResolutionStrategy.NEAREST_WINS, 0).resolve(VersionRequirementTest::available))
              .isEqualTo("10");
        }

        @Test
        void multipleSoftRequirements() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("1", ResolutionStrategy.NEAREST_WINS, 1).addRequirement("2").resolve(VersionRequirementTest::available))
              .isEqualTo("1");
        }

        @Test
        void softRequirementThenHardRequirement() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("1", ResolutionStrategy.NEAREST_WINS, 1).addRequirement("[1,11]")
              .resolve(VersionRequirementTest::available))
              .isEqualTo("10");
        }

        @Test
        void hardRequirementThenSoftRequirement() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("[1,11]", ResolutionStrategy.NEAREST_WINS, 1).addRequirement("1")
              .resolve(VersionRequirementTest::available))
              .isEqualTo("10");
        }

        @Test
        void nearestRangeWins() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("[1,2]", ResolutionStrategy.NEAREST_WINS, 1).addRequirement("[9,10]")
              .resolve(VersionRequirementTest::available))
              .isEqualTo("2");
        }

        @Test
        void emptyUnboundedRange() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("(,)", ResolutionStrategy.NEAREST_WINS, 0).resolve(VersionRequirementTest::available))
              .isEqualTo("10");
        }
    }

    @Nested
    class NewestWins {

        @Test
        void rangeSet() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("[1,11)", ResolutionStrategy.NEWEST_WINS, 0).resolve(VersionRequirementTest::available))
              .isEqualTo("10");
        }

        @Test
        void multipleSpecificVersions() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("1", ResolutionStrategy.NEWEST_WINS, 0).addRequirement("2").resolve(VersionRequirementTest::available))
              .isEqualTo("2");
        }

        @Test
        void versionAndRange() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("1", ResolutionStrategy.NEWEST_WINS, 0).addRequirement("[1,11]")
              .resolve(VersionRequirementTest::available))
              .isEqualTo("10");
        }

        @Test
        void highestRangeWins() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("[1,2]", ResolutionStrategy.NEWEST_WINS, 0).addRequirement("[9,10]")
              .resolve(VersionRequirementTest::available))
              .isEqualTo("10");
        }

        @Test
        void emptyUnboundedRange() throws MavenDownloadingException {
            assertThat(VersionRequirement.fromVersion("(,)", ResolutionStrategy.NEWEST_WINS, 0).resolve(VersionRequirementTest::available))
              .isEqualTo("10");
        }
    }
}
