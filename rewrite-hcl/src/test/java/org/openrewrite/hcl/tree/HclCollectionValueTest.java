/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class HclCollectionValueTest implements RewriteTest {

    @Test
    void tupleValue() {
        rewriteRun(
          hcl("a = [ 1, 2 ]")
        );
    }

    @Test
    void tupleValueNewline() {
        rewriteRun(
          hcl(
            """
              a = [
              1]
              """
          )
        );
    }

    @Test
    void objectValue() {
        rewriteRun(
          hcl(
            """
              a = { k1 = 1, k2 = 2 }
              b = { k1 = 1, k2 = 2, }
              c = { k1: 1, k2: 2 }
              d = { k1: 1, k2: 2, }
              """
          )
        );
    }

    @Test
    void emptyMapInTwoLines() {
        rewriteRun(
          hcl(
            """
              locals {
                known_weird_cases = {
                }
              }
              """
          )
        );
    }

    @Test
    void emptyArrayWithNewLine() {
        rewriteRun(
          hcl(
            """
              default = [
              ]
              """
          )
        );
    }
}
