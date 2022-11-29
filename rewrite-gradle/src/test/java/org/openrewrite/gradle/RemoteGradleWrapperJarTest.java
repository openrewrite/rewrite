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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.Checksum;
import org.openrewrite.gradle.util.DistributionInfos;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.remote.Remote;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteGradleWrapperJarTest {

    @ParameterizedTest
    @CsvSource(value = {"7.4.2,29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda,575098db54a998ff1c6770b352c3b16766c09848bee7555dab09afc34e8cf590",
                        "6.9.3,dcf350b8ae1aa192fc299aed6efc77b43825d4fedb224c94118ae7faf5fb035d,e996d452d2645e70c01c11143ca2d3742734a28da2bf61f25c82bdc288c9e637",
                        "7.5-rc-1,8ba57a37e1e0b8c415e4d91718d51035223aa73131cf719a50c95a2a88269eb2,91a239400bb638f36a1795d8fdf7939d532cdc7d794d1119b7261aac158b1e60"})
    void gradleWrapper(String version, String distChecksum, String jarChecksum) {
        Remote remoteArchive = new GradleWrapper(version,
          new DistributionInfos("https://services.gradle.org/distributions/gradle-" + version + "-bin.zip",
            Checksum.fromHex("sha256", distChecksum),
            Checksum.fromHex("sha256", jarChecksum))).asRemote();

        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender())).isNotEmpty();
    }

}
