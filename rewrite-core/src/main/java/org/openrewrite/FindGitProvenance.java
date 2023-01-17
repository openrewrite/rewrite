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
package org.openrewrite;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.marker.Markup;
import org.openrewrite.table.DistinctGitProvenance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FindGitProvenance extends Recipe {
    private final DistinctGitProvenance distinct = new DistinctGitProvenance(this);

    @Override
    public String getDisplayName() {
        return "Show Git source control metadata";
    }

    @Override
    public String getDescription() {
        return "List out the contents of each unique `GitProvenance` marker in the set of source files. " +
               "When everything is working correctly, exactly one such marker should be printed as all source files are " +
               "expected to come from the same repository / branch / commit hash.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Set<GitProvenance> provenances = new HashSet<>();

        // we are looking for substantive differences, not just ID differences
        UUID dontConsiderIdInHashCode = UUID.randomUUID();

        return ListUtils.map(before, sourceFile -> {
            GitProvenance provenance = sourceFile.getMarkers().findFirst(GitProvenance.class).orElse(null);
            if (provenance == null || !provenances.add(provenance.withId(dontConsiderIdInHashCode))) {
                return sourceFile;
            }
            distinct.insertRow(ctx, new DistinctGitProvenance.Row(
                    provenance.getOrigin(),
                    provenance.getBranch(),
                    provenance.getChange(),
                    provenance.getAutocrlf(),
                    provenance.getEol())
            );
            return Markup.info(sourceFile, String.format("GitProvenance:\n" +
                                                         "    origin: %s\n" +
                                                         "    branch: %s\n" +
                                                         "    changeset: %s\n" +
                                                         "    autocrlf: %s\n" +
                                                         "    eol: %s",
                    provenance.getOrigin(),
                    provenance.getBranch(),
                    provenance.getChange(),
                    provenance.getAutocrlf() != null ? provenance.getAutocrlf().toString() : "null",
                    provenance.getEol() != null ? provenance.getEol().toString() : "null"));
        });
    }
}
