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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
public class UseListSortTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseListSort());
    }

    @Test
    void hasSelect() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.Comparator;
              import java.util.List;
              import java.util.Collections;
                          
              class T {
                  public void sortUsersById(List<String> names) {
                      Collections.sort(names);
                      Collections.sort(names, Comparator.naturalOrder());
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.Comparator;
              import java.util.List;
                          
              class T {
                  public void sortUsersById(List<String> names) {
                      names.sort(null);
                      names.sort(Comparator.naturalOrder());
                  }
              }
              """
          )
        );
    }

    @Test
    void staticImportNoSelect() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.Comparator;
              import java.util.List;
                          
              import static java.util.Collections.sort;
                          
              class T {
                  public void sortUsersById(List<String> names) {
                      sort(names);
                      sort(names, Comparator.naturalOrder());
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.Comparator;
              import java.util.List;
                          
              class T {
                  public void sortUsersById(List<String> names) {
                      names.sort(null);
                      names.sort(Comparator.naturalOrder());
                  }
              }
              """
          )
        );
    }
}
