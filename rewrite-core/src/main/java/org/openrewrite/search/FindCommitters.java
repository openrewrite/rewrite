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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.table.CommitsByDay;
import org.openrewrite.table.DistinctCommitters;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindCommitters extends ScanningRecipe<AtomicReference<GitProvenance>> {

    transient DistinctCommitters committers = new DistinctCommitters(this);
    transient CommitsByDay commitsByDay = new CommitsByDay(this);

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
    public int maxCycles() {
        // This recipe does not modify the LSTs and only requires the `GitProvenance` marker
        return 1;
    }

    @Override
    public AtomicReference<GitProvenance> getInitialValue(ExecutionContext ctx) {
        return new AtomicReference<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicReference<GitProvenance> acc) {
        return Preconditions.check(acc.get() == null, new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    sourceFile.getMarkers().findFirst(GitProvenance.class).ifPresent(provenance -> {
                        if (provenance.getCommitters() != null) {
                            acc.set(provenance);
                        }
                    });
                }
                return tree;
            }
        });
    }

    @Override
    public Collection<? extends SourceFile> generate(AtomicReference<GitProvenance> acc, ExecutionContext ctx) {
        GitProvenance gitProvenance = acc.get();
        if (gitProvenance != null) {
            LocalDate from = StringUtils.isBlank(fromDate) ? null : LocalDate.parse(fromDate).minusDays(1);
            for (GitProvenance.Committer committer : requireNonNull(gitProvenance.getCommitters())) {
                if (from == null || committer.getCommitsByDay().keySet().stream().anyMatch(day -> day.isAfter(from))) {
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
            }
        }
        return Collections.emptyList();
    }
}
