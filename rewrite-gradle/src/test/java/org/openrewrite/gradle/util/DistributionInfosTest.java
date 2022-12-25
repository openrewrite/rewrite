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
import org.openrewrite.Checksum;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DistributionInfosTest {

    @Test
    void fetch() throws IOException {
        HttpSender httpSender = new HttpUrlConnectionSender(Duration.ofSeconds(30), Duration.ofMinutes(1));
        GradleWrapper.GradleVersion gradleVersion = new GradleWrapper.GradleVersion(
          "7.6",
          "https://services.gradle.org/distributions/gradle-7.6-bin.zip",
          "https://services.gradle.org/distributions/gradle-7.6-bin.zip.sha256",
          "https://services.gradle.org/distributions/gradle-7.6-wrapper.jar.sha256"
          );
        DistributionInfos infos = DistributionInfos.fetch(httpSender, GradleWrapper.DistributionType.Bin, gradleVersion);

        assertThat(infos).isEqualTo(new DistributionInfos(
          "https://services.gradle.org/distributions/gradle-7.6-bin.zip",
          Checksum.fromHex("SHA-256", "7ba68c54029790ab444b39d7e293d3236b2632631fb5f2e012bb28b4ff669e4b"),
          Checksum.fromHex("SHA-256", "c5a643cf80162e665cc228f7b16f343fef868e47d3a4836f62e18b7e17ac018a")));
    }
}
