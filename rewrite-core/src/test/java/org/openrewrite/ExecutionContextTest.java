/*
 * Copyright 2020 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class ExecutionContextTest implements RewriteTest {

    @Test
    void anotherCycleIfNewMessagesInExecutionContext() {
        var cycles = new AtomicInteger();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new PlainTextVisitor<>() {
              @Override
              public PlainText visitText(PlainText text, ExecutionContext ctx) {
                  if (ctx.pollMessage("test") == null) {
                      ctx.putMessage("test", "test");
                  }
                  cycles.incrementAndGet();
                  return text;
              }
          }).withCausesAnotherCycle(true))
            .recipeQualityValidation(RecipeQualityValidation.builder().executionContextUnchanged(false).build()),
          text("hello world")
        );
        assertThat(cycles.get()).isEqualTo(2);
    }

    @Test
    void computeMessageIfAbsentFunctionMayWriteToContext() {
        var ctx = new InMemoryExecutionContext();

        String computed = ctx.computeMessageIfAbsent("computed", k -> {
            ctx.computeMessageIfAbsent("nested", k2 -> "nested");
            // More messages than a default ConcurrentHashMap holds before it resizes
            for (int i = 0; i < 16; i++) {
                ctx.putMessage("written" + i, i);
            }
            return "computed";
        });

        assertThat(computed).isEqualTo("computed");
        assertThat(ctx.getMessages())
          .containsEntry("computed", "computed")
          .containsEntry("nested", "nested")
          .containsEntry("written15", 15);
    }

    @Test
    void computeMessageIfAbsentReturnsValueStoredWhileFunctionRan() {
        var ctx = new InMemoryExecutionContext();

        // The function's own write stands in for another thread winning the race on the key
        String computed = ctx.computeMessageIfAbsent("key", k -> {
            ctx.putMessage("key", "stored first");
            return "computed";
        });

        assertThat(computed).isEqualTo("stored first");
        assertThat(ctx.<String>getMessage("key")).isEqualTo("stored first");
    }

    @Test
    void computeMessageIfAbsentReturnsNullResultWithoutStoringIt() {
        var ctx = new InMemoryExecutionContext();
        assertThat(ctx.<String>computeMessageIfAbsent("absent", k -> null)).isNull();
        assertThat(ctx.getMessages()).doesNotContainKey("absent");
    }
}
