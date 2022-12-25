/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class MaybeUsesImportTest implements RewriteTest {
    @Test
    void usesType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new
            MaybeUsesImport<>("java.util.Collections"))),
          java(
            """
              import java.io.File;
              import java.util.Collections;
              import java.util.Collections.Inner;
              import java.util.List;
              import static java.util.*;
              import static java.util.Collections.Inner.something;
              import static java.util.Collections.singleton;
              import static java.util.Collections.*;
              import java.util.Map;
                              
              class Test {
              }
              """,
            """
              import java.io.File;
              /*~~>*/import java.util.Collections;
              /*~~>*/import java.util.Collections.Inner;
              import java.util.List;
              /*~~>*/import static java.util.*;
              /*~~>*/import static java.util.Collections.Inner.something;
              /*~~>*/import static java.util.Collections.singleton;
              /*~~>*/import static java.util.Collections.*;
              import java.util.Map;
                              
              class Test {
              }
              """
          )
        );
    }

    /**
     * Type wildcards are greedy.
     */
    @Test
    void usesTypeWildcard() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new MaybeUsesImport<>("java.util.*"))),
          java(
            """
              import java.io.File;
              import java.util.Collections;
              import java.util.Collections.Inner;
              import java.util.List;
              import static java.util.*;
              import static java.util.Collections.Inner.something;
              import static java.util.Collections.singleton;
              import static java.util.Collections.*;
              import java.util.concurrent.ConcurrentHashMap;
                            
              class Test {
              }
              """,
            """
              import java.io.File;
              /*~~>*/import java.util.Collections;
              /*~~>*/import java.util.Collections.Inner;
              /*~~>*/import java.util.List;
              /*~~>*/import static java.util.*;
              /*~~>*/import static java.util.Collections.Inner.something;
              /*~~>*/import static java.util.Collections.singleton;
              /*~~>*/import static java.util.Collections.*;
              /*~~>*/import java.util.concurrent.ConcurrentHashMap;
                            
              class Test {
              }
              """
          )
        );
    }
}
