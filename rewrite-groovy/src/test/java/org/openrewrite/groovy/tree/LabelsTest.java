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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

import static org.openrewrite.groovy.Assertions.groovy;

class LabelsTest implements RewriteTest {

    @Test
    void multipleLabelsBeforeIf() {
        rewriteRun(
          groovy(
            """
              setup : /* first */
              when: if (true) assert false
              """,
            spec -> spec.beforeRecipe(cu -> {
                assertThat(cu.getStatements().get(0)).isInstanceOf(J.Label.class);
                J.Label setup = (J.Label) cu.getStatements().get(0);
                assertThat(setup.getLabel().getSimpleName()).isEqualTo("setup");
                assertThat(setup.getStatement()).isInstanceOf(J.Label.class);
                J.Label when = (J.Label) setup.getStatement();
                assertThat(when.getLabel().getSimpleName()).isEqualTo("when");
                assertThat(when.getStatement()).isInstanceOf(J.If.class);
            })
          )
        );
    }

    @Test
    void labelsInIfBranches() {
        rewriteRun(
          groovy(
            """
              class T {
                  void test(boolean a, boolean b) {
                      setup: if (a) {
                          expect: if (b) assert true
                          else assert false
                      } else if (b) {
                          when: if (a) assert false
                      } else {
                          cleanup: if (b) {
                              assert true
                          } else {
                              assert false
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void labelBeforeIf() {
        rewriteRun(
          groovy(
            """
              setup: if (true) assert false
              """,
            spec -> spec.beforeRecipe(cu -> {
                assertThat(cu.getStatements().get(0)).isInstanceOf(J.Label.class);
                J.Label label = (J.Label) cu.getStatements().get(0);
                assertThat(label.getLabel().getSimpleName()).isEqualTo("setup");
                assertThat(label.getStatement()).isInstanceOf(J.If.class);
                assertThat(label.getStatement().getPrefix().getWhitespace()).isEqualTo(" ");
            })
          )
        );
    }

    @Test
    void singleLabel() {
        rewriteRun(
          //language=groovy
          groovy(
                """
            given: "hello"
            """
          )
        );
    }

    @Test
    void multiLabel() {
        rewriteRun(
          //language=groovy
          groovy(
                """
            def foo() {}
            label1:
            label2:
            label3:
            foo()
            """
          )
        );
    }

    @Test
    void labelBeforeAssert() {
        rewriteRun(
          //language=groovy
          groovy(
                """
            def foo() {
                then:
                assert 1 == 0
            }
            """
          )
        );
    }
}
