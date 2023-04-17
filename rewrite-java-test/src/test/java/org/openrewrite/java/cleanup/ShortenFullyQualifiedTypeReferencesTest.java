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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ShortenFullyQualifiedTypeReferencesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ShortenFullyQualifiedTypeReferences());
    }

    @Test
    void redundantImport() {
        rewriteRun(
          java(
            """
              import java.util.List;
                            
              class T {
                  java.util.List<String> list;
              }
              """,
            """
              import java.util.List;
                            
              class T {
                  List<String> list;
              }
              """
          )
        );
    }

    @Test
    void withinStaticFieldAccess() {
        rewriteRun(
          java(
            """
              class T {
                  int dotall = java.util.regex.Pattern.DOTALL;
              }
              """,
            """
              import java.util.regex.Pattern;
                            
              class T {
                  int dotall = Pattern.DOTALL;
              }
              """
          )
        );
    }

    @Test
    void inGenericTypeParameter() {
        rewriteRun(
          java(
            """
              import java.util.List;
                            
              class T {
                  List<java.util.List<String>> list;
              }
              """,
            """
              import java.util.List;
                            
              class T {
                  List<List<String>> list;
              }
              """
          )
        );
    }

    @Test
    void noImport() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<String> list;
              }
              """,
            """
              import java.util.List;
                                
              class T {
                  List<String> list;
              }
              """
          )
        );
    }

    @Test
    void multipleConflictingTypesWithoutImport() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<String> list;
                  java.awt.List list2;
              }
              """,
            """
              import java.util.List;
                                
              class T {
                  List<String> list;
                  java.awt.List list2;
              }
              """
          )
        );
    }

    @Test
    void conflictWithLocalType() {
        rewriteRun(
          java(
            """
              class T {
                  java.util.List<String> list;
                  class List {
                  }
              }
              """
          )
        );
    }

    @Test
    void conflictExistingImport() {
        rewriteRun(
          java(
            """
              import java.awt.List;
              class T {
                  java.util.List<String> list;
              }
              """
          )
        );
    }
}
