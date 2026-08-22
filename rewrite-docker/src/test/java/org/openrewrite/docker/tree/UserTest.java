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

    /// A quote the end of its line leaves open is a character of the name around it, as it is of any
    /// other argument, rather than the start of a string reaching into the instructions below.
    @Test
    void unpairedQuoteInAUserSpecification() {
        rewriteRun(
          docker(
            """
              FROM ubuntu:20.04
              USER 'nobody
              RUN echo hi
              """,
            spec -> spec.afterRecipe(doc -> {
                var user = (Docker.User) doc.getStages().getFirst().getInstructions().getFirst();
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("'nobody");
                assertThat(doc.getStages().getFirst().getInstructions()).hasSize(2);
            })
          )
        );
    }

    /// A continuation that splits a name rather than ending it stays in the name, as in
    /// `continuationBeforeTheSeparator`.
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

    /// Unlike the same shape in a `FROM`, Docker accepts this: `USER` reads everything after the
    /// keyword as one specification, so the whitespace the continuation leaves behind is part of the
    /// value, which is why the user holds it rather than the space around it.
    /// As in a `FROM`, only an unindented continuation leaves a specification Docker still reads the
    /// same way: it keeps the indent of the line that follows, and `USER app\<newline>  :group` reaches
    /// it as the user `app  :group`.
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
                assertThat(ArgumentContents.text(user.getUser())).isEqualTo("app");
                assertThat(ArgumentContents.text(user.getGroup())).isEqualTo("group");
            })
          )
        );
    }
}
