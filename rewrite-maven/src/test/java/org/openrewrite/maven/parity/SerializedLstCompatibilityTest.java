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
package org.openrewrite.maven.parity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.marker.Marker;
import org.openrewrite.maven.tree.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@link MavenResolutionResult} wire format by deserializing committed payloads from
 * {@code parity/serialized-lsts/} with the same ObjectMapper configuration LST serialization
 * uses ({@link ObjectMappers#propertyBasedMapper}). A change to identity info, type info, or
 * field shape on any {@code org.openrewrite.maven.tree} type shows up here first.
 */
class SerializedLstCompatibilityTest {
    // Field-based visibility is the LST wire format convention (same as RocksdbMavenPomCache and
    // the external TreeSerializer); default getter detection would emit derived properties like
    // MavenResolutionResult.getProjectPoms() whose Path map keys cannot even deserialize.
    private static final ObjectMapper MAPPER = ObjectMappers.propertyBasedMapper(null)
      .setVisibility(new ObjectMapper().getSerializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY));

    private static String payload(String name) throws Exception {
        try (InputStream is = requireNonNull(
          SerializedLstCompatibilityTest.class.getResourceAsStream("/parity/serialized-lsts/" + name),
          name + " payload not committed")) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void currentPayloadDeserializesWithKeyFields() throws Exception {
        MavenResolutionResult mrr = (MavenResolutionResult) MAPPER.readValue(payload("current.json"), Marker.class);

        assertThat(mrr.getPom().getGav().toString()).contains("org.parity:bom-import-app:1");
        assertThat(mrr.getPom().getPackaging()).isEqualTo("jar");
        assertThat(mrr.getDependencies().keySet())
          .containsExactly(Scope.Compile, Scope.Runtime, Scope.Test, Scope.Provided);

        List<ResolvedDependency> compile = mrr.getDependencies().get(Scope.Compile);
        assertThat(compile).extracting(d -> d.getGav().getArtifactId())
          .containsExactly("managed-a", "defined-c", "managed-b");
        assertThat(compile).extracting(ResolvedDependency::getDepth).containsExactly(0, 0, 1);

        assertThat(mrr.getActiveProfiles()).isEmpty();
        assertThat(mrr.getUserProperties()).containsKey("parity.repo.url");
    }

    @Test
    void currentPayloadPreservesIdentityTopology() throws Exception {
        MavenResolutionResult mrr = (MavenResolutionResult) MAPPER.readValue(payload("current.json"), Marker.class);

        // Flat scope list and nested graph reference-share through @ref
        List<ResolvedDependency> compile = mrr.getDependencies().get(Scope.Compile);
        ResolvedDependency managedA = compile.get(0);
        ResolvedDependency managedB = compile.get(2);
        assertThat(managedA.getDependencies()).hasSize(1);
        assertThat(managedA.getDependencies().get(0)).isSameAs(managedB);

        // ManagedDependency carries @ref, so DM threading survives a round-trip of one document
        List<ManagedDependency> declarations = mrr.getPom().getRequested().getDependencyManagement();
        assertThat(declarations).hasSize(2);
        for (ManagedDependency declaration : declarations) {
            assertThat(mrr.getResolvedManagedDependency(declaration))
              .as("DM declaration %s must still be found by reference after deserialization", declaration.getArtifactId())
              .isNotNull();
        }
        ManagedDependency imported = declarations.stream()
          .filter(d -> d instanceof ManagedDependency.Imported).findFirst().orElseThrow();
        assertThat(requireNonNull(mrr.getResolvedManagedDependency(imported)).getRequestedBom()).isSameAs(imported);
    }

    /**
     * {@link Dependency} deliberately has no {@code @JsonIdentityInfo}: requested-dependency
     * reference threading does not survive serialization, and adding identity info would break
     * old readers. Pins the absence so an accidental annotation change is caught.
     */
    @Test
    void dependencyRequestedThreadingDoesNotSurviveRoundTrip() throws Exception {
        MavenResolutionResult mrr = (MavenResolutionResult) MAPPER.readValue(payload("current.json"), Marker.class);
        for (Dependency requested : mrr.getPom().getRequestedDependencies()) {
            assertThat(mrr.getResolvedDependency(requested)).isNull();
        }
    }

    @Test
    void currentPayloadWireFormatShape() throws Exception {
        JsonNode root = MAPPER.readTree(payload("current.json"));

        assertThat(root.path("@c").asText()).isEqualTo("org.openrewrite.maven.tree.MavenResolutionResult");
        assertThat(root.path("@ref").isInt()).isTrue();
        assertThat(root.path("pom").path("@ref").isInt()).isTrue();

        String json = payload("current.json");
        assertThat(json).contains("\"org.openrewrite.maven.tree.ManagedDependency$Imported\"");
        assertThat(json).contains("\"org.openrewrite.maven.tree.ManagedDependency$Defined\"");
        // Scope names are the dependencies map keys
        for (String scope : new String[]{"Compile", "Runtime", "Test", "Provided"}) {
            assertThat(root.path("dependencies").has(scope)).isTrue();
        }
        // Credentials and managedReference are WRITE_ONLY and must never be written
        assertThat(json).doesNotContain("\"username\"", "\"password\"", "\"managedReference\"");
    }

    /**
     * Payload produced by released {@code org.openrewrite:rewrite-maven:8.41.1} on an isolated
     * classpath (throwaway Gradle project) from the same {@code bom-import-single} fixture. It
     * predates {@code dependencyManagementSorted} (exercising the lazy re-sort path) and
     * {@code @JsonIdentityInfo} on the GAV types (exercising
     * {@code BackwardCompatibleObjectIdModule}'s objects-without-{@code @ref} bridging).
     */
    @Test
    void oldReleasePayloadDeserializesWithCurrentCode() throws Exception {
        MavenResolutionResult mrr = (MavenResolutionResult) MAPPER.readValue(payload("old-8.41.1.json"), Marker.class);

        assertThat(mrr.getPom().getGav().toString()).contains("org.parity:bom-import-app:1");
        // 8.41.1 resolved scopes in {Compile, Test, Runtime, Provided} order; readers of old
        // LSTs must not assume today's {Compile, Runtime, Test, Provided} insertion order
        assertThat(mrr.getDependencies().keySet())
          .containsExactly(Scope.Compile, Scope.Test, Scope.Runtime, Scope.Provided);

        List<ResolvedDependency> compile = mrr.getDependencies().get(Scope.Compile);
        assertThat(compile).extracting(d -> d.getGav().getArtifactId())
          .containsExactly("managed-a", "defined-c", "managed-b");
        assertThat(compile.get(0).getDependencies().get(0)).isSameAs(compile.get(2));

        // ManagedDependency @ref threading and the lazy-sort managed lookup work on the old shape
        for (ManagedDependency declaration : mrr.getPom().getRequested().getDependencyManagement()) {
            assertThat(mrr.getResolvedManagedDependency(declaration)).isNotNull();
        }
        assertThat(mrr.getPom().getManagedVersion("org.parity", "managed-b", null, null)).isEqualTo("2.0");
        assertThat(mrr.getActiveProfiles()).isEmpty();
        assertThat(mrr.getUserProperties()).isEmpty();
    }

    /**
     * Regenerates {@code current.json} from the {@code bom-import-single} fixture into
     * {@code build/parity-capture/}; copy it to {@code src/test/resources/parity/serialized-lsts/}
     * after reviewing the diff. Fixture paths are replaced with a stable URI so the payload is
     * machine-independent.
     */
    @Disabled("Run manually to regenerate the committed payload")
    @Test
    void regenerateCurrentPayload() throws Exception {
        ParityHarness.Resolution resolution = ParityHarness.resolve("bom-import-single");
        String json = MAPPER.writerFor(Marker.class).withDefaultPrettyPrinter()
          .writeValueAsString(resolution.getMarker());

        Path fixtureDir = Path.of(requireNonNull(getClass().getResource("/parity/fixtures/bom-import-single")).toURI());
        json = json.replace(fixtureDir.toUri().toString(), "file:///parity/fixture/");
        // Jackson serializes the relative sourcePath as a URI resolved against user.dir
        json = json.replace(Path.of("").toAbsolutePath().toUri().toString(), "file:///parity/project/");

        Path out = Path.of("build/parity-capture/current.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, json);
        System.out.println("Payload written to " + out.toAbsolutePath());
    }
}
