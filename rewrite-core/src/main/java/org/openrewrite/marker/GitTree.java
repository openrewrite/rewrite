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
package org.openrewrite.marker;

import lombok.Value;
import lombok.With;

import java.util.List;
import java.util.UUID;

/**
 * Every entry of the tree of the commit a repository's source files were parsed from,
 * including symlinks, submodules and files that have no source file. Enough to rebuild
 * the commit's tree objects without the repository, and to check the result against
 * {@link #treeId}.
 */
@Value
@With
public class GitTree implements Marker {
    UUID id;

    String commitId;

    String treeId;

    /**
     * In the order a recursive tree walk yields them.
     */
    List<Entry> entries;

    @Value
    public static class Entry {
        String path;
        int fileMode;
        String objectId;
    }
}
