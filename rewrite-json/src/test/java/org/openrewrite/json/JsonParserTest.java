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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;

@SuppressWarnings({"JsonStandardCompliance", "JsonDuplicatePropertyKeys"})
class JsonParserTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JsonParser.builder())
          .recipe(Recipe.noop());
    }

    @Test
    void parseJsonDocument() {
        rewriteRun(
          json(
            """
            {
              // comments
              unquoted: 'and you can quote me on that',
              singleQuotes: 'I can use "double quotes" here',
              hexadecimal: 0xdecaf,
              leadingDecimalPoint: .8675309, andTrailing: 8675309.,
              positiveSign: +1,
              trailingComma: 'in objects', andIn: ['arrays',],
              "backwardsCompatible": "with JSON",
            }
            //
            """
          )
        );
    }

    @Test
    void stringLiteral() {
        rewriteRun(
          json("'hello world'")
        );
    }

    @Test
    void booleanLiteral() {
        rewriteRun(
          json("true")
        );
    }

    @Test
    void doubleLiteralExpSigned() {
        rewriteRun(
          json("-1e3")
        );
    }

    @Test
    void doubleLiteralExpSignedUpperCase() {
        rewriteRun(
          json("1E-3")
        );
    }

    @Test
    void bigInteger() {
        rewriteRun(
          json("-10000000000000000999")
        );
    }

    @Test
    void array() {
        rewriteRun(
          json("[ 1 , 2 , 3 , ]")
        );
    }

    @Test
    void object() {
        rewriteRun(
          json(
            """
            {
                key: "value",
                "key": 1,
            }
            """
          )
        );
    }

    @Test
    void comments() {
        rewriteRun(
          json(
            """
            // test
            {
                /* test */
                key: "value",
                // test
                "key": 1,
            }
            """
          )
        );
    }

    @Test
    void multilineCommentWithUrl() {
        rewriteRun(
          json(
            """
            {
              /* https://foo.bar */
            }
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1145")
    @Test
    void longValue() {
        rewriteRun(
          json(
            """
            {
                "timestamp": 1577000812973
            }
            """
          )
        );
    }

    @Test
    void nullValue() {
        rewriteRun(
          json(
            """
            null
            """,
            spec -> spec.afterRecipe(doc -> {
                JsonValue value = doc.getValue();
                assertThat(value).isInstanceOf(Json.Literal.class);
                assertThat(((Json.Literal) value).getSource()).isEqualTo("null");
                //noinspection DataFlowIssue
                assertThat(((Json.Literal) value).getValue()).isNull();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2511")
    @Test
    void empty() {
        rewriteRun(
          json("")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3582")
    @Test
    void multiBytesUnicode() {
        rewriteRun(
          json(
            """
              {
                "🤖"    : "robot",
                "robot" : "🤖",
                "நடித்த" : 3 /* 🇩🇪 */
              }
              """
          )
        );
    }

    @Test
    void unicodeEscapes() {
        rewriteRun(
          json(
            """
              {
                "nul": "\\u0000",
                "reverse-solidus": "\\u005c",
              }
              """
          )
        );
    }

    @Test
    void accentedCharactersInValues() {
        rewriteRun(
          json(
            """
              {
                "title_history": "Histoire",
                "keyword_com": "Mot-cl\u00e9 / com...",
                "keyword_doc": "Mot-cl\u00e9 / doc...",
                "title_period": "P\u00e9riode"
              }
              """
          )
        );
    }

    @Test
    void doubleAsteriskInStringValue() {
        rewriteRun(
          json(
            """
              {
                "name": "The existing CI name**",
                "knowledge": "KBID"
              }
              """
          )
        );
    }

    @Test
    void noTrailingNewline() {
        rewriteRun(
          json("{\"a\": 1}")
        );
    }

    @Test
    void invalidJsonProducesParseError() {
        ExecutionContext ctx = new InMemoryExecutionContext(e -> {});
        List<SourceFile> results = JsonParser.builder().build()
          .parse(ctx, "{\"offset\": %s, \"limit\": %s}")
          .toList();
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isInstanceOf(ParseError.class);
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6631")
    @Test
    void stringWithDoubleAsterisk() {
        rewriteRun(
          json(
            """
              {"pattern": "**/*.java"}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6631")
    @Test
    void stringWithDelimitersInside() {
        rewriteRun(
          json(
            """
              {
                  "message": "Hello: World, how are {you}?",
                  "array": ["item: one, two"]
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6631")
    @Test
    void stringWithCommentLikeSequences() {
        rewriteRun(
          json(
            """
              {
                  "url": "https://example.com/path",
                  "code": "/* not a comment */",
                  "regex": "// pattern"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6631")
    @Test
    void stringWithEscapedQuotes() {
        rewriteRun(
          json(
            """
              {"nested": "He said \\"Hello\\""}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6631")
    @Test
    void stringWithPrintfAndTemplatesInsideStrings() {
        rewriteRun(
          json(
            """
              {
                  "printf": "Hello %s, count: %d",
                  "freemarker": "<#if x>value</#if>"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6631")
    @Test
    void singleQuotedStringsWithSpecialChars() {
        rewriteRun(
          json(
            """
              {
                  'pattern': '**/*.java',
                  'url': 'https://example.com'
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6631")
    @Test
    void complexNestedWithSpecialStrings() {
        rewriteRun(
          json(
            """
              {
                  "config": {
                      "patterns": ["**/*.java", "src/**/*.ts"],
                      "message": "Build: { status: 'ok' }"
                  }
              }
              """
          )
        );
    }
}
