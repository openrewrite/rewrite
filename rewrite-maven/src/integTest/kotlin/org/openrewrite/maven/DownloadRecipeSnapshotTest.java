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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DownloadRecipeSnapshotTest {

    @Test
    void downloadRecipe(@TempDir Path temp) {
        MavenParser parser = MavenParser.builder().build();

        MavenExecutionContextView ctx = new MavenExecutionContextView(new InMemoryExecutionContext(Throwable::printStackTrace));
        ctx.setPinnedSnapshotVersions(Collections.singleton(new GroupArtifactVersion("org.openrewrite.recipe",
                "rewrite-testing-frameworks", "1.7.0-SNAPSHOT",
                "20210614.172805-1")));

        Maven parsed = parser.parse(ctx, "" +
                "<project>" +
                "    <groupId>io.moderne</groupId>" +
                "    <artifactId>recipe-downloader</artifactId>" +
                "    <version>1</version>" +
                "    <dependencies>" +
                "        <dependency>" +
                "            <groupId>org.openrewrite.recipe</groupId>" +
                "            <artifactId>rewrite-testing-frameworks</artifactId>" +
                "            <version>1.7.0-SNAPSHOT</version>" +
                "        </dependency>" +
                "    </dependencies>" +
                "    <repositories>" +
                "        <repository>" +
                "            <id>ossrh-snapshots</id>" +
                "            <url>https://oss.sonatype.org/content/repositories/snapshots</url>" +
                "            <snapshots>" +
                "                <enabled>true</enabled>" +
                "            </snapshots>" +
                "            <releases>" +
                "                <enabled>false</enabled>" +
                "            </releases>" +
                "        </repository>" +
                "    </repositories>" +
                "</project>"
        ).get(0);

        // set up an artifact cache that is shared by every recipe download request
        MavenArtifactCache artifactCache = new LocalMavenArtifactCache(temp);
        MavenArtifactDownloader artifactDownloader = new MavenArtifactDownloader(artifactCache,
                null, Throwable::printStackTrace);

        List<Path> artifacts = parsed.getModel().getDependencies(Scope.Runtime)
                .stream()
                .map(artifactDownloader::downloadArtifact)
                .collect(Collectors.toList());

        for (Path artifact : artifacts) {
            System.out.println(artifact.toString());
        }
    }
}
