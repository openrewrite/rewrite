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
import static org.openrewrite.properties.AddProperty.InsertMode;
import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class AddPropertyTest implements RewriteTest {

    @DocumentExample
    @Test
    void newProperty() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("management.metrics.enable.process.files")
            .value("true")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("")
            .value("true")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("management.metrics.enable.process.files")
            .value("")
            .build()),
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
          spec -> spec.recipe(
            AddProperty.builder()
              .property("management.metrics.enable.process.files")
              .value("true")
              .build()
          ),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("management.metrics.enable.process.files")
            .value("true")
            .delimiter(":")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("management.metrics.enable.process.files")
            .value("true")
            .delimiter("    ")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("management.metrics.enable.process.files")
            .value("true")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("management.metrics.enable.process.files")
            .value("true")
            .comment("Management metrics")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("management.metrics.enable.process.files")
            .value("true")
            .comment("Management metrics")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("management.metrics.enable.process.files")
            .value("true")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("com.sam")
            .value("true")
            .comment("sam")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("com.sam")
            .value("true")
            .comment("sam")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("com.sam")
            .value("true")
            .comment("sam")
            .build()),
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
          spec -> spec.recipe(AddProperty.builder()
            .property("com.sam")
            .value("true")
            .comment("sam")
            .orderedInsertion(false)
            .build()),
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
    void insertBeforeProperty() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("new.prop")
            .value("val")
            .insertMode(InsertMode.Before)
            .insertProperty("com.bea")
            .build()),
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
    void insertAfterProperty() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("new.prop")
            .value("val")
            .insertMode(InsertMode.After)
            .insertProperty("com.bea")
            .build()),
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
    void insertBeforePropertyWithPrecedingComment() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("new.prop")
            .value("val")
            .insertMode(InsertMode.Before)
            .insertProperty("com.bea")
            .build()),
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
    void insertBeforePropertyWithNewPropertyComment() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("new.prop")
            .value("val")
            .comment("new property comment")
            .insertMode(InsertMode.Before)
            .insertProperty("com.bea")
            .build()),
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
    void insertAfterPropertyWithNewPropertyComment() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("new.prop")
            .value("val")
            .comment("new property comment")
            .insertMode(InsertMode.After)
            .insertProperty("com.bea")
            .build()),
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
    void insertBeforePropertyNotFoundFallsBackToAlphabetical() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("com.sam")
            .value("true")
            .insertMode(InsertMode.Before)
            .insertProperty("nonexistent.key")
            .build()),
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
    void insertAfterPropertyNotFoundUnordered() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("com.sam")
            .value("true")
            .orderedInsertion(false)
            .insertMode(InsertMode.After)
            .insertProperty("nonexistent.key")
            .build()),
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
    void insertAfterLastEntry() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("zzz.last")
            .value("val")
            .insertMode(InsertMode.After)
            .insertProperty("com.zoe")
            .build()),
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
    void insertBeforeOverridesOrderedInsertion() {
        rewriteRun(
          spec -> spec.recipe(AddProperty.builder()
            .property("zzz.prop")
            .value("val")
            .orderedInsertion(true)
            .insertMode(InsertMode.Before)
            .insertProperty("com.amy")
            .build()),
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

    @Test
    void insertPropertyRequiredForBeforeMode() {
        AddProperty recipe = AddProperty.builder()
            .property("new.prop")
            .value("val")
            .insertMode(InsertMode.Before)
            .build();
        assertThat(recipe.validate().isInvalid()).isTrue();
    }

    @Test
    void insertPropertyRequiredForAfterMode() {
        AddProperty recipe = AddProperty.builder()
            .property("new.prop")
            .value("val")
            .insertMode(InsertMode.After)
            .build();
        assertThat(recipe.validate().isInvalid()).isTrue();
    }

    @Test
    void insertPropertyNotRequiredForLastMode() {
        AddProperty recipe = AddProperty.builder()
            .property("new.prop")
            .value("val")
            .insertMode(InsertMode.Last)
            .build();
        assertThat(recipe.validate().isValid()).isTrue();
    }

}
