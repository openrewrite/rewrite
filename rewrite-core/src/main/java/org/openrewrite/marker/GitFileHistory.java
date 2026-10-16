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
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The commits that changed one file. The first and last commit and the counts follow the file's
 * path, so they start again at a rename; {@link #blame} follows renames, as git blame does.
 */
@Value
@With
public class GitFileHistory implements Marker {
    UUID id;

    GitHistory.Commit firstCommit;

    GitHistory.Commit lastCommit;

    int commitCount;

    int authorCount;

    /**
     * The commit that last changed each line, as runs of consecutive lines in file order, or null
     * when line history was not recorded.
     */
    @Nullable
    List<Lines> blame;

    @Value
    public static class Lines {
        int count;

        GitHistory.Commit commit;
    }
}
