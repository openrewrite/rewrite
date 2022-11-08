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
package org.openrewrite.groovy.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.cleanup.ExplicitInitialization;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings("GroovyUnusedAssignment")
class ExplicitInitializationVisitorGroovyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExplicitInitialization());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1272")
    @Test
    void gTypes() {
        rewriteRun(
          groovy("int a = 0")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1272")
    @Test
    void removeExplicitInitialization() {
        rewriteRun(
          groovy(
            """
              class Test {
                  private int a = 0
                  private long b = 0L
                  private short c = 0
                  private int d = 1
                  private long e = 2L
                  private int f

                  private boolean h = false
                  private boolean i = true

                  private Object j = new Object()
                  private Object k = null

                  int[] l = null
                  
                  private final Long n = null
              }
              """,
            """
              class Test {
                  private int a
                  private long b
                  private short c
                  private int d = 1
                  private long e = 2L
                  private int f
              
                  private boolean h
                  private boolean i = true
              
                  private Object j = new Object()
                  private Object k
              
                  int[] l
                  
                  private final Long n = null
              }
              """
          )
        );
    }
}
