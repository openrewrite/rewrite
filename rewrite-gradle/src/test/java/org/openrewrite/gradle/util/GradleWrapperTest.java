/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Validated;

import static org.assertj.core.api.Assertions.assertThat;

public class GradleWrapperTest {

    @Test
    void validateDistributionTypeBin() {
        Validated validated = GradleWrapper.validate(new InMemoryExecutionContext(), "7.x", "bin", null);
        GradleWrapper gw = validated.getValue();
        assertThat(validated.isValid()).isTrue();
        assertThat(gw).isNotNull();
        assertThat(gw.getVersion()).startsWith("7.");
        assertThat(gw.getDistributionInfos().getDownloadUrl()).endsWith("-bin.zip");
    }

    @Test
    void validateDistributionTypeAll() {
        Validated validated = GradleWrapper.validate(new InMemoryExecutionContext(), "6.x", "all", null);
        GradleWrapper gw = validated.getValue();
        assertThat(validated.isValid()).isTrue();
        assertThat(gw).isNotNull();
        assertThat(gw.getVersion()).startsWith("6.");
        assertThat(gw.getDistributionInfos().getDownloadUrl()).endsWith("-all.zip");
    }

    @Test
    void validateWithDistributionNull() {
        Validated validated = GradleWrapper.validate(new InMemoryExecutionContext(), "7.x", null, null);
        GradleWrapper gw = validated.getValue();
        assertThat(validated.isValid()).isTrue();
        assertThat(gw).isNotNull();
        assertThat(gw.getVersion()).startsWith("7.");
        assertThat(gw.getDistributionInfos().getDownloadUrl()).endsWith("-bin.zip");
    }
}
