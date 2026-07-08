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
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Parent;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Repository;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.RepositoryPolicy;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelSource;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.resolution.ModelResolver;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.resolution.UnresolvableModelException;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * The model builder's {@link ModelResolver} over {@link CacheBridge}: it turns a parent/BOM GAV into a
 * {@link ModelSource} of real pom XML. It accumulates descriptor-declared {@code <repositories>} the way Maven's
 * {@code DefaultModelResolver} does — recessively (an existing id is not overwritten unless {@code replace}), so the
 * root/parent poms' own repositories become resolvable for their descendants. Parent and BOM-import versions honor
 * ranges via {@link CacheBridge#resolveHighestMatchingVersion}.
 * <p>
 * Per Maven's contract {@link #newCopy()} returns an independent resolver seeded with the current repository set, so a
 * recursive BOM build starts from the aggregated repositories without polluting the caller's list. The shared
 * {@link CacheBridge} keeps one pom-bytes cache and one {@code gav → repository} map across the whole build.
 */
public class EngineModelResolver implements ModelResolver {

    private final CacheBridge bridge;
    private final List<MavenRepository> repositories;

    public EngineModelResolver(CacheBridge bridge, List<MavenRepository> repositories) {
        this.bridge = bridge;
        this.repositories = new ArrayList<>(repositories);
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        return bridge.resolvePom(groupId, artifactId, version, repositories);
    }

    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        String version = bridge.resolveHighestMatchingVersion(
                parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), repositories);
        parent.setVersion(version);
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), version);
    }

    @Override
    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        String version = bridge.resolveHighestMatchingVersion(
                dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), repositories);
        dependency.setVersion(version);
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), version);
    }

    @Override
    public void addRepository(Repository repository) {
        addRepository(repository, false);
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
        for (int i = 0; i < repositories.size(); i++) {
            if (repositories.get(i).getId() != null && repositories.get(i).getId().equals(repository.getId())) {
                if (replace) {
                    repositories.set(i, toMavenRepository(repository));
                }
                return; // recessive: an already-known id is not re-added
            }
        }
        repositories.add(toMavenRepository(repository));
    }

    @Override
    public ModelResolver newCopy() {
        return new EngineModelResolver(bridge, repositories);
    }

    private static MavenRepository toMavenRepository(Repository repository) {
        return new MavenRepository(repository.getId(), repository.getUrl(),
                enabled(repository.getReleases()), enabled(repository.getSnapshots()),
                false, null, null, null, null);
    }

    private static @Nullable String enabled(@Nullable RepositoryPolicy policy) {
        return policy == null ? null : policy.getEnabled();
    }
}
