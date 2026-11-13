/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.ruby.Assertions.ruby;

public class ModuleTest implements RewriteTest {

    @Test
    void module() {
        rewriteRun(
          ruby(
            """
              module Base64
                  DIGITS = '0123456789'
              end
              """,
            spec -> spec.afterRecipe(cu -> {
                Rb.Module module = (Rb.Module) cu.getStatements().get(0);
                assertThat(module.getName()).isInstanceOf(J.Identifier.class);
                assertThat(((J.Identifier) module.getName()).getSimpleName()).isEqualTo("Base64");
            })
          )
        );
    }

    @Test
    void nested() {
        rewriteRun(
          ruby(
            """
              module Api
                module V1
                end
              end
              """
          )
        );
    }

    /**
     * A compact name keeps every segment: collapsing `Api::V1` to `V1` is what breaks reconstructing
     * a Rails namespace from the tree.
     */
    @Test
    void compactName() {
        rewriteRun(
          ruby(
            """
              module Api::V1
              end
              """,
            spec -> spec.afterRecipe(cu -> {
                Rb.Module module = (Rb.Module) cu.getStatements().get(0);
                J.MemberReference name = (J.MemberReference) module.getName();
                assertThat(((J.Identifier) name.getContaining()).getSimpleName()).isEqualTo("Api");
                assertThat(name.getReference().getSimpleName()).isEqualTo("V1");
            })
          )
        );
    }

    @Test
    void deeplyCompactName() {
        rewriteRun(
          ruby(
            """
              module Api::V1::Admin
                class UsersController
                end
              end
              """
          )
        );
    }

    /**
     * A leading `::` roots the name at the top level, and is a member reference with nothing on its
     * left.
     */
    @Test
    void topLevelName() {
        rewriteRun(
          ruby(
            """
              module ::TopLevel
              end
              """,
            spec -> spec.afterRecipe(cu -> {
                Rb.Module module = (Rb.Module) cu.getStatements().get(0);
                J.MemberReference name = (J.MemberReference) module.getName();
                assertThat(name.getContaining()).isInstanceOf(J.Empty.class);
                assertThat(name.getReference().getSimpleName()).isEqualTo("TopLevel");
            })
          )
        );
    }
}
