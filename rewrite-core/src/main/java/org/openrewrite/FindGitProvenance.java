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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.table.DistinctGitProvenance;

import java.util.*;

import static java.util.Objects.requireNonNull;

public class FindGitProvenance extends ScanningRecipe<Set<GitProvenance>> {
    // we are looking for substantive differences, not just ID differences
    private static final UUID DONT_CONSIDER_ID_IN_HASH_CODE = UUID.randomUUID();

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
    public Set<GitProvenance> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<GitProvenance> provenances) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                sourceFile.getMarkers().findFirst(GitProvenance.class).ifPresent(provenance ->
                        provenances.add(provenance.withId(DONT_CONSIDER_ID_IN_HASH_CODE)));
                return sourceFile;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(Set<GitProvenance> provenances, ExecutionContext ctx) {
        for (GitProvenance provenance : provenances) {
            distinct.insertRow(ctx, new DistinctGitProvenance.Row(
                    provenance.getOrigin(),
                    provenance.getBranch(),
                    provenance.getChange(),
                    provenance.getAutocrlf(),
                    provenance.getEol())
            );
        }
        return Collections.emptyList();
    }
}
