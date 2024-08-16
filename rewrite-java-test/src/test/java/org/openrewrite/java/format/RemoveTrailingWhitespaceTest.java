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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class RemoveTrailingWhitespaceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveTrailingWhitespace());
    }

    @DocumentExample
    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @Test
    void removeTrailing() {
        rewriteRun(
          java(
            """
              class Test {  
              
                  public void method(Test t) {\s\s
                  }\s\s
              }\s\s
              """,
            """
              class Test {
              
                  public void method(Test t) {
                  }
              }
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Disabled // TODO: This exposes bug around trailing commas in the parser
    @Issue("https://github.com/openrewrite/rewrite/issues/1053")
    @Test
    void doNotRemoveTrailingComma() {
        rewriteRun(
          java(
            """
              public class Test {
                  Integer[] integerArray = new Integer[] {
                      1,
                      2,
                      4,
                  };
              }
              """
          )
        );
    }
}
