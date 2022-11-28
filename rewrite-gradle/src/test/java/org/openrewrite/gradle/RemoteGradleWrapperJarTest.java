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
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.remote.Remote;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteGradleWrapperJarTest {

    @Test
    void gradleWrapper() {
        Remote remoteArchive = new GradleWrapper("7.4.2", GradleWrapper.DistributionType.Bin).asRemote();
        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender()))
                .isNotEmpty();
    }

    @Test
    void gradleWrapper69() {
        Remote remoteArchive = new GradleWrapper("6.9.3", GradleWrapper.DistributionType.Bin).asRemote();
        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender()))
                .isNotEmpty();
    }
    @Test
    void gradleWrapper75() {
        Remote remoteArchive = new GradleWrapper("7.5-rc-1", GradleWrapper.DistributionType.Bin).asRemote();
        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender()))
                .isNotEmpty();
    }

    @Test
    void gradleWrapper76() {
        Remote remoteArchive = new GradleWrapper("7.6", GradleWrapper.DistributionType.Bin).asRemote();
        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender()))
                .isNotEmpty();
    }
}
