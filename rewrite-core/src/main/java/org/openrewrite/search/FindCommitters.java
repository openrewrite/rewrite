/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.CommitsByDay;
import org.openrewrite.table.DistinctCommitters;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindCommitters extends ScanningRecipe<Map<String, GitProvenance.Committer>> {
    private transient final DistinctCommitters committers = new DistinctCommitters(this);
    private transient final CommitsByDay commitsByDay = new CommitsByDay(this);

    @Option(displayName = "From date",
            required = false,
            description = "Optional. Take into account only commits since this date (inclusive). Default will be the entire history.",
            example = "2023-01-01")
    @Nullable
    String fromDate;

    @Override
    public String getDisplayName() {
        return "Find committers on repositories";
    }

    @Override
    public String getDescription() {
        return "List the committers on a repository.";
    }

    @Override
    public Map<String, GitProvenance.Committer> getInitialValue(ExecutionContext ctx) {
        return new TreeMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<String, GitProvenance.Committer> acc) {
        LocalDate from = this.fromDate == null ? null : LocalDate.parse(this.fromDate).minusDays(1);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    sourceFile.getMarkers().findFirst(GitProvenance.class).ifPresent(provenance -> {
                        if (provenance.getCommitters() != null) {
                            for (GitProvenance.Committer committer : provenance.getCommitters()) {
                                if (from == null || committer.getCommitsByDay().keySet().stream().anyMatch(day -> day.isAfter(from))) {
                                    acc.put(committer.getEmail(), committer);
                                }
                            }
                        }
                    });
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Map<String, GitProvenance.Committer> acc, ExecutionContext ctx) {
        for (GitProvenance.Committer committer : acc.values()) {
            committers.insertRow(ctx, new DistinctCommitters.Row(
                    committer.getName(),
                    committer.getEmail(),
                    committer.getCommitsByDay().lastKey(),
                    committer.getCommitsByDay().values().stream().mapToInt(Integer::intValue).sum()
            ));

            committer.getCommitsByDay().forEach((day, commits) -> commitsByDay.insertRow(ctx, new CommitsByDay.Row(
                    committer.getName(),
                    committer.getEmail(),
                    day,
                    commits
            )));
        }
        return Collections.emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<String, GitProvenance.Committer> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            @Nullable
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    return SearchResult.found(tree, String.join("\n", acc.keySet()));
                }
                return tree;
            }
        };
    }
}
