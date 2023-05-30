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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class RecipeRunStats extends DataTable<RecipeRunStats.Row> {
    private final MeterRegistry registry = new SimpleMeterRegistry();

    public RecipeRunStats(Recipe recipe) {
        super(recipe,
                "Recipe performance",
                "Statistics used in analyzing the performance of recipes.");
    }

    public SourceFile recordScan(Recipe recipe, Callable<SourceFile> scan) throws Exception {
        //noinspection DataFlowIssue
        return Timer.builder("rewrite.recipe.scan")
                .tag("name", recipe.getName())
                .publishPercentiles(0.99)
                .register(registry)
                .recordCallable(scan);
    }

    @Nullable
    public SourceFile recordEdit(Recipe recipe, Callable<SourceFile> edit) throws Exception {
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
            insertRow(ctx, new Row(
                    recipeName,
                    Long.valueOf(editor.count()).intValue(),
                    scanner == null ? 0 : (long) scanner.totalTime(TimeUnit.NANOSECONDS),
                    scanner == null ? 0 : scanner.takeSnapshot().percentileValues()[0].percentile(),
                    scanner == null ? 0 : (long) scanner.max(TimeUnit.NANOSECONDS),
                    (long) editor.totalTime(TimeUnit.NANOSECONDS),
                    editor.takeSnapshot().percentileValues()[0].percentile(),
                    (long) editor.max(TimeUnit.NANOSECONDS)
            ));
        }
    }

    @Value
    public static class Row {
        @Column(displayName = "The recipe",
                description = "The recipe whose stats are being measured both individually and cumulatively.")
        String recipe;

        @Column(displayName = "Source files",
                description = "The number of source files the recipe ran over.")
        Integer sourceFiles;

        @Column(displayName = "Cumulative scanning time",
                description = "The total time spent across the scanning phase of this recipe.")
        Long scanTotalTime;

        @Column(displayName = "99th percentile scanning time",
                description = "99 out of 100 scans completed in this amount of time.")
        Double scanP99;

        @Column(displayName = "Max scanning time",
                description = "The max time scanning any one source file.")
        Long scanMax;

        @Column(displayName = "Cumulative edit time",
                description = "The total time spent across the editing phase of this recipe.")
        Long editTotalTime;

        @Column(displayName = "99th percentile edit time",
                description = "99 out of 100 edits completed in this amount of time.")
        Double editP99;

        @Column(displayName = "Max edit time",
                description = "The max time editing any one source file.")
        Long editMax;
    }
}
