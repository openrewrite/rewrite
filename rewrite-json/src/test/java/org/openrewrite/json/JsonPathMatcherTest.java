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
package org.openrewrite.json;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.Space;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonPathMatcherTest {

    private final List<String> simple = List.of(
      //language=json5
      """
          {
            "root": {
              "literal": "$.root.literal",
              "object": {
                "literal": "$.root.object.literal",
                "list": [{
                  "literal": "$.root.object.list[0]"
                }]
              },
              "list": [{
                "literal": "$.root.list[0]"
              }]
            }
          }
        """
    );

    private final List<String> listOfScalars = List.of(
      //language=json5
      """
            {
              "list": [
                "item 1",
                "item 2",
                "item 3"
              ]
            }
        """
    );

    private final List<String> sliceList = List.of(
      //language=json5
      """
            {
              "list": [
                {
                  "item1": "index0",
                  "property": "property"
                },
                {
                  "item2": "index1",
                  "property": "property"
                },
                {
                  "item3": "index2",
                  "property": "property"
                }
              ]
            }
        """
    );

    private final List<String> complex = List.of(
      //language=json5
      """
        {
          "literal": "$.literal",
          "object": {
            "literal": "$.object.literal",
            "object": {
              "literal": "$.object.object.literal"
            },
            "list": [{
              "literal": "$.object.list[0].literal"
            }]
          },
          "literals": [
            "$.literal[0]"
          ],
          "objects": {
            "literal": "$.objects.literal",
            "object": {
              "literal": "$.objects.object.literal",
              "object": {
                "literal": "$.objects.object.object.literal"
              },
              "list": [{
                "literal": "$.objects.object.list[0].literal"
              }]
            }
          },
          "lists": [{
            "list": [{
              "object": {
                "literal": "$.lists[0].list[0].object.literal",
                "object": {
                  "literal": "$.lists[0].list[0].object.object.literal"
                },
                "list": [{
                  "literal": "$.lists[0].list[0].object.list[0].literal"
                }]
              }
            }]
          }]
        }
        """
    );

    @Test
    void doesNotMatchMissingProperty() {
        assertNotMatched(
          "$.none",
          simple
        );
    }

    @Test
    void findScopeOfObject() {
        assertMatched(
          "$.root.object[?(@.literal == '$.root.object.literal')]",
          simple,
          List.of(
            """
                  "object": {
                    "literal": "$.root.object.literal",
                    "list": [{
                      "literal": "$.root.object.list[0]"
                    }]
                  }
              """)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1514")
    @Test
    void findLiteralInObject() {
        assertMatched(
          "$.root.object[?(@.literal == '$.root.object.literal')].literal",
          simple,
          List.of("\"literal\": \"$.root.object.literal\"")
        );
    }

    @Test
    void wildcardAtRoot() {
        assertMatched(
          "$.*",
          simple,
          List.of(
            """
                  "root": {
                    "literal": "$.root.literal",
                    "object": {
                      "literal": "$.root.object.literal",
                      "list": [{
                        "literal": "$.root.object.list[0]"
                      }]
                    },
                    "list": [{
                      "literal": "$.root.list[0]"
                    }]
                  }
              """
          )
        );
    }

    @Test
    void multipleWildcards() {
        assertMatched(
          "$.*.*",
          simple,
          List.of(
            "\"literal\": \"$.root.literal\"",
            """
                  "object": {
                    "literal": "$.root.object.literal",
                    "list": [{
                      "literal": "$.root.object.list[0]"
                    }]
                  }
              """,
            """
                  "list": [{
                    "literal": "$.root.list[0]"
                  }]
              """)
        );
    }

    @Test
    void allPropertiesInKeyFromRoot() {
        assertMatched(
          // This produces two false positives from $.literal and $.list.literal.
          "$.*.literal",
          simple,
          List.of("\"literal\": \"$.root.literal\"")
        );
    }

    @Test
    void matchObjectAtRoot() {
        assertMatched(
          "$.root.object",
          simple,
          List.of(
            """
                  "object": {
                    "literal": "$.root.object.literal",
                    "list": [{
                      "literal": "$.root.object.list[0]"
                    }]
                  }
              """)
        );
    }

    @Test
    void matchLiteral() {
        assertMatched(
          "$.root.literal",
          simple,
          List.of("\"literal\": \"$.root.literal\"")
        );
    }

    @Test
    void dotOperatorIntoObject() {
        assertMatched(
          "$.root.object.literal",
          simple,
          List.of("\"literal\": \"$.root.object.literal\"")
        );
    }

    @Test
    void dotOperatorByBracketName() {
        assertMatched(
          "$.['root'].['object'].['literal']",
          simple,
          List.of("\"literal\": \"$.root.object.literal\"")
        );
    }

    @Test
    void bracketNameAtRoot() {
        assertMatched(
          "['root'].['object']",
          simple,
          List.of(
            """
                  "object": {
                    "literal": "$.root.object.literal",
                    "list": [{
                      "literal": "$.root.object.list[0]"
                    }]
                  }
              """)
        );
    }

    @Test
    void relativePath() {
        assertMatched(
          ".literal",
          simple,
          List.of(
            "\"literal\": \"$.root.literal\"",
            "\"literal\": \"$.root.object.literal\"",
            "\"literal\": \"$.root.object.list[0]\"",
            "\"literal\": \"$.root.list[0]\"")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1599")
    @Test
    void containsInList() {
        assertMatched(
          "$.root.object[?(@.literal contains 'object')].list",
          simple,
          List.of(
            """
                  "list": [{
                    "literal": "$.root.object.list[0]"
                  }]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1599")
    @Test
    void containsInLhsString() {
        assertMatched(
          "$.root.object[?(@.literal contains 'literal')].list",
          simple,
          List.of(
            """
                  "list": [{
                    "literal": "$.root.object.list[0]"
                  }]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1599")
    @Test
    void containsInRhsString() {
        assertMatched(
          "$.root.object[?('$.root.object.literal' contains @.literal)].list",
          simple,
          List.of(
            """
                  "list": [{
                    "literal": "$.root.object.list[0]"
                  }]
              """)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1590")
    @Test
    void returnScopeOfMemberOnBinaryEx() {
        assertMatched(
          "$.root.object[?($.root.literal == '$.root.literal')].list",
          simple,
          List.of(
            """
                  "list": [{
                    "literal": "$.root.object.list[0]"
                  }]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1599")
    @Test
    void matchByConditionAndItemInList() {
        assertMatched(
          "$.root.object[?($.root.literal == '$.root.literal' && @.literal contains 'literal')].list",
          simple,
          List.of(
            """
                  "list": [{
                    "literal": "$.root.object.list[0]"
                  }]
              """)
        );
    }

    @Test
    void recurseToMatchProperties() {
        assertMatched(
          "$..object.literal",
          complex,
          List.of(
            "\"literal\": \"$.object.literal\"",
            "\"literal\": \"$.object.object.literal\"",
            "\"literal\": \"$.objects.object.literal\"",
            "\"literal\": \"$.objects.object.object.literal\"",
            "\"literal\": \"$.lists[0].list[0].object.literal\"",
            "\"literal\": \"$.lists[0].list[0].object.object.literal\""
          )
        );
    }

    @Test
    void recurseFromScopeOfObject() {
        assertMatched(
          "$.object..literal",
          complex,
          List.of(
            "\"literal\": \"$.object.literal\"",
            "\"literal\": \"$.object.object.literal\"",
            "\"literal\": \"$.object.list[0].literal\"")
        );
    }

    @Test
    void firstNElements() {
        assertMatched(
          "$.list[:1]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item1": "index0",
                    "property": "property"
                  }
              """)
        );
    }

    @Test
    void lastNElements() {
        assertMatched(
          "$.list[-1:]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item3": "index2",
                    "property": "property"
                  }
              """)
        );
    }

    @Test
    void fromStartToEndPos() {
        assertMatched(
          "$.list[0:1]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item1": "index0",
                    "property": "property"
                  }
              """, """
                  {
                    "item2": "index1",
                    "property": "property"
                  }
              """)
        );
    }

    @Test
    void allElementsFromStartPos() {
        assertMatched(
          "$.list[1:]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item2": "index1",
                    "property": "property"
                  }
              """, """
                  {
                    "item3": "index2",
                    "property": "property"
                  }
              """)
        );
    }

    @Test
    void allElements() {
        assertMatched(
          "$.list[*]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item1": "index0",
                    "property": "property"
                  }
              """, """
                  {
                    "item2": "index1",
                    "property": "property"
                  }
              """, """
                  {
                    "item3": "index2",
                    "property": "property"
                  }
              """)
        );
    }

    @Test
    void bracketOperatorByNames() {
        assertMatched(
          "$.root['literal', 'object']",
          simple,
          List.of(
            "\"literal\": \"$.root.literal\"",
            """
                  "object": {
                    "literal": "$.root.object.literal",
                    "list": [{
                      "literal": "$.root.object.list[0]"
                    }]
                  }
              """)
        );
    }

    @Test
    void doesNotMatchIndex() {
        assertNotMatched(
          "$.list[4]",
          sliceList
        );
    }

    @Test
    void bracketOperatorByIndexes() {
        assertMatched(
          "$.list[0, 1]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item1": "index0",
                    "property": "property"
                  }
              """, """
                  {
                    "item2": "index1",
                    "property": "property"
                  }
              """)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    void doesNotMatchWrongValue() {
        assertNotMatched(
          "$..list[?(@.literal == 'no-match')].literal",
          complex
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    void filterOnPropertyWithEquality() {
        assertMatched(
          "$..list[?(@.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
          complex,
          List.of("\"literal\": \"$.lists[0].list[0].object.list[0].literal\"")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1063")
    @Test
    void doesNotMatchWrongAnd() {
        assertNotMatched(
          "$..list[?(@.literal == 'no-match' && @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
          complex
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1063")
    @Test
    void filterOnPropertyWithAnd() {
        assertMatched(
          "$..list[?($.literal == '$.literal' && @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
          complex,
          List.of("\"literal\": \"$.lists[0].list[0].object.list[0].literal\"")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1063")
    @Test
    void doesNotMatchWrongOr() {
        assertNotMatched(
          "$..list[?(@.literal == 'no-match-1' || @.literal == 'no-match-2')].literal",
          complex
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1063")
    @Test
    void filterOnPropertyWithOr() {
        assertMatched(
          "$..list[?(@.literal == 'no-match' || @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
          complex,
          List.of("\"literal\": \"$.lists[0].list[0].object.list[0].literal\"")
        );
    }

    @Test
    void unaryExpressionByAt() {
        assertMatched(
          "$.list[?(@ == 'item 1')]",
          listOfScalars,
          List.of("\"item 1\"")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1607")
    @Test
    void unaryExpressionByBracketOperator() {
        assertMatched(
          "$.list[?(@.['item1'])]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item1": "index0",
                    "property": "property"
                  }
              """
          )
        );
    }

    @Test
    void unaryExpressionByScope() {
        assertMatched(
          "$.list[?(@.property)]",
          sliceList,
          //language=json5
          List.of(
            """
                  {
                    "item1": "index0",
                    "property": "property"
                  }
              """,
            """
                  {
                    "item2": "index1",
                    "property": "property"
                  }
              """,
            """
                  {
                    "item3": "index2",
                    "property": "property"
                  }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1567")
    @Test
    void unaryExpressionByValue() {
        assertMatched(
          "$.list[?(@.property == 'property')]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item1": "index0",
                    "property": "property"
                  }
              """,
            """
                  {
                    "item2": "index1",
                    "property": "property"
                  }
              """,
            """
                  {
                    "item3": "index2",
                    "property": "property"
                  }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    void filterOnPropertyWithMatches() {
        assertMatched(
          "$..list[?(@.literal =~ '.*objects.*')].literal",
          complex,
          List.of("\"literal\": \"$.objects.object.list[0].literal\"")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    void recursiveDecentToFilter() {
        assertMatched(
          "$..[?(@.literal =~ '.*objects.object.literal.*')].literal",
          complex,
          List.of("\"literal\": \"$.objects.object.literal\"")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    void nestedSequences() {
        assertMatched(
          "$..list[*].[?(@.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
          complex,
          List.of("\"literal\": \"$.lists[0].list[0].object.list[0].literal\"")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    void matchesLiteralInListByFilterCondition() {
        assertMatched(
          "$.list.*[?(@.item1 == 'index0')].item1",
          sliceList,
          List.of("\"item1\": \"index0\"")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1419")
    @Test
    void matchesListWithByFilterConditionInList() {
        assertMatched(
          "$.list.*[?(@.item3 == 'index2')]",
          sliceList,
          List.of(
            //language=json5
            """
                  {
                    "item3": "index2",
                    "property": "property"
                  }
              """
          )
        );
    }

    @Test
    void returnResultsWithVisitDocument() {
        var matcher = new JsonPathMatcher("$.root.literal");
        assertThat(new JsonVisitor<List<Json>>() {
            @Override
            public Json visitDocument(Json.Document document, List<Json> p) {
                if (matcher.find(getCursor()).isPresent()) {
                    p.add(document);
                }
                return super.visitDocument(document, p);
            }
        }
          .reduce(JsonParser.builder().build()
            .parse(simple.toArray(new String[0]))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Could not parse as JSON")), new ArrayList<>()))
          .hasSize(1);
    }

    private void assertNotMatched(String jsonPath, List<String> before) {
        var results = visit(before,
          jsonPath, false);
        assertThat(results).hasSize(0);
    }

    private void assertMatched(String jsonPath, List<String> before, List<String> after) {
        assertMatched(before, after, jsonPath, false);
    }

    private void assertMatched(List<String> before, List<String> after, String jsonPath,
                               @SuppressWarnings("SameParameterValue") boolean printMatches) {
        var results = visit(before, jsonPath, printMatches);
        assertThat(results).hasSize(after.size());
        for (int i = 0; i < results.size(); i++) {
            assertThat(StringUtils.trimIndent(results.get(i)))
              .isEqualTo(StringUtils.trimIndent(after.get(i)));
        }
    }

    private List<String> visit(List<String> before, String jsonPath, boolean printMatches) {
        var matcher = new JsonPathMatcher(jsonPath);
        return new JsonVisitor<List<String>>() {
            @Override
            public Json visitMember(Json.Member member, List<String> p) {
                var e = super.visitMember(member, p);
                if (matcher.matches(getCursor())) {
                    var match = e.printTrimmed(getCursor().getParentOrThrow());
                    if (printMatches) {
                        System.out.println("matched in visitMember");
                        System.out.println(match);
                        System.out.println();
                    }
                    p.add(match);
                }
                return e;
            }

            @Override
            public Json visitObject(Json.JsonObject obj, List<String> p) {
                var e = super.visitObject(obj, p);
                if (matcher.matches(getCursor())) {
                    var match = e.printTrimmed(getCursor().getParentOrThrow());
                    if (printMatches) {
                        System.out.println("matched in visitObject");
                        System.out.println(match);
                        System.out.println();
                    }
                    p.add(match);
                }
                return e;
            }

            @Override
            public Json visitArray(Json.Array array, List<String> p) {
                var e = super.visitArray(array, p);
                if (matcher.matches(getCursor())) {
                    var j = e.withPrefix(Space.EMPTY);
                    var match = j.printTrimmed(getCursor().getParentOrThrow());
                    if (printMatches) {
                        System.out.println("matched in visitArray");
                        System.out.println(match);
                        System.out.println();
                    }
                    p.add(match);
                }
                return e;
            }

            @Override
            public Json visitLiteral(Json.Literal literal, List<String> p) {
                var e = super.visitLiteral(literal, p);
                if (matcher.matches(getCursor())) {
                    var j = e.withPrefix(Space.EMPTY);
                    var match = j.printTrimmed(getCursor().getParentOrThrow());
                    if (printMatches) {
                        System.out.println("matched in visitLiteral");
                        System.out.println(match);
                        System.out.println();
                    }
                    p.add(match);
                }
                return e;
            }
        }
          .reduce(new JsonParser().parse(before.toArray(new String[0]))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Could not parse as XML")), new ArrayList<>());
    }
}
