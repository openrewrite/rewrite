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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.EqualsAvoidsNullStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"ClassInitializerMayBeStatic", "StatementWithEmptyBody", "ConstantConditions"})
class EqualsAvoidsNullTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new EqualsAvoidsNull());
    }

    @Test
    void invertConditional() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      String s = null;
                      if(s.equals("test")) {}
                      if(s.equalsIgnoreCase("test")) {}
                  }
              }
              """,
            """
              public class A {
                  {
                      String s = null;
                      if("test".equals(s)) {}
                      if("test".equalsIgnoreCase(s)) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void ignoreEqualsIgnoreCase() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(randomId(), "test", "", "", emptySet(), singletonList(
              new EqualsAvoidsNullStyle(true)))))
          ),
          java(
            """
              public class A {
                  {
                      String s = null;
                      if(s.equals("test")) {}
                      if(s.equalsIgnoreCase("test")) {}
                  }
              }
              """,
            """
              public class A {
                  {
                      String s = null;
                      if("test".equals(s)) {}
                      if(s.equalsIgnoreCase("test")) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void removeUnnecessaryNullCheckAndParens() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      String s = null;
                      if((s != null && s.equals("test"))) {}
                      if(s != null && s.equals("test")) {}
                      if(null != s && s.equals("test")) {}
                  }
              }
              """,
            """
              public class A {
                  {
                      String s = null;
                      if("test".equals(s)) {}
                      if("test".equals(s)) {}
                      if("test".equals(s)) {}
                  }
              }
              """
          )
        );
    }
}
