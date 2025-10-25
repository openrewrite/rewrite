/*
 * Copyright 2023 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class RemoteVisitorTest {

    @Test
    void visitsMarkers() throws Exception {
        AtomicBoolean markersVisited = new AtomicBoolean();
        RemoteVisitor<Integer> remoteVisitor = new RemoteVisitor<>() {
            @Override
            public Markers visitMarkers(@Nullable Markers markers, Integer integer) {
                Markers m = super.visitMarkers(markers, integer);
                markersVisited.set(true);
                return m;
            }
        };
        remoteVisitor.visitRemote(new RemoteArchive(
          Tree.randomId(),
          Path.of("foo/bar/gradle-wrapper.jar"),
          Markers.EMPTY,
          new URI("https://gradle.gradle/gradle-wrapper.jar"),
          null,
          false,
          null,
          "Gradle wrapper jar",
          emptyList(),
          null
        ), 0);

        assertThat(markersVisited.get()).isTrue();
    }

}
