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
package org.openrewrite.yaml.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class DocumentTest implements RewriteTest {

    @Test
    void explicitStart() {
        rewriteRun(
          yaml(
            """
              ---
              type: specs.openrewrite.org/v1beta/visitor
              ---
              type: specs.openrewrite.org/v1beta/recipe
              """,
            spec -> spec.afterRecipe(y -> {
                assertThat(y.getDocuments()).hasSize(2);
                assertThat(y.getDocuments().get(0).isExplicit()).isTrue();
            })
          )
        );
    }

    @Test
    void explicitEnd() {
        rewriteRun(
          yaml(
            """
              type: specs.openrewrite.org/v1beta/visitor
              ...
              ---
              type: specs.openrewrite.org/v1beta/recipe
              
              ...
              """,
            spec -> spec.afterRecipe(y -> {
                assertThat(y.getDocuments()).hasSize(2);
                assertThat(y.getDocuments().get(0).getEnd().isExplicit()).isTrue();
            })
          )
        );
    }

    @Test
    void implicitStart() {
        rewriteRun(
          yaml(
            "type: specs.openrewrite.org/v1beta/visitor",
            spec -> spec.afterRecipe(y -> {
                assertThat(y.getDocuments()).hasSize(1);
                assertThat(y.getDocuments().get(0).isExplicit()).isFalse();
            })
          )
        );
    }

    @Test
    void implicitEnd() {
        rewriteRun(
          yaml(
            "type: specs.openrewrite.org/v1beta/visitor",
            spec -> spec.afterRecipe(y -> {
                assertThat(y.getDocuments()).hasSize(1);
                assertThat(y.getDocuments().get(0).getEnd().isExplicit()).isFalse();
            })
          )
        );
    }
}
