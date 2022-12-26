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
import org.openrewrite.internal.lang.Nullable;
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
              public @Nullable PlainText visit(@Nullable Tree tree, ExecutionContext p) {
                  if(p.pollMessage("test") == null) {
                      p.putMessage("test", "test");
                  }
                  cycles.incrementAndGet();
                  return super.visit(tree, p);
              }
          }).withCausesAnotherCycle(true)),
          text("hello world")
        );
        assertThat(cycles.get()).isEqualTo(2);
    }
}
