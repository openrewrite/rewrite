/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scope;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMavenArtifactCacheTest {

    LocalMavenArtifactCache cache;

    @BeforeEach
    void setup() {

    }

    @Test
    void saveFileWhenInputStringExists(@TempDir Path tempDir) {
        cache = new LocalMavenArtifactCache(tempDir);
        Path output = cache.putArtifact(findDependency(), new ByteArrayInputStream("hi".getBytes()), Throwable::printStackTrace);
        assertThat(output).exists();
    }

    @Test
    void dontCreateFileWhenInputStringNull(@TempDir Path tempDir) {
        cache = new LocalMavenArtifactCache(tempDir);
        Path output = cache.putArtifact(findDependency(), null, Throwable::printStackTrace);
        assertThat(output).isNull();
        assertThat(tempDir).isEmptyDirectory();
    }

    private static ResolvedDependency findDependency() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ResolvedGroupArtifactVersion recipeGav = new ResolvedGroupArtifactVersion(
          "https://repo1.maven.org/maven2",
          "org.openrewrite.recipe",
          "rewrite-testing-frameworks",
          "1.6.0", null);

        MavenParser mavenParser = MavenParser.builder().build();
        SourceFile parsed = mavenParser.parse(ctx,
          String.format(
            //language=xml
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>maven-downloader-test</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>%s</groupId>
                          <artifactId>%s</artifactId>
                          <version>%s</version>
                      </dependency>
                  </dependencies>
              </project>
              """.formatted(recipeGav.getGroupId(), recipeGav.getArtifactId(), recipeGav.getVersion()))
        ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"));

        MavenResolutionResult mavenModel = parsed.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
        assertThat(mavenModel.getDependencies()).isNotEmpty();
        List<ResolvedDependency> runtimeDependencies = mavenModel.getDependencies().get(Scope.Runtime);
        return runtimeDependencies.get(0);
    }
}
