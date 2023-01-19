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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

public class RecipeSchedulerTest implements RewriteTest {

    @Test
    void exceptionsCauseResult() {
        rewriteRun(
          spec -> spec
            .executionContext(new InMemoryExecutionContext())
            .recipe(new BoomRecipe())
            .afterRecipe(run -> assertThat(run.getResults().get(0).getRecipeErrors())
              .singleElement()
              .satisfies(t -> assertThat(t.getMessage())
                .matches("Exception while visiting project file 'file\\.txt', caused by: org\\.openrewrite\\.BoomException: boom, at org\\.openrewrite\\.BoomRecipe\\$1\\.visitText\\(RecipeSchedulerTest\\.java:\\d+\\)"))
            ),
          text(
            "hello",
            "~~(boom)~~>hello"
          )
        );
    }
}

class BoomRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "We go boom";
    }

    @Override
    public String getDescription() {
        return "Test recipe.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                throw new BoomException();
            }
        };
    }
}

/**
 * Simplified exception that only displays stack trace elements within the [BoomRecipe].
 */
class BoomException extends RuntimeException {
    public BoomException() {
        super("boom");
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return Arrays.stream(super.getStackTrace())
          .filter(st -> st.getClassName().startsWith(BoomRecipe.class.getName()))
          .toArray(StackTraceElement[]::new);
    }
}
