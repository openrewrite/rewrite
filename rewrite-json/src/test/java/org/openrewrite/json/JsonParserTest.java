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
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

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
}
