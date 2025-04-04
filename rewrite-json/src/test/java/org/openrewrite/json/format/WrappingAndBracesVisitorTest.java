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
package org.openrewrite.json.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.json.style.TabsAndIndentsStyle;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;
import static org.openrewrite.json.style.WrappingAndBracesStyle.OBJECTS_WRAP_ARRAYS_DONT;

class WrappingAndBracesVisitorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new WrappingAndBraces());
    }

    @DocumentExample
    @Test
    void simple() {
        rewriteRun(
          json(
            """
            {"a": 3,"b": 5}
            """,
            """
            {
            "a": 3,
            "b": 5
            }
            """
          )
        );
    }

    @Test
    void keepWrongIndentsIfTheLinesAreFine() {
        rewriteRun(
          json(
            """
            {
               "x": "x",
                   "key": {
               "a": "b"
                   }
            }
            """
          )
        );
    }

    @Test
    void splitBracesToNewLines() {
        rewriteRun(
          json(
            """
            {
               "x": "x",
               "key": {"a": "b"}}
            """,
            """
            {
               "x": "x",
               "key": {
            "a": "b"
               }
            }
            """
          )
        );
    }

    @Test
    void wrapArrays() {
        rewriteRun(
          json(
            """
            {
               "one": [1,
               11,
               111
               ],
               "two": [2, 22, 222],
               "three": [
                3,
                33,
                333
               ]
            }
            """,
            """
            {
               "one": [
            1,
               11,
               111
               ],
               "two": [
            2,
             22,
             222
               ],
               "three": [
                3,
                33,
                333
               ]
            }
            """
          )
        );
    }



    @Test
    void oneLineArrays() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new WrappingAndBraces(OBJECTS_WRAP_ARRAYS_DONT, TabsAndIndentsStyle.DEFAULT, GeneralFormatStyle.DEFAULT)),
          json(
            """
            {
               "one": [1, 11, 111        ],
               "two": [2, 22         , 222],
               "three": [3         , 33, 333]
            }
            """,
            """
            {
               "one": [1, 11, 111],
               "two": [2, 22, 222],
               "three": [3, 33, 333]
            }
            """
          )
        );
    }

}
