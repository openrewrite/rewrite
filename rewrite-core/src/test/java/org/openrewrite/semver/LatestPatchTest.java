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

class LatestPatchTest {
    private final LatestPatch latestPatch = new LatestPatch(null);
    private final LatestPatch latestMetadataPatch = new LatestPatch("-fred");

    @Test
    void isValidWhenCurrentIsNull() {
        assertThat(latestPatch.isValid(null, "1.0.0")).isTrue();
    }

    @Test
    void isValid() {
        assertThat(latestPatch.isValid("1.0.0", "1.0.0")).isTrue();
        assertThat(latestPatch.isValid("1.0.0", "1.0.0.1")).isTrue();
        assertThat(latestPatch.isValid("1.0.0", "1.0.1")).isTrue();
        assertThat(latestPatch.isValid("1.0", "1.0.1")).isTrue();
        assertThat(latestPatch.isValid("1.0.0", "1.1.0")).isFalse();
        assertThat(latestPatch.isValid("1.0.0", "2.0.0")).isFalse();
    }

    @Test
    void upgrade() {
        var upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.0"));
        assertThat(upgrade.isPresent()).isFalse();

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.11.0"));
        assertThat(upgrade.isPresent()).isFalse();
        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.9"));
        assertThat(upgrade.isPresent()).isFalse();

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.11"));
        assertThat(upgrade.isPresent()).isTrue();
        assertThat(upgrade.get()).isEqualTo("2.10.11");

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.10.3.23"));
        assertThat(upgrade.isPresent()).isFalse();

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.10.2.25"));
        assertThat(upgrade.isPresent()).isFalse();

        upgrade = latestPatch.upgrade("2.10.10.3.24", List.of("2.10.10.3.25"));
        assertThat(upgrade.isPresent()).isTrue();
        assertThat(upgrade.get()).isEqualTo("2.10.10.3.25");
    }

    @Test
    void compare() {
        assertThat(latestPatch.compare("1.0", "1.0.1", "1.0.2")).isLessThan(0);
        assertThat(latestPatch.compare("1.0", "1.0.0.1", "1.0.1")).isLessThan(0);
    }

    @Test
    void metadataValid() {
        assertThat(latestMetadataPatch.isValid("1.0.0-fred", "1.0.4-fred")).isTrue();
        assertThat(latestMetadataPatch.isValid("1.0-fred", "1.0.1-fred")).isTrue();
        assertThat(latestMetadataPatch.isValid("1.0.0-fred", "1.0.4-not-fred")).isFalse();
    }

    @Test
    void metadataUpgrade() {
        var upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.0-fred"));
        assertThat(upgrade.isPresent()).isFalse();

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.11.0-fred"));
        assertThat(upgrade.isPresent()).isFalse();
        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.9-fred"));
        assertThat(upgrade.isPresent()).isFalse();

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.11-fred"));
        assertThat(upgrade.isPresent()).isTrue();
        assertThat(upgrade.get()).isEqualTo("2.10.11-fred");

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.10.3.23-fred"));
        assertThat(upgrade.isPresent()).isFalse();

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.10.2.25-fred"));
        assertThat(upgrade.isPresent()).isFalse();

        upgrade = latestMetadataPatch.upgrade("2.10.10.3.24-fred", List.of("2.10.10.3.25-fred"));
        assertThat(upgrade.isPresent()).isTrue();
        assertThat(upgrade.get()).isEqualTo("2.10.10.3.25-fred");
    }

}
