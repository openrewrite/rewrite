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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyEmptyStatementBody", "GroovyUnusedAssignment", "GrUnnecessarySemicolon", "GroovyUnnecessaryContinue"})
class ForLoopTest implements RewriteTest {

    @Test
    void forLoopMultipleInit() {
        rewriteRun(
          groovy(
            // IntelliJ's Groovy support is confused by multiple assignment in a for loop,
            // but Groovy itself does support this construct:
            //    https://groovy-lang.org/semantics.html#_enhanced_classic_java_style_for_loop
            """
              int i
              int j
              for(i = 0, j = 0;;) {
              }
              """
          )
        );
    }

    @Test
    void forLoopMultipleUpdate() {
        rewriteRun(
          groovy(
            """
             int i = 0
             int j = 10
             for(; i < j; i++ , j-- ) { }
              """
          )
        );
    }

    @Test
    void forLoop() {
        rewriteRun(
          groovy(
            """
              for(int i = 0; i < 10; i++) {
              }
              """
          )
        );
    }

    @Test
    void infiniteLoop() {
        rewriteRun(
          groovy(
            """
              for(;;) {
              }
              """
          )
        );
    }

    @Test
    void format() {
        rewriteRun(
          groovy(
            """
              for ( int i = 0 ; i < 10 ; i++ ) {
              }
              """
          )
        );
    }

    @Test
    void formatInfiniteLoop() {
        rewriteRun(
          groovy(
            """
              for ( ; ; ) {}
              """
          )
        );
    }

    @Test
    void formatLoopNoInit() {
        rewriteRun(
          groovy(
            """
              for ( ; i < 10 ; i++ ) {}
              """
          )
        );
    }

    @Test
    void formatLoopNoCondition() {
        rewriteRun(
          groovy(
            """
              int i = 0;
              for(; i < 10; i++) {}
              """
          )
        );
    }

    @Test
    void statementTerminatorForSingleLineForLoops() {
        rewriteRun(
          groovy(
            """
              for(;;) test()
              """
          )
        );
    }

    @Test
    void initializerIsAnAssignment() {
        rewriteRun(
          groovy(
            """
              def a = [1,2]
              int i=0
              for(i=0; i<a.length; i++) {}
              """
          )
        );
    }

    @Disabled
    @Test
    void multiVariableInitialization() {
        rewriteRun(
          groovy(
            """
              for(int i, j = 0;;) {}
              """
          )
        );
    }

    @Test
    void forEachWithColon() {
        rewriteRun(
          groovy(
            """
              for(int i : [1, 2, 3]) {}
              """
          )
        );
    }

    @Test
    void forIn() {
        rewriteRun(
          groovy(
            """
              def dependenciesType = ['implementation', 'testImplementation']
              for (type in dependenciesType) {
              }
              """
          )
        );
    }

    @Test
    void forEachWithIn() {
        rewriteRun(
          groovy(
            """
              for(int i in [1, 2, 3]) {}
              """
          )
        );
    }

    @Test
    void forWithContinue() {
        rewriteRun(
          groovy(
            """
              for(int i in [1, 2, 3]) { continue }
              """
          )
        );
    }

    @Test
    void forWithLabeledContinue() {
        rewriteRun(
          groovy(
            """
              f: for(int i in [1, 2, 3]) { continue f }
              """
          )
        );
    }
}
