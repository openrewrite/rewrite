/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scheduling;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeList;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SourcesFileResults;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

/**
 * Exercises {@link ScanningRecipe#nextStage}: a scanning recipe discovers work in one stage and
 * schedules a genuine downstream recipe to act on it in the next stage. The scheduled recipe runs
 * as a first-class recipe (its own visitor lifecycle), and its edit reflects a value — the total
 * number of files discovered — that no single-file pass could know until scanning had completed.
 */
class NextStageTest implements RewriteTest {

    @Test
    void schedulesDownstreamStageFromScan() {
        rewriteRun(
          spec -> spec.recipe(new DiscoverVulnerable()),
          // Both vulnerable files are fixed, and each records "2" — the cross-file count discovered
          // in stage one, which stage two could only receive because it ran after scanning finished.
          text("this is vulnerable", "this is vulnerable FIXED(2)", spec -> spec.path("a.txt")),
          text("also vulnerable here", "also vulnerable here FIXED(2)", spec -> spec.path("b.txt")),
          text("clean file", spec -> spec.path("c.txt"))
        );
    }

    @Test
    void schedulesNothingWhenScanFindsNothing() {
        // No discovery -> nextStage schedules no downstream stage -> classic single-stage convergence.
        rewriteRun(
          spec -> spec.recipe(new DiscoverVulnerable()).expectedCyclesThatMakeChanges(0),
          text("clean"),
          text("also clean")
        );
    }

    @Test
    void syntheticStageWrapperIsNotAPhantomParent() {
        // When a stage schedules more than one recipe, the scheduler groups them under a synthetic
        // StageRecipe. That grouping must not surface as a phantom parent in results attribution.
        rewriteRun(
          spec -> spec.recipe(new ScheduleTwoFixes())
            .dataTable(SourcesFileResults.Row.class, rows -> {
                assertThat(rows).isNotEmpty();
                assertThat(rows)
                        .as("the two genuine downstream recipes are attributed")
                        .anyMatch(r -> r.getRecipe().endsWith("AppendAlpha"))
                        .anyMatch(r -> r.getRecipe().endsWith("AppendBeta"));
                assertThat(rows)
                        .as("the synthetic stage grouping is not a phantom recipe or parent")
                        .noneMatch(r -> r.getRecipe().contains("StageRecipe"))
                        .noneMatch(r -> r.getParentRecipe().contains("StageRecipe"));
            })
            .dataTable(RecipeRunStats.Row.class, rows ->
                assertThat(rows)
                        .as("the synthetic stage grouping gets no recipe-run-stats row")
                        .noneMatch(r -> r.getRecipe().contains("StageRecipe"))),
          text("go", "go alpha beta")
        );
    }

    static class ScheduleTwoFixes extends ScanningRecipe<AtomicBoolean> {
        @Override
        public String getDisplayName() {
            return "Schedule two downstream fixes";
        }

        @Override
        public String getDescription() {
            return "Schedules two recipes for a downstream stage, forcing a synthetic StageRecipe grouping.";
        }

        @Override
        public AtomicBoolean getInitialValue(ExecutionContext ctx) {
            return new AtomicBoolean();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
            return new PlainTextVisitor<ExecutionContext>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    if (text.getText().startsWith("go")) {
                        acc.set(true);
                    }
                    return text;
                }
            };
        }

        @Override
        public void nextStage(RecipeList stage, ExecutionContext ctx, AtomicBoolean acc) {
            if (acc.get()) {
                stage.recipe(new AppendAlpha());
                stage.recipe(new AppendBeta());
            }
        }
    }

    static class AppendAlpha extends Recipe {
        @Override
        public String getDisplayName() {
            return "Append alpha";
        }

        @Override
        public String getDescription() {
            return "Appends alpha.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    return text.getText().contains(" alpha") ? text : text.withText(text.getText() + " alpha");
                }
            };
        }
    }

    static class AppendBeta extends Recipe {
        @Override
        public String getDisplayName() {
            return "Append beta";
        }

        @Override
        public String getDescription() {
            return "Appends beta.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    return text.getText().contains(" beta") ? text : text.withText(text.getText() + " beta");
                }
            };
        }
    }

    static class DiscoverVulnerable extends ScanningRecipe<List<String>> {
        @Override
        public String getDisplayName() {
            return "Discover vulnerable files";
        }

        @Override
        public String getDescription() {
            return "Stage one: find the files that need a fix.";
        }

        @Override
        public List<String> getInitialValue(ExecutionContext ctx) {
            return new ArrayList<>();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(List<String> acc) {
            return new PlainTextVisitor<ExecutionContext>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    if (text.getText().contains("vulnerable")) {
                        acc.add(text.getSourcePath().toString());
                    }
                    return text;
                }
            };
        }

        @Override
        public void nextStage(RecipeList stage, ExecutionContext ctx, List<String> acc) {
            if (!acc.isEmpty()) {
                stage.recipe(new ApplyFix(acc.size(), new ArrayList<>(acc)));
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class ApplyFix extends Recipe {
        int discovered;
        List<String> paths;

        @Override
        public String getDisplayName() {
            return "Apply fix";
        }

        @Override
        public String getDescription() {
            return "Stage two: fix the files discovered by stage one.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    if (paths.contains(text.getSourcePath().toString()) && !text.getText().contains(" FIXED(")) {
                        return text.withText(text.getText() + " FIXED(" + discovered + ")");
                    }
                    return text;
                }
            };
        }
    }
}
