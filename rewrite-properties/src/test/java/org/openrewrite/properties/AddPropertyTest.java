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
import org.openrewrite.Validated;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class AddPropertyTest implements RewriteTest {

    @DocumentExample
    @Test
    void newProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            null,
            null,
            null,
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

    @Test
    void emptyProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "",
            "true",
            null,
            null,
            null,
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
            null,
            null,
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
            null,
            null,
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

    @Issue("https://github.com/openrewrite/rewrite/issues/2532")
    @Test
    void delimitedByColon() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "management.metrics.enable.process.files",
            "true",
            null,
            ":",
            null,
            null,
            null
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
            "    ",
            null,
            null,
            null
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
            null,
            null,
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
            null,
            null,
            null,
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
            null,
            null,
            null,
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
            null,
            null,
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
                var entry = (Properties.Entry) after.getContent().getFirst();
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
            null,
            null,
            null,
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
              """
          )
        );
    }

    @Test
    void orderedInsertionMiddle() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "com.sam",
            "true",
            "sam",
            null,
            null,
            null,
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
              """
          )
        );
    }

    @Test
    void orderedInsertionEnd() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "com.sam",
            "true",
            "sam",
            null,
            null,
            null,
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
              """
          )
        );
    }

    @Test
    void unorderedInsertion() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "com.sam",
            "true",
            "sam",
            null,
            false,
            null,
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
              # seb
              com.seb=true
              # zoe
              com.zoe=true
              # sam
              com.sam=true
              """
          )
        );
    }

    @Test
    void beforePropertyBasic() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "new.prop",
            "val",
            null,
            null,
            null,
            "com.bea",
            null
          )),
          properties(
            """
              com.amy=true
              com.bea=true
              com.zoe=true
              """,
            """
              com.amy=true
              new.prop=val
              com.bea=true
              com.zoe=true
              """
          )
        );
    }

    @Test
    void afterPropertyBasic() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "new.prop",
            "val",
            null,
            null,
            null,
            null,
            "com.bea"
          )),
          properties(
            """
              com.amy=true
              com.bea=true
              com.zoe=true
              """,
            """
              com.amy=true
              com.bea=true
              new.prop=val
              com.zoe=true
              """
          )
        );
    }

    @Test
    void beforePropertyWithPrecedingComment() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "new.prop",
            "val",
            null,
            null,
            null,
            "com.bea",
            null
          )),
          properties(
            """
              com.amy=true
              # bea comment
              com.bea=true
              com.zoe=true
              """,
            """
              com.amy=true
              new.prop=val
              # bea comment
              com.bea=true
              com.zoe=true
              """
          )
        );
    }

    @Test
    void beforePropertyWithNewPropertyComment() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "new.prop",
            "val",
            "new property comment",
            null,
            null,
            "com.bea",
            null
          )),
          properties(
            """
              com.amy=true
              com.bea=true
              com.zoe=true
              """,
            """
              com.amy=true
              # new property comment
              new.prop=val
              com.bea=true
              com.zoe=true
              """
          )
        );
    }

    @Test
    void afterPropertyWithNewPropertyComment() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "new.prop",
            "val",
            "new property comment",
            null,
            null,
            null,
            "com.bea"
          )),
          properties(
            """
              com.amy=true
              com.bea=true
              com.zoe=true
              """,
            """
              com.amy=true
              com.bea=true
              # new property comment
              new.prop=val
              com.zoe=true
              """
          )
        );
    }

    @Test
    void beforePropertyNotFoundFallsBackToAlphabetical() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "com.sam",
            "true",
            null,
            null,
            null,
            "nonexistent.key",
            null
          )),
          properties(
            """
              com.amy=true
              com.zoe=true
              """,
            """
              com.amy=true
              com.sam=true
              com.zoe=true
              """
          )
        );
    }

    @Test
    void afterPropertyNotFoundUnordered() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "com.sam",
            "true",
            null,
            null,
            false,
            null,
            "nonexistent.key"
          )),
          properties(
            """
              com.amy=true
              com.zoe=true
              """,
            """
              com.amy=true
              com.zoe=true
              com.sam=true
              """
          )
        );
    }

    @Test
    void afterPropertyLastEntry() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "zzz.last",
            "val",
            null,
            null,
            null,
            null,
            "com.zoe"
          )),
          properties(
            """
              com.amy=true
              com.zoe=true
              """,
            """
              com.amy=true
              com.zoe=true
              zzz.last=val
              """
          )
        );
    }

    @Test
    void beforeAndAfterBothSetValidation() {
        AddProperty recipe = new AddProperty(
            "new.prop",
            "val",
            null,
            null,
            null,
            "some.key",
            "other.key"
        );
        assertThat(recipe.validate().isInvalid()).isTrue();
    }

    @Test
    void beforePropertyOverridesOrderedInsertion() {
        rewriteRun(
          spec -> spec.recipe(new AddProperty(
            "zzz.prop",
            "val",
            null,
            null,
            true,
            "com.amy",
            null
          )),
          properties(
            """
              com.amy=true
              com.zoe=true
              """,
            """
              zzz.prop=val
              com.amy=true
              com.zoe=true
              """
          )
        );
    }

}
