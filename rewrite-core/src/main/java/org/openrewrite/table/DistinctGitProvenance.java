/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.marker.GitProvenance;

@JsonIgnoreType
public class DistinctGitProvenance extends DataTable<DistinctGitProvenance.Row> {

    public DistinctGitProvenance(Recipe recipe) {
        super(recipe,
                "Distinct Git Provenance",
                "List out the contents of each unique `GitProvenance` marker in the set of source files. " +
                "When everything is working correctly, exactly one such marker should be printed as all source files are " +
                "expected to come from the same repository / branch / commit hash."
        );
    }

    @Value
    public static class Row {
        @Column(displayName = "Origin", description = "The URL of the git remote.")
        String origin;

        @Column(displayName = "Branch",
                description = "The branch that was checked out at the time the source file was parsed.")
        String branch;

        @Column(displayName = "Changeset", description = "The commit hash of the changeset that was checked out.")
        String changeset;

        @Column(displayName = "AutoCRLF", description = "The value of the `core.autocrlf` git config setting.")
        GitProvenance.AutoCRLF autoCRLF;

        @Column(displayName = "EOL", description = "The value of the `core.eol` git config setting.")
        GitProvenance.EOL eol;
    }
}
