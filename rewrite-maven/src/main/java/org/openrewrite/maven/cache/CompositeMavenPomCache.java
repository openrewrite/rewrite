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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.net.URI;

public class CompositeMavenPomCache implements MavenPomCache {
    MavenPomCache l1;
    MavenPomCache l2;

    @Nullable
    @Override
    public CacheResult<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav) {
        CacheResult<MavenMetadata> l1m = l1.getMavenMetadata(repo, gav);
        if(l1m != null) {
            return l1m;
        }
        CacheResult<MavenMetadata> l2m = l2.getMavenMetadata(repo, gav);
        if(l2m != null) {
            l1.putMavenMetadata(repo, gav, l2m.getData());
        }
        return l2m;
    }

    @Override
    public CacheResult<MavenMetadata> putMavenMetadata(URI repo, GroupArtifactVersion gav, MavenMetadata metadata) {
        l1.putMavenMetadata(repo, gav, metadata);
        return l2.putMavenMetadata(repo, gav, metadata);
    }

    @Nullable
    @Override
    public CacheResult<Pom> getPom(ResolvedGroupArtifactVersion gav) {
        CacheResult<Pom> l1p = l1.getPom(gav);
        if(l1p != null) {
            return l1p;
        }
        CacheResult<Pom> l2p = l2.getPom(gav);
        if(l2p != null) {
            l1.putPom(gav, l2p.getData());
        }
        return l2p;
    }

    @Override
    public CacheResult<Pom> putPom(ResolvedGroupArtifactVersion gav, Pom pom) {
        l1.putPom(gav, pom);
        return l2.putPom(gav, pom);
    }

    @Nullable
    @Override
    public CacheResult<MavenRepository> getNormalizedRepository(MavenRepository repository) {
        CacheResult<MavenRepository> l1r = l1.getNormalizedRepository(repository);
        if(l1r != null) {
            return l1r;
        }
        CacheResult<MavenRepository> l2r = l2.getNormalizedRepository(repository);
        if(l2r != null) {
            l1.putNormalizedRepository(repository, l2r.getData());
        }
        return l2r;
    }

    @Override
    public CacheResult<MavenRepository> putNormalizedRepository(MavenRepository repository, MavenRepository normalized) {
        l1.putNormalizedRepository(repository, normalized);
        return l2.putNormalizedRepository(repository, normalized);
    }
}
