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
