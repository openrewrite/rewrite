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

public class FinalizePrivateFieldsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FinalizePrivateFields());
    }

    @Test
    void initValue() {
        rewriteRun(
          java(
            """
                class A {
                    private String name = "ABC";
                
                    String getName() {
                        return name;
                    }
                }
              """,
            """
                class A {
                    private final String name = "ABC";
                
                    String getName() {
                        return name;
                    }
                }
              """
          )
        );
    }

    @Test
    void initByMethod() {
        rewriteRun(
          java(
            """
                class A {
                    private String name = initName();
                
                    String initName() {
                        return "A name";
                    }
                }
            """,
            """
                class A {
                    private final String name = initName();
                
                    String initName() {
                        return "A name";
                    }
                }
            """
          )
        );
    }

    @Test
    void constructorInit() {
        rewriteRun(
          java(
            """
                class A {
                    private String name;
                
                    A() {
                        name = "XYZ";
                    }
                }
            """,
            """
                class A {
                    private final String name;
                
                    A() {
                        name = "XYZ";
                    }
                }
            """
          )
        );
    }

    @Test
    void fieldReassignedByAMethod() {
        rewriteRun(
          java(
            """
                class A {
                    private String name = "ABC";
                
                    void func() {
                        name = "XYZ";
                    }
                
                    String getName() {
                        return name;
                    }
                }
              """
          )
        );
    }

    @Test
    void fieldReassignedByConstructor() {
        rewriteRun(
          java(
            """
                class A {
                    private String name = "ABC";
                
                    A() {
                        name = "XYZ";
                    }
                }
            """
          )
        );
    }

    @Test
    void constructorInitTwiceOrMore() {
        rewriteRun(
          java(
            """
                class A {
                    private String name;
                
                    A() {
                        name = "ABC";
                        name = "XYZ";
                    }
                }
            """
          )
        );
    }

    @Test
    void ignoreNonPrivateFields() {
        rewriteRun(
          java(
            """
                class A {
                    int num = 10;
                }
            """
          )
        );
    }

}
