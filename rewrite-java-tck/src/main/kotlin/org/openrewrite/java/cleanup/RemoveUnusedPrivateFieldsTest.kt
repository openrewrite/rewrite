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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface RemoveUnusedPrivateFieldsTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RemoveUnusedPrivateFields())
    }

    @Test
    fun doNotRemoveSerialVersionUid() = rewriteRun(
        java("""
            public class Test implements java.io.Serializable {
                private static final long serialVersionUID = 42L;
            }
        """)
    )

    @Test
    fun doNotRemoveAnnotatedField() = rewriteRun(
        java("""
            public class Test {
                @Deprecated
                public String annotated;
            }
        """)
    )

    @Test
    fun doNotChangeFieldsOnClassWithNativeMethod() = rewriteRun(
        java("""
            public class Test {
                public String notUsed;
                public native void method();
            }
        """)
    )

    @Test
    fun notPrivateField() = rewriteRun(
        java("""
            public class Test {
                public String notUsed;
            }
        """)
    )

    @Test
    fun fieldIsUsed() = rewriteRun(
        java("""
            public class Test {
                private String value;
                void method() {
                    String useValue = value;
                }
            }
        """)
    )

    @Test
    fun removeUnusedPrivateField() = rewriteRun(
        java(
            """
            public class Test {
                private String notUsed;
            }
            """,
            """
            public class Test {
            }
            """
        )
    )

    @Test
    fun nameIsShadowed() = rewriteRun(
        java(
            """
                public class Test {
                    private String value;
                    @SuppressWarnings({"UnnecessaryLocalVariable", "RedundantSuppression"})
                    void method() {
                        String value = "name shadow";
                        String shadowedUse = value;
                    }
                }
            """,
            """
                public class Test {
                    @SuppressWarnings({"UnnecessaryLocalVariable", "RedundantSuppression"})
                    void method() {
                        String value = "name shadow";
                        String shadowedUse = value;
                    }
                }
            """
        )
    )

    @Test
    fun onlyRemoveUnusedNamedVariable() = rewriteRun(
        java(
            """
                public class Test {
                    private String aOne, aTwo, aThree;
                    private String bOne, bTwo, bThree;
                    private String cOne, cTwo, cThree;
                    void method() {
                        String removeAOne = aTwo + aThree;
                        String removeBTwo = bOne + bThree;
                        String removeCThree = cOne + cTwo;
                    }
                }
            """,
            """
                public class Test {
                    private String aTwo, aThree;
                    private String bOne, bThree;
                    private String cOne, cTwo;
                    void method() {
                        String removeAOne = aTwo + aThree;
                        String removeBTwo = bOne + bThree;
                        String removeCThree = cOne + cTwo;
                    }
                }
            """
        )
    )
}

