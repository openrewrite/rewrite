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
package org.openrewrite.remote;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;
import org.openrewrite.test.MockHttpSender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class RemoteArchiveTest {

    @ParameterizedTest
    @ValueSource(strings = {"7.4.2", "7.5-rc-1", "7.6"})
    void gradleWrapper(String version) throws Exception {
        URL distributionUrl = requireNonNull(RemoteArchiveTest.class.getClassLoader().getResource("gradle-" + version + "-bin.zip"));

        RemoteArchive remoteArchive = Remote
          .builder(
            Paths.get("gradle/wrapper/gradle-wrapper.jar"),
            distributionUrl.toURI()
          )
          .build("gradle-[^\\/]+\\/(?:.*\\/)+gradle-wrapper-(?!shared).*\\.jar");

        byte[] actual = readAll(remoteArchive.getInputStream(new MockHttpSender(distributionUrl::openStream)));
        assertThat(actual).hasSizeGreaterThan(50_000);
    }

    private byte[] readAll(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int count;
            byte[] buf = new byte[4096];
            while ((count = is.read(buf)) != -1) {
                baos.write(buf, 0, count);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
