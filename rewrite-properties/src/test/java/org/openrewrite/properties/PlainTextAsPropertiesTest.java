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
package org.openrewrite.properties;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.SourceFile;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class PlainTextAsPropertiesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PlainTextAsProperties("**/*.cprops"));
    }

    @DocumentExample
    @Test
    void parsePlainTextAsPropertiesThenApplyPropertiesRecipe() {
        rewriteRun(
          spec -> spec.recipes(
            new PlainTextAsProperties("**/*.cprops"),
            new ChangePropertyKey("old.key", "new.key", null, null)
          ),
          text(
            """
              old.key=value
              """,
            """
              new.key=value
              """,
            spec -> spec.path("config.cprops")
          )
        );
    }

    @Test
    void convertsToPropertiesFile() {
        rewriteRun(
          spec -> spec.afterRecipe(run -> {
              SourceFile result = run.getChangeset().getAllResults().getFirst().getAfter();
              assertThat(result).isInstanceOf(Properties.File.class);
          }).recipes(
            new PlainTextAsProperties("**/*.cprops"),
            new ChangePropertyValue("my.key", "newValue", null, null, null)
          ),
          text(
            """
              my.key=oldValue
              other=value
              """,
            """
              my.key=newValue
              other=value
              """,
            spec -> spec.path("config.cprops")
          )
        );
    }

    @Test
    void doesNotMatchWhenPatternDoesNotMatch() {
        rewriteRun(
          text(
            """
              key=value
              """,
            spec -> spec.path("config.other")
          )
        );
    }

    @Test
    void changePropertyValue() {
        rewriteRun(
          spec -> spec.recipes(
            new PlainTextAsProperties("**/*.cprops"),
            new ChangePropertyValue("my.key", "newValue", null, null, null)
          ),
          text(
            """
              my.key=oldValue
              other=value
              """,
            """
              my.key=newValue
              other=value
              """,
            spec -> spec.path("config.cprops")
          )
        );
    }
}
