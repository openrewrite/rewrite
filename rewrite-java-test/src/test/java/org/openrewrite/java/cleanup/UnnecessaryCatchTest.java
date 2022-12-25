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

@SuppressWarnings("RedundantThrows")
class UnnecessaryCatchTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UnnecessaryCatch());
    }

    @Test
    void unwrapTry() {
        rewriteRun(
          java(
            """
              import java.io.IOException;
                            
              public class AnExample {
                  public void method() {
                      try {
                          java.util.Base64.getDecoder().decode("abc".getBytes());
                      } catch (IOException e) {
                          System.out.println("an exception!");
                      }
                  }
              }
              """,
            """
              public class AnExample {
                  public void method() {
                      java.util.Base64.getDecoder().decode("abc".getBytes());
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCatch() {
        rewriteRun(
          java(
            """
                  import java.io.IOException;
                  
                  public class AnExample {
                      public void method() {
                          try {
                              java.util.Base64.getDecoder().decode("abc".getBytes());
                          } catch (IOException e1) {
                              System.out.println("an exception!");
                          } catch (IllegalStateException e2) {
                              System.out.println("another exception!");
                          }
                      }
                  }
              """,
            """
                  public class AnExample {
                      public void method() {
                          try {
                              java.util.Base64.getDecoder().decode("abc".getBytes());
                          } catch (IllegalStateException e2) {
                              System.out.println("another exception!");
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void doNotRemoveRuntimeException() {
        rewriteRun(
          java(
            """ 
              public class AnExample {
                  public void method() {
                      try {
                          java.util.Base64.getDecoder().decode("abc".getBytes());
                      } catch (IllegalStateException e) {
                          System.out.println("an exception!");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveThrownException() {
        rewriteRun(
          java(
            """
              import java.io.IOException;
              
              public class AnExample {
                  public void method() {
                      try {
                          fred();
                      } catch (IOException e) {
                          System.out.println("an exception!");
                      }
                  }
                  
                  public void fred() throws IOException {
                  }
              }
              """
          )
        );
    }
}
