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
package org.openrewrite.maven.utilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class MavenArtifactDownloaderTest {

    @Test
    void itDownloadsDependencies(@TempDir Path tempDir) {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenArtifactCache artifactCache = new LocalMavenArtifactCache(tempDir);
        MavenArtifactDownloader downloader = new MavenArtifactDownloader(
          artifactCache, null,  t -> ctx.getOnError().accept(t));
        ResolvedGroupArtifactVersion recipeGav = new ResolvedGroupArtifactVersion(
          "https://repo1.maven.org/maven2",
          "org.openrewrite.recipe",
          "rewrite-testing-frameworks",
          "1.6.0", null);

        MavenParser mavenParser = MavenParser.builder().build();
        Xml.Document parsed = mavenParser.parse(ctx,
          "" +
            "<project>" +
            "    <groupId>org.openrewrite</groupId>" +
            "    <artifactId>maven-downloader-test</artifactId>" +
            "    <version>1</version>" +
            "    <dependencies>" +
            "        <dependency>" +
            "            <groupId>" + recipeGav.getGroupId() + "</groupId>" +
            "            <artifactId>" + recipeGav.getArtifactId() + "</artifactId>" +
            "            <version>" + recipeGav.getVersion() + "</version>" +
            "        </dependency>" +
            (recipeGav.getGroupId().equals("org.openrewrite") && recipeGav.getArtifactId().equals("rewrite-java") ?
              "" +
                "        <dependency>" +
                "            <groupId>org.openrewrite</groupId>" +
                "            <artifactId>rewrite-java-17</artifactId>" +
                "            <version>" + recipeGav.getVersion() + "</version>" +
                "        </dependency>" : "") +
            "    </dependencies>" +
            "</project>"
        ).get(0);

        MavenResolutionResult mavenModel = parsed.getMarkers().findFirst(MavenResolutionResult.class)
          .orElseThrow(() -> new IllegalStateException("No MavenResolutionResult marker on newly parsed Maven AST"));

        assertTrue(!mavenModel.getDependencies().isEmpty());

        List<ResolvedDependency> runtimeDependencies = mavenModel.getDependencies().get(Scope.Runtime);

        List<Path> artifacts = runtimeDependencies
          .stream()
          .map(downloader::downloadArtifact)
          .collect(Collectors.toList());

        artifacts.forEach( artifact -> assertNotNull(artifact));

    }
}
