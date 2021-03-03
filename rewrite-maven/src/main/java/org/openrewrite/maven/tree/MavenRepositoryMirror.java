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

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class MavenRepositoryMirror {
    String id;

    String url;

    /**
     * The server ID of the repository being mirrored, e.g., "central".
     * This can be a literal id, but it can also take a few other patterns:
     * * = everything
     * external:* = everything not on the localhost and not file based.
     * repo,repo1 = repo or repo1
     * !repo1 = everything except repo1
     * <p>
     * See: https://maven.apache.org/guides/mini/guide-mirror-settings.html#advanced-mirror-specification
     */
    String mirrorOf;

    public static MavenRepository apply(Collection<MavenRepositoryMirror> mirrors, MavenRepository repo) {
        MavenRepository mapped = repo;
        MavenRepository next = null;
        while (next != mapped) {
            next = mapped;
            for (MavenRepositoryMirror mirror : mirrors) {
                mapped = mirror.apply(mapped);
            }
        }
        return mapped;
    }

    public MavenRepository apply(MavenRepository repo) {
        URI uri = URI.create(url);
        if (matches(repo) && !(repo.getUri().equals(uri) && repo.getId().equals(id))) {
            return repo.withUri(uri).withId(id);
        } else {
            return repo;
        }
    }

    public boolean matches(MavenRepository repository) {
        if (mirrorOf == null) {
            return false;
        }
        if (mirrorOf.equals("*")) {
            return true;
        }

        int colonIndex = mirrorOf.indexOf(':');
        String mirrorOfWithoutExternal = mirrorOf;
        boolean externalOnly = false;
        if (colonIndex != -1) {
            externalOnly = true;
            mirrorOfWithoutExternal = mirrorOf.substring(colonIndex + 1);
        }

        List<String> mirrorsOf = Arrays.stream(mirrorOfWithoutExternal.split(",")).collect(Collectors.toList());
        Set<String> excludedRepos = new HashSet<>();
        Set<String> includedRepos = new HashSet<>();
        for (String mirror : mirrorsOf) {
            if (mirror.startsWith("!")) {
                excludedRepos.add(mirror.substring(1));
            } else {
                includedRepos.add(mirror);
            }
        }

        if (externalOnly && isInternal(repository)) {
            return false;
        }
        // Named inclusion/exclusion beats wildcard inclusion/exclusion
        if (excludedRepos.stream().anyMatch(it -> it.equals("*"))) {
            return includedRepos.contains(repository.getId());
        }
        if (includedRepos.stream().anyMatch(it -> it.equals("*"))) {
            return !excludedRepos.contains(repository.getId());
        }
        return !excludedRepos.contains(repository.getId()) && includedRepos.contains(repository.getId());
    }

    private boolean isInternal(MavenRepository repo) {
        if (repo.getUri().getScheme().startsWith("file")) {
            return true;
        }
        // Best-effort basis, by no means a full guarantee of detecting all possible local URIs
        return repo.getUri().getHost().equals("localhost") || repo.getUri().getHost().equals("127.0.0.1");
    }
}
