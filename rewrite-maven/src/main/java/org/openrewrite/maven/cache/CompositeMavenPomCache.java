/*
 * Copyright 2021 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.*;

import java.net.URI;
import java.util.Optional;

@SuppressWarnings("OptionalAssignedToNull")
@RequiredArgsConstructor
public class CompositeMavenPomCache implements MavenPomCache {
    private final MavenPomCache l1;
    private final MavenPomCache l2;

    @Nullable
    @Override
    public ResolvedPom getResolvedDependencyPom(ResolvedGroupArtifactVersion dependency) {
        ResolvedPom l1r = l1.getResolvedDependencyPom(dependency);
        if(l1r != null) {
            return l1r;
        }
        ResolvedPom l2r = l2.getResolvedDependencyPom(dependency);
        if(l2r != null) {
            l1.putResolvedDependencyPom(dependency, l2r);
        }
        return l2r;
    }

    @Override
    public void putResolvedDependencyPom(ResolvedGroupArtifactVersion dependency, ResolvedPom resolved) {
        l1.putResolvedDependencyPom(dependency, resolved);
        l2.putResolvedDependencyPom(dependency, resolved);
    }

    @Nullable
    @Override
    public Optional<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav) {
        Optional<MavenMetadata> l1m = l1.getMavenMetadata(repo, gav);
        if(l1m != null) {
            return l1m;
        }
        Optional<MavenMetadata> l2m = l2.getMavenMetadata(repo, gav);
        if(l2m != null && l2m.isPresent()) {
            l1.putMavenMetadata(repo, gav, l2m.get());
        }
        return l2m;
    }

    @Override
    public void putMavenMetadata(URI repo, GroupArtifactVersion gav, MavenMetadata metadata) {
        l1.putMavenMetadata(repo, gav, metadata);
        l2.putMavenMetadata(repo, gav, metadata);
    }

    @Nullable
    @Override
    public Optional<Pom> getPom(ResolvedGroupArtifactVersion gav) throws MavenDownloadingException {
        Optional<Pom> l1p = l1.getPom(gav);
        if(l1p != null) {
            return l1p;
        }
        Optional<Pom> l2p = l2.getPom(gav);
        if(l2p != null && l2p.isPresent()) {
            l1.putPom(gav, l2p.get());
        }
        return l2p;
    }

    @Override
    public void putPom(ResolvedGroupArtifactVersion gav, Pom pom) {
        l1.putPom(gav, pom);
        l2.putPom(gav, pom);
    }

    @Nullable
    @Override
    public Optional<MavenRepository> getNormalizedRepository(MavenRepository repository) {
        Optional<MavenRepository> l1r = l1.getNormalizedRepository(repository);
        if(l1r != null) {
            return l1r;
        }
        Optional<MavenRepository> l2r = l2.getNormalizedRepository(repository);
        if(l2r != null && l2r.isPresent()) {
            l1.putNormalizedRepository(repository, l2r.get());
        }
        return l2r;
    }

    @Override
    public void putNormalizedRepository(MavenRepository repository, MavenRepository normalized) {
        l1.putNormalizedRepository(repository, normalized);
        l2.putNormalizedRepository(repository, normalized);
    }
}
