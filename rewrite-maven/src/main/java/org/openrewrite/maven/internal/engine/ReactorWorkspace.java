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

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.io.DefaultModelReader;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.io.DefaultModelWriter;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.io.ModelReader;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.openrewrite.maven.engine.shaded.org.apache.maven.repository.internal.MavenWorkspaceReader;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.Artifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.WorkspaceRepository;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * Serves the reactor (other poms in the same build) to Maven's model builder and descriptor reader, so an in-project
 * parent/BOM/module is read from its printed XML instead of the repository. Implements the resolver's
 * {@link org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.WorkspaceReader} +
 * {@link MavenWorkspaceReader} (artifact/model lookup) and the model-builder's {@link WorkspaceModelResolver}
 * (raw-model lookup for parents).
 * <p>
 * Matching ports {@code MavenPomDownloader}'s project-pom short-circuit exactly: tier 1 exact resolved GAV, tier 2 raw
 * {@code g:a} with raw-version equality, tier 3 the property-merged version (the {@code ${revision}}
 * raw-GAV-before-interpolation behavior). {@code <relativePath>} resolution is deliberately absent — Maven owns it now,
 * so a {@code .m2}-relative parent is never mistaken for a reactor member (only in-reactor poms are ever matched).
 * <p>
 * A monotonic {@link #epoch()} keys workspace identity ({@link #getRepository()}); {@link #bumpEpoch()} is called on
 * {@code UpdateMavenModel} marker replacement so the engine's GAV-keyed caches invalidate and stale printed models are
 * never served. Slice B wires the epoch into those cache keys; this only exposes it.
 */
public class ReactorWorkspace implements MavenWorkspaceReader, WorkspaceModelResolver {

    private final Map<Path, Pom> projectPoms;
    private final Map<Pom, Path> pathByPom;
    private final Map<GroupArtifactVersion, Pom> projectPomsByGav;

    /** Printed-XML bytes for a project pom path; {@code null} for synthetic {@code Pom.builder()} graphs — the seam
     *  slice B's {@code PomToModelConverter} fills. */
    private final Function<Path, byte @Nullable []> pomXmlSource;

    private final ModelReader modelReader = new DefaultModelReader();
    private final Map<Path, Model> modelCache = new HashMap<>();

    private volatile int epoch;

    public ReactorWorkspace(Map<Path, Pom> projectPoms, Function<Path, byte @Nullable []> pomXmlSource) {
        this.projectPoms = projectPoms;
        this.pomXmlSource = pomXmlSource;
        this.pathByPom = new IdentityHashMap<>();
        for (Map.Entry<Path, Pom> entry : projectPoms.entrySet()) {
            pathByPom.put(entry.getValue(), entry.getKey());
        }
        this.projectPomsByGav = projectPomsByGav(projectPoms);
    }

    @Override
    public WorkspaceRepository getRepository() {
        // The key carries the epoch, so aether treats a post-bumpEpoch workspace as a distinct one and never serves a
        // stale printed model from its own cache. Slice B additionally keys the engine's model/descriptor caches on epoch().
        return new WorkspaceRepository("default", "reactor-" + epoch);
    }

    /** The current reactor epoch; incorporated into the workspace identity so GAV-keyed caches can key on it. */
    public int epoch() {
        return epoch;
    }

    /** Whether this GAV resolves to a reactor pom (three-tier match); the model cache keys such GAVs on {@link #epoch()}. */
    public boolean isReactorMember(@Nullable String groupId, @Nullable String artifactId, @Nullable String version) {
        return match(groupId, artifactId, version) != null;
    }

    /** Increment the epoch (on marker replacement) and drop cached models so re-resolution re-reads printed bytes. */
    public synchronized void bumpEpoch() {
        epoch++;
        modelCache.clear();
    }

    @Override
    public @Nullable File findArtifact(Artifact artifact) {
        // Reactor modules aren't built to jars during parsing; only the pom itself is served, and only when it is a
        // real file on disk (in-memory printed documents resolve through findModel/resolveRawModel instead).
        if (!"pom".equals(artifact.getExtension())) {
            return null;
        }
        Pom pom = match(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        if (pom == null) {
            return null;
        }
        Path path = pathByPom.get(pom);
        if (path == null) {
            return null;
        }
        File file = path.toFile();
        return file.isFile() ? file : null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
        Set<String> versions = new LinkedHashSet<>();
        for (Map.Entry<GroupArtifactVersion, Pom> entry : projectPomsByGav.entrySet()) {
            GroupArtifactVersion gav = entry.getKey();
            if (Objects.equals(gav.getGroupId(), artifact.getGroupId()) &&
                Objects.equals(gav.getArtifactId(), artifact.getArtifactId()) &&
                gav.getVersion() != null) {
                versions.add(gav.getVersion());
            }
        }
        return new ArrayList<>(versions);
    }

    @Override
    public @Nullable Model findModel(Artifact artifact) {
        Pom pom = match(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        if (pom == null && !Objects.equals(artifact.getVersion(), artifact.getBaseVersion())) {
            pom = match(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
        }
        return pom == null ? null : readModel(pom);
    }

    @Override
    public @Nullable Model resolveRawModel(String groupId, String artifactId, String versionId) {
        Pom pom = match(groupId, artifactId, versionId);
        return pom == null ? null : readModel(pom);
    }

    @Override
    public @Nullable Model resolveEffectiveModel(String groupId, String artifactId, String versionId) {
        // The workspace serves raw models only; effective models are the model builder's job. Returning null defers
        // reactor-BOM effective resolution to slice B rather than fabricating a half-built model here.
        return null;
    }

    /**
     * Three-tier match, faithful to {@code MavenPomDownloader.download}: exact resolved GAV, then raw {@code g:a} with
     * raw-version equality, then the property-merged version.
     */
    private @Nullable Pom match(@Nullable String groupId, @Nullable String artifactId, @Nullable String version) {
        Pom exact = projectPomsByGav.get(new GroupArtifactVersion(groupId, artifactId, version));
        if (exact != null) {
            return exact;
        }
        for (Pom pom : projectPoms.values()) {
            if (!Objects.equals(pom.getGroupId(), groupId) || !Objects.equals(pom.getArtifactId(), artifactId)) {
                continue;
            }
            if (Objects.equals(pom.getVersion(), version)) {
                return pom;
            }
            if (version != null) {
                Map<String, String> mergedProperties = mergeProperties(getAncestryWithinProject(pom));
                String versionWithReplacements =
                        ResolvedPom.placeholderHelper.replacePlaceholders(version, mergedProperties::get);
                if (Objects.equals(pom.getVersion(), versionWithReplacements)) {
                    return pom;
                }
            }
        }
        return null;
    }

    private synchronized @Nullable Model readModel(Pom pom) {
        Path path = pathByPom.get(pom);
        if (path == null) {
            return null;
        }
        if (modelCache.containsKey(path)) {
            Model cached = modelCache.get(path);
            return cached == null ? null : cached.clone();
        }
        byte[] bytes = pomXmlSource.apply(path);
        Model model = null;
        try {
            if (bytes != null) {
                try (InputStream in = new ByteArrayInputStream(bytes)) {
                    model = modelReader.read(in, Collections.emptyMap());
                }
                // The model builder resolves a workspace parent as a FileModelSource(model.getPomFile()), so the
                // printed bytes need a real file behind them; the model cache makes this once per epoch.
                model.setPomFile(materialize(bytes));
            } else {
                // Synthetic Pom.builder() graph (rewrite-gradle) with no backing XML: convert to a raw Model and back
                // its pomFile with printed bytes so a workspace-parent read still gets a real FileModelSource.
                model = new PomToModelConverter().convert(pom);
                model.setPomFile(materialize(printModel(model)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // Cache the pristine model but hand out a clone: the model builder mutates the raw model it receives, which
        // would otherwise corrupt the cached copy and make a re-resolution fall through to the repository.
        modelCache.put(path, model);
        return model == null ? null : model.clone();
    }

    private static File materialize(byte[] bytes) throws IOException {
        File file = Files.createTempFile("rewrite-reactor-", ".pom.xml").toFile();
        file.deleteOnExit();
        Files.write(file.toPath(), bytes);
        return file;
    }

    private static byte[] printModel(Model model) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new DefaultModelWriter().write(out, Collections.emptyMap(), model);
        return out.toByteArray();
    }

    private Map<GroupArtifactVersion, Pom> projectPomsByGav(Map<Path, Pom> projectPoms) {
        Map<GroupArtifactVersion, Pom> result = new HashMap<>();
        for (Pom projectPom : projectPoms.values()) {
            Map<String, String> mergedProperties = mergeProperties(getAncestryWithinProject(projectPom));
            result.put(new GroupArtifactVersion(
                    ResolvedPom.placeholderHelper.replacePlaceholders(projectPom.getGroupId(), mergedProperties::get),
                    ResolvedPom.placeholderHelper.replacePlaceholders(projectPom.getArtifactId(), mergedProperties::get),
                    ResolvedPom.placeholderHelper.replacePlaceholders(projectPom.getVersion(), mergedProperties::get)
            ), projectPom);
        }
        return result;
    }

    private Map<String, String> mergeProperties(List<Pom> pomAncestry) {
        Map<String, String> mergedProperties = new HashMap<>();
        for (Pom pom : pomAncestry) {
            for (Map.Entry<String, String> property : pom.getProperties().entrySet()) {
                mergedProperties.putIfAbsent(property.getKey(), Objects.toString(property.getValue(), ""));
            }
        }
        return mergedProperties;
    }

    private List<Pom> getAncestryWithinProject(Pom projectPom) {
        Pom parentPom = getParentWithinProject(projectPom);
        if (parentPom == null) {
            return Collections.singletonList(projectPom);
        }
        List<Pom> ancestry = new ArrayList<>();
        ancestry.add(projectPom);
        ancestry.addAll(getAncestryWithinProject(parentPom));
        return ancestry;
    }

    private @Nullable Pom getParentWithinProject(Pom projectPom) {
        Parent parent = projectPom.getParent();
        if (parent == null || projectPom.getSourcePath() == null) {
            return null;
        }
        String relativePath = parent.getRelativePath();
        // An explicit empty <relativePath/> means "do not look for the parent locally".
        if (relativePath != null && relativePath.isEmpty()) {
            return null;
        }
        if (relativePath == null) {
            relativePath = "../pom.xml";
        }
        Path resolvedRelativePath = Paths.get(relativePath);
        if (!relativePath.endsWith(".xml")) {
            resolvedRelativePath = resolvedRelativePath.resolve("pom.xml");
        }
        Path parentPath = projectPom.getSourcePath().resolve("..").resolve(resolvedRelativePath).normalize();
        Pom parentPom = projectPoms.get(parentPath);
        return parentPom != null &&
               parentPom.getGav().getGroupId().equals(parent.getGav().getGroupId()) &&
               parentPom.getGav().getArtifactId().equals(parent.getGav().getArtifactId()) ? parentPom : null;
    }
}
