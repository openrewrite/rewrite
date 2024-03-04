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

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class TreeObserverTest implements RewriteTest {

    @DocumentExample("Observer Property Change in a tree.")
    @Test
    void observePropertyChange() {
        var observed = new AtomicInteger(0);

        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                    return text.withText("hello jonathan");
                }
            }))
            .executionContext(new InMemoryExecutionContext().addObserver(new TreeObserver.Subscription(new TreeObserver() {
                @Override
                public Tree propertyChanged(String property, Cursor cursor, Tree newTree, Object oldValue, Object newValue) {
                    if (property.equals("text")) {
                        observed.incrementAndGet();
                    }
                    return newTree;
                }
            }).subscribeToType(PlainText.class))),
          text(
            "hello jon",
            "hello jonathan"
          )
        );

        assertThat(observed.get()).isEqualTo(1);
    }
}
