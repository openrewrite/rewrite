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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeRunStats extends DataTable<RecipeRunStats.Row> {
    private final Map<String, RecipeTimers> recipeTimers = new ConcurrentHashMap<>();
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
        recipeTimers.computeIfAbsent(recipe.getName(), k -> new RecipeTimers()).recordScan(scan);
    }

    public @Nullable SourceFile recordEdit(Recipe recipe, Callable<SourceFile> edit) throws Exception {
        return recipeTimers.computeIfAbsent(recipe.getName(), k -> new RecipeTimers()).recordEdit(edit);
    }

    public void flush(ExecutionContext ctx) {
        for (Map.Entry<String, RecipeTimers> entry : recipeTimers.entrySet()) {
            String recipeName = entry.getKey();
            RecipeTimers timers = entry.getValue();

            Row row = new Row(
                    recipeName,
                    sourceFileVisited.size(),
                    sourceFileChanged.size(),
                    timers.scan.getTotalNs(),
                    timers.scan.getMaxNs(),
                    timers.edit.getTotalNs(),
                    timers.edit.getMaxNs()
            );
            addRowToDataTable(ctx, row);
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

        @Column(displayName = "Max scanning time (ns)",
                description = "The max time scanning any one source file.")
        Long scanMaxNs;

        @Column(displayName = "Cumulative edit time (ns)",
                description = "The total time spent across the editing phase of this recipe.")
        Long editTotalTimeNs;

        @Column(displayName = "Max edit time (ns)",
                description = "The max time editing any one source file.")
        Long editMaxNs;
    }

    private static class RecipeTimers {
        final PhaseTimer scan = new PhaseTimer();
        final PhaseTimer edit = new PhaseTimer();

        void recordScan(Callable<SourceFile> scanCallable) throws Exception {
            scan.recordTimed(scanCallable);
        }

        @Nullable
        SourceFile recordEdit(Callable<SourceFile> editCallable) throws Exception {
            return edit.recordTimed(editCallable);
        }
    }

    @Getter
    private static class PhaseTimer {
        private long totalNs = 0;
        private long maxNs = 0;

        <T> T recordTimed(Callable<T> callable) throws Exception {
            long startNs = System.nanoTime();
            try {
                return callable.call();
            } finally {
                long elapsedNs = System.nanoTime() - startNs;
                record(elapsedNs);
            }
        }

        private void record(long elapsedNs) {
            totalNs += elapsedNs;
            maxNs = Math.max(maxNs, elapsedNs);
        }
    }
}
