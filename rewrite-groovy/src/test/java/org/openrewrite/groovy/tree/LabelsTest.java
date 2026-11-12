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
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

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

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void aLabelOnASwitch() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                switch (x) {
                  case 1:
                    break
                }
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void aLabelOnAWhile() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                while (x) {
                  b()
                }
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void aLabelOnADoWhile() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                do {
                  b()
                } while (x)
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void aLabelOnATry() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                try {
                  b()
                } catch (e) {}
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void aLabelOnASynchronizedBlock() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                synchronized (x) {
                  b()
                }
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void aLabelOnAReturn() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                return 1
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void aLabelOnAThrow() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                throw new E()
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void aLabelOnABlock() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                {
                  b()
                }
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void twoLabelsAndALabeledBreak() {
        rewriteRun(
          groovy(
            """
              def m() {
                given:
                def a = 1
                when:
                b: switch (x) {
                  default:
                    break b
                }
                then:
                a == 1
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/9873")
    @Test
    void labelsOnAForAndAnIfParseAlready() {
        rewriteRun(
          groovy(
            """
              def m() {
                when:
                for (i in a) {
                  b()
                }
                then:
                if (x) {
                  b()
                }
              }
              """,
            spec -> spec.afterRecipe(LabelsTest::nothingKeptAsWhitespace)
          )
        );
    }

    /**
     * Printing back alone can't tell whether source text was parsed: text the parser never visits is kept as
     * whitespace and still prints. Each source here must leave none.
     */
    private static void nothingKeptAsWhitespace(SourceFile sf) {
        List<String> kept = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer p) {
                if (!space.getWhitespace().isBlank()) {
                    kept.add(loc + ": " + space.getWhitespace().strip());
                }
                for (Comment c : space.getComments()) {
                    if (!c.getSuffix().isBlank()) {
                        kept.add(loc + ": " + c.getSuffix().strip());
                    }
                }
                return space;
            }
        }.visit(sf, 0);
        assertThat(kept).as("source text kept as whitespace").isEmpty();
    }
}
