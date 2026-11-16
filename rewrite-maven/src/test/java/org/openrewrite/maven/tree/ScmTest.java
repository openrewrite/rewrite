/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.SourceFile;
import org.openrewrite.marker.Marker;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.text.PlainText;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class ScmTest {

    @Language("xml")
    private static final String GRANDPARENT = """
      <project>
        <groupId>org.example</groupId>
        <artifactId>grandparent</artifactId>
        <version>1</version>
        <packaging>pom</packaging>
        <properties>
          <scm.host>git.example.org</scm.host>
        </properties>
        <scm>
          <connection>scm:git:https://${scm.host}/group/subgroup/repo.git</connection>
          <developerConnection>scm:git:git@${scm.host}:group/subgroup/repo.git</developerConnection>
          <url>https://${scm.host}/group/subgroup/grandparent</url>
          <tag>HEAD</tag>
        </scm>
      </project>
      """;

    @Language("xml")
    private static final String PARENT = """
      <project>
        <parent>
          <groupId>org.example</groupId>
          <artifactId>grandparent</artifactId>
          <version>1</version>
          <relativePath>../pom.xml</relativePath>
        </parent>
        <artifactId>parent</artifactId>
        <packaging>pom</packaging>
        <scm>
          <url>https://${scm.host}/group/subgroup/${project.artifactId}</url>
        </scm>
      </project>
      """;

    @Language("xml")
    private static final String CHILD = """
      <project>
        <parent>
          <groupId>org.example</groupId>
          <artifactId>parent</artifactId>
          <version>1</version>
          <relativePath>../pom.xml</relativePath>
        </parent>
        <artifactId>child</artifactId>
        <scm>
          <tag>v${project.version}</tag>
        </scm>
        <dependencies>
          <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
          </dependency>
          <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>29.0-jre</version>
          </dependency>
          <dependency>
            <groupId>aopalliance</groupId>
            <artifactId>aopalliance</artifactId>
            <version>1.0</version>
          </dependency>
        </dependencies>
      </project>
      """;

    @Test
    void pomKeepsTheScmItDeclares() {
        Pom pom = parsePom("""
          <project>
            <groupId>org.example</groupId>
            <artifactId>app</artifactId>
            <version>1</version>
            <scm child.scm.url.inherit.append.path="false">
              <connection>
                scm:git:https://${scm.host}/example/app.git
              </connection>
              <developerConnection>scm:git:git@github.com:example/app.git</developerConnection>
              <url>https://github.com/example/app</url>
              <tag>v1</tag>
            </scm>
          </project>
          """);

        assertThat(pom.getScm()).isEqualTo(new Scm(
          "scm:git:https://${scm.host}/example/app.git",
          "scm:git:git@github.com:example/app.git",
          "https://github.com/example/app",
          "v1"));
    }

    @Test
    void pomWithoutScmHasNone() {
        assertThat(parsePom("""
          <project>
            <groupId>org.example</groupId>
            <artifactId>app</artifactId>
            <version>1</version>
          </project>
          """).getScm()).isNull();
        assertThat(parsePom("""
          <project>
            <groupId>org.example</groupId>
            <artifactId>app</artifactId>
            <version>1</version>
            <scm>
              <url> </url>
            </scm>
          </project>
          """).getScm()).isNull();
    }

    @Test
    void eachValueComesFromTheNearestPomThatDeclaresIt() {
        MavenResolutionResult child = parseReactor();

        assertThat(child.getPom().getRequested().getScm())
          .as("what the POM itself declares")
          .isEqualTo(new Scm(null, null, null, "v${project.version}"));
        assertThat(child.getPom().getScm())
          .as("inherited values are the repository's, without the child's artifactId appended")
          .isEqualTo(new Scm(
            "scm:git:https://git.example.org/group/subgroup/repo.git",
            "scm:git:git@git.example.org:group/subgroup/repo.git",
            // declared by the parent, whose placeholder is the child's once inherited
            "https://git.example.org/group/subgroup/child",
            "v1"));
    }

    @Test
    void resolvedDependencyHasTheEffectiveScmOfItsPom() {
        MavenResolutionResult child = parseReactor();

        assertThat(compileDependency(child, "junit").getScm())
          .as("declared by the dependency's own POM")
          .isEqualTo(new Scm(
            "scm:git:git://github.com/junit-team/junit4.git",
            "scm:git:git@github.com:junit-team/junit4.git",
            "https://github.com/junit-team/junit4",
            "r4.13.2"));
        assertThat(compileDependency(child, "guava").getScm())
          .as("declared by guava-parent only, and inherited without '/guava' appended")
          .isEqualTo(new Scm(
            "scm:git:https://github.com/google/guava.git",
            "scm:git:git@github.com:google/guava.git",
            "https://github.com/google/guava",
            null));
        assertThat(compileDependency(child, "aopalliance").getScm())
          .as("no POM in its parent chain declares one")
          .isNull();
    }

    @Test
    void existingConstructorStillWorks() {
        ResolvedDependency dependency = new ResolvedDependency(null,
          new ResolvedGroupArtifactVersion(null, "org.example", "app", "1", null),
          Dependency.builder().gav(new GroupArtifactVersion("org.example", "app", "1")).build(),
          emptyList(), emptyList(), null, null, null, 0, null);

        assertThat(dependency.getScm()).isNull();
        assertThat(dependency.withScm(new Scm(null, null, "https://github.com/example/app", null)).getScm())
          .extracting(Scm::getUrl)
          .isEqualTo("https://github.com/example/app");
    }

    @Test
    void serializationRoundTrip() throws IOException {
        MavenResolutionResult child = parseReactor();
        ObjectMapper mapper = new RecipeSerializer().getMapper();

        MavenResolutionResult read = mapper.readValue(mapper.writeValueAsBytes(child), MavenResolutionResult.class);

        assertScmSurvived(read, child);
    }

    @Test
    void readsModelSerializedBeforeScm() throws IOException {
        MavenResolutionResult child = parseReactor();
        ObjectMapper olderWriter = new RecipeSerializer().getMapper()
          .addMixIn(Pom.class, WithoutScm.class)
          .addMixIn(ResolvedPom.class, WithoutScm.class)
          .addMixIn(ResolvedDependency.class, WithoutScm.class);
        byte[] older = olderWriter.writeValueAsBytes(child);
        assertThat(olderWriter.readTree(older).findValues("scm")).isEmpty();

        MavenResolutionResult read = new RecipeSerializer().getMapper().readValue(older, MavenResolutionResult.class);

        assertThat(read.getPom().getScm()).isNull();
        assertThat(read.getPom().getRequested().getScm()).isNull();
        assertThat(read.getDependencies().get(Scope.Compile))
          .hasSameSizeAs(child.getDependencies().get(Scope.Compile))
          .allSatisfy(dependency -> assertThat(dependency.getScm()).isNull());
        assertThat(compileDependency(read, "junit").getVersion()).isEqualTo("4.13.2");
    }

    /**
     * {@link MavenResolutionResult} and {@code GradleProject} have no codec of their own: what they hold crosses
     * as the JSON of a marker's value, which is what this marker does with the same model.
     */
    @Test
    void rpcRoundTrip() throws IOException {
        MavenResolutionResult child = parseReactor();
        ResolvedModel model = new ResolvedModel(randomId(), child.getPom(), child.getDependencies().get(Scope.Compile));
        PlainText sent = PlainText.builder().sourcePath(Paths.get("pom.xml")).text("").build();
        sent = sent.withMarkers(sent.getMarkers().add(model));

        // two peers over pipes, so the marker crosses as the JSON a real peer would be sent
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        RewriteRpc server = peer(new PipedInputStream(clientOut), serverOut);
        RewriteRpc client = peer(new PipedInputStream(serverOut), clientOut);
        try {
            server.print(sent);
            PlainText received = client.getObject(sent.getId().toString(), PlainText.class.getName());
            ResolvedModel read = received.getMarkers().findFirst(ResolvedModel.class).orElseThrow();

            assertThat(read).isNotSameAs(model);
            assertThat(read.getPom().getRequested().getScm())
              .isNotNull()
              .isEqualTo(child.getPom().getRequested().getScm());
            assertThat(read.getPom().getScm())
              .isNotNull()
              .isEqualTo(child.getPom().getScm());
            assertThat(read.getDependencies())
              .hasSameSizeAs(model.getDependencies())
              .extracting(ResolvedDependency::getScm)
              .containsExactlyElementsOf(model.getDependencies().stream().map(ResolvedDependency::getScm).toList())
              .filteredOn(Objects::nonNull)
              .hasSizeGreaterThanOrEqualTo(2);
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Value
    @With
    static class ResolvedModel implements Marker {
        UUID id;
        ResolvedPom pom;
        List<ResolvedDependency> dependencies;
    }

    private static void assertScmSurvived(MavenResolutionResult read, MavenResolutionResult written) {
        assertThat(read.getPom().getRequested().getScm())
          .isNotNull()
          .isEqualTo(written.getPom().getRequested().getScm());
        assertThat(read.getPom().getScm())
          .isNotNull()
          .isEqualTo(written.getPom().getScm());
        for (String artifactId : new String[]{"junit", "guava"}) {
            assertThat(compileDependency(read, artifactId).getScm())
              .isNotNull()
              .isEqualTo(compileDependency(written, artifactId).getScm());
        }
        assertThat(compileDependency(read, "aopalliance").getScm()).isNull();
    }

    private static ResolvedDependency compileDependency(MavenResolutionResult result, String artifactId) {
        return result.getDependencies().get(Scope.Compile).stream()
          .filter(dependency -> dependency.getArtifactId().equals(artifactId))
          .findFirst()
          .orElseThrow();
    }

    private static Pom parsePom(@Language("xml") String pom) {
        return RawPom.parse(new ByteArrayInputStream(pom.getBytes()), null).toPom(null, null);
    }

    private static MavenResolutionResult parseReactor() {
        List<SourceFile> poms = MavenParser.builder().build()
          .parseInputs(List.of(
            org.openrewrite.Parser.Input.fromString(Path.of("pom.xml"), GRANDPARENT),
            org.openrewrite.Parser.Input.fromString(Path.of("parent/pom.xml"), PARENT),
            org.openrewrite.Parser.Input.fromString(Path.of("parent/child/pom.xml"), CHILD)
          ), null, new InMemoryExecutionContext(t -> {
              throw new AssertionError(t);
          }))
          .toList();
        return poms.stream()
          .filter(pom -> pom.getSourcePath().equals(Path.of("parent/child/pom.xml")))
          .findFirst()
          .flatMap(pom -> pom.getMarkers().findFirst(MavenResolutionResult.class))
          .orElseThrow();
    }

    private static RewriteRpc peer(InputStream in, OutputStream out) {
        JsonMessageFormatter formatter = new JsonMessageFormatter(new ParameterNamesModule());
        return new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(formatter, in, out), new JsonMessageFormatter()), new RecipeMarketplace());
    }

    @JsonIgnoreProperties("scm")
    private abstract static class WithoutScm {
    }
}
