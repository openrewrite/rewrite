/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.docker.Assertions.docker;

class UserTest implements RewriteTest {

    @Test
    void userInstruction() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER nobody
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.Literal) user.getUser().getContents().getFirst()).getText()).isEqualTo("nobody");
                assertThat(user.getGroup()).isNull();
            })
          )
        );
    }

    @Test
    void userWithGroup() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER app:group
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.Literal) user.getUser().getContents().getFirst()).getText()).isEqualTo("app");
                assertThat(((Docker.Literal) user.getGroup().getContents().getFirst()).getText()).isEqualTo("group");
            })
          )
        );
    }

    @Test
    void onlyTheFirstColonSeparatesTheGroup() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER app:group:extra
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("app");
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("group:extra");
            })
          )
        );
    }

    /// A quoted specification is one lexical unit, as a quoted image reference is to FROM, so the parser
    /// does not look inside it for a separator. Docker itself strips the quotes before splitting, so it
    /// reads this as the group `group` of the user `app`.
    @Test
    void quotedSpecificationIsOneName() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER "app:group"
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                var name = (Docker.Literal) user.getUser().getContents().getFirst();
                assertThat(name.getText()).isEqualTo("app:group");
                assertThat(name.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.DOUBLE);
                assertThat(user.getGroup()).isNull();
            })
          )
        );
    }

    @Test
    void quotedGroupKeepsItsQuoteStyle() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER app:'group'
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("app");
                var group = (Docker.Literal) user.getGroup().getContents().getFirst();
                assertThat(group.getText()).isEqualTo("group");
                assertThat(group.getQuoteStyle()).isEqualTo(Docker.Literal.QuoteStyle.SINGLE);
            })
          )
        );
    }

    @Test
    void separatorWithoutAGroup() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER app:
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("app");
                assertThat(user.getGroup().getContents()).isEmpty();
            })
          )
        );
    }

    @Test
    void separatorWithoutAUser() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER :group
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(user.getUser().getContents()).isEmpty();
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("group");
            })
          )
        );
    }

    @Test
    void variableReferenceKeepsItsDefaultValue() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER ${APP_USER:-root}
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                var name = (Docker.EnvironmentVariable) user.getUser().getContents().getFirst();
                assertThat(name.getName()).isEqualTo("APP_USER:-root");
                assertThat(name.isBraced()).isTrue();
                assertThat(user.getGroup()).isNull();
            })
          )
        );
    }

    @Test
    void variableReferencesOnBothSidesOfTheSeparator() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER $UID:$GID
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(((Docker.EnvironmentVariable) user.getUser().getContents().getFirst()).getName()).isEqualTo("UID");
                assertThat(((Docker.EnvironmentVariable) user.getGroup().getContents().getFirst()).getName()).isEqualTo("GID");
            })
          )
        );
    }

    /// The continuation that ends the line is not part of the group. Lexing longest-match-first used to
    /// take it into the token before it, which left the newline in `getText()` and so in any match on it.
    @Test
    void continuationAfterTheGroup() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER root:group\\

              RUN echo done
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getFirst();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("root");
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("group");
            })
          )
        );
    }

    @Test
    void continuationAfterAUserWithNoGroup() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER root\\

              RUN echo done
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getFirst();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("root");
                assertThat(user.getGroup()).isNull();
            })
          )
        );
    }

    /// A backtick continues a line as a backslash does, since the lexer reads both without asking which
    /// one the `escape` directive names.
    @Test
    void backtickContinuationAfterTheGroup() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER root:group`

              RUN echo done
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getFirst();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("root");
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("group");
            })
          )
        );
    }

    /// Spaces and tabs may sit between the escape character and the newline it continues over, so the
    /// group ends before the escape character rather than before the newline.
    @Test
    void continuationPaddedWithSpacesAfterTheGroup() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER root:group\\  \s

              RUN echo done
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getFirst();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("root");
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("group");
            })
          )
        );
    }

    /// Where a continuation splits a name rather than ending it, the name is still one unbroken run of
    /// source, so it keeps the continuation the way `continuationBeforeTheSeparator` keeps it.
    @Test
    void continuationInsideTheGroup() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER root:gr\\
              oup
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("root");
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("gr\\\noup");
            })
          )
        );
    }

    /// An escape character that no newline follows is text, and stays in the name that holds it.
    @Test
    void escapeCharactersInsideANameAreText() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER ro\\ ot:gr`oup
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("ro\\ ot");
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("gr`oup");
            })
          )
        );
    }

    @Test
    void continuationBeforeTheSeparator() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER app\\
                :group
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getLast();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("app\\\n  ");
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("group");
            })
          )
        );
    }
}
