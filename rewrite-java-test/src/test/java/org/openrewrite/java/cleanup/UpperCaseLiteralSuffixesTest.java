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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
public class UpperCaseLiteralSuffixesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpperCaseLiteralSuffixes());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2429")
    @Test
    void usesPrimitive() {
        rewriteRun(
          java(
            """
              class Test {
                  long lp = 1l;
                  double dp = 1d;
                  float df = 1f;
              }
              """,
            """
              class Test {
                  long lp = 1L;
                  double dp = 1D;
                  float df = 1F;
              }
              """
          )
        );
    }

    @Test
    void useUppercaseLiteralSuffix() {
        rewriteRun(
          java(
            """
              class Test {
                  long lp = 1l;
                  Long l = 100l;
                  Double d = 100.0d;
                  Float f = 100f;
                  Integer i = 0;
                  Long l2 = 0x100000000l;
                  String s = "hello";
              }
              """,
            """
              class Test {
                  long lp = 1L;
                  Long l = 100L;
                  Double d = 100.0D;
                  Float f = 100F;
                  Integer i = 0;
                  Long l2 = 0x100000000L;
                  String s = "hello";
              }
              """
          )
        );
    }
}
