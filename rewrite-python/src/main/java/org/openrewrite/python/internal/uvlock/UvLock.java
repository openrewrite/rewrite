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
package org.openrewrite.python.internal.uvlock;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Typed document model of a uv.lock file. Field values are stored verbatim as read
 * (already normalized by uv); {@link UvLockWriter} re-emits them without reformatting.
 */
@Value
@With
public class UvLock {

    int version;

    /**
     * Lockfile revision: absent for uv &lt; 0.6 locks, 2 for ~0.7, 3 for current uv.
     * Drives whether artifacts carry {@code upload-time}; surgical edits must match it.
     */
    @Nullable
    Integer revision;

    @Nullable
    String requiresPython;

    @Nullable
    List<String> resolutionMarkers;

    /**
     * The forks the resolution supports (from {@code [tool.uv] environments}); same shape as
     * {@code resolutionMarkers}, emitted just after it.
     */
    @Nullable
    List<String> supportedMarkers;

    /**
     * The forks a resolution must cover (from {@code [tool.uv] required-environments}); emitted
     * after {@code supportedMarkers}.
     */
    @Nullable
    List<String> requiredMarkers;

    /**
     * Mutually-exclusive extra/group sets (from {@code [tool.uv] conflicts}); an array of arrays
     * of inline tables, emitted as the last header key before {@code [options]}.
     */
    @Nullable
    List<List<UvLockConflictItem>> conflicts;

    @Nullable
    UvLockOptions options;

    @Nullable
    UvLockManifest manifest;

    List<UvLockPackage> packages;

    /**
     * Whether artifacts in this lock's style carry {@code upload-time} (revision >= 2).
     */
    public boolean expectsUploadTime() {
        return revision != null && revision >= 2;
    }

    public @Nullable UvLockPackage getPackage(String name) {
        for (UvLockPackage pkg : packages) {
            if (pkg.getName().equals(name)) {
                return pkg;
            }
        }
        return null;
    }
}
