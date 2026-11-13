/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.json;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.json.tree.Comment;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Pins where a member's leading whitespace lives; mirrored by
 * {@code rewrite-javascript/rewrite/test/json/member-prefix.test.ts} so both parsers build the same LST.
 */
class JsonMemberPrefixTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JsonParser.builder());
    }

    private static List<Json.Member> members(Json.Document doc) {
        return ((Json.JsonObject) doc.getValue()).getMembers().stream()
          .map(Json.Member.class::cast)
          .toList();
    }

    @Test
    void leadingWhitespaceBelongsToTheMemberNotItsKey() {
        rewriteRun(
          json(
            """
              {
                "_version": "1.65.0"
              }
              """,
            spec -> spec.afterRecipe(doc -> {
                Json.Member member = members(doc).get(0);
                assertThat(member.getPrefix().getWhitespace()).isEqualTo("\n  ");
                assertThat(member.getKey().getPrefix().getWhitespace()).isEmpty();
            })
          )
        );
    }

    @Test
    void everyMemberInAMultiMemberObject() {
        rewriteRun(
          json(
            """
              {
                "a": 1,
                "b": 2
              }
              """,
            spec -> spec.afterRecipe(doc -> {
                for (Json.Member member : members(doc)) {
                    assertThat(member.getPrefix().getWhitespace()).isEqualTo("\n  ");
                    assertThat(member.getKey().getPrefix().getWhitespace()).isEmpty();
                }
            })
          )
        );
    }

    @Test
    void nestedObjectMembers() {
        rewriteRun(
          json(
            """
              {
                "libs": {
                  "sap.makit": {}
                }
              }
              """,
            spec -> spec.afterRecipe(doc -> {
                Json.Member outer = members(doc).get(0);
                Json.Member inner = (Json.Member) ((Json.JsonObject) outer.getValue()).getMembers().get(0);
                assertThat(outer.getPrefix().getWhitespace()).isEqualTo("\n  ");
                assertThat(inner.getPrefix().getWhitespace()).isEqualTo("\n    ");
                assertThat(inner.getKey().getPrefix().getWhitespace()).isEmpty();
            })
          )
        );
    }

    @Test
    void commentsPrecedingAMemberBelongToTheMember() {
        rewriteRun(
          json(
            """
              {
                // a comment
                "a": 1
              }
              """,
            spec -> spec.afterRecipe(doc -> {
                Json.Member member = members(doc).get(0);
                assertThat(member.getPrefix().getWhitespace()).isEqualTo("\n  ");
                assertThat(member.getPrefix().getComments()).extracting(Comment::getText).containsExactly(" a comment");
                assertThat(member.getKey().getPrefix().getComments()).isEmpty();
            })
          )
        );
    }

    @Test
    void markingAMemberRendersTheMarkerAgainstTheKey() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JsonIsoVisitor<ExecutionContext>() {
              @Override
              public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                  return SearchResult.found(super.visitMember(member, ctx));
              }
          })),
          json(
            """
              {
                "libs": {
                  "sap.makit": {}
                }
              }
              """,
            """
              {
                /*~~>*/"libs": {
                  /*~~>*/"sap.makit": {}
                }
              }
              """
          )
        );
    }
}
