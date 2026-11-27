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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class FieldAccessTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"\n .", "\n ?.", "\n *.", "\r\n .", "\r\n ?.", "\r\n *."})
    void propertyAccessAfterLineContinuation(String continuation) {
        // given
        String source = "foo \\" + continuation + "bar";

        // when
        SourceFile parsed = GroovyParser.builder().build().parse(source).findFirst().orElseThrow();

        // then
        assertThat(parsed).isInstanceOf(G.CompilationUnit.class);
        assertThat(parsed.printAll()).isEqualTo(source);
        G.CompilationUnit cu = (G.CompilationUnit) parsed;
        assertThat(cu.getStatements()).singleElement().isInstanceOfSatisfying(J.FieldAccess.class,
          access -> assertThat(access.getSimpleName()).isEqualTo("bar"));
    }

    @Test
    void starAccess() {
        rewriteRun(
          groovy(
            """
              all*.exclude group: 'com.netflix.archaius', module: 'archaius-core'
              """
          )
        );
    }

    @Test
    void starAccessInsideParenthesesFollowedByMethodCall() {
        rewriteRun(
          groovy(
            """
              def updates = (push*.remoteUpdates).flatten()
              """
          )
        );
    }

    @Test
    void fieldAccess() {
        rewriteRun(
          groovy(
            """
              class Test {
                  public Test field = new Test()
                  Test b = new Test() . field . field
              }
              """
          )
        );
    }

    @Test
    void nullSafeDereference() {
        rewriteRun(
          groovy(
            """
              class Test {
                  Integer n
              }
              Test t = new Test()
              t?.n
              """
          )
        );
    }

    @Test
    void dynamicPropertyNameWithGString() {
        rewriteRun(
          groovy(
            """
              def f(node, key) {
                  node."@$key"
              }
              """
          )
        );
    }
}
