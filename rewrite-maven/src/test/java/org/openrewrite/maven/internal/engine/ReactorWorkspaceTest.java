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
package org.openrewrite.maven.internal.engine;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.DefaultArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

class ReactorWorkspaceTest {

    private static Pom pom(String path, String groupId, String artifactId, String version,
                           Map<String, String> properties, Parent parent) {
        return Pom.builder()
                .sourcePath(Paths.get(path))
                .gav(new ResolvedGroupArtifactVersion(null, groupId, artifactId, version, null))
                .parent(parent)
                .properties(properties)
                .build();
    }

    private static byte[] xml(String groupId, String artifactId, String version) {
        return ("<project><modelVersion>4.0.0</modelVersion>" +
                "<groupId>" + groupId + "</groupId>" +
                "<artifactId>" + artifactId + "</artifactId>" +
                "<version>" + version + "</version></project>").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void resolveRawModelExactGav() {
        Path path = Paths.get("pom.xml");
        Pom app = pom("pom.xml", "com.example", "app", "1.0", emptyMap(), null);
        Function<Path, byte[]> source = p -> xml("com.example", "app", "1.0");

        ReactorWorkspace workspace = new ReactorWorkspace(singletonMap(path, app), source);

        Model model = workspace.resolveRawModel("com.example", "app", "1.0");
        assertNotNull(model);
        assertEquals("com.example", model.getGroupId());
        assertEquals("app", model.getArtifactId());
        assertEquals("1.0", model.getVersion());
    }

    @Test
    void revisionMatchesByBothInterpolatedAndRawVersion() {
        // A CI-friendly ${revision} module: raw version placeholder, revision resolved from the module's own properties.
        Path path = Paths.get("pom.xml");
        Pom app = pom("pom.xml", "com.example", "app", "${revision}", singletonMap("revision", "1.0"), null);
        Function<Path, byte[]> source = p -> xml("com.example", "app", "${revision}");

        ReactorWorkspace workspace = new ReactorWorkspace(singletonMap(path, app), source);

        // Tier 1: the interpolated GAV.
        assertNotNull(workspace.resolveRawModel("com.example", "app", "1.0"));
        // Tier 2: the raw ${revision} version (raw-GAV-before-interpolation) resolves to the same module.
        Model raw = workspace.resolveRawModel("com.example", "app", "${revision}");
        assertNotNull(raw);
        assertEquals("${revision}", raw.getVersion(), "the raw model keeps the un-interpolated version");
    }

    @Test
    void childRevisionResolvesFromInProjectParentAncestry() {
        // Flattened multi-module ${revision}: the property lives in the parent, the child inherits it within the reactor.
        Pom parent = pom("pom.xml", "com.example", "parent", "1.0", singletonMap("revision", "1.0"), null);
        Pom child = pom("child/pom.xml", "com.example", "child", "${revision}", emptyMap(),
                new Parent(new GroupArtifactVersion("com.example", "parent", "1.0"), null));

        Map<Path, Pom> poms = new LinkedHashMap<>();
        poms.put(Paths.get("pom.xml"), parent);
        poms.put(Paths.get("child/pom.xml"), child);
        Function<Path, byte[]> source = p -> p.equals(Paths.get("child/pom.xml")) ?
                xml("com.example", "child", "${revision}") : xml("com.example", "parent", "1.0");

        ReactorWorkspace workspace = new ReactorWorkspace(poms, source);

        assertNotNull(workspace.resolveRawModel("com.example", "child", "1.0"),
                "child version resolved from the in-project parent's revision property");
    }

    @Test
    void findModelAndFindVersionsByArtifact() {
        Path path = Paths.get("pom.xml");
        Pom app = pom("pom.xml", "com.example", "app", "1.0", emptyMap(), null);
        ReactorWorkspace workspace = new ReactorWorkspace(singletonMap(path, app), p -> xml("com.example", "app", "1.0"));

        Model model = workspace.findModel(new DefaultArtifact("com.example", "app", "pom", "1.0"));
        assertNotNull(model);
        assertEquals("app", model.getArtifactId());

        assertEquals(Collections.singletonList("1.0"),
                workspace.findVersions(new DefaultArtifact("com.example", "app", "pom", "1.0")));
    }

    @Test
    void unknownGavReturnsNull() {
        Pom app = pom("pom.xml", "com.example", "app", "1.0", emptyMap(), null);
        ReactorWorkspace workspace = new ReactorWorkspace(
                singletonMap(Paths.get("pom.xml"), app), p -> xml("com.example", "app", "1.0"));

        assertNull(workspace.resolveRawModel("com.example", "other", "1.0"));
        assertNull(workspace.findModel(new DefaultArtifact("com.example", "other", "pom", "1.0")));
    }

    @Test
    void syntheticPomWithoutXmlBytesReturnsNullModel() {
        // The seam slice B's PomToModelConverter fills: a project pom with no printed XML yields no model.
        Path path = Paths.get("pom.xml");
        Pom app = pom("pom.xml", "com.example", "app", "1.0", emptyMap(), null);
        ReactorWorkspace workspace = new ReactorWorkspace(singletonMap(path, app), p -> null);

        assertNull(workspace.resolveRawModel("com.example", "app", "1.0"));
    }

    @Test
    void epochIsMonotonicAndInvalidatesTheModelCache() {
        Path path = Paths.get("pom.xml");
        Pom app = pom("pom.xml", "com.example", "app", "1.0", emptyMap(), null);
        Map<Path, byte[]> bytes = new HashMap<>();
        bytes.put(path, xml("com.example", "app", "1.0"));

        ReactorWorkspace workspace = new ReactorWorkspace(singletonMap(path, app), bytes::get);

        assertEquals(0, workspace.epoch());
        assertEquals("reactor-0", workspace.getRepository().getKey());
        assertEquals("1.0", workspace.resolveRawModel("com.example", "app", "1.0").getVersion());

        // Mutate the printed document (UpdateMavenModel) without changing the GAV; the cached model stands until bump.
        bytes.put(path, xml("com.example", "app", "2.0"));
        assertEquals("1.0", workspace.resolveRawModel("com.example", "app", "1.0").getVersion(), "cached until bumpEpoch");

        workspace.bumpEpoch();
        assertEquals(1, workspace.epoch());
        assertEquals("reactor-1", workspace.getRepository().getKey());
        assertEquals("2.0", workspace.resolveRawModel("com.example", "app", "1.0").getVersion(), "re-read after bumpEpoch");

        int previous = workspace.epoch();
        for (int i = 0; i < 3; i++) {
            workspace.bumpEpoch();
            assertTrue(workspace.epoch() > previous, "epoch must be strictly increasing");
            previous = workspace.epoch();
        }
    }
}
