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
package org.openrewrite.table;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class RecipeRunStats extends DataTable<RecipeRunStats.Row> {
    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final Set<Path> sourceFileVisited = new HashSet<>();
    private final Set<Path> sourceFileChanged = new HashSet<>();

    public RecipeRunStats(Recipe recipe) {
        super(recipe,
                "Recipe performance",
                "Statistics used in analyzing the performance of recipes.");
    }

    public void recordSourceVisited(@Nullable SourceFile source) {
        if (source != null) {
            sourceFileVisited.add(source.getSourcePath());
        }
    }

    public void recordSourceFileChanged(@Nullable SourceFile before, @Nullable SourceFile after) {
        if (after != null) {
            sourceFileChanged.add(after.getSourcePath());
        } else if (before != null) {
            sourceFileChanged.add(before.getSourcePath());
        }
    }

    public void recordScan(Recipe recipe, Callable<SourceFile> scan) throws Exception {
        Timer.builder("rewrite.recipe.scan")
                .tag("name", recipe.getName())
                .publishPercentiles(0.99)
                .register(registry)
                .recordCallable(scan);
    }

    public @Nullable SourceFile recordEdit(Recipe recipe, Callable<SourceFile> edit) throws Exception {
        return Timer.builder("rewrite.recipe.edit")
                .tag("name", recipe.getName())
                .publishPercentiles(0.99)
                .register(registry)
                .recordCallable(edit);
    }

    public void flush(ExecutionContext ctx) {
        for (Timer editor : registry.find("rewrite.recipe.edit").timers()) {
            String recipeName = requireNonNull(editor.getId().getTag("name"));
            Timer scanner = registry.find("rewrite.recipe.scan").tag("name", recipeName).timer();
            Row row = new Row(
                    recipeName,
                    sourceFileVisited.size(),
                    sourceFileChanged.size(),
                    scanner == null ? 0 : (long) scanner.totalTime(TimeUnit.NANOSECONDS),
                    scanner == null ? 0 : scanner.takeSnapshot().percentileValues()[0].value(TimeUnit.NANOSECONDS),
                    scanner == null ? 0 : (long) scanner.max(TimeUnit.NANOSECONDS),
                    (long) editor.totalTime(TimeUnit.NANOSECONDS),
                    editor.takeSnapshot().percentileValues()[0].value(TimeUnit.NANOSECONDS),
                    (long) editor.max(TimeUnit.NANOSECONDS)
            );
            addRowToDataTable(ctx, row);
        }

        // find scanners that never finished their edit phase
        for (Timer scanner : registry.find("rewrite.recipe.scan").timers()) {
            String recipeName = requireNonNull(scanner.getId().getTag("name"));
            if (registry.find("rewrite.recipe.edit").tag("name", recipeName).timer() == null) {
                Row row = new Row(
                        recipeName,
                        Long.valueOf(scanner.count()).intValue(),
                        sourceFileChanged.size(),
                        (long) scanner.totalTime(TimeUnit.NANOSECONDS),
                        scanner.takeSnapshot().percentileValues()[0].value(TimeUnit.NANOSECONDS),
                        (long) scanner.max(TimeUnit.NANOSECONDS),
                        0L,
                        0.0,
                        0L
                );

                addRowToDataTable(ctx, row);
            }
        }
    }

    private void addRowToDataTable(ExecutionContext ctx, Row row) {
        //noinspection DuplicatedCode
        ctx.computeMessage(ExecutionContext.DATA_TABLES, row, ConcurrentHashMap::new, (extract, allDataTables) -> {
            //noinspection unchecked
            List<Row> dataTablesOfType = (List<Row>) allDataTables.computeIfAbsent(this, c -> new ArrayList<>());
            dataTablesOfType.add(row);
            return allDataTables;
        });
    }

    @Value
    public static class Row {
        @Column(displayName = "The recipe",
                description = "The recipe whose stats are being measured both individually and cumulatively.")
        String recipe;

        @Column(displayName = "Source file count",
                description = "The number of source files the recipe ran over.")
        Integer sourceFiles;

        @Column(displayName = "Source file changed count",
                description = "The number of source files which were changed in the recipe run. Includes files created, deleted, and edited.")
        Integer sourceFilesChanged;

        @Column(displayName = "Cumulative scanning time (ns)",
                description = "The total time spent across the scanning phase of this recipe.")
        Long scanTotalTimeNs;

        @Column(displayName = "99th percentile scanning time (ns)",
                description = "99 out of 100 scans completed in this amount of time.")
        Double scanP99Ns;

        @Column(displayName = "Max scanning time (ns)",
                description = "The max time scanning any one source file.")
        Long scanMaxNs;

        @Column(displayName = "Cumulative edit time (ns)",
                description = "The total time spent across the editing phase of this recipe.")
        Long editTotalTimeNs;

        @Column(displayName = "99th percentile edit time (ns)",
                description = "99 out of 100 edits completed in this amount of time.")
        Double editP99Ns;

        @Column(displayName = "Max edit time (ns)",
                description = "The max time editing any one source file.")
        Long editMaxNs;
    }
}
