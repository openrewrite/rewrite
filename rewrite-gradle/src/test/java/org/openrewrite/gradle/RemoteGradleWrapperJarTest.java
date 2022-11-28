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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.util.DistributionInfos;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.remote.Remote;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteGradleWrapperJarTest {

    @Test
    void gradleWrapper() {
        Remote remoteArchive = new GradleWrapper("7.4.2",
          new DistributionInfos("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip",
            "29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda")).asRemote();
        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender()))
                .isNotEmpty();
    }

    @Test
    void gradleWrapper693() {
        Remote remoteArchive = new GradleWrapper("6.9.3",
          new DistributionInfos("https://services.gradle.org/distributions/gradle-6.9.3-bin.zip",
            "dcf350b8ae1aa192fc299aed6efc77b43825d4fedb224c94118ae7faf5fb035d")).asRemote();
        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender()))
                .isNotEmpty();
    }
    @Test
    void gradleWrapper75rc1() {
        Remote remoteArchive = new GradleWrapper("7.5-rc-1",
          new DistributionInfos("https://services.gradle.org/distributions/gradle-7.5-rc-1-bin.zip",
            "8ba57a37e1e0b8c415e4d91718d51035223aa73131cf719a50c95a2a88269eb2")).asRemote();
        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender()))
                .isNotEmpty();
    }
}
