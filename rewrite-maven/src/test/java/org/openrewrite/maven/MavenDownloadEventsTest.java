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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.*;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.table.MavenDownloadEvents;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.RewriteTest.toRecipe;

class MavenDownloadEventsTest implements RewriteTest {

    @Test
    void resolvableMavenMetadata() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeDependencyVersion("io.moderne", "moderne-cli", "latest.patch", null, null, null))
            .executionContext(MavenExecutionContextView.view(new InMemoryExecutionContext())
              .setRepositories(List.of(MavenRepository.builder().id("jenkins").uri("https://repo.jenkins-ci.org/public").build())))
            .recipeExecutionContext(new InMemoryExecutionContext())
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .dataTable(MavenDownloadEvents.Row.class, events ->
              assertThat(events.stream().map(MavenDownloadEvents.Row::getUrl).distinct())
                .containsExactlyInAnyOrder("https://repo.maven.apache.org/maven2/io/moderne/moderne-cli/maven-metadata.xml",
                  "https://repo.maven.apache.org/maven2/io/moderne/moderne-cli/3.34.5/moderne-cli-3.34.5.pom")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>io.moderne</groupId>
                    <artifactId>moderne-cli</artifactId>
                    <version>3.34.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>io.moderne</groupId>
                    <artifactId>moderne-cli</artifactId>
                    <version>3.34.5</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void unresolvableMavenMetadata() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeDependencyVersion("*", "*", "latest.patch", null, null, null))
            .executionContext(MavenExecutionContextView.view(new InMemoryExecutionContext())
              .setRepositories(List.of(MavenRepository.builder().id("jenkins").uri("https://repo.jenkins-ci.org/public").build())))
            .recipeExecutionContext(new InMemoryExecutionContext())
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .dataTable(MavenDownloadEvents.Row.class, events ->
              assertThat(events.stream().map(MavenDownloadEvents.Row::getUrl).distinct())
                .containsExactlyInAnyOrder("https://repo.maven.apache.org/maven2/org/jenkins-ci/plugins/credentials/maven-metadata.xml",
                  "https://repo.maven.apache.org/maven2/org/jenkins-ci/plugins/credentials/",
                  "https://repo.maven.apache.org/maven2/org/jenkins-ci/plugins/appio/maven-metadata.xml",
                  "https://repo.maven.apache.org/maven2/org/jenkins-ci/plugins/appio/",
                  "https://repo.maven.apache.org/maven2/org/jenkins-ci/plugins/credentials/2.3.0/credentials-2.3.0.pom",
                  "https://repo.maven.apache.org/maven2/org/jenkins-ci/plugins/credentials/2.3.0/credentials-2.3.0.jar",
                  "https://repo.maven.apache.org/maven2/org/jenkins-ci/plugins/appio/1.3/appio-1.3.pom",
                  "https://repo.maven.apache.org/maven2/org/jenkins-ci/plugins/appio/1.3/appio-1.3.jar")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>credentials</artifactId>
                    <version>2.3.0</version>
                  </dependency>
                  <dependency>
                    <groupId>org.jenkins-ci.plugins</groupId>
                    <artifactId>appio</artifactId>
                    <version>1.3</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.after(after -> {
                //There should be two errors (one for each failed metadata download)
                assertThat(after.split("Unable to download metadata")).hasSize(3);
                return after;
            })
          )
        );
    }

    @Test
    void unresolvableTransitiveDependencyDueToInvalidPom(@TempDir Path localRepository) throws IOException {
        // it's hard to simulate a transitive dependency failure since Maven Central validates
        // transitive dependency resolvability on publishing.
        //
        // this test creates a pom in a local repository that is resolvable at parse time because
        // it has no dependencies, but becomes unresolvable when the model is refreshed because it
        // is overwritten on disk with dependencies that don't exist.
        Path localPom = localRepository.resolve("com/bad/bad-artifact/1/bad-artifact-1.pom");
        assertThat(localPom.getParent().toFile().mkdirs()).isTrue();
        Files.writeString(localPom,
          //language=xml
          """
             <project>
               <groupId>com.bad</groupId>
               <artifactId>bad-artifact</artifactId>
               <version>1</version>
             </project>
            """
        );
        Path localJar = localRepository.resolve("com/bad/bad-artifact/1/bad-artifact-1.jar");
        Files.writeString(localJar, "dummy");

        MavenRepository mavenLocal = MavenRepository.builder().id("local").uri(localRepository.toUri().toString())
          .snapshots(false).knownToExist(true).build();

        rewriteRun(
          spec -> spec
            .recipe(updateModel())
            .executionContext(MavenExecutionContextView.view(new InMemoryExecutionContext())
              .setLocalRepository(mavenLocal)
            )
            .recipeExecutionContext(MavenExecutionContextView.view(new InMemoryExecutionContext())
              .setLocalRepository(mavenLocal)
              .setPomCache(new InMemoryMavenPomCache())
            )
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .dataTable(MavenDownloadEvents.Row.class, events -> assertThat(
              events.stream()
                .map(MavenDownloadEvents.Row::getUrl)
                .collect(Collectors.toSet())
            ).containsExactlyInAnyOrder(
              "https://repo.maven.apache.org/maven2/doesnotexist/doesnotexist/1/doesnotexist-1.pom",
              "https://repo.maven.apache.org/maven2/doesnotexist/doesnotexist/1/doesnotexist-1.jar",
              "https://repo.maven.apache.org/maven2/doesnotexist/another/1/another-1.jar",
              "https://repo.maven.apache.org/maven2/doesnotexist/another/1/another-1.pom"
            )),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.bad</groupId>
                    <artifactId>bad-artifact</artifactId>
                    <version>1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <!--~~(doesnotexist:doesnotexist:1 failed. Unable to download POM: doesnotexist:doesnotexist:1. Tried repositories:
              https://repo.maven.apache.org/maven2: HTTP 404)~~>--><!--~~(doesnotexist:another:1 failed. Unable to download POM: doesnotexist:another:1. Tried repositories:
              https://repo.maven.apache.org/maven2: HTTP 404)~~>--><dependency>
                    <groupId>com.bad</groupId>
                    <artifactId>bad-artifact</artifactId>
                    <version>1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.beforeRecipe(maven -> {
                // make the local pom bad before running the recipe
                Files.writeString(localPom,
                  //language=xml
                  """
                     <project>
                       <groupId>com.bad</groupId>
                       <artifactId>bad-artifact</artifactId>
                       <version>1</version>
                       <dependencies>
                         <dependency>
                           <groupId>doesnotexist</groupId>
                           <artifactId>doesnotexist</artifactId>
                           <version>1</version>
                         </dependency>
                         <dependency>
                           <groupId>doesnotexist</groupId>
                           <artifactId>another</artifactId>
                           <version>1</version>
                         </dependency>
                       </dependencies>
                     </project>
                    """
                );
            })
          )
        );
    }

    private Recipe updateModel() {
        return toRecipe(() -> new MavenIsoVisitor<>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenExecutionContextView.view(ctx).setDownloadEventsDataTable(new MavenDownloadEvents(toRecipe(MavenIsoVisitor::new)));
                if (document.getMarkers().findFirst(Markup.class).isEmpty()) {
                    maybeUpdateModel();
                }
                MavenExecutionContextView.view(ctx).setDownloadEventsDataTable(null);
                return document;
            }
        });
    }
}
