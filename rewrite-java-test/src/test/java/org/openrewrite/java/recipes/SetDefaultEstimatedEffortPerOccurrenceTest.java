/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SetDefaultEstimatedEffortPerOccurrenceTest implements RewriteTest {

    @SuppressWarnings("NullableProblems")
    @Test
    void setDefault() {
        rewriteRun(
          spec -> spec.recipe(new SetDefaultEstimatedEffortPerOccurrence()),
          java(
            """
                  package org.openrewrite;
                  import java.time.Duration;

                  public class Recipe {
                      public Duration getEstimatedEffortPerOccurrence() {
                          return null;
                      }
                  }
              """
          ),
          java(
            """
                  import org.openrewrite.Recipe;
                  class SampleRecipe extends Recipe {
                      public String getDisplayName() { return null; }
                  
                      public Object getVisitor() { return null; }
                  }
              """,
            """
                  import org.openrewrite.Recipe;
                  
                  import java.time.Duration;
                  
                  class SampleRecipe extends Recipe {
                      public String getDisplayName() { return null; }
                  
                      @Override
                      public Duration getEstimatedEffortPerOccurrence() {
                          return Duration.ofMinutes(5);
                      }
                  
                      public Object getVisitor() { return null; }
                  }
              """
          )
        );
    }
}
