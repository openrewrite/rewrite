/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = false)
@With
public class GitTreeEntry implements Marker {
    UUID id;

    String objectId;

    int fileMode;

    /**
     * How the bytes the source file was parsed from compare to this blob, or null when not recorded.
     */
    @Nullable
    WorkingTreeMatch workingTreeMatch;

    public GitTreeEntry(UUID id, String objectId, int fileMode) {
        this(id, objectId, fileMode, null);
    }

    @JsonCreator
    public GitTreeEntry(UUID id, String objectId, int fileMode, @Nullable WorkingTreeMatch workingTreeMatch) {
        this.id = id;
        this.objectId = objectId;
        this.fileMode = fileMode;
        this.workingTreeMatch = workingTreeMatch;
    }

    public enum WorkingTreeMatch {
        IDENTICAL,

        /**
         * Identical once CRLF line endings are converted to LF, as git does on commit with autocrlf.
         */
        CRLF,

        /**
         * An uncommitted change, or a symlink whose target was parsed in its place.
         */
        DIFFERENT
    }
}
