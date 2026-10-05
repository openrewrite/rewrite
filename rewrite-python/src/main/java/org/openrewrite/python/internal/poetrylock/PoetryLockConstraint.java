/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.poetrylock;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One constraint on a dependency edge in a {@code [package.dependencies]} table. Poetry emits a
 * bare version string when the only key is {@code version}, otherwise an inline table whose keys
 * appear (when present) in the order: version|path|url|{@literal <}vcs{@literal >}, branch/tag/rev,
 * subdirectory, extras, optional, markers, develop.
 */
@Value
@With
@Builder
public class PoetryLockConstraint {
    @Nullable String version;
    @Nullable String path;
    @Nullable String url;

    /** VCS kind: one of {@code git}, {@code hg}, {@code bzr}, {@code svn}. */
    @Nullable String vcs;
    @Nullable String vcsUrl;

    @Nullable String branch;
    @Nullable String tag;
    @Nullable String rev;
    @Nullable String subdirectory;

    @Nullable List<String> extras;
    @Nullable Boolean optional;
    @Nullable String markers;
    @Nullable Boolean develop;

    /**
     * Whether this constraint carries only a version, so poetry renders it as a bare string
     * rather than an inline table.
     */
    public boolean isVersionOnly() {
        return version != null && path == null && url == null && vcs == null &&
                branch == null && tag == null && rev == null && subdirectory == null &&
                extras == null && optional == null && markers == null && develop == null;
    }
}
