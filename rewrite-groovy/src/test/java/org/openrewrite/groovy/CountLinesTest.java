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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class CountLinesTest implements RewriteTest {

    @Test
    void countsLines() {
        rewriteRun(
          groovy(
            """
              package com.whatever
              
              import java.util.List
              
              // comments don't count
              class A {
                  
                  
                  List<String> foo() {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(CountLinesVisitor.countLines(cu)).isEqualTo(4))
          )
        );
    }
}

