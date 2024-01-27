/*
 * Copyright 2020 the original author or authors.
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

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class MavenRepositoryMirror {
    @Nullable
    String id;

    @Nullable
    String url;

    /**
     * The server ID of the repository being mirrored, e.g., "central".
     * This can be a literal id, but it can also take a few other patterns:
     * * = everything
     * external:* = everything not on the localhost and not file based.
     * repo,repo1 = repo or repo1
     * !repo1 = everything except repo1
     * <p>
     * See: <a href="https://maven.apache.org/guides/mini/guide-mirror-settings.html#advanced-mirror-specification">Maven's Mirrors documentation</a>
     */
    @Nullable
    String mirrorOf;

    @Nullable
    Boolean releases;

    @Nullable
    Boolean snapshots;

    private final boolean externalOnly;
    private final List<String> mirrorsOf;
    private final Set<String> excludedRepos;
    private final Set<String> includedRepos;

    public MavenRepositoryMirror(@Nullable String id, @Nullable String url, @Nullable String mirrorOf, @Nullable Boolean releases, @Nullable Boolean snapshots) {
        this.id = id;
        this.url = url;
        this.mirrorOf = mirrorOf;
        this.releases = releases;
        this.snapshots = snapshots;

        if (mirrorOf != null) {
            int colonIndex = mirrorOf.indexOf(':');
            String mirrorOfWithoutExternal;
            if (colonIndex == -1) {
                mirrorOfWithoutExternal = mirrorOf;
                externalOnly = false;
            } else {
                externalOnly = true;
                mirrorOfWithoutExternal = mirrorOf.substring(colonIndex + 1);
            }
            mirrorsOf = Arrays.stream(mirrorOfWithoutExternal.split(",")).collect(Collectors.toList());
            excludedRepos = new HashSet<>();
            includedRepos = new HashSet<>();
            for (String mirror : mirrorsOf) {
                if (mirror.startsWith("!")) {
                    excludedRepos.add(mirror.substring(1));
                } else {
                    includedRepos.add(mirror);
                }
            }
        } else {
            externalOnly = false;
            mirrorsOf = null;
            includedRepos = null;
            excludedRepos = null;
        }
    }

    public static MavenRepository apply(Collection<MavenRepositoryMirror> mirrors, MavenRepository repo) {
        for (MavenRepositoryMirror mirror : mirrors) {
            MavenRepository mapped = mirror.apply(repo);
            if (mapped != repo) {
                return  mapped;
            }
        }
        return repo;
    }

    public MavenRepository apply(MavenRepository repo) {
        if (matches(repo)) {
            return repo.withUri(url)
                    .withId(id)
                    .withReleases(!Boolean.FALSE.equals(releases) ? "true" : "false")
                    .withSnapshots(!Boolean.FALSE.equals(snapshots) ? "true" : "false")
                    // Since the URL has likely changed we cannot assume that the new repository is known to exist
                    .withKnownToExist(false);
        }
        return repo;
    }

    public boolean matches(MavenRepository repository) {
        if (mirrorOf == null) {
            return false;
        }
        if ("*".equals(mirrorOf)) {
            return true;
        }

        if (externalOnly && isInternal(repository)) {
            return false;
        }
        // Named inclusion/exclusion beats wildcard inclusion/exclusion
        if (excludedRepos.contains("*")) {
            return includedRepos.contains(repository.getId());
        }
        if (includedRepos.contains("*")) {
            return !excludedRepos.contains(repository.getId());
        }
        return !excludedRepos.contains(repository.getId()) && includedRepos.contains(repository.getId());
    }

    private boolean isInternal(MavenRepository repo) {
        if (repo.getUri().regionMatches(true, 0,"file:", 0, 5)) {
            return true;
        }
        try {
            URI uri = new URI(repo.getUri());
            return "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        } catch (URISyntaxException ignored) {
            // might be a property reference
        }
        return false;
    }
}
