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
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.DistributionManagement;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Relocation;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelSource;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.resolution.UnresolvableModelException;
import org.openrewrite.maven.engine.shaded.org.apache.maven.repository.internal.ArtifactDescriptorUtils;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.Artifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.DefaultArtifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.RemoteRepository;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ArtifactDescriptorPolicy;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ArtifactDescriptorPolicyRequest;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * The engine's {@code ArtifactDescriptorReader} — the one seam the collector reads a pom through — implemented over
 * Phase 2's {@link EngineEffectivePom} so a transitive dependency's effective model is built by the exact same machinery
 * that built the root's (the {@link EngineModelBuilderFactory} model builder, property overlay, reactor, and pom-bytes
 * cache). Stateless: the run's inputs arrive on the session's config property as a {@link CollectContext}.
 * <p>
 * The control flow mirrors Maven 3.9's {@code DefaultArtifactDescriptorReader.loadPom} exactly, including its tolerance
 * split (DESIGN §0 "fail where Maven fails, tolerate where Maven tolerates"):
 * <ul>
 *   <li>the dependency's own pom missing → {@code IGNORE_MISSING} returns an empty descriptor (childless node);</li>
 *   <li>an <em>unresolvable parent/BOM</em> during model building → always fails the read (Maven never tolerates it,
 *       {@code DefaultArtifactDescriptorReader:287-290});</li>
 *   <li>any other model-building failure (invalid model, cycle) → {@code IGNORE_INVALID} returns an empty descriptor;</li>
 *   <li>{@code <distributionManagement><relocation>} is followed with a g:a:baseVersion cycle guard.</li>
 * </ul>
 * The {@link CollectContext#getDescriptorFailures()} record lets {@link EngineDependencyCollector} fail direct
 * dependencies whose descriptor was tolerated here while warning on the transitive ones.
 */
class EngineDescriptorReader implements ArtifactDescriptorReader {

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(RepositorySystemSession session,
                                                           ArtifactDescriptorRequest request)
            throws ArtifactDescriptorException {
        CollectContext cc = CollectContext.from(session);
        cc.getDescriptorReads().incrementAndGet();
        ArtifactDescriptorResult result = new ArtifactDescriptorResult(request);
        List<MavenRepository> repositories = toMavenRepositories(request.getRepositories(), cc);
        CacheBridge bridge = new CacheBridge(cc.getSystem(), session, cc.getPomCache(), cc.getMaterializeDir());
        EngineEffectivePom effectivePom =
                new EngineEffectivePom(cc.getSystem(), session, repositories, cc.getMaterializeDir());

        Set<String> visited = new LinkedHashSet<>();
        Artifact artifact = request.getArtifact();
        while (true) {
            String g = artifact.getGroupId(), a = artifact.getArtifactId(), v = artifact.getVersion();
            if (!visited.add(g + ':' + a + ':' + artifact.getBaseVersion())) {
                return failInvalid(cc, session, request, result, artifact,
                        "Artifact relocations form a cycle: " + visited);
            }

            byte[] xml = pomXml(cc, bridge, repositories, g, a, v);
            if (xml == null) {
                // The pom itself is missing. IGNORE_MISSING tolerates it (empty descriptor); STRICT fails the read.
                cc.getDescriptorFailures().put(new GroupArtifactVersion(g, a, v),
                        new CollectContext.DescriptorFailure(true, "not found", responses(bridge, g, a, v)));
                if ((policy(session, request, artifact) & ArtifactDescriptorPolicy.IGNORE_MISSING) != 0) {
                    return result;
                }
                result.addException(new UnresolvableModelException("Could not find " + g + ':' + a + ':' + v, g, a, v));
                throw new ArtifactDescriptorException(result);
            }

            EngineModelBuildingOutcome outcome = effectivePom.build(
                    xml, syntheticRequested(g, a, v), cc.getSettings(), cc.getReactor(), cc.getCtx());
            cc.getServedBy().putAll(bridge.servedBy());
            cc.getServedBy().putAll(outcome.getServedBy());
            if (!outcome.isSuccess()) {
                Throwable failure = requireNonNull(outcome.getFailure());
                // An unresolvable parent/BOM is never tolerated (mirrors DefaultArtifactDescriptorReader:287-290); a
                // model that merely fails validation (a cycle, a bad packaging) is tolerated by IGNORE_INVALID.
                if (failure instanceof MavenDownloadingException) {
                    result.addException((Exception) failure);
                    throw new ArtifactDescriptorException(result, failure.getMessage(), failure);
                }
                return failInvalid(cc, session, request, result, artifact,
                        failure.getMessage() == null ? failure.toString() : failure.getMessage());
            }

            Model model = requireNonNull(outcome.getResult()).getEffectiveModel();
            Relocation relocation = relocation(model);
            if (relocation == null) {
                populateResult(session, result, model);
                return result;
            }
            result.addRelocation(artifact);
            artifact = relocate(artifact, relocation);
            result.setArtifact(artifact);
        }
    }

    /** Reactor members are served from their printed XML; everything else is fetched (and cached) through {@link CacheBridge}. */
    private byte @Nullable [] pomXml(CollectContext cc, CacheBridge bridge, List<MavenRepository> repositories,
                                     String g, String a, String v) {
        byte[] reactorXml = cc.getReactor().reactorPomXml(g, a, v);
        if (reactorXml != null) {
            return MavenEngineResolution.ensureModelVersion(reactorXml);
        }
        try {
            return MavenEngineResolution.ensureModelVersion(readAll(bridge.resolvePom(g, a, v, repositories)));
        } catch (UnresolvableModelException e) {
            return null;
        }
    }

    private ArtifactDescriptorResult failInvalid(CollectContext cc, RepositorySystemSession session,
                                                 ArtifactDescriptorRequest request, ArtifactDescriptorResult result,
                                                 Artifact artifact, String reason) throws ArtifactDescriptorException {
        cc.getDescriptorFailures().put(gav(artifact),
                new CollectContext.DescriptorFailure(false, reason, Collections.emptyMap()));
        if ((policy(session, request, artifact) & ArtifactDescriptorPolicy.IGNORE_INVALID) != 0) {
            return result;
        }
        result.addException(new IllegalStateException(reason));
        throw new ArtifactDescriptorException(result, reason);
    }

    // --- Model -> ArtifactDescriptorResult (a port of ArtifactDescriptorReaderDelegate.populateResult) --------------

    private void populateResult(RepositorySystemSession session, ArtifactDescriptorResult result, Model model) {
        ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();
        for (org.openrewrite.maven.engine.shaded.org.apache.maven.model.Repository r : model.getRepositories()) {
            result.addRepository(ArtifactDescriptorUtils.toRemoteRepository(r));
        }
        for (org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency d : model.getDependencies()) {
            result.addDependency(DependencyConversions.toAether(d, stereotypes));
        }
        if (model.getDependencyManagement() != null) {
            for (org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency d :
                    model.getDependencyManagement().getDependencies()) {
                result.addManagedDependency(DependencyConversions.toAether(d, stereotypes));
            }
        }
    }

    // --- helpers ---------------------------------------------------------------------------------------------------

    private static @Nullable Relocation relocation(Model model) {
        DistributionManagement distMgmt = model.getDistributionManagement();
        return distMgmt == null ? null : distMgmt.getRelocation();
    }

    // A relocation's absent coordinates default to the original's (Maven's RelocatedArtifact semantics), preserving the
    // original type/classifier/extension.
    private static Artifact relocate(Artifact artifact, Relocation relocation) {
        String g = relocation.getGroupId() != null ? relocation.getGroupId() : artifact.getGroupId();
        String a = relocation.getArtifactId() != null ? relocation.getArtifactId() : artifact.getArtifactId();
        String v = relocation.getVersion() != null ? relocation.getVersion() : artifact.getVersion();
        return new DefaultArtifact(g, a, artifact.getClassifier(), artifact.getExtension(), v);
    }

    private static int policy(RepositorySystemSession session, ArtifactDescriptorRequest request, Artifact artifact) {
        ArtifactDescriptorPolicy policy = session.getArtifactDescriptorPolicy();
        return policy == null ? ArtifactDescriptorPolicy.STRICT :
                policy.getPolicy(session, new ArtifactDescriptorPolicyRequest(artifact, request.getRequestContext()));
    }

    private static Map<MavenRepository, String> responses(CacheBridge bridge, String g, String a, String v) {
        Map<MavenRepository, String> r = bridge.responsesFor(new GroupArtifactVersion(g, a, v));
        return r == null ? Collections.emptyMap() : r;
    }

    private static GroupArtifactVersion gav(Artifact artifact) {
        return new GroupArtifactVersion(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    private static Pom syntheticRequested(String g, String a, String v) {
        return Pom.builder().gav(new ResolvedGroupArtifactVersion(null, g, a, v, null)).build();
    }

    private static byte[] readAll(ModelSource source) {
        try (InputStream in = source.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<MavenRepository> toMavenRepositories(List<RemoteRepository> remotes, CollectContext cc) {
        Map<String, MavenRepository> byUri = new HashMap<>();
        for (MavenRepository repo : cc.getRequestRepositories()) {
            byUri.put(repo.getUri(), repo);
        }
        List<MavenRepository> out = new ArrayList<>(remotes.size());
        for (RemoteRepository remote : remotes) {
            // Reuse the caller's MavenRepository (carries auth) when the URL matches; otherwise a descriptor-discovered
            // repo, which carries no credentials on the hermetic path.
            MavenRepository known = byUri.get(remote.getUrl());
            out.add(known != null ? known : new MavenRepository(remote.getId(), remote.getUrl(),
                    enabled(remote, false), enabled(remote, true), false, null, null, null, null));
        }
        return out;
    }

    private static String enabled(RemoteRepository remote, boolean snapshot) {
        return String.valueOf(remote.getPolicy(snapshot).isEnabled());
    }

    private static <T> T requireNonNull(@Nullable T value) {
        if (value == null) {
            throw new IllegalStateException("Expected a non-null value");
        }
        return value;
    }
}
