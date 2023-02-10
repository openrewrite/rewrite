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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@FieldDefaults(makeFinal = true, level = AccessLevel.PACKAGE)
@Incubating(since = "7.29.0")
public class RecipeRunStats {
    @Getter
    Recipe recipe;

    public RecipeRunStats(Recipe recipe) {
        this.recipe = recipe;
        if (recipe.getRecipeList().isEmpty()) {
            this.called = new ArrayList<>();
        } else {
            this.called = new ArrayList<>(recipe.getRecipeList().size());
            for (Recipe callee : recipe.getRecipeList()) {
                called.add(new RecipeRunStats(callee));
            }
        }
    }

    /**
     * Recipes that were ran via {@link Recipe#doNext(Recipe)} by the recipe
     * this stats instance is for.
     */
    @Getter
    List<RecipeRunStats> called;

    AtomicInteger calls = new AtomicInteger();

    /**
     * The number of times the recipe ran over all cycles.
     */
    public int getCalls() {
        return calls.get();
    }

    final AtomicLong cumulative = new AtomicLong();

    /**
     * The total time spent across all executions of this recipe.
     */
    public Duration getCumulative() {
        return Duration.ofNanos(cumulative.get());
    }

    final AtomicLong max = new AtomicLong();

    /**
     * The max time spent in any one execution of this recipe.
     */
    public Duration getMax() {
        return Duration.ofNanos(max.get());
    }

    AtomicLong ownGetVisitor = new AtomicLong();

    /**
     * The total time spent in running the visitor returned by {@link Recipe#getVisitor()}
     * for this recipe.
     */
    public Duration getOwnGetVisitor() {
        return Duration.ofNanos(ownGetVisitor.get());
    }

    AtomicLong ownVisit = new AtomicLong();

    /**
     * The total time spent in running the visitor returned by {@link Recipe#visit(List, ExecutionContext)}()}
     * for this recipe.
     */
    public Duration getOwnVisit() {
        return Duration.ofNanos(ownVisit.get());
    }

    @Incubating(since = "7.29.0")
    public String printAsMermaidGantt(double scale) {
        StringBuilder gantt = new StringBuilder("gantt\n");
        gantt.append("  axisFormat %M:%S\n");
        gantt.append("  dateFormat S\n\n");
        gantt.append("  section Recipe run\n");
        printAsMermaidGanttRecursive(gantt, null, this, new IdentityHashMap<>(), scale);

        return gantt.toString();
    }

    private void printAsMermaidGanttRecursive(StringBuilder gantt, @Nullable RecipeRunStats after,
                                              RecipeRunStats stats,
                                              Map<RecipeRunStats, Integer> seen,
                                              double scale) {
        seen.putIfAbsent(stats, seen.size() + 1);
        String id = "r" + seen.size();
        Duration time = stats.getOwnGetVisitor().plus(stats.getOwnVisit());

        String label = stats.getRecipe().getClass().getSimpleName();
        if (label.isEmpty()) {
            label = "Recipe";
        }
        gantt.append("  ").append(label).append("  :").append(id);

        if (after != null) {
            gantt.append(", ").append("after r").append(seen.get(after));
        } else {
            gantt.append(", 0");
        }

        long scaled = (long) (time.toNanos() / (1e6 / scale));
        gantt.append(", ").append(scaled).append("ms");

        gantt.append("\n");

        for (RecipeRunStats called : stats.getCalled()) {
            printAsMermaidGanttRecursive(gantt, stats, called, seen, scale);
        }
    }

    public String printAsCsv() {
        StringBuilder csv = new StringBuilder("id,caller,name,cumulative,max,ownGetVisitor,ownVisit,ownTotal\n");
        printAsCsvRecursive(csv, null, this, new IdentityHashMap<>());
        return csv.toString();
    }

    private void printAsCsvRecursive(StringBuilder csv, @Nullable RecipeRunStats after,
                                     RecipeRunStats stats,
                                     Map<RecipeRunStats, Integer> seen) {
        seen.putIfAbsent(stats, seen.size() + 1);
        String label = "r" + seen.size();
        Duration ownTotal = stats.getOwnGetVisitor().plus(stats.getOwnVisit());

        csv.append(label)
                .append(",").append(after == null ? "" : "r" + seen.get(after))
                .append(",").append(stats.getRecipe().getClass().getSimpleName())
                .append(",").append(humanReadableFormat(stats.getCumulative()))
                .append(",").append(humanReadableFormat(stats.getMax()))
                .append(",").append(humanReadableFormat(stats.getOwnGetVisitor()))
                .append(",").append(humanReadableFormat(stats.getOwnVisit()))
                .append(",").append(humanReadableFormat(ownTotal))
                .append("\n");

        for (RecipeRunStats called : stats.getCalled()) {
            printAsCsvRecursive(csv, stats, called, seen);
        }
    }

    private static String humanReadableFormat(Duration duration) {
        long seconds = duration.getSeconds();
        long micros = (duration.toNanos() / 1000) - TimeUnit.SECONDS.toMicros(duration.getSeconds());
        return String.format("%s.%06d",
                seconds,
                micros);
    }
}
