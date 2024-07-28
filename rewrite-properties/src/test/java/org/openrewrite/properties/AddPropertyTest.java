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
package org.openrewrite.properties;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class AddPropertyTest implements RewriteTest {

    @Test
    void emptyProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "",
            "true",
            null,
            null
          )),
          properties(
            """
              management.metrics.enable.process.files=true
              """
          )
        );
    }

    @Test
    void emptyValue() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "",
            null,
            null
          )),
          properties(
            """
              management.metrics.enable.process.files=true
              """
          )
        );
    }

    @Test
    void containsProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            null,
            null
          )),
          properties(
            """
              management.metrics.enable.process.files=true
              """
          )
        );
    }

    @DocumentExample
    @Test
    void newProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            null,
            null
          )),
          properties(
            """
              management=true
              """,
            """
              management=true
              management.metrics.enable.process.files=true
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2532")
    @Test
    void delimitedByColon() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            null,
            ":"
          )),
          properties(
            """
              management=true
              """,
            """
              management=true
              management.metrics.enable.process.files:true
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2532")
    @Test
    void delimitedByWhitespace() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            null,
            "    "
          )),
          properties(
            """
              management=true
              """,
            """
              management=true
              management.metrics.enable.process.files    true
              """
          )
        );
    }

    @Test
    void addToEmptyFile() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            null,
            null
          )),
          properties(
            "",
            """
              management.metrics.enable.process.files=true
              """
          )
        );
    }

    @Test
    void addCommentedPropertyToEmptyFile() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            "Management metrics",
            null
          )),
          properties(
            "",
            """
              # Management metrics
              management.metrics.enable.process.files=true
              """
          )
        );
    }

    @Test
    void addCommentedPropertyToExistingFile() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            "Management metrics",
            null
          )),
          properties(
            "management=true",
            """
              management=true
              # Management metrics
              management.metrics.enable.process.files=true
              """
          )
        );
    }

    @Test
    void keepPropertyValueWithLineContinuations() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            null,
            null
          )),
          properties(
            """
              management=tr\\
                ue
              """,
            """
              management=tr\\
                ue
              management.metrics.enable.process.files=true
              """,
            spec -> spec.afterRecipe(after -> {
                Properties.Entry entry = (Properties.Entry) after.getContent().get(0);
                assertThat(entry.getValue().getText()).isEqualTo("true");
                assertThat(entry.getValue().getSource()).isEqualTo("tr\\\n  ue");
            })
          )
        );
    }

    @Test
    void orderedInsertionBeginning() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "com.sam",
            "true",
            "sam",
            null
          )),
          properties(
            """
              com.zoe=true
              """,
            """
              # sam
              com.sam=true
              com.zoe=true
              """)
        );
    }

    @Test
    void orderedInsertionMiddle() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "com.sam",
            "true",
            "sam",
            null
          )),
          properties(
            """
              # amy
              com.amy=true
              # bea
              com.bea=true
              # seb
              com.seb=true
              # zoe
              com.zoe=true
              """,
            """
              # amy
              com.amy=true
              # bea
              com.bea=true
              # sam
              com.sam=true
              # seb
              com.seb=true
              # zoe
              com.zoe=true
              """)
        );
    }

    @Test
    void orderedInsertionEnd() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "com.sam",
            "true",
            "sam",
            null
          )),
          properties(
            """
              com.amy=true
              """,
            """
              com.amy=true
              # sam
              com.sam=true
              """)
        );
    }
}
