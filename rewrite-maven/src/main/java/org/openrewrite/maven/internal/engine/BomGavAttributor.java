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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.DependencyManagement;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.tree.GroupArtifactClassifierType;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ManagedDependency;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reconstructs {@code bomGav}/{@code requestedBom} for dependency-management entries that arrived through a
 * {@code scope=import} BOM. Maven 3.9 {@link org.openrewrite.maven.engine.shaded.org.apache.maven.model.InputLocation}
 * attributes an imported entry to the model that <em>defined</em> the version, which for a multi-level BOM (a BOM that
 * inherits its own management from a parent BOM) is the parent BOM, not the directly-imported one rewrite reports
 * (SPIKE-RESULTS §4b). So instead of trusting the location, this resolves each directly-imported BOM's effective
 * dependency-management (through the same cached {@link EngineEffectivePom} pipeline) and stamps membership in import
 * declaration order — first import wins ties, matching Maven's first-wins merge.
 */
public class BomGavAttributor {

    private final EngineEffectivePom service;
    private final EffectiveSettings settings;
    private final ReactorWorkspace reactor;
    private final ExecutionContext ctx;
    private final MavenPomCache pomCache;

    public BomGavAttributor(EngineEffectivePom service, EffectiveSettings settings, ReactorWorkspace reactor,
                            ExecutionContext ctx, MavenPomCache pomCache) {
        this.service = service;
        this.settings = settings;
        this.reactor = reactor;
        this.ctx = ctx;
        this.pomCache = pomCache;
    }

    public Attribution attribute(Pom requested, Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy) {
        List<Membership> memberships = new ArrayList<>();
        // BOM imports declared in the requested pom AND inherited from its ancestry (a parent's import contributes its
        // managed entries to the child's effective DM — see indirectBomImportedFromParent). Import declaration order,
        // child-first, matches Maven's first-wins merge.
        for (ManagedDependency md : inheritedImports(requested, servedBy)) {
            ResolvedGroupArtifactVersion bomKey = findServedKey(md, servedBy);
            if (bomKey == null) {
                continue;
            }
            Set<GroupArtifactClassifierType> members = effectiveManagementKeys(bomKey);
            if (members != null) {
                ResolvedGroupArtifactVersion bomGav = new ResolvedGroupArtifactVersion(
                        bomKey.getRepository(), bomKey.getGroupId(), bomKey.getArtifactId(), bomKey.getVersion(), null);
                memberships.add(new Membership(md, bomGav, members));
            }
        }
        return new Attribution(memberships);
    }

    private List<ManagedDependency> inheritedImports(Pom requested,
                                                     Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy) {
        List<ManagedDependency> imports = new ArrayList<>();
        Set<GroupArtifactVersion> visited = new HashSet<>();
        for (Pom pom = requested; pom != null && visited.add(pom.getGav().asGroupArtifactVersion()); pom = parentOf(pom, servedBy)) {
            for (ManagedDependency md : pom.getDependencyManagement()) {
                if (md instanceof ManagedDependency.Imported) {
                    imports.add(md);
                }
            }
        }
        return imports;
    }

    private @Nullable Pom parentOf(Pom pom, Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy) {
        Parent parent = pom.getParent();
        if (parent == null) {
            return null;
        }
        String groupId = parent.getGroupId();
        String artifactId = parent.getArtifactId();
        String version = parent.getVersion();
        Pom fromReactor = reactor.findReactorPom(groupId, artifactId, version);
        if (fromReactor != null) {
            return fromReactor;
        }
        for (ResolvedGroupArtifactVersion key : servedBy.keySet()) {
            if (Objects.equals(groupId, key.getGroupId()) && Objects.equals(artifactId, key.getArtifactId()) &&
                    (version == null || Objects.equals(version, key.getVersion()))) {
                try {
                    Optional<Pom> parentPom = pomCache.getPom(key);
                    //noinspection OptionalAssignedToNull
                    if (parentPom != null && parentPom.isPresent()) {
                        return parentPom.get();
                    }
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }
        return null;
    }

    private @Nullable Set<GroupArtifactClassifierType> effectiveManagementKeys(ResolvedGroupArtifactVersion bomKey) {
        try {
            Optional<byte[]> bytes = pomCache.getPomBytes(bomKey);
            Optional<Pom> bomPom = pomCache.getPom(bomKey);
            //noinspection OptionalAssignedToNull
            if (bytes == null || !bytes.isPresent() || bomPom == null || !bomPom.isPresent()) {
                return null;
            }
            EngineModelBuildingOutcome outcome = service.build(bytes.get(), bomPom.get(), settings, reactor, ctx);
            if (!outcome.isSuccess()) {
                return null;
            }
            return managementKeys(outcome.getResult().getEffectiveModel());
        } catch (MavenDownloadingException e) {
            return null;
        }
    }

    private static Set<GroupArtifactClassifierType> managementKeys(Model model) {
        Set<GroupArtifactClassifierType> keys = new LinkedHashSet<>();
        DependencyManagement dm = model.getDependencyManagement();
        if (dm != null) {
            for (Dependency d : dm.getDependencies()) {
                keys.add(key(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType()));
            }
        }
        return keys;
    }

    static GroupArtifactClassifierType key(@Nullable String groupId, String artifactId,
                                           @Nullable String classifier, @Nullable String type) {
        return new GroupArtifactClassifierType(groupId, artifactId, classifier, type == null ? "jar" : type);
    }

    private static @Nullable ResolvedGroupArtifactVersion findServedKey(
            ManagedDependency imported, Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy) {
        ResolvedGroupArtifactVersion fallback = null;
        for (ResolvedGroupArtifactVersion key : servedBy.keySet()) {
            if (imported.getGroupId().equals(key.getGroupId()) && imported.getArtifactId().equals(key.getArtifactId())) {
                if (imported.getVersion() != null && imported.getVersion().equals(key.getVersion())) {
                    return key;
                }
                fallback = key;
            }
        }
        return fallback;
    }

    /** Per-import membership: the directly-imported BOM's identity plus the GACT keys its effective management covers. */
    @Value
    private static class Membership {
        ManagedDependency imported;
        ResolvedGroupArtifactVersion bomGav;
        Set<GroupArtifactClassifierType> members;
    }

    @Value
    public static class Match {
        ManagedDependency requestedBom;
        ResolvedGroupArtifactVersion bomGav;
    }

    /** The membership index; {@link #match} returns the first import (declaration order) that manages a key. */
    public static class Attribution {
        private final List<Membership> memberships;

        private Attribution(List<Membership> memberships) {
            this.memberships = memberships;
        }

        public @Nullable Match match(GroupArtifactClassifierType key) {
            for (Membership membership : memberships) {
                if (membership.getMembers().contains(key)) {
                    return new Match(membership.getImported(), membership.getBomGav());
                }
            }
            return null;
        }
    }
}
