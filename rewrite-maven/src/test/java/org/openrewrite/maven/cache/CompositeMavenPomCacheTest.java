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
package org.openrewrite.maven.cache;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenMetadata;

import java.net.URI;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class CompositeMavenPomCacheTest {

    private static final URI REPO = URI.create("https://repo.example/maven2");
    private static final GroupArtifactVersion GAV = new GroupArtifactVersion("com.example", "example", "1.0");

    private static MavenMetadata metadata() {
        return new MavenMetadata(new MavenMetadata.Versioning(emptyList(), emptyList(), null, null, null, null));
    }

    @Test
    void forwardsValidatorsToL2OnPut() {
        InMemoryMavenPomCache l2 = new InMemoryMavenPomCache();
        CompositeMavenPomCache cache = new CompositeMavenPomCache(new InMemoryMavenPomCache(), l2);

        MavenMetadata metadata = metadata();
        cache.putMavenMetadata(REPO, GAV, MavenMetadataCacheEntry.fresh(metadata, "\"etag-1\"", "Wed, 21 Oct 2015 07:28:00 GMT"));

        // The persistent layer retains the value and its validators, not just the bare metadata.
        MavenMetadataCacheEntry l2Entry = l2.getMavenMetadata(REPO, GAV);
        assertThat(l2Entry).isNotNull();
        assertThat(l2Entry.getMetadata()).isSameAs(metadata);
        assertThat(l2Entry.getEtag()).isEqualTo("\"etag-1\"");
        assertThat(l2Entry.getLastModified()).isEqualTo("Wed, 21 Oct 2015 07:28:00 GMT");
    }

    @Test
    void returnsL2EntryWithValidatorsWhenL1Misses() {
        InMemoryMavenPomCache l1 = new InMemoryMavenPomCache();
        InMemoryMavenPomCache l2 = new InMemoryMavenPomCache();
        l2.putMavenMetadata(REPO, GAV, MavenMetadataCacheEntry.fresh(metadata(), "\"etag-1\"", "Wed, 21 Oct 2015 07:28:00 GMT"));
        CompositeMavenPomCache cache = new CompositeMavenPomCache(l1, l2);

        MavenMetadataCacheEntry entry = cache.getMavenMetadata(REPO, GAV);
        assertThat(entry).isNotNull();
        assertThat(entry.getEtag()).isEqualTo("\"etag-1\"");
        assertThat(entry.getLastModified()).isEqualTo("Wed, 21 Oct 2015 07:28:00 GMT");

        // A fresh l2 hit is promoted into l1 so the validators are available without re-consulting l2.
        MavenMetadataCacheEntry promoted = l1.getMavenMetadata(REPO, GAV);
        assertThat(promoted).isNotNull();
        assertThat(promoted.getEtag()).isEqualTo("\"etag-1\"");
    }
}
