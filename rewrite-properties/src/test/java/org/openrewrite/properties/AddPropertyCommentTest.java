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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

class AddPropertyCommentTest implements RewriteTest {

    @Test
    void shouldAddCommentToFirstProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddPropertyComment(
            "management.metrics.enable.process.files",
            "myComment",
            false
          )),
          properties(
            """
              management.metrics.enable.process.files=true
              yyy=true
              """,
            """
              # myComment
              management.metrics.enable.process.files=true
              yyy=true
              """
          )
        );
    }

    @Test
    void shouldAddCommentToMiddleProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddPropertyComment(
            "management.metrics.enable.process.files",
            "myComment",
            false
          )),
          properties(
            """
              xxx=true
              management.metrics.enable.process.files=true
              yyy=true
              """,
            """
              xxx=true
              # myComment
              management.metrics.enable.process.files=true
              yyy=true
              """
          )
        );
    }

    @Test
    void shouldAcceptExistingComment() {
        rewriteRun(
          spec -> spec.recipe(new AddPropertyComment(
            "management.metrics.enable.process.files",
            "myComment",
            false
          )),
          properties(
            """
              # myComment
              management.metrics.enable.process.files=true
              """
          )
        );
    }

    @Test
    void shouldSkipNotExistingProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddPropertyComment(
            "xxx",
            "myComment",
            false
          )),
          properties(
            """
              yyy=true
              """
          )
        );
    }

    @Test
    void shouldCommentOutFirstProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddPropertyComment(
            "management.metrics.enable.process.files",
            "myComment",
            true
          )),
          properties(
            """
              management.metrics.enable.process.files=true
              yyy=true
              """,
            """
              # myComment
              # management.metrics.enable.process.files=true
              yyy=true
              """
          )
        );
    }

    @Test
    void shouldCommentOutMiddleProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddPropertyComment(
            "management.metrics.enable.process.files",
            "myComment",
            true
          )),
          properties(
            """
              xxx=true
              management.metrics.enable.process.files=true
              yyy=true
              """,
            """
              xxx=true
              # myComment
              # management.metrics.enable.process.files=true
              yyy=true
              """
          )
        );
    }

    @Test
    void shouldAcceptExistingCommentAndCommentOutProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddPropertyComment(
            "management.metrics.enable.process.files",
            "myComment",
            true
          )),
          properties(
            """
              # myComment
              management.metrics.enable.process.files=true
              """,
            """
              # myComment
              # management.metrics.enable.process.files=true
              """
          )
        );
    }

    @Test
    void shouldASkipCommentOutProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddPropertyComment(
            "management.metrics.enable.process.files",
            "myComment",
            true
          )),
          properties(
            """
              # myComment
              # management.metrics.enable.process.files=true
              """
          )
        );
    }
}
