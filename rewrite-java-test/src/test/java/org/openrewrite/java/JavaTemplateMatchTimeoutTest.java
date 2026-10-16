/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.SourcesFileErrors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateMatchTimeoutTest {
    final JavaTemplate println = JavaTemplate.builder("System.out.println(#{any(String)})").build();
    final List<SourceFile> sources = JavaParser.fromJavaVersion().build()
      .parse("class A { void m() { System.out.println(\"a\"); } }")
      .collect(toList());

    @Test
    void sourceFileTimeoutDiscardsEditActingOnSwallowedTimeout() {
        AtomicBoolean reportedNoMatch = new AtomicBoolean();
        List<Throwable> errors = new ArrayList<>();

        RecipeRun run = new RecipeScheduler().scheduleRun(reportNoMatch(Duration.ZERO, reportedNoMatch),
          new InMemoryLargeSourceSet(sources), new InMemoryExecutionContext(errors::add), 1, 1);
        assertThat(reportedNoMatch).as("the template matches when there is no timeout").isFalse();
        assertThat(run.getChangeset().getAllResults()).isEmpty();
        assertThat(errors).isEmpty();

        // matchesTemplate() reports the timeout thrown by its nested visitors as "no match"
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        ctx.putMessage(ExecutionContext.SOURCE_FILE_TIMEOUT, Duration.ofMillis(100));
        run = new RecipeScheduler().scheduleRun(reportNoMatch(Duration.ofMillis(200), reportedNoMatch),
          new InMemoryLargeSourceSet(sources), ctx, 1, 1);

        assertThat(reportedNoMatch).isTrue();
        assertThat(run.getChangeset().getAllResults()).isEmpty();
        assertThat(errors).singleElement().isInstanceOf(SourceFileTimeoutException.class);
        assertThat(run.getDataTableRows(SourcesFileErrors.class))
          .singleElement()
          .satisfies(row -> {
              assertThat(row.getSourcePath()).isEqualTo("A.java");
              assertThat(row.getStackTrace()).startsWith(SourceFileTimeoutException.class.getName());
          });
    }

    private Recipe reportNoMatch(Duration delay, AtomicBoolean reportedNoMatch) {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                sleep(delay);
                if (println.matches(getCursor())) {
                    return method;
                }
                reportedNoMatch.set(true);
                return SearchResult.found(method, "no match");
            }
        });
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
