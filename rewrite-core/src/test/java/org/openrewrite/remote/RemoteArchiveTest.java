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

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;

import java.net.URI;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteArchiveTest {

    @Test
    void gradleWrapper() {
        RemoteArchive remoteArchive = Remote.builder(new Quark(Tree.randomId(), Paths.get("gradle/wrapper/gradle-wrapper.jar"),
                Markers.EMPTY, null, null), URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip")
        ).build(Paths.get("gradle-7.4.2/lib/gradle-wrapper-7.4.2.jar"));
        assertThat(remoteArchive.getInputStream(new HttpUrlConnectionSender()))
                .isNotEmpty();
    }
}
