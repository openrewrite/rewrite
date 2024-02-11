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
import org.openrewrite.Issue;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class ShortenFullyQualifiedTypeReferencesAdaptabilityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ShortenFullyQualifiedTypeReferences());
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3736")
    void keepHeader() {
        rewriteRun(
          groovy(
            """
              /*
               * header comment will be removed from this groovy script, but not from similar java file
               */
              import java.util.regex.Pattern
              
              def pattern = Pattern.compile("pattern")
              def list = new java.util.ArrayList<String>(1)
              """,
            """
              /*
               * header comment will be removed from this groovy script, but not from similar java file
               */
              import java.util.ArrayList
              import java.util.regex.Pattern
              
              def pattern = Pattern.compile("pattern")
              def list = new ArrayList<String>(1)
              """
          )
        );
    }

    @Test
    void importWithLeadingComment() {
        rewriteRun(
          groovy(
            """
              package foo
              
              /* comment */
              import java.util.List
              
              class Test {
                  List<String> l = new java.util.ArrayList<>()
              }
              """,
            """
              package foo
              
              /* comment */
              import java.util.ArrayList
              import java.util.List
              
              class Test {
                  List<String> l = new ArrayList<>()
              }
              """
          )
        );
    }
}
