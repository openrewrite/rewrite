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
package org.openrewrite.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.test.SourceSpecs.text;

class RemoveTrailingWhitespaceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveTrailingWhitespace());
    }

    @DocumentExample
    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @Test
    void removeTrailingFirst() {
        rewriteRun(
          text(
            """
               Lorem ipsum dolor sit amet, consectetur adipiscing elit,\s\s 
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
              """,
            """
               Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
              """,
            SourceSpec::noTrim
          )
        );
    }

    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @Test
    void removeTrailingLast() {
        rewriteRun(
          text(
            """
               Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\s\s
              """,
            """
               Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
              """,
            SourceSpec::noTrim
          )
        );
    }

    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @Test
    void removeTrailingAll() {
        rewriteRun(
          text(
            """
               Lorem ipsum dolor sit amet, consectetur adipiscing elit,\s\s 
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\s\s
              """,
            """
               Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
              """,
            SourceSpec::noTrim
          )
        );
    }

    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @Test
    void removeTrailingBetween() {
        rewriteRun(
          text(
            """
               \s\s
               Lorem ipsum dolor sit amet, consectetur adipiscing elit,
               \s\s 
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
               \s\s
              """,
            """
               
               Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
               
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
               
              """,
            SourceSpec::noTrim
          )
        );
    }


    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    @Test
    void removeTrailingConsecutive() {
        rewriteRun(
          text(
            """
               Lorem ipsum dolor sit amet, consectetur adipiscing elit,\s\s
               \s\s 
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
              """,
            """
               Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
               
               sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
              """,
            SourceSpec::noTrim
          )
        );
    }
}
