/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.javascript.tree;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javaScript;

@SuppressWarnings({"JSUnresolvedVariable", "JSUnusedLocalSymbols"})
class TupleTest implements RewriteTest {

    @Test
    void emptyTuple() {
        rewriteRun(
          javaScript(
            """
              let tuple : [ ]
              """
          )
        );
    }

    @Test
    void tuple() {
        rewriteRun(
          javaScript(
            """
              let tuple : [ number , boolean ] = [ 1 , true ]
              """
          )
        );
    }

    @ExpectedToFail("Requires NamedTupleMember.")
    @Test
    void namedTupleMember() {
        rewriteRun(
          javaScript(
            """
              type NewLocation = [lat: number, long: number]
              """
          )
        );
    }

    @Test
    void spreadOperators() {
        rewriteRun(
          javaScript(
            """
              function concat(arr1, arr2) {
                  return [...arr1, ...arr2]
              }
              """
          )
        );
    }

    @ExpectedToFail("Requires ArrayBindingPattern.")
    @Test
    void arrayBindingPattern() {
        rewriteRun(
          javaScript(
            """
              function tail(arg) {
                  const [_, ...result] = arg;
                  return result;
              }
              """
          )
        );
    }

    @Test
    void trailingCommas() {
        rewriteRun(
          javaScript(
            """
              let input : [  number , boolean , ] = [ 1 , true , ]
              """
          )
        );
    }
}
