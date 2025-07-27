/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class TypeAliasTest implements RewriteTest {

    @Test
    void typeAlias() {
        rewriteRun(
          kotlin(
            """
              class Test
              typealias TestAlias = Test
              val a : TestAlias = Test ( )
              """
          )
        );
    }

    @Test
    void parameterizedTypeAlias() {
        rewriteRun(
          kotlin(
            """
              class Test < T >
              
              typealias OldAlias  <   T    >     = Test < T >
              val a : OldAlias < String > = Test < String> ( )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/300")
    @Test
    void typeAliasForFunctionType() {
        rewriteRun(
          kotlin(
            """
              typealias Action = (String) -> Unit
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/375")
    @Test
    void typeAliasWithModifier() {
        rewriteRun(
          kotlin(
            """
              internal  typealias   Action = (String) -> Unit
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/444")
    @Test
    void semiColonOnAlias() {
        rewriteRun(
          kotlin(
            """
              typealias A = Int ;
              """
          )
        );
    }

}
