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
package org.openrewrite.text;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.List;

import static org.openrewrite.test.SourceSpecs.text;

class FindAndReplaceTest implements RewriteTest {
    @DocumentExample
    @Test
    void nonTxtExtension() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", null, null, null, null, null)),
          text(
            """
              This is text.
              """,
            """
              This is textG
              """,
            spec -> spec.path("test.yml")
          )
        );
    }

    @Test
    void removeWhenNullOrEmpty() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("Bar", null, null, null, null, null, null)),
          text(
            """
              Foo
              Bar
              Quz
              """,
            """
              Foo
              
              Quz
              """
          )
        );
    }

    @Test
    void defaultNonRegex() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", null, null, null, null, null)),
          text(
            """
              This is text.
              """,
            """
              This is textG
              """
          )
        );
    }

    @Test
    void regexReplace() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(".", "G", true, null, null, null, null)),
          text(
            """
              This is text.
              """,
            """
              GGGGGGGGGGGGG
              """
          )
        );
    }

    @Test
    void captureGroups() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("This is ([^.]+).", "I like $1.", true, null, null, null, null)),
          text(
            """
              This is text.
              """,
            """
              I like text.
              """
          )
        );
    }

    @Test
    void noRecursive() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace("test", "tested", false, null, null, null, null)),
          text("test", "tested")
        );
    }

    @Test
    void dollarSignsTolerated() {
        String find = "This is text ${dynamic}.";
        String replace = "This is text ${dynamic}. Stuff";
        rewriteRun(
          spec -> spec.recipe(new FindAndReplace(find, replace, null, null, null, null, null)),
          text(find, replace)
        );
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class MultiFindAndReplace extends Recipe {

        @Override
        public String getDisplayName() {
            return "Replaces \"one\" with \"two\" then \"three\" then \"four\"";
        }

        @Override
        public String getDescription() {
            return "Replaces \"one\" with \"two\" then \"three\" then \"four\".";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return Arrays.asList(
              new FindAndReplace("one", "two", null, null, null, null, null),
              new FindAndReplace("two", "three", null, null, null, null, null),
              new FindAndReplace("three", "four", null, null, null, null, null));
        }
    }

    @Test
    void successiveReplacement() {
        rewriteRun(
          spec -> spec.recipe(new MultiFindAndReplace()),
          text(
            """
              one
              """,
            """
              four
              """
          )
        );
    }
}
